package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class CancelChairHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int id = p.readShort();
      MapleCharacter mc = c.getPlayer();

      if (id >= mc.getMap().getSeats()) {
         return;
      }

      if (c.tryacquireClient()) {
         try {
            mc.sitChair(id);
         } finally {
            c.releaseClient();
         }
      }
   }
}
