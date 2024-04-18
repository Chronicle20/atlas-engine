/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package tools;

import java.io.ByteArrayOutputStream;
import java.util.HexFormat;

import constants.string.CharsetConstants;

public class HexTool {
   private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

   private static String toString(byte byteValue) {
      int tmp = byteValue << 8;
      char[] retstr = new char[]{HEX[(tmp >> 12) & 0x0F], HEX[(tmp >> 8) & 0x0F]};
      return String.valueOf(retstr);
   }

   public static String toString(byte[] bytes) {
      StringBuilder hexed = new StringBuilder();
      for (byte aByte : bytes) {
         hexed.append(toString(aByte));
         hexed.append(' ');
      }
      return hexed.substring(0, hexed.length() - 1);
   }

   public static String toCompressedString(byte[] bytes) {
      StringBuilder hexed = new StringBuilder();
      for (byte aByte : bytes) {
         hexed.append(toString(aByte));
      }
      return hexed.substring(0, hexed.length());
   }

   public static byte[] getByteArrayFromHexString(String hex) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      int nexti = 0;
      int nextb = 0;
      boolean highoc = true;
      outer:
      for (; ; ) {
         int number = -1;
         while (number == -1) {
            if (nexti == hex.length()) {
               break outer;
            }
            char chr = hex.charAt(nexti);
            if (chr >= '0' && chr <= '9') {
               number = chr - '0';
            } else if (chr >= 'a' && chr <= 'f') {
               number = chr - 'a' + 10;
            } else if (chr >= 'A' && chr <= 'F') {
               number = chr - 'A' + 10;
            } else {
               number = -1;
            }
            nexti++;
         }
         if (highoc) {
            nextb = number << 4;
            highoc = false;
         } else {
            nextb |= number;
            highoc = true;
            baos.write(nextb);
         }
      }
      return baos.toByteArray();
   }

   public static String toStringFromAscii(final byte[] bytes) {
      byte[] ret = new byte[bytes.length];
      for (int x = 0; x < bytes.length; x++) {
         if (bytes[x] < 32 && bytes[x] >= 0) {
            ret[x] = '.';
         } else {
            int chr = ((short) bytes[x]) & 0xFF;
            ret[x] = (byte) chr;
         }
      }
      String encode = CharsetConstants.MAPLE_TYPE.getAscii();
      try {
         return new String(ret, encode);
      } catch (Exception e) {
      }
      return "";
   }

   /**
    * Convert a byte array to its hex string representation (upper case).
    * Each byte value is converted to two hex characters delimited by a space.
    *
    * @param bytes Byte array to convert to a hex string.
    *              Example: {1, 16, 127, -1} is converted to "01 F0 7F FF"
    * @return The hex string
    */
   public static String toHexString(byte[] bytes) {
      return HexFormat.ofDelimiter(" ").withUpperCase().formatHex(bytes);
   }

   /**
    * Convert a hex string to its byte array representation. Two consecutive hex characters are converted to one byte.
    *
    * @param hexString Hex string to convert to bytes. May be lower or upper case, and hex character pairs may be
    *                  delimited by a space or not.
    *                  Example: "01 10 7F FF" is converted to {1, 16, 127, -1}.
    *                  The following hex strings are considered identical and are converted to the same byte array:
    *                  "01 10 7F FF", "01107FFF", "01 10 7f ff", "01107fff"
    * @return The byte array
    */
   public static byte[] toBytes(String hexString) {
      return HexFormat.of().parseHex(removeAllSpaces(hexString));
   }

   private static String removeAllSpaces(String input) {
      return input.replaceAll("\\s", "");
   }

}
