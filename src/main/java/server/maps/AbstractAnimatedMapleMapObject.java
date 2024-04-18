package server.maps;

import java.util.Arrays;

import io.netty.buffer.Unpooled;
import net.packet.ByteBufInPacket;
import net.packet.ByteBufOutPacket;
import net.packet.InPacket;
import net.packet.OutPacket;
import net.packet.Packet;

public abstract class AbstractAnimatedMapleMapObject extends AbstractMapleMapObject implements AnimatedMapleMapObject {

   private static final Packet idleMovementPacketData = createIdleMovementPacket();

   private int stance;

   private static Packet createIdleMovementPacket() {
      OutPacket p = new ByteBufOutPacket();
      p.writeByte(1); //movement command count
      p.writeByte(0);
      p.writeShort(-1); //x
      p.writeShort(-1); //y
      p.writeShort(0); //xwobble
      p.writeShort(0); //ywobble
      p.writeShort(0); //fh
      p.writeByte(-1); //stance
      p.writeShort(0); //duration
      return p;
   }

   public static long getIdleMovementDataLength() {
      return 15;
   }

   @Override
   public int getStance() {
      return stance;
   }

   @Override
   public void setStance(int stance) {
      this.stance = stance;
   }

   @Override
   public boolean isFacingLeft() {
      return Math.abs(stance) % 2 == 1;
   }

   public InPacket getIdleMovement() {
      final byte[] idleMovementBytes = idleMovementPacketData.getBytes();
      byte[] movementData = Arrays.copyOf(idleMovementBytes, idleMovementBytes.length);
      //seems wasteful to create a whole packet writer when only a few values are changed
      int x = getPosition().x;
      int y = getPosition().y;
      movementData[2] = (byte) (x & 0xFF); //x
      movementData[3] = (byte) (x >> 8 & 0xFF);
      movementData[4] = (byte) (y & 0xFF); //y
      movementData[5] = (byte) (y >> 8 & 0xFF);
      movementData[12] = (byte) (getStance() & 0xFF);
      return new ByteBufInPacket(Unpooled.wrappedBuffer(movementData));
   }
}
