package net.server.channel.handlers;

import java.util.Optional;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MaplePet;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class PetExcludeItemsHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      final int petId = p.readInt();
      p.skip(4);

      MapleCharacter chr = c.getPlayer();
      byte petIndex = chr.getPetIndex(petId);
      if (petIndex < 0) {
         return;
      }

      final Optional<MaplePet> pet = chr.getPet(petIndex);
      if (pet.isEmpty()) {
         return;
      }

      chr.resetExcluded(petId);
      byte amount = p.readByte();
      for (int i = 0; i < amount; i++) {
         chr.addExcluded(petId, p.readInt());
      }
      chr.commitExcludedItems();
   }
}
