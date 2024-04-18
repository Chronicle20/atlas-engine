package server.movement;

import java.awt.*;

import net.packet.OutPacket;

public class TeleportMovement extends AbsoluteLifeMovement {

   public TeleportMovement(int type, Point position, int newstate) {
      super(type, position, 0, newstate);
   }

   @Override
   public void serialize(OutPacket p) {
      p.writeByte(getType());
      p.writeShort(getPosition().x);
      p.writeShort(getPosition().y);
      p.writeShort(getPixelsPerSecond().x);
      p.writeShort(getPixelsPerSecond().y);
      p.writeByte(getNewstate());
   }
}
