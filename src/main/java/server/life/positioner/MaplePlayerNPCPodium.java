package server.life.positioner;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.YamlConfig;
import connection.packets.CNpcPool;
import net.server.Server;
import net.server.channel.Channel;
import server.life.MaplePlayerNPC;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;

public class MaplePlayerNPCPodium {

   private static final Logger log = LoggerFactory.getLogger(MaplePlayerNPCPodium.class);

   private static int getPlatformPosX(int platform) {
      return switch (platform) {
         case 0 -> -50;
         case 1 -> -170;
         default -> 70;
      };
   }

   private static int getPlatformPosY(int platform) {
      if (platform == 0) {
         return -47;
      }
      return 40;
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
            log.debug("Reorganizing pnpc map, step {}", newStep);
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

         List<MapleMapObject> mmoList =
               map.getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, List.of(MapleMapObjectType.PLAYER_NPC));
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
