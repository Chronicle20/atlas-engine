package connection.constants;

public enum CharacterNameResponseCode {
    OK(0),
    ALREADY_REGISTERED(1), // This name is already registered. Please enter another name.
    NOT_ALLOWED(2), // This name is not allowed. Please enter another name.
    SYSTEM_ERROR(3); // An error occurred for an unknown reason. Please reconnect after a while.
    final byte code;

    CharacterNameResponseCode(int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return code;
    }
}
