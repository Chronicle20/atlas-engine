package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.autoban.AutobanFactory;
import client.command.CommandsExecutor;
import config.YamlConfig;
import connection.packets.CUser;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import tools.FilePrinter;
import tools.LogHelper;

public final class GeneralChatHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int updateTime = p.readInt();
      String message = p.readString();
      byte nType = p.readByte();

      MapleCharacter chr = c.getPlayer();
      if (chr.getAutobanManager().getLastSpam(7) + 200 > currentServerTime()) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }
      if (message.length() > Byte.MAX_VALUE && !chr.isGM()) {
         AutobanFactory.PACKET_EDIT.alert(c.getPlayer(), c.getPlayer().getName() + " tried to packet edit in General Chat.");
         FilePrinter.printError(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt",
               c.getPlayer().getName() + " tried to send text with length of " + message.length());
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