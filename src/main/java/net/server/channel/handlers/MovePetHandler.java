package net.server.channel.handlers;

import java.util.List;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CPet;
import net.packet.InPacket;
import server.movement.LifeMovementFragment;
import tools.exceptions.EmptyMovementException;

public final class MovePetHandler extends AbstractMovementPacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int petId = p.readInt();
      p.readLong();
      //        Point startPos = StreamUtil.readShortPoint(slea);
      List<LifeMovementFragment> res;

      try {
         res = parseMovement(p);
      } catch (EmptyMovementException e) {
         return;
      }
      MapleCharacter player = c.getPlayer();
      byte slot = player.getPetIndex(petId);
      if (slot == -1) {
         return;
      }
      player.getPet(slot).ifPresent(pet -> pet.updatePosition(res));
      player.getMap().broadcastMessage(player, CPet.movePet(player.getId(), petId, slot, res), false);
   }
}
