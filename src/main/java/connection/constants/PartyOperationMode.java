package connection.constants;

public enum PartyOperationMode {
   INVITE(0x04);

   final byte mode;

   PartyOperationMode(int code) {
      this.mode = (byte) code;
   }

   public byte getMode() {
      return mode;
   }
}
