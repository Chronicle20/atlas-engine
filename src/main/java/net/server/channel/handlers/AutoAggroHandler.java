package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class AutoAggroHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter player = c.getPlayer();
      if (player.isHidden()) {
         return; // Don't auto aggro GM's in hide...
      }

      int oid = p.readInt();
      player.getMap().getMonsterByOid(oid).ifPresent(m -> m.aggroAutoAggroUpdate(player));
   }
}
