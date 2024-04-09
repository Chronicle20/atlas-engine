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

import client.MapleCharacter;
import client.MapleClient;
import client.autoban.AutobanFactory;
import client.command.CommandsExecutor;
import config.YamlConfig;
import connection.packets.CUser;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import tools.FilePrinter;
import tools.LogHelper;
import tools.data.input.SeekableLittleEndianAccessor;

public final class GeneralChatHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int updateTime = slea.readInt();
        String message = slea.readMapleAsciiString();
        byte nType = slea.readByte();

        MapleCharacter chr = c.getPlayer();
        if (chr.getAutobanManager().getLastSpam(7) + 200 > currentServerTime()) {
            c.announce(CWvsContext.enableActions());
            return;
        }
        if (message.length() > Byte.MAX_VALUE && !chr.isGM()) {
            AutobanFactory.PACKET_EDIT.alert(c.getPlayer(), c.getPlayer().getName() + " tried to packet edit in General Chat.");
            FilePrinter.printError(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt", c.getPlayer().getName() + " tried to send text with length of " + message.length());
            c.disconnect(true, false);
            return;
        }
        char heading = message.charAt(0);
        if (CommandsExecutor.isCommand(c, message)) {
            CommandsExecutor.getInstance().handle(c, message);
        } else if (heading != '/') {
            if (chr.getMap().isMuted() && !chr.isGM()) {
                chr.dropMessage(5, "The map you are in is currently muted. Please try again later.");
                return;
            }

            if (!chr.isHidden()) {
                chr.getMap().broadcastMessage(CUser.getChatText(chr.getId(), message, chr.getWhiteChat(), nType));
                if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
                    LogHelper.logChat(c, "General", message);
                }
            } else {
                chr.getMap().broadcastGMMessage(CUser.getChatText(chr.getId(), message, chr.getWhiteChat(), nType));
                if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
                    LogHelper.logChat(c, "GM General", message);
                }
            }

            chr.getAutobanManager().spam(7);
        }
    }
}