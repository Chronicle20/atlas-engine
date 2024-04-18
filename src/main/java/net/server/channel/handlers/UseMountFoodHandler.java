package net.server.channel.handlers;

import java.util.Optional;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleMount;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import connection.packets.CWvsContext;
import constants.game.ExpTable;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class UseMountFoodHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      p.skip(4);
      short pos = p.readShort();
      int itemid = p.readInt();

      MapleCharacter chr = c.getPlayer();
      Optional<MapleMount> mount = chr.getMount();
      MapleInventory useInv = chr.getInventory(MapleInventoryType.USE);

      if (c.tryacquireClient()) {
         try {
            Boolean mountLevelup = null;

            useInv.lockInventory();
            try {
               Item item = useInv.getItem(pos);
               if (item != null && item.getItemId() == itemid && mount.isPresent()) {
                  int curTiredness = mount.get().getTiredness();
                  int healedTiredness = Math.min(curTiredness, 30);

                  float healedFactor = (float) healedTiredness / 30;
                  mount.get().setTiredness(curTiredness - healedTiredness);

                  if (healedFactor > 0.0f) {
                     mount.get().setExp(mount.get().getExp() + (int) Math.ceil(healedFactor * (2 * mount.get().getLevel() + 6)));
                     int level = mount.get().getLevel();
                     boolean levelup = mount.get().getExp() >= ExpTable.getMountExpNeededForLevel(level) && level < 31;
                     if (levelup) {
                        mount.get().setLevel(level + 1);
                     }

                     mountLevelup = levelup;
                  }

                  MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemid, 1, true, false);
               }
            } finally {
               useInv.unlockInventory();
            }

            if (mountLevelup != null) {
               chr.getMap().broadcastMessage(CWvsContext.updateMount(chr.getId(), mount.get(), mountLevelup));
            }
         } finally {
            c.releaseClient();
         }
      }
   }
}