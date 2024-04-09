package connection.constants;

public enum LoginStatusCode {
    BCRYPT_MIGRATION(-10),
    BCRYPT_MIGRATION_AND_TOS(-23),
    SYSTEM_ERROR(-1),
    OK(0),
    BANNED(2),
    ID_DELETED_OR_BLOCKED(3),
    INCORRECT_PASSWORD(4), // Maple ID or password is incorrect. Please check and enter again.
    NOT_REGISTERED_ID(5), // Maple ID or password is incorrect. Please check and enter again.
    SYSTEM_ERROR_1(6), // Unable to connect due to system error.
    ALREADY_LOGGED_IN(7), // This is the currently connected ID. Please check again.
    SYSTEM_ERROR_2(8), // Unable to connect due to system error.
    SYSTEM_ERROR_3(9), // Unable to connect due to system error.
    CANNOT_PROCESS_SO_MANY_CONNECTIONS(10), // There are currently too many connection requests to the server. Please reconnect after a while.
    ONLY_USERS_OLDER_THAN_20(11), // (Korean) Only those over 20 years of age can access. Please use another channel.

    // 12 - seems to also be ok?
    UNABLE_TO_LOG_ON_AS_MASTER(13), // Master login is not possible with the current IP. Please check again.
    WRONG_GATEWAY(14), // (Korean) This accounts use of the game has been suspended due to the expiration of the temporary subscription period. Users under the age of 14 can use the game by logging in to Nexon.com and receiving consent from their parents (legal representative).
    PROCESSING_REQUEST(15), // (Korean) The Nexon ID of this account is a Nexon ID that does not exist.
    WRONG_GATEWAY_2(17), // (Korean) Web credentials do not match. Please log out of the currently open Maple Story website and log back in.

    // 19 - I connected via a temporarily blocked IP. For more information, please contact the support desk on the official page.

    // 23 - (Korean) Identity verification completed. Please enter your one-time password (U-OTP).

    // 24 - (Korean) One-time password (U-OTP) does not match. Please check again.

    // 25 - (Korean) The number of authentication errors for the one-time password (U-OTP) has been exceeded. Authentication is possible after resetting the number of authentication errors in the mobile phone U-OTP program.

    // 26 - (Korean) Current one-time password (U-OTP). The service is unavailable due to system maintenance.

    // 28 - The U-OTP trial period has ended. Safe and secure login U-OTP's official service can be used after subscribing to the flat rate system. MapleStory games are free and unlimited.
    LICENSE_AGREEMENT(23), // TODO i don't think this exists
    ;

    /**
     * JMS has
     *  1001
     */

    final byte code;

    LoginStatusCode(int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return code;
    }

    public boolean isError() {
        return code != 0;
    }
}
