package net.server.channel.handlers;

import java.awt.*;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CUser;
import net.packet.InPacket;
import server.maps.MapleDragon;
import tools.exceptions.EmptyMovementException;

public class MoveDragonHandler extends AbstractMovementPacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      final MapleCharacter chr = c.getPlayer();
      final Point startPos = new Point(p.readShort(), p.readShort());
      final MapleDragon dragon = chr.getDragon();
      if (dragon != null) {
         try {
            int movementDataStart = p.getPosition();
            updatePosition(p, dragon, 0);
            long movementDataLength = p.getPosition() - movementDataStart; //how many bytes were read by updatePosition
            p.seek(movementDataStart);

            if (chr.isHidden()) {
               chr.getMap().broadcastGMMessage(chr, CUser.moveDragon(dragon, startPos, p, movementDataLength));
            } else {
               chr.getMap().broadcastMessage(chr, CUser.moveDragon(dragon, startPos, p, movementDataLength), dragon.getPosition());
            }
         } catch (EmptyMovementException e) {
         }
      }
   }
}