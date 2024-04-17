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
package server.life.positioner;

import config.YamlConfig;
import connection.packets.CNpcPool;
import net.server.Server;
import net.server.channel.Channel;
import server.life.MaplePlayerNPC;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author RonanLana
 * <p>
 * Note: the podium uses getGroundBelow that in its turn uses inputted posY minus 7.
 * Podium system will implement increase-by-7 to negate that behaviour.
 */
public class MaplePlayerNPCPodium {
    private static int getPlatformPosX(int platform) {
       return switch (platform) {
          case 0 -> -50;
          case 1 -> -170;
          default -> 70;
       };
    }

    private static int getPlatformPosY(int platform) {
       return switch (platform) {
          case 0 -> -47;
          default -> 40;
       };
    }

    private static Point calcNextPos(int rank, int step) {
        int podiumPlatform = rank / step;
        int relativePos = (rank % step) + 1;

        return new Point(getPlatformPosX(podiumPlatform) + ((100 * relativePos) / (step + 1)), getPlatformPosY(podiumPlatform));
    }

    private static Point rearrangePlayerNpcs(MapleMap map, int newStep, List<MaplePlayerNPC> pnpcs) {
        int i = 0;
        for (MaplePlayerNPC pn : pnpcs) {
            pn.updatePlayerNPCPosition(map, calcNextPos(i, newStep));
            i++;
        }

        return calcNextPos(i, newStep);
    }

    private static Point reorganizePlayerNpcs(MapleMap map, int newStep, List<MapleMapObject> mmoList) {
        if (!mmoList.isEmpty()) {
            if (YamlConfig.config.server.USE_DEBUG) {
                System.out.println("Reorganizing pnpc map, step " + newStep);
            }

            List<MaplePlayerNPC> playerNpcs = new ArrayList<>(mmoList.size());
            for (MapleMapObject mmo : mmoList) {
                playerNpcs.add((MaplePlayerNPC) mmo);
            }

            playerNpcs.sort(Comparator.comparingInt(MaplePlayerNPC::getScriptId));

            for (Channel ch : Server.getInstance().getChannelsFromWorld(map.getWorldId())) {
                MapleMap m = ch.getMapFactory().getMap(map.getId());

                for (MaplePlayerNPC pn : playerNpcs) {
                    m.removeMapObject(pn);
                    m.broadcastMessage(CNpcPool.removeNPCController(pn.getObjectId()));
                    m.broadcastMessage(CNpcPool.removePlayerNPC(pn.getObjectId()));
                }
            }

            Point ret = rearrangePlayerNpcs(map, newStep, playerNpcs);

            for (Channel ch : Server.getInstance().getChannelsFromWorld(map.getWorldId())) {
                MapleMap m = ch.getMapFactory().getMap(map.getId());

                for (MaplePlayerNPC pn : playerNpcs) {
                    m.addPlayerNPCMapObject(pn);
                    m.broadcastMessage(CNpcPool.spawnPlayerNPC(pn));
                    m.broadcastMessage(CNpcPool.getPlayerNPC(pn));
                }
            }

            return ret;
        }

        return null;
    }

    private static int encodePodiumData(int podiumStep, int podiumCount) {
        return (podiumCount * (1 << 5)) + podiumStep;
    }

    private static Point getNextPlayerNpcPosition(MapleMap map, int podiumData) {   // automated playernpc position thanks to Ronan
        int podiumStep = podiumData % (1 << 5), podiumCount = (podiumData / (1 << 5));

        if (podiumCount >= 3 * podiumStep) {
            if (podiumStep >= YamlConfig.config.server.PLAYERNPC_AREA_STEPS) {
                return null;
            }

            List<MapleMapObject> mmoList = map.getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, List.of(MapleMapObjectType.PLAYER_NPC));
            map.getWorldServer().setPlayerNpcMapPodiumData(map.getId(), encodePodiumData(podiumStep + 1, podiumCount + 1));
            return reorganizePlayerNpcs(map, podiumStep + 1, mmoList);
        } else {
            map.getWorldServer().setPlayerNpcMapPodiumData(map.getId(), encodePodiumData(podiumStep, podiumCount + 1));
            return calcNextPos(podiumCount, podiumStep);
        }
    }

    public static Point getNextPlayerNpcPosition(MapleMap map) {
        Point pos = getNextPlayerNpcPosition(map, map.getWorldServer().getPlayerNpcMapPodiumData(map.getId()));
        if (pos == null) {
            return null;
        }

        return map.getGroundBelow(pos);
    }
}
