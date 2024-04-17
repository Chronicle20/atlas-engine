/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

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

import client.TemporaryStatType;
import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CSummonedPool;
import net.AbstractMaplePacketHandler;
import server.maps.MapleMapObject;
import server.maps.MapleSummon;
import tools.data.input.SeekableLittleEndianAccessor;

public final class DamageSummonHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
      int oid = slea.readInt();
      slea.skip(1);   // -1
      int damage = slea.readInt();
      int monsterIdFrom = slea.readInt();

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
