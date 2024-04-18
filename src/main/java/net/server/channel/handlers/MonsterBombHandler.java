package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CMobPool;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.life.MapleMonster;

public final class MonsterBombHandler extends AbstractMaplePacketHandler {
   private static void handleBomb(MapleClient c, MapleMonster monster) {
      if (monster.getId() == 8500003 || monster.getId() == 8500004) {
         monster.getMap().broadcastMessage(CMobPool.killMonster(monster.getObjectId(), 4));
         c.getPlayer().getMap().removeMapObject(monster.getObjectId());
      }
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int oid = p.readInt();
      if (!c.getPlayer().isAlive()) {
         return;
      }

      c.getPlayer().getMap().getMonsterByOid(oid).ifPresent(m -> handleBomb(c, m));
   }
}
