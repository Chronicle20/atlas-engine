package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import scripting.event.EventInstanceManager;
import tools.packets.Wedding;

public final class WeddingTalkHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      byte action = p.readByte();
      if (action == 1) {
         EventInstanceManager eim = c.getPlayer().getEventInstance().orElse(null);

         if (eim != null && !(c.getPlayer().getId() == eim.getIntProperty("groomId") || c.getPlayer().getId() == eim.getIntProperty(
               "brideId"))) {
            c.sendPacket(Wedding.OnWeddingProgress(false, 0, 0, (byte) 2));
         } else {
            c.sendPacket(Wedding.OnWeddingProgress(true, 0, 0, (byte) 3));
         }
      } else {
         c.sendPacket(Wedding.OnWeddingProgress(true, 0, 0, (byte) 3));
      }

      c.sendPacket(CWvsContext.enableActions());
   }
}