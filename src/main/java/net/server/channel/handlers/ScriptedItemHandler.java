package net.server.channel.handlers;

import java.util.Optional;

import client.MapleClient;
import client.inventory.Item;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import scripting.item.ItemScriptManager;
import server.ItemInformationProvider;
import server.ScriptedItem;

public final class ScriptedItemHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      p.readInt();
      short itemSlot = p.readShort();
      int itemId = p.readInt();

      ItemInformationProvider ii = ItemInformationProvider.getInstance();
      Optional<ScriptedItem> info = ii.getScriptedItemInfo(itemId);
      if (info.isEmpty()) {
         return;
      }

      Item item = c.getPlayer().getInventory(ItemConstants.getInventoryType(itemId)).getItem(itemSlot);
      if (item == null || item.getItemId() != itemId || item.getQuantity() < 1) {
         return;
      }

      ItemScriptManager.getInstance().runItemScript(c, info.get());
   }
}
