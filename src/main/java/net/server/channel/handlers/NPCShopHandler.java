package net.server.channel.handlers;

import client.MapleClient;
import client.autoban.AutobanFactory;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import tools.FilePrinter;

public final class NPCShopHandler extends AbstractMaplePacketHandler {
   public void handlePacket(InPacket p, MapleClient c) {
      byte bmode = p.readByte();
      if (bmode == 0) { // mode 0 = buy :)
         short slot = p.readShort();// slot
         int itemId = p.readInt();
         short quantity = p.readShort();
         if (quantity < 1) {
            AutobanFactory.PACKET_EDIT.alert(c.getPlayer(), c.getPlayer().getName() + " tried to packet edit a npc shop.");
            FilePrinter.printError(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt",
                  c.getPlayer().getName() + " tried to buy quantity " + quantity + " of item id " + itemId);
            c.disconnect(true, false);
            return;
         }
         c.getPlayer().getShop().buy(c, slot, itemId, quantity);
      } else if (bmode == 1) { // sell ;)
         short slot = p.readShort();
         int itemId = p.readInt();
         short quantity = p.readShort();
         c.getPlayer().getShop().sell(c, ItemConstants.getInventoryType(itemId), slot, quantity);
      } else if (bmode == 2) { // recharge ;)
         byte slot = (byte) p.readShort();
         c.getPlayer().getShop().recharge(c, slot);
      } else if (bmode == 3) {
         // TODO sub_7CAB93
      } else if (bmode == 4) { // leaving :(
         c.getPlayer().setShop(null);
      }
   }
}
