/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.server.channel.handlers;

import buddy.BuddyProcessor;
import client.MapleCharacter;
import client.MapleClient;
import client.autoban.AutobanFactory;
import config.YamlConfig;
import connection.packets.CField;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import net.server.guild.MapleGuild;
import net.server.world.World;
import tools.FilePrinter;
import tools.LogHelper;
import tools.data.input.SeekableLittleEndianAccessor;

public final class MultiChatHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
      MapleCharacter player = c.getPlayer();
      if (player.getAutobanManager().getLastSpam(7) + 200 > currentServerTime()) {
         return;
      }

      int type = slea.readByte(); // 0 for buddys, 1 for partys
      int numRecipients = slea.readByte();
      int[] recipients = new int[numRecipients];
      for (int i = 0; i < numRecipients; i++) {
         recipients[i] = slea.readInt();
      }
      String chattext = slea.readMapleAsciiString();
      if (chattext.length() > Byte.MAX_VALUE && !player.isGM()) {
         AutobanFactory.PACKET_EDIT.alert(c.getPlayer(), c.getPlayer().getName() + " tried to packet edit chats.");
         FilePrinter.printError(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt",
               c.getPlayer().getName() + " tried to send text with length of " + chattext.length());
         c.disconnect(true, false);
         return;
      }
      World world = c.getWorldServer();
      if (type == 0) {
         BuddyProcessor.getInstance()
               .buddyChat(c.getWorld(), player.getId(), recipients, CField.multiChat(player.getName(), chattext, 0));
         if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
            LogHelper.logChat(c, "Buddy", chattext);
         }
      } else if (type == 1 && player.getParty().isPresent()) {
         world.partyChat(player.getParty().get(), chattext, player.getName());
         if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
            LogHelper.logChat(c, "Party", chattext);
         }
      } else if (type == 2 && player.getGuildId() > 0) {
         Server.getInstance().guildChat(player.getGuildId(), player.getName(), player.getId(), chattext);
         if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
            LogHelper.logChat(c, "Guild", chattext);
         }
      } else if (type == 3 && player.getGuild().isPresent()) {
         int allianceId = player.getGuild().map(MapleGuild::getAllianceId).orElse(0);
         if (allianceId > 0) {
            Server.getInstance().allianceMessage(allianceId, CField.multiChat(player.getName(), chattext, 3), player.getId(), -1);
            if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
               LogHelper.logChat(c, "Ally", chattext);
            }
         }
      }
      player.getAutobanManager().spam(7);
   }
}
