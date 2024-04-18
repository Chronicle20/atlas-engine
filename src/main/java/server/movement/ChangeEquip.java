package server.movement;

import java.awt.*;

import net.packet.OutPacket;

public class ChangeEquip implements LifeMovementFragment {
   private int wui;

   public ChangeEquip(int wui) {
      this.wui = wui;
   }

   @Override
   public void serialize(OutPacket p) {
      p.writeByte(10);
      p.writeByte(wui);
   }

   @Override
   public Point getPosition() {
      return new Point(0, 0);
   }
}
