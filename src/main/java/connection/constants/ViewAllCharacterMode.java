package connection.constants;

import java.util.Arrays;
import java.util.Optional;

public enum ViewAllCharacterMode {
   NORMAL(0),
   CHARACTER_COUNT(1),
   ERROR_VIEW_ALL_CHARACTERS(2),
   SEARCH_FAILED(3),
   SEARCH_FAILED_2(4),
   ERROR_VIEW_ALL_CHARACTERS_2(5);


   final byte mode;

   ViewAllCharacterMode(int code) {
      this.mode = (byte) code;
   }

   public static Optional<ViewAllCharacterMode> from(byte b) {
      return Arrays.stream(ViewAllCharacterMode.values()).filter(m -> m.getMode() == b).findFirst();
   }

   public byte getMode() {
      return mode;
   }
}
