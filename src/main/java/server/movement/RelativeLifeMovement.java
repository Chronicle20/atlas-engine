package server.movement;

import java.awt.*;

import net.packet.OutPacket;

public class RelativeLifeMovement extends AbstractLifeMovement {
   public RelativeLifeMovement(int type, Point position, int duration, int newstate) {
      super(type, position, duration, newstate);
   }

   @Override
   public void serialize(OutPacket p) {
      p.writeByte(getType());
      p.writeShort(getPosition().x);
      p.writeShort(getPosition().y);
      p.writeByte(getNewstate());
      p.writeShort(getDuration());
   }
}
