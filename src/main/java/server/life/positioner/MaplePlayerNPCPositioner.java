package server.life.positioner;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
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

public class MaplePlayerNPCPositioner {

   private static final Logger log = LoggerFactory.getLogger(MaplePlayerNPCPositioner.class);

   private static boolean isPlayerNpcNearby(List<Point> otherPos, Point searchPos, int xLimit, int yLimit) {
      int xLimit2 = xLimit / 2, yLimit2 = yLimit / 2;

      Rectangle searchRect = new Rectangle(searchPos.x - xLimit2, searchPos.y - yLimit2, xLimit, yLimit);
      for (Point pos : otherPos) {
         Rectangle otherRect = new Rectangle(pos.x - xLimit2, pos.y - yLimit2, xLimit, yLimit);

         if (otherRect.intersects(searchRect)) {
            return true;
         }
      }

      return false;
   }

   private static int calcDx(int newStep) {
      return YamlConfig.config.server.PLAYERNPC_AREA_X / (newStep + 1);
   }

   private static int calcDy(int newStep) {
      return (YamlConfig.config.server.PLAYERNPC_AREA_Y / 2) + (YamlConfig.config.server.PLAYERNPC_AREA_Y / (1 << (newStep + 1)));
   }

   private static List<Point> rearrangePlayerNpcPositions(MapleMap map, int newStep, int pnpcsSize) {
      Rectangle mapArea = map.getMapArea();

      int leftPx = mapArea.x + YamlConfig.config.server.PLAYERNPC_INITIAL_X, px, py =
            mapArea.y + YamlConfig.config.server.PLAYERNPC_INITIAL_Y;
      int outx = mapArea.x + mapArea.width - YamlConfig.config.server.PLAYERNPC_INITIAL_X, outy = mapArea.y + mapArea.height;
      int cx = calcDx(newStep), cy = calcDy(newStep);

      List<Point> otherPlayerNpcs = new LinkedList<>();
      while (py < outy) {
         px = leftPx;

         while (px < outx) {
            Point searchPos = map.getPointBelow(new Point(px, py));
            if (searchPos != null) {
               if (!isPlayerNpcNearby(otherPlayerNpcs, searchPos, cx, cy)) {
                  otherPlayerNpcs.add(searchPos);

                  if (otherPlayerNpcs.size() == pnpcsSize) {
                     return otherPlayerNpcs;
                  }
               }
            }

            px += cx;
         }

         py += cy;
      }

      return null;
   }

   private static Point rearrangePlayerNpcs(MapleMap map, int newStep, List<MaplePlayerNPC> pnpcs) {
      Rectangle mapArea = map.getMapArea();

      int leftPx = mapArea.x + YamlConfig.config.server.PLAYERNPC_INITIAL_X, px, py =
            mapArea.y + YamlConfig.config.server.PLAYERNPC_INITIAL_Y;
      int outx = mapArea.x + mapArea.width - YamlConfig.config.server.PLAYERNPC_INITIAL_X, outy = mapArea.y + mapArea.height;
      int cx = calcDx(newStep), cy = calcDy(newStep);

      List<Point> otherPlayerNpcs = new LinkedList<>();
      int i = 0;

      while (py < outy) {
         px = leftPx;

         while (px < outx) {
            Point searchPos = map.getPointBelow(new Point(px, py));
            if (searchPos != null) {
               if (!isPlayerNpcNearby(otherPlayerNpcs, searchPos, cx, cy)) {
                  if (i == pnpcs.size()) {
                     return searchPos;
                  }

                  MaplePlayerNPC pn = pnpcs.get(i);
                  i++;

                  pn.updatePlayerNPCPosition(map, searchPos);
                  otherPlayerNpcs.add(searchPos);
               }
            }

            px += cx;
         }

         py += cy;
      }

      return null;    // this area should not be reached under any scenario
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

   private static Point getNextPlayerNpcPosition(MapleMap map, int initStep) {   // automated playernpc position thanks to Ronan
      List<MapleMapObject> mmoList =
            map.getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, List.of(MapleMapObjectType.PLAYER_NPC));
      List<Point> otherPlayerNpcs = new LinkedList<>();
      for (MapleMapObject mmo : mmoList) {
         otherPlayerNpcs.add(mmo.getPosition());
      }

      int cx = calcDx(initStep), cy = calcDy(initStep);
      Rectangle mapArea = map.getMapArea();
      int outx = mapArea.x + mapArea.width - YamlConfig.config.server.PLAYERNPC_INITIAL_X, outy = mapArea.y + mapArea.height;
      boolean reorganize = false;

      int i = initStep;
      while (i < YamlConfig.config.server.PLAYERNPC_AREA_STEPS) {
         int leftPx = mapArea.x + YamlConfig.config.server.PLAYERNPC_INITIAL_X, px, py =
               mapArea.y + YamlConfig.config.server.PLAYERNPC_INITIAL_Y;

         while (py < outy) {
            px = leftPx;

            while (px < outx) {
               Point searchPos = map.getPointBelow(new Point(px, py));
               if (searchPos != null) {
                  if (!isPlayerNpcNearby(otherPlayerNpcs, searchPos, cx, cy)) {
                     if (i > initStep) {
                        map.getWorldServer().setPlayerNpcMapStep(map.getId(), i);
                     }

                     if (reorganize && YamlConfig.config.server.PLAYERNPC_ORGANIZE_AREA) {
                        return reorganizePlayerNpcs(map, i, mmoList);
                     }

                     return searchPos;
                  }
               }

               px += cx;
            }

            py += cy;
         }

         reorganize = true;
         i++;

         cx = calcDx(i);
         cy = calcDy(i);
         if (YamlConfig.config.server.PLAYERNPC_ORGANIZE_AREA) {
            otherPlayerNpcs = rearrangePlayerNpcPositions(map, i, mmoList.size());
         }
      }

      if (i > initStep) {
         map.getWorldServer().setPlayerNpcMapStep(map.getId(), YamlConfig.config.server.PLAYERNPC_AREA_STEPS - 1);
      }
      return null;
   }

   public static Point getNextPlayerNpcPosition(MapleMap map) {
      return getNextPlayerNpcPosition(map, map.getWorldServer().getPlayerNpcMapStep(map.getId()));
   }
}
