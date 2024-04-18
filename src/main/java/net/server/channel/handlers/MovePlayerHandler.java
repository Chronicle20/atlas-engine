package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CUserRemote;
import net.packet.InPacket;
import server.movement.Elem;
import server.movement.MovePath;

public final class MovePlayerHandler extends AbstractMovementPacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      p.readInt(); // dr0
      p.readInt(); //dr1
      p.readByte(); //field key
      p.readInt(); //dr2
      p.readInt(); //dr3
      p.readInt(); //crc
      p.readInt(); //dwKey
      p.readInt(); //crc32

      final MovePath res = new MovePath();
      res.decode(p);

      res.Movement().stream().filter(m -> m.getType() == 0).map(m -> m.getPosition((short) 0))
            .forEach(pos -> c.getPlayer().setPosition(pos));
      res.Movement().stream().map(Elem::getBMoveAction).forEach(ma -> c.getPlayer().setStance(ma));

      c.getPlayer().getMap().movePlayer(c.getPlayer(), c.getPlayer().getPosition());
      if (c.getPlayer().isHidden()) {
         c.getPlayer().getMap().broadcastGMMessage(c.getPlayer(), CUserRemote.movePlayer(c.getPlayer().getId(), res), false);
      } else {
         c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CUserRemote.movePlayer(c.getPlayer().getId(), res), false);
      }
   }
}
