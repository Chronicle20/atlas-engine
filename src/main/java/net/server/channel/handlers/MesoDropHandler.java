package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class MesoDropHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter player = c.getPlayer();
      if (!player.isAlive()) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }
      p.skip(4);
      int meso = p.readInt();

      if (c.tryacquireClient()) {     // thanks imbee for noticing players not being able to throw mesos too fast
         try {
            if (meso <= player.getMeso() && meso > 9 && meso < 50001) {
               player.gainMeso(-meso, false, true, false);
            } else {
               c.sendPacket(CWvsContext.enableActions());
               return;
            }
         } finally {
            c.releaseClient();
         }
      } else {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }

      if (player.attemptCatchFish(meso)) {
         player.getMap().disappearingMesoDrop(meso, player, player, player.getPosition());
      } else {
         player.getMap().spawnMesoDrop(meso, player.getPosition(), player, player, true, (byte) 2);
      }
   }
}