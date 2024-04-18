package net.server.channel.handlers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import connection.packets.CClientSocket;
import connection.packets.CMapLoadable;
import connection.packets.CUser;
import connection.packets.CUserLocal;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.MapleTrade;
import server.maps.MapleMap;
import server.maps.MaplePortal;
import tools.FilePrinter;

public final class ChangeMapHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();

      if (chr.isChangingMaps() || chr.isBanned()) {
         if (chr.isChangingMaps()) {
            FilePrinter.printError(FilePrinter.PORTAL_STUCK + chr.getName() + ".txt",
                  "Player " + chr.getName() + " got stuck when changing maps. Timestamp: " + Calendar.getInstance().getTime()
                        + " Last visited mapids: " + chr.getLastVisitedMapids());
         }

         c.sendPacket(CWvsContext.enableActions());
         return;
      }
      if (chr.getTrade() != null) {
         MapleTrade.cancelTrade(chr, MapleTrade.TradeResult.UNSUCCESSFUL_ANOTHER_MAP);
      }
      if (p.available() == 0) { //Cash Shop :)
         if (!chr.getCashShop().isOpened()) {
            c.disconnect(false, false);
            return;
         }
         String[] socket = c.getChannelServer().getIP().split(":");
         chr.getCashShop().open(false);

         chr.setSessionTransitionState();
         try {
            c.sendPacket(CClientSocket.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
         } catch (UnknownHostException ex) {
            ex.printStackTrace();
         }
      } else {
         if (chr.getCashShop().isOpened()) {
            c.disconnect(false, false);
            return;
         }
         try {
            p.readByte(); // 1 = from dying 0 = regular portals
            int targetid = p.readInt();
            String startwp = p.readString();
            MaplePortal portal = chr.getMap().getPortal(startwp);
            p.readByte();
            boolean wheel = p.readByte() > 0;

            if (targetid != -1) {
               if (!chr.isAlive()) {
                  MapleMap map = chr.getMap();
                  if (wheel && chr.haveItemWithId(5510000, false)) {
                     // thanks lucasziron (lziron) for showing revivePlayer() triggering by Wheel

                     MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5510000, 1, true, false);
                     chr.sendPacket(CUser.showWheelsLeft(chr.getItemQuantity(5510000, false)));

                     chr.updateHp(50);
                     chr.changeMap(map, map.findClosestPlayerSpawnpoint(chr.getPosition()));
                  } else {
                     boolean executeStandardPath = true;
                     if (chr.getEventInstance().isPresent()) {
                        executeStandardPath = chr.getEventInstance().get().revivePlayer(chr);
                     }
                     if (executeStandardPath) {
                        chr.respawn(map.getReturnMapId());
                     }
                  }
               } else {
                  if (chr.isGM()) {
                     MapleMap to = chr.getWarpMap(targetid);
                     chr.changeMap(to, to.getPortal(0));
                  } else {
                     final int divi = chr.getMapId() / 100;
                     boolean warp = false;
                     if (divi == 0) {
                        if (targetid == 10000) {
                           warp = true;
                        }
                     } else if (divi == 20100) {
                        if (targetid == 104000000) {
                           c.sendPacket(CUserLocal.lockUI(false));
                           c.sendPacket(CUserLocal.disableUI(false));
                           warp = true;
                        }
                     } else if (divi == 9130401) { // Only allow warp if player is already in Intro map, or else = hack
                        if (targetid == 130000000 || targetid / 100 == 9130401) { // Cygnus introduction
                           warp = true;
                        }
                     } else if (divi == 9140900) { // Aran Introduction
                        if (targetid == 914090011 || targetid == 914090012 || targetid == 914090013 || targetid == 140090000) {
                           warp = true;
                        }
                     } else if (divi / 10 == 1020) { // Adventurer movie clip Intro
                        if (targetid == 1020000) {
                           warp = true;
                        }
                     } else if (divi / 10 >= 980040 && divi / 10 <= 980045) {
                        if (targetid == 980040000) {
                           warp = true;
                        }
                     }
                     if (warp) {
                        final MapleMap to = chr.getWarpMap(targetid);
                        chr.changeMap(to, to.getPortal(0));
                     }
                  }
               }
            }

            if (portal != null && !portal.getPortalStatus()) {
               c.sendPacket(CMapLoadable.blockedMessage(1));
               c.sendPacket(CWvsContext.enableActions());
               return;
            }

            if (chr.getMapId() == 109040004) {
               chr.getFitness().resetTimes();
            } else if (chr.getMapId() == 109030003 || chr.getMapId() == 109030103) {
               chr.getOla().resetTimes();
            }

            if (portal != null) {
               if (portal.getPosition().distanceSq(chr.getPosition()) > 400000) {
                  c.sendPacket(CWvsContext.enableActions());
                  return;
               }

               portal.enterPortal(c);
            } else {
               c.sendPacket(CWvsContext.enableActions());
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }
}