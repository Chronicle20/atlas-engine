package tools.data.output;

import java.awt.*;
import java.io.UnsupportedEncodingException;

/**
 * Provides a generic writer of a little-endian sequence of bytes.
 *
 * @author Frz
 * @version 1.0
 * @since Revision 323
 */
public class GenericLittleEndianWriter implements LittleEndianWriter {
   private ByteOutputStream bos;

   /**
    * Class constructor - Protected to prevent instantiation with no arguments.
    */
   protected GenericLittleEndianWriter() {
      // Blah!
   }

   /**
    * Sets the byte-output stream for this instance of the object.
    *
    * @param bos The new output stream to set.
    */
   void setByteOutputStream(ByteOutputStream bos) {
      this.bos = bos;
   }

   /**
    * Write an array of bytes to the stream.
    *
    * @param b The bytes to write.
    */
   @Override
   public void write(byte[] b) {
      for (byte value : b) {
         bos.writeByte(value);
      }
   }

   /**
    * Write a byte to the stream.
    *
    * @param b The byte to write.
    */
   @Override
   public void write(byte b) {
      bos.writeByte(b);
   }

   /**
    * Write a byte in integer form to the stream.
    *
    * @param b The byte as an <code>Integer</code> to write.
    */
   @Override
   public void write(int b) {
      bos.writeByte((byte) b);
   }

   @Override
   public void skip(int b) {
      write(new byte[b]);
   }

   /**
    * Writes an integer to the stream.
    *
    * @param i The integer to write.
    */
   @Override
   public void writeInt(int i) {
      bos.writeByte((byte) (i & 0xFF));
      bos.writeByte((byte) ((i >>> 8) & 0xFF));
      bos.writeByte((byte) ((i >>> 16) & 0xFF));
      bos.writeByte((byte) ((i >>> 24) & 0xFF));
   }

   /**
    * Write a short integer to the stream.
    *
    * @param i The short integer to write.
    */
   @Override
   public void writeShort(int i) {
      bos.writeByte((byte) (i & 0xFF));
      bos.writeByte((byte) ((i >>> 8) & 0xFF));
   }

   /**
    * Write a long integer to the stream.
    *
    * @param l The long integer to write.
    */
   @Override
   public void writeLong(long l) {
      bos.writeByte((byte) (l & 0xFF));
      bos.writeByte((byte) ((l >>> 8) & 0xFF));
      bos.writeByte((byte) ((l >>> 16) & 0xFF));
      bos.writeByte((byte) ((l >>> 24) & 0xFF));
      bos.writeByte((byte) ((l >>> 32) & 0xFF));
      bos.writeByte((byte) ((l >>> 40) & 0xFF));
      bos.writeByte((byte) ((l >>> 48) & 0xFF));
      bos.writeByte((byte) ((l >>> 56) & 0xFF));
   }

   /**
    * Writes an ASCII string the the stream.
    *
    * @param s The ASCII string to write.
    */
   @Override
   public void writeAsciiString(String s) {
      byte[] bytes;
      try {
         bytes = s.getBytes("windows-31j");
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }
      write(bytes);
   }

   /**
    * Writes a null-terminated ASCII string to the stream.
    *
    * @param s The ASCII string to write.
    */
   @Override
   public void writeNullTerminatedAsciiString(String s) {
      writeAsciiString(s);
      write(0);
   }

   /**
    * Writes a null-terminated ASCII string to the stream.
    *
    * @param s The ASCII string to write.
    */
   @Override
   public void writeNullTerminatedAsciiString(String s, int size) {
      writeAsciiString(s);
      for (int i = s.length(); i < size; i++) {
         write(0);
      }
   }

   /**
    * Writes a maple-convention ASCII string to the stream.
    *
    * @param s The ASCII string to use maple-convention to write.
    */
   @Override
   public void writeMapleAsciiString(String s) {
      byte[] bytes;
      String conv_str;
      try {
         bytes = s.getBytes("windows-31j");
         conv_str = new String(bytes, "windows-31j");
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }

      writeShort((short) bytes.length);
      writeAsciiString(conv_str);
   }

   /**
    * Writes a 2D 4 byte position information
    *
    * @param s The Point position to write.
    */
   @Override
   public void writePos(Point s) {
      writeShort(s.x);
      writeShort(s.y);
   }

   /**
    * Writes a boolean true ? 1 : 0
    *
    * @param b The boolean to write.
    */
   @Override
   public void writeBool(final boolean b) {
      write(b ? 1 : 0);
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
