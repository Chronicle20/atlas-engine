package connection.constants;

public enum BuddylistMode {
   RESET(0x7),
   UPDATE_FRIEND(0x8),
   REQUEST_BUDDY_ADD(0x9),
   RESET_2(0xA),
   RESET_3(0x12),
   UPDATE_BUDDY_CHANNEL(0x14),
   UPDATE_BUDDY_CAPACITY(0x15);

   final byte mode;

   BuddylistMode(int mode) {
      this.mode = (byte) mode;
   }

   public byte getMode() {
      return mode;
   }
}
