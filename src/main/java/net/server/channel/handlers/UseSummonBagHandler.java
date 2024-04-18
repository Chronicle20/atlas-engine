package net.server.channel.handlers;

import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.ItemInformationProvider;
import server.life.MapleLifeFactory;
import tools.Randomizer;

public final class UseSummonBagHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (!c.getPlayer().isAlive()) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }
      p.readInt();
      short slot = p.readShort();
      int itemId = p.readInt();
      Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
      if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemId) {
         MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
         int[][] toSpawn = ItemInformationProvider.getInstance().getSummonMobs(itemId);
         for (int[] toSpawnChild : toSpawn) {
            if (Randomizer.nextInt(100) < toSpawnChild[1]) {
               c.getPlayer().getMap()
                     .spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(toSpawnChild[0]), c.getPlayer().getPosition());
            }
         }
      }
      c.sendPacket(CWvsContext.enableActions());
   }
}
