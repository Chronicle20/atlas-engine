package net.server.channel.handlers;

import client.MapleClient;
import client.inventory.Item;
import connection.packets.CCashShop;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.CashShop;
import tools.Pair;

public class CashShopSurpriseHandler extends AbstractMaplePacketHandler {
   @Override
   public final void handlePacket(InPacket p, MapleClient c) {
      CashShop cs = c.getPlayer().getCashShop();

      if (cs.isOpened()) {
         Pair<Item, Item> cssResult = cs.openCashShopSurprise();

         if (cssResult != null) {
            Item cssItem = cssResult.getLeft(), cssBox = cssResult.getRight();
            c.sendPacket(CCashShop.onCashGachaponOpenSuccess(c.getAccID(), cssBox.getSN(), cssBox.getQuantity(), cssItem,
                  cssItem.getItemId(), cssItem.getQuantity(), true));
         } else {
            c.sendPacket(CCashShop.onCashItemGachaponOpenFailed());
         }
      }
   }
}
