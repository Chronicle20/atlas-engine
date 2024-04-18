package net.server.channel.handlers;

import java.util.Optional;
import java.util.Set;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MaplePet;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;

public final class PetLootHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();

      int petIndex = chr.getPetIndex(p.readInt());
      Optional<MaplePet> pet = chr.getPet(petIndex);
      if (pet.isEmpty() || !pet.get().isSummoned()) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }

      p.skip(13);
      int oid = p.readInt();
      MapleMapObject ob = chr.getMap().getMapObject(oid).orElse(null);
      try {
         MapleMapItem mapitem = (MapleMapItem) ob;
         if (mapitem.getMeso() > 0) {
            //TODO ensure that the pet can loot meso.
            final Set<Integer> petIgnore = chr.getExcludedItems();
            if (!petIgnore.isEmpty() && petIgnore.contains(Integer.MAX_VALUE)) {
               c.sendPacket(CWvsContext.enableActions());
               return;
            }
         } else {
            //TODO ensure the pet can loot item.
            final Set<Integer> petIgnore = chr.getExcludedItems();
            if (!petIgnore.isEmpty() && petIgnore.contains(mapitem.getItem().getItemId())) {
               c.sendPacket(CWvsContext.enableActions());
               return;
            }
         }

         chr.pickupItem(ob, petIndex);
      } catch (NullPointerException | ClassCastException e) {
         c.sendPacket(CWvsContext.enableActions());
      }
   }
}
