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
package client.processor.action;    // thanks Alex for pointing out some package structures containing broad modules

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CField;
import connection.packets.CUser;
import connection.packets.CWvsContext;
import server.maps.MapleMap;

/**
 * @author RonanLana
 */
public class BuybackProcessor {

    public static void processBuyback(MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        boolean buyback;

        c.lockClient();
        try {
            buyback = !chr.isAlive() && chr.couldBuyback();
        } finally {
            c.unlockClient();
        }

        if (buyback) {
            String jobString = switch (chr.getJobStyle()) {
               case WARRIOR -> "warrior";
               case MAGICIAN -> "magician";
               case BOWMAN -> "bowman";
               case THIEF -> "thief";
               case BRAWLER, GUNSLINGER -> "pirate";
               default -> "beginner";
            };

           chr.healHpMp();
            chr.purgeDebuffs();
            chr.broadcastStance(chr.isFacingLeft() ? 5 : 4);

            MapleMap map = chr.getMap();
            map.broadcastMessage(CField.playSound("Buyback/" + jobString));
            map.broadcastMessage(CWvsContext.earnTitleMessage(chr.getName() + " just bought back into the game!"));

            chr.sendPacket(CUser.showBuybackEffect());
            map.broadcastMessage(chr, CUser.showForeignBuybackEffect(chr.getId()), false);
        }
    }
}
