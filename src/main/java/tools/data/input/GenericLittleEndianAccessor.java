package tools.data.input;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Provides a generic interface to a Little Endian stream of bytes.
 *
 * @author Frz
 * @version 1.0
 * @since Revision 323
 */
public class GenericLittleEndianAccessor implements LittleEndianAccessor {
   private ByteInputStream bs;

   /**
    * Class constructor - Wraps the accessor around a stream of bytes.
    *
    * @param bs The byte stream to wrap the accessor around.
    */
   public GenericLittleEndianAccessor(ByteInputStream bs) {
      this.bs = bs;
   }

   /**
    * Read a single byte from the stream.
    *
    * @return The byte read.
    * @see tools.data.input.ByteInputStream#readByte
    */
   @Override
   public byte readByte() {
      return (byte) bs.readByte();
   }

   @Override
   public boolean readBool() {
      return bs.readByte() == 1;
   }

   /**
    * Reads a single character from the stream.
    *
    * @return The character read.
    */
   @Override
   public char readChar() {
      return (char) readShort();
   }

   /**
    * Reads a short integer from the stream.
    *
    * @return The short read.
    */
   @Override
   public short readShort() {
      return (short) (bs.readByte() + (bs.readByte() << 8));
   }

   /**
    * Reads an integer from the stream.
    *
    * @return The integer read.
    */
   @Override
   public int readInt() {
      return bs.readByte() + (bs.readByte() << 8) + (bs.readByte() << 16) + (bs.readByte() << 24);
   }

   /**
    * Reads a MapleStory Position information.
    * This consists of 2 short integer.
    *
    * @return The Position read.
    */
   @Override
   public final Point readPos() {
      final int x = readShort();
      final int y = readShort();
      return new Point(x, y);
   }

   /**
    * Reads a long integer from the stream.
    *
    * @return The long integer read.
    */
   @Override
   public long readLong() {
      long byte1 = bs.readByte();
      long byte2 = bs.readByte();
      long byte3 = bs.readByte();
      long byte4 = bs.readByte();
      long byte5 = bs.readByte();
      long byte6 = bs.readByte();
      long byte7 = bs.readByte();
      long byte8 = bs.readByte();
      return (byte8 << 56) + (byte7 << 48) + (byte6 << 40) + (byte5 << 32) + (byte4 << 24) + (byte3 << 16) + (byte2 << 8) + byte1;
   }

   /**
    * Skips the current position of the stream <code>num</code> bytes ahead.
    *
    * @param num Number of bytes to skip.
    */
   @Override
   public void skip(int num) {
      for (int x = 0; x < num; x++) {
         readByte();
      }
   }

   /**
    * Reads <code>num</code> bytes off the stream.
    *
    * @param num The number of bytes to read.
    * @return An array of bytes with the length of <code>num</code>
    */
   @Override
   public byte[] read(int num) {
      byte[] ret = new byte[num];
      for (int x = 0; x < num; x++) {
         ret[x] = readByte();
      }
      return ret;
   }

   /**
    * Reads a floating point integer from the stream.
    *
    * @return The float-type integer read.
    */
   @Override
   public float readFloat() {
      return Float.intBitsToFloat(readInt());
   }

   /**
    * Reads a double-precision integer from the stream.
    *
    * @return The double-type integer read.
    */
   @Override
   public double readDouble() {
      return Double.longBitsToDouble(readLong());
   }

   /**
    * Reads an ASCII string from the stream with length <code>n</code>.
    *
    * @param n Number of characters to read.
    * @return The string read.
    */
   public final String readAsciiString(int n) {
      byte[] ret = new byte[n];
      for (int x = 0; x < n; x++) {
         ret[x] = readByte();
      }
      try {
         return new String(ret, "windows-31j");
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Reads a null-terminated string from the stream.
    *
    * @return The string read.
    */
   public final String readNullTerminatedAsciiString() {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte b;
      while (true) {
         b = readByte();
         if (b == 0) {
            break;
         }
         baos.write(b);
      }
      byte[] buf = baos.toByteArray();
      char[] chrBuf = new char[buf.length];
      for (int x = 0; x < buf.length; x++) {
         chrBuf[x] = (char) buf[x];
      }
      return String.valueOf(chrBuf);
   }

   /**
    * Reads a MapleStory convention lengthed ASCII string.
    * This consists of a short integer telling the length of the string,
    * then the string itself.
    *
    * @return The string read.
    */
   @Override
   public String readMapleAsciiString() {
      return readAsciiString(readShort());
   }

   /**
    * Gets the number of bytes read from the stream so far.
    *
    * @return A long integer representing the number of bytes read.
    * @see tools.data.input.ByteInputStream#getBytesRead()
    */
   public long getBytesRead() {
      return bs.getBytesRead();
   }

   /**
    * @see tools.data.input.ByteInputStream#available
    */
   @Override
   public long available() {
      return bs.available();
   }

   /**
    * @see java.lang.Object#toString
    */
   @Override
   public String toString() {
      return bs.toString();
   }
}