package server.movement;

import java.awt.*;

import net.packet.OutPacket;

public class JumpDownMovement extends AbstractLifeMovement {
   private Point pixelsPerSecond;
   private int fh;
   private int originFh;

   public JumpDownMovement(int type, Point position, int duration, int newstate) {
      super(type, position, duration, newstate);
   }

   public Point getPixelsPerSecond() {
      return pixelsPerSecond;
   }

   public void setPixelsPerSecond(Point wobble) {
      this.pixelsPerSecond = wobble;
   }

   public int getFh() {
      return fh;
   }

   public void setFh(int fh) {
      this.fh = fh;
   }

   public int getOriginFh() {
      return originFh;
   }

   public void setOriginFh(int fh) {    // fh actually originFh, thanks Spoon for pointing this out
      this.originFh = fh;
   }

   @Override
   public void serialize(OutPacket p) {
      p.writeByte(getType());
      p.writeShort(getPosition().x);
      p.writeShort(getPosition().y);
      p.writeShort(pixelsPerSecond.x);
      p.writeShort(pixelsPerSecond.y);
      p.writeShort(fh);
      p.writeShort(originFh);
      p.writeByte(getNewstate());
      p.writeShort(getDuration());
   }
}
