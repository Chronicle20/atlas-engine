package net.server.channel.handlers;

import client.MapleClient;
import client.inventory.MapleInventoryType;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class UseChairHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int itemId = p.readInt();

      if (!ItemConstants.isChair(itemId) || c.getPlayer().getInventory(MapleInventoryType.SETUP).findById(itemId).isEmpty()) {
         return;
      }

      if (c.tryacquireClient()) {
         try {
            c.getPlayer().sitChair(itemId);
         } finally {
            c.releaseClient();
         }
      }
   }
}
