package net.packet;

import java.awt.*;
import java.io.UnsupportedEncodingException;

import connection.constants.SendOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class ByteBufOutPacket implements OutPacket {
   private final ByteBuf byteBuf;

   public ByteBufOutPacket() {
      this.byteBuf = Unpooled.buffer();
   }

   public ByteBufOutPacket(SendOpcode op) {
      ByteBuf byteBuf = Unpooled.buffer();
      byteBuf.writeShortLE((short) op.getValue());
      this.byteBuf = byteBuf;
   }

   public ByteBufOutPacket(SendOpcode op, int initialCapacity) {
      ByteBuf byteBuf = Unpooled.buffer(initialCapacity);
      byteBuf.writeShortLE((short) op.getValue());
      this.byteBuf = byteBuf;
   }

   @Override
   public byte[] getBytes() {
      return ByteBufUtil.getBytes(byteBuf);
   }

   @Override
   public void writeByte(byte value) {
      byteBuf.writeByte(value);
   }

   @Override
   public void writeByte(int value) {
      writeByte((byte) value);
   }

   @Override
   public void writeBytes(byte[] value) {
      byteBuf.writeBytes(value);
   }

   @Override
   public void writeShort(int value) {
      byteBuf.writeShortLE(value);
   }

   @Override
   public void writeInt(int value) {
      byteBuf.writeIntLE(value);
   }

   @Override
   public void writeLong(long value) {
      byteBuf.writeLongLE(value);
   }

   @Override
   public void writeBool(boolean value) {
      byteBuf.writeByte(value ? 1 : 0);
   }

   @Override
   public void writeString(String value) {
      byte[] bytes;
      try {
         bytes = value.getBytes("windows-31j");
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }
      writeShort(bytes.length);
      writeBytes(bytes);
   }

   @Override
   public void writeFixedString(String value) {
      try {
         writeBytes(value.getBytes("windows-31j"));
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public void writePos(Point value) {
      writeShort((short) value.getX());
      writeShort((short) value.getY());
   }

   @Override
   public void skip(int numberOfBytes) {
      writeBytes(new byte[numberOfBytes]);
   }

   @Override
   public void writeTime(long tTime) {
      long tCur = System.currentTimeMillis();

      boolean bInterval = false;
      if (tTime >= tCur) {
         tTime -= tCur;
      } else {
         bInterval = true;
         tTime = tCur - tTime;
      }
      tTime /= 1000;//I believe Nexon uses seconds here.

      writeBool(bInterval);
      writeInt((int) tTime);
   }
}
