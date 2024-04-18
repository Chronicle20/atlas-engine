package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.TemporaryStatType;
import connection.packets.CSummonedPool;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.maps.MapleMapObject;
import server.maps.MapleSummon;

public final class DamageSummonHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int oid = p.readInt();
      p.skip(1);   // -1
      int damage = p.readInt();
      int monsterIdFrom = p.readInt();

      MapleCharacter player = c.getPlayer();
      MapleMapObject mmo = player.getMap().getMapObject(oid).orElse(null);

      if (mmo instanceof MapleSummon summon) {
         summon.addHP(-damage);
         if (summon.getHP() <= 0) {
            player.cancelEffectFromBuffStat(TemporaryStatType.PUPPET);
         }
         player.getMap().broadcastMessage(player, CSummonedPool.damageSummon(player.getId(), oid, damage, monsterIdFrom),
               summon.getPosition());
      }
   }
}
