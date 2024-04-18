package connection.constants;

public enum DeleteCharacterCode {
   SUCCESS(0),
   UNABLE_TO_CONNECT_SYSTEM_ERROR(0x6),
   UNKNOWN_ERROR(0x9),
   TOO_MANY_CONNECTIONS(0xA),
   NEXON_ID_DIFFERENT_THEN_REGISTERED(0x10),
   CANNOT_DELETE_AS_GUILD_MASTER(0x12),
   SECONDARY_PIN_DOES_NOT_MATCH(0x14),
   CANNOT_DELETE_WHEN_ENGAGED(0x15),
   ONE_TIME_PASSWORD_DOES_NOT_MATCH(0x18), // untranslated
   ONE_TIME_PASSWORD_ATTEMPTS_EXCEEDED(0x19), // untranslated
   ONE_TIME_PASSWORD_SERVICE_NOT_AVAILABLE(0x1A), // untranslated
   ONE_TIME_PASSWORD_TRIAL_PERIOD_ENDED(0x1C),
   CANNOT_DELETE_WITH_FAMILY(0x1D),;
   // 0xE8 crashes game

   final byte code;

   DeleteCharacterCode(int code) {
      this.code = (byte) code;
   }

   public byte getCode() {
      return code;
   }
}
