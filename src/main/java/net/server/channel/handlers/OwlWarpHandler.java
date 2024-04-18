package net.server.channel.handlers;

import java.util.Optional;

import client.MapleClient;
import connection.packets.CMiniRoomBaseDlg;
import connection.packets.CWvsContext;
import constants.game.GameConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.maps.MapleHiredMerchant;
import server.maps.MaplePlayerShop;

public final class OwlWarpHandler extends AbstractMaplePacketHandler {

   private static void warpHiredMerchant(MapleClient client, int mapId, MapleHiredMerchant hiredMerchant) {
      if (!hiredMerchant.isOpen()) {
         //c.sendPacket(MaplePacketCreator.serverNotice(1, "That merchant has either been closed or is under maintenance."));
         client.sendPacket(CWvsContext.getOwlMessage(18));
         return;
      }

      if (!GameConstants.isFreeMarketRoom(mapId)) {
         client.sendPacket(CWvsContext.serverNotice(1,
               "That merchant is currently located outside of the FM area. Current location: Channel " + hiredMerchant.getChannel()
                     + ", '" + hiredMerchant.getMap().getMapName() + "'."));
         return;
      }

      if (hiredMerchant.getChannel() != client.getChannel()) {
         client.sendPacket(CWvsContext.serverNotice(1,
               "That merchant is currently located in another channel. Current location: Channel " + hiredMerchant.getChannel()
                     + ", '" + hiredMerchant.getMap().getMapName() + "'."));
         return;
      }

      client.getPlayer().changeMap(mapId);

      if (!hiredMerchant.isOpen()) {
         //c.sendPacket(MaplePacketCreator.serverNotice(1, "That merchant has either been closed or is under maintenance."));
         client.sendPacket(CWvsContext.getOwlMessage(18));
         return;
      }

      if (!hiredMerchant.addVisitor(client.getPlayer())) {
         //c.sendPacket(MaplePacketCreator.serverNotice(1, hm.getOwner() + "'s merchant is full. Wait awhile before trying again."));
         client.sendPacket(CWvsContext.getOwlMessage(2));
         return;
      }

      client.sendPacket(CMiniRoomBaseDlg.getHiredMerchant(client.getPlayer(), hiredMerchant, false));
      client.getPlayer().setHiredMerchant(hiredMerchant);
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int ownerId = p.readInt();
      int mapId = p.readInt();

      if (ownerId == c.getPlayer().getId()) {
         c.sendPacket(CWvsContext.serverNotice(1, "You cannot visit your own shop."));
         return;
      }

      Optional<MapleHiredMerchant> hm =
            c.getWorldServer().getHiredMerchant(ownerId);   // if both hired merchant and player shop is on the same map
      if (hm.isEmpty() || hm.get().getMapId() != mapId || !hm.get().hasItem(c.getPlayer().getOwlSearch())) {
         Optional<MaplePlayerShop> ps = c.getWorldServer().getPlayerShop(ownerId);

         if (ps.isEmpty() || ps.get().getMapId() != mapId || !ps.get().hasItem(c.getPlayer().getOwlSearch())) {
            if (hm.isEmpty() && ps.isEmpty()) {
               c.sendPacket(CWvsContext.getOwlMessage(1));
            } else {
               c.sendPacket(CWvsContext.getOwlMessage(3));
            }
            return;
         }

         if (!ps.get().isOpen()) {
            //c.sendPacket(MaplePacketCreator.serverNotice(1, "That merchant has either been closed or is under maintenance."));
            c.sendPacket(CWvsContext.getOwlMessage(18));
            return;
         }

         if (!GameConstants.isFreeMarketRoom(mapId)) {
            c.sendPacket(CWvsContext.serverNotice(1,
                  "That shop is currently located outside of the FM area. Current location: Channel " + ps.get().getChannel()
                        + ", '" + c.getPlayer().getMap().getMapName() + "'."));
            return;
         }

         if (ps.get().getChannel() != c.getChannel()) {
            c.sendPacket(CWvsContext.serverNotice(1,
                  "That shop is currently located in another channel. Current location: Channel " + ps.get().getChannel() + ", '"
                        + c.getPlayer().getMap().getMapName() + "'."));
            return;
         }

         c.getPlayer().changeMap(mapId);

         if (!ps.get().isOpen()) {
            //c.sendPacket(MaplePacketCreator.serverNotice(1, "That merchant has either been closed or is under maintenance."));
            c.sendPacket(CWvsContext.getOwlMessage(18));
            return;
         }

         if (ps.get().visitShop(c.getPlayer())) {
            return;
         }

         if (ps.get().isBanned(c.getPlayer().getName())) {
            c.sendPacket(CWvsContext.getOwlMessage(17));
            return;
         }

         c.sendPacket(CWvsContext.getOwlMessage(2));
      } else {
         warpHiredMerchant(c, mapId, hm.get());
      }
   }
}