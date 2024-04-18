package net.server.channel.handlers;

import java.awt.*;
import java.util.Collection;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CSummonedPool;
import net.packet.InPacket;
import server.maps.MapleSummon;
import tools.exceptions.EmptyMovementException;

public final class MoveSummonHandler extends AbstractMovementPacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int oid = p.readInt();
      Point startPos = new Point(p.readShort(), p.readShort());
      MapleCharacter player = c.getPlayer();
      Collection<MapleSummon> summons = player.getSummonsValues();
      MapleSummon summon = null;
      for (MapleSummon sum : summons) {
         if (sum.getObjectId() == oid) {
            summon = sum;
            break;
         }
      }
      if (summon != null) {
         try {
            int movementDataStart = p.getPosition();
            updatePosition(p, summon, 0);
            long movementDataLength = p.getPosition() - movementDataStart; //how many bytes were read by updatePosition
            p.seek(movementDataStart);

            player.getMap().broadcastMessage(player, CSummonedPool.moveSummon(player.getId(), oid, startPos, p, movementDataLength),
                  summon.getPosition());
         } catch (EmptyMovementException e) {
         }
      }
   }
}
