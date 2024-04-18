package net.server.channel.handlers;

import java.util.Optional;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import config.YamlConfig;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import server.ItemInformationProvider;

public final class InventoryMergeHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
      p.readInt();
      chr.getAutobanManager().setTimestamp(2, Server.getInstance().getCurrentTimestamp(), 4);

      if (!YamlConfig.config.server.USE_ITEM_SORT) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }

      byte invType = p.readByte();
      if (invType < 1 || invType > 5) {
         c.disconnect(false, false);
         return;
      }

      Optional<MapleInventoryType> inventoryType = MapleInventoryType.getByType(invType);
      if (inventoryType.isEmpty()) {
         return;
      }

      MapleInventory inventory = c.getPlayer().getInventory(inventoryType.get());
      inventory.lockInventory();
      try {
         //------------------- RonanLana's SLOT MERGER -----------------

         ItemInformationProvider ii = ItemInformationProvider.getInstance();
         Item srcItem, dstItem;

         for (short dst = 1; dst <= inventory.getSlotLimit(); dst++) {
            dstItem = inventory.getItem(dst);
            if (dstItem == null) {
               continue;
            }

            for (short src = (short) (dst + 1); src <= inventory.getSlotLimit(); src++) {
               srcItem = inventory.getItem(src);
               if (srcItem == null) {
                  continue;
               }

               if (dstItem.getItemId() != srcItem.getItemId()) {
                  continue;
               }
               if (dstItem.getQuantity() == ii.getSlotMax(c, inventory.getItem(dst).getItemId())) {
                  break;
               }

               MapleInventoryManipulator.move(c, inventoryType.get(), src, dst);
            }
         }

         //------------------------------------------------------------

         inventory = c.getPlayer().getInventory(inventoryType.get());
         boolean sorted = false;

         while (!sorted) {
            short freeSlot = inventory.getNextFreeSlot();

            if (freeSlot != -1) {
               short itemSlot = -1;
               for (short i = (short) (freeSlot + 1); i <= inventory.getSlotLimit(); i = (short) (i + 1)) {
                  if (inventory.getItem(i) != null) {
                     itemSlot = i;
                     break;
                  }
               }
               if (itemSlot > 0) {
                  MapleInventoryManipulator.move(c, inventoryType.get(), itemSlot, freeSlot);
               } else {
                  sorted = true;
               }
            } else {
               sorted = true;
            }
         }
      } finally {
         inventory.unlockInventory();
      }

      c.sendPacket(CWvsContext.finishedSort(inventoryType.get().getType()));
      c.sendPacket(CWvsContext.enableActions());
   }
}