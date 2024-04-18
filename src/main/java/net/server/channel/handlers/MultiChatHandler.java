package net.server.channel.handlers;

import buddy.BuddyProcessor;
import client.MapleCharacter;
import client.MapleClient;
import client.autoban.AutobanFactory;
import config.YamlConfig;
import connection.packets.CField;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.guild.MapleGuild;
import net.server.world.World;
import tools.FilePrinter;
import tools.LogHelper;

public final class MultiChatHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter player = c.getPlayer();
      if (player.getAutobanManager().getLastSpam(7) + 200 > currentServerTime()) {
         return;
      }

      int type = p.readByte(); // 0 for buddys, 1 for partys
      int numRecipients = p.readByte();
      int[] recipients = new int[numRecipients];
      for (int i = 0; i < numRecipients; i++) {
         recipients[i] = p.readInt();
      }
      String chattext = p.readString();
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
