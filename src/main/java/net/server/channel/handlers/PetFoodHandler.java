package net.server.channel.handlers;

import java.util.Optional;

import client.MapleCharacter;
import client.MapleClient;
import client.autoban.AutobanManager;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.manipulator.MapleInventoryManipulator;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;

public final class PetFoodHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
      AutobanManager abm = chr.getAutobanManager();
      if (abm.getLastSpam(2) + 500 > currentServerTime()) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }
      abm.spam(2);
      p.readInt(); // timestamp issue detected thanks to Masterrulax
      abm.setTimestamp(1, Server.getInstance().getCurrentTimestamp(), 3);
      if (chr.getNoPets() == 0) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }
      int previousFullness = 100;
      byte slot = 0;
      MaplePet[] pets = chr.getPets();
      for (byte i = 0; i < 3; i++) {
         if (pets[i] != null) {
            if (pets[i].getFullness() < previousFullness) {
               slot = i;
               previousFullness = pets[i].getFullness();
            }
         }
      }

      Optional<MaplePet> pet = chr.getPet(slot);
      if (pet.isEmpty()) {
         return;
      }

      short pos = p.readShort();
      int itemId = p.readInt();

      if (c.tryacquireClient()) {
         try {
            MapleInventory useInv = chr.getInventory(MapleInventoryType.USE);
            useInv.lockInventory();
            try {
               Item use = useInv.getItem(pos);
               if (use == null || (itemId / 10000) != 212 || use.getItemId() != itemId || use.getQuantity() < 1) {
                  return;
               }

               pet.get().gainClosenessFullness(chr, (pet.get().getFullness() <= 75) ? 1 : 0, 30,
                     1);   // 25+ "emptyness" to get +1 closeness
               MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, pos, (short) 1, false);
            } finally {
               useInv.unlockInventory();
            }
         } finally {
            c.releaseClient();
         }
      }
   }
}
