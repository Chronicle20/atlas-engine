package net.server.channel.handlers;

import java.util.Optional;

import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import connection.packets.CUserRemote;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class UseItemEffectHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      Optional<Item> toUse;
      int itemId = p.readInt();
      if (itemId == 4290001 || itemId == 4290000) {
         toUse = c.getPlayer().getInventory(MapleInventoryType.ETC).findById(itemId);
      } else {
         toUse = c.getPlayer().getInventory(MapleInventoryType.CASH).findById(itemId);
      }
      if (toUse.isEmpty() || toUse.get().getQuantity() < 1) {
         if (itemId != 0) {
            return;
         }
      }
      c.getPlayer().setItemEffect(itemId);
      c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CUserRemote.itemEffect(c.getPlayer().getId(), itemId), false);
   }
}
