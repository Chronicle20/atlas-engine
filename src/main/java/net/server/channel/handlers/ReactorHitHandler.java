package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class ReactorHitHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int oid = p.readInt();
      int charPos = p.readInt();

      int dwHitOption = p.readInt();// bMoveAction & 1 | 2 * (m_pfh != 0), if on ground, left/right
      short bMoveAction = (short) (dwHitOption & 1);
      short m_pfh = (short) ((dwHitOption >> 1) & 1);

      p.readShort(); // tDelay
      int skillId = p.readInt();
      c.getPlayer().getMap().getReactorByOid(oid).ifPresent(r -> r.hitReactor(true, charPos, bMoveAction, skillId, c));
   }
}
