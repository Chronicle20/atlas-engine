package net.server.channel.handlers;

import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.ItemFactory;
import connection.packets.CMiniRoomBaseDlg;
import connection.packets.CWvsContext;
import constants.game.GameConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MaplePlayerShop;
import server.maps.MaplePortal;

public final class HiredMerchantRequest extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();

      try {
         for (MapleMapObject mmo : chr.getMap().getMapObjectsInRange(chr.getPosition(), 23000,
               Arrays.asList(MapleMapObjectType.HIRED_MERCHANT, MapleMapObjectType.PLAYER))) {
            if (mmo instanceof MapleCharacter mc) {
               MaplePlayerShop shop = mc.getPlayerShop();
               if (shop != null && shop.isOwner(mc)) {
                  chr.sendPacket(CMiniRoomBaseDlg.getMiniRoomError(13));
                  return;
               }
            } else {
               chr.sendPacket(CMiniRoomBaseDlg.getMiniRoomError(13));
               return;
            }
         }

         Point cpos = chr.getPosition();
         MaplePortal portal = chr.getMap().findClosestTeleportPortal(cpos);
         if (portal != null && portal.getPosition().distance(cpos) < 120.0) {
            chr.sendPacket(CMiniRoomBaseDlg.getMiniRoomError(10));
            return;
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      if (GameConstants.isFreeMarketRoom(chr.getMapId())) {
         if (!chr.hasMerchant()) {
            try {
               if (ItemFactory.MERCHANT.loadItems(chr.getId(), false).isEmpty() && chr.getMerchantMeso() == 0) {
                  c.sendPacket(CWvsContext.hiredMerchantBox());
               } else {
                  chr.sendPacket(CWvsContext.retrieveFirstMessage());
               }
            } catch (SQLException ex) {
               ex.printStackTrace();
            }
         } else {
            chr.dropMessage(1, "You already have a store open.");
         }
      } else {
         chr.dropMessage(1, "You cannot open your hired merchant here.");
      }
   }
}
