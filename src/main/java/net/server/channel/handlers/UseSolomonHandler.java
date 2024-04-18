package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.ItemInformationProvider;

public final class UseSolomonHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      p.readInt();
      short slot = p.readShort();
      int itemId = p.readInt();
      ItemInformationProvider ii = ItemInformationProvider.getInstance();

      if (c.tryacquireClient()) {
         try {
            MapleCharacter chr = c.getPlayer();
            MapleInventory inv = chr.getInventory(MapleInventoryType.USE);
            inv.lockInventory();
            try {
               Item slotItem = inv.getItem(slot);
               if (slotItem == null) {
                  return;
               }

               long gachaexp = ii.getExpById(itemId);
               if (slotItem.getItemId() != itemId || slotItem.getQuantity() <= 0 || chr.getLevel() > ii.getMaxLevelById(itemId)) {
                  return;
               }
               if (gachaexp + chr.getGachaExp() > Integer.MAX_VALUE) {
                  return;
               }
               chr.addGachaExp((int) gachaexp);
               MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            } finally {
               inv.unlockInventory();
            }
         } finally {
            c.releaseClient();
         }
      }

      c.sendPacket(CWvsContext.enableActions());
   }
}
