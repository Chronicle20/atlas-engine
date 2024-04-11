/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.server.channel.handlers;

import java.util.Collections;
import java.util.List;

import client.TemporaryStatValue;
import client.TemporaryStatType;
import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CMobPool;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import server.life.MapleMonster;
import server.maps.MapleMapObject;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Ronan
 */
public final class PlayerMapTransitionHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
      chr.setMapTransitionComplete();

      int beaconid = chr.getBuffSource(TemporaryStatType.HOMING_BEACON);
      if (beaconid != -1) {
         chr.cancelBuffStats(TemporaryStatType.HOMING_BEACON);

         final List<Pair<TemporaryStatType, TemporaryStatValue>> stat = Collections.singletonList(new Pair<>(TemporaryStatType.HOMING_BEACON,
               TemporaryStatValue.empty()));
         chr.announce(CWvsContext.giveBuff(chr, 1, beaconid, stat));
      }

      if (!chr.isHidden()) {  // thanks Lame (Conrad) for noticing hidden characters controlling mobs
         for (MapleMapObject mo : chr.getMap()
               .getMonsters()) {    // thanks BHB, IxianMace, Jefe for noticing several issues regarding mob statuses (such as freeze)
            MapleMonster m = (MapleMonster) mo;
            if (m.getSpawnEffect() == 0 || m.getHp() < m.getMaxHp()) {     // avoid effect-spawning mobs
               if (m.getController().filter(controller -> controller == chr).isPresent()) {
                  c.announce(CMobPool.stopControllingMonster(m.getObjectId()));
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