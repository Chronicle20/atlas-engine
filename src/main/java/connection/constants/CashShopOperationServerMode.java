package connection.constants;

import java.util.Arrays;
import java.util.Optional;

public enum CashShopOperationServerMode {
    ON_BUY(0x3),
    ON_GIFT_MATE_INFO_RESULT(0x4),
    ON_SET_WISH(0x5),
    ON_BUY_SLOT_INC(0x6),
    ON_INC_TRUNK_COUNT(0x7),
    ON_INC_CHARACTER_SLOT_COUNT(0x8),
    ON_BUY_CHARACTER(0x9),
    ON_ENABLE_EQUIP_SLOT_EXT(0xA),
    ON_MOVE_CASH_ITEM_L_TO_S(0xE),
    ON_MOVE_CASH_ITEM_S_TO_L(0xF),
    ON_REBATE_LOCKER_ITEM(0x1B),
    ON_BUY_COUPLE(0x1E),
    ON_BUY_PACKAGE(0x1F),
    ON_BUY_NORMAL(0x21),
    APPLY_WISH_LIST_EVENT(0x22),
    ON_BUY_FRIENDSHIP(0x23),
    REQUEST_CASH_PURCHASE_RECORD(0x29),
    SEND_GIFTS_PACKET(0x2E),
    BUY_TRANSFER_WORLD_ITEM_PACKET(0x31),
    ;


    final byte mode;

    CashShopOperationServerMode(int code) {
        this.mode = (byte) code;
    }

    public static Optional<CashShopOperationServerMode> from(byte b) {
        return Arrays.stream(CashShopOperationServerMode.values()).filter( m -> m.getMode() == b).findFirst();
    }

    public byte getMode() {
        return mode;
    }
}
