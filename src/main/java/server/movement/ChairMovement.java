package server.movement;

import java.awt.*;

import net.packet.OutPacket;

public class ChairMovement extends AbstractLifeMovement {
   private int fh;

   public ChairMovement(int type, Point position, int duration, int newstate) {
      super(type, position, duration, newstate);
   }

   public int getFh() {
      return fh;
   }

   public void setFh(int fh) {
      this.fh = fh;
   }

   @Override
   public void serialize(OutPacket p) {
      p.writeByte(getType());
      p.writeShort(getPosition().x);
      p.writeShort(getPosition().y);
      p.writeShort(fh);
      p.writeByte(getNewstate());
      p.writeShort(getDuration());
   }
}

