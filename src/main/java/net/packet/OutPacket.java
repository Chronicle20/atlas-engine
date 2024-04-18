package net.packet;

import java.awt.*;

import connection.constants.SendOpcode;

public interface OutPacket extends Packet {
   static OutPacket create(SendOpcode opcode) {
      return new ByteBufOutPacket(opcode);
   }

   void writeByte(byte value);

   void writeByte(int value);

   void writeBytes(byte[] value);

   void writeShort(int value);

   void writeInt(int value);

   void writeLong(long value);

   void writeBool(boolean value);

   void writeString(String value);

   void writeFixedString(String value);

   void writePos(Point value);

   void skip(int numberOfBytes);

   void writeTime(long time);
}
