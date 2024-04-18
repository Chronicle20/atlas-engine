package net.server.channel.handlers;

import java.util.Collections;
import java.util.List;

import client.MapleCharacter;
import client.MapleClient;
import client.TemporaryStatType;
import client.TemporaryStatValue;
import connection.packets.CMobPool;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.life.MapleMonster;
import server.maps.MapleMapObject;
import tools.Pair;

public final class PlayerMapTransitionHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
      chr.setMapTransitionComplete();

      int beaconid = chr.getBuffSource(TemporaryStatType.HOMING_BEACON);
      if (beaconid != -1) {
         chr.cancelBuffStats(TemporaryStatType.HOMING_BEACON);

         final List<Pair<TemporaryStatType, TemporaryStatValue>> stat =
               Collections.singletonList(new Pair<>(TemporaryStatType.HOMING_BEACON, TemporaryStatValue.empty()));
         chr.sendPacket(CWvsContext.giveBuff(chr, 1, beaconid, stat));
      }

      if (!chr.isHidden()) {  // thanks Lame (Conrad) for noticing hidden characters controlling mobs
         for (MapleMapObject mo : chr.getMap()
               .getMonsters()) {    // thanks BHB, IxianMace, Jefe for noticing several issues regarding mob statuses (such as freeze)
            MapleMonster m = (MapleMonster) mo;
            if (m.getSpawnEffect() == 0 || m.getHp() < m.getMaxHp()) {     // avoid effect-spawning mobs
               if (m.getController().filter(controller -> controller == chr).isPresent()) {
                  c.sendPacket(CMobPool.stopControllingMonster(m.getObjectId()));
                  m.sendDestroyData(c);
                  m.aggroRemoveController();
               } else {
                  m.sendDestroyData(c);
               }

               m.sendSpawnData(c);
               m.aggroSwitchController(chr, false);
            }
         }
      }
   }
}