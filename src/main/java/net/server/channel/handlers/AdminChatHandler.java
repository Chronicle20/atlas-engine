package net.server.channel.handlers;

import client.MapleClient;
import config.YamlConfig;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.packet.Packet;
import tools.LogHelper;

public class AdminChatHandler extends AbstractMaplePacketHandler {

   @Override
   public final void handlePacket(InPacket p, MapleClient c) {
      if (!c.getPlayer().isGM()) {
         return;
      }
      byte mode = p.readByte();
      String message = p.readString();
      Packet packet = CWvsContext.serverNotice(p.readByte(), message);

      switch (mode) {
         case 0:// /alertall, /noticeall, /slideall
            c.getWorldServer().broadcastPacket(packet);
            if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
               LogHelper.logChat(c, "Alert All", message);
            }
            break;
         case 1:// /alertch, /noticech, /slidech
            c.getChannelServer().broadcastPacket(packet);
            if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
               LogHelper.logChat(c, "Alert Ch", message);
            }
            break;
         case 2:// /alertm /alertmap, /noticem /noticemap, /slidem /slidemap
            c.getPlayer().getMap().broadcastMessage(packet);
            if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
               LogHelper.logChat(c, "Alert Map", message);
            }
            break;
      }
   }
}
