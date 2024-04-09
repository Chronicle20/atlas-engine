package connection.constants;

public enum ShowItemGainInChatCode {
    LEVEL_UP(0x0),
    SKILL_USE(0x1),
    SKILL_AFFECTED(0x2),

    QUEST(0x3),
    PET(0x4),
    SKILL_SPECIAL(0x5),
    PROTECT_ON_DIE_ITEM_USE(0x6),

    // something with a decode4
    PLAY_PORTAL_SE(0x8),
    JOB_CHANGED(0x9),
    QUEST_COMPLETE(0xA),
    INC_DEC_HP_EFFECT(0xB),
    BUFF_ITEM_EFFECT(0xC),
    SQUIB_EFFECT(0xD),
    MONSTER_BOOK_CARD_GET(0xE),
    LOTTERY_USE(0xF),
    ITEM_LEVEL_UP(0x10),
    ITEM_MAKER(0x11),
    EXP_ITEM_CONSUMED(0x12),
    RESERVED_EFFECT(0x13),
    BUFF(9999),
    CONSUME_EFFECT(9999),
    UPGRADE_TOMB_ITEM_USE(0x14),
    BATTLEFIELD_ITEM_USE(0x15),

    // 0x16 cash item effect? decode 4
    // 0x17 decode 4/4/1
    // 0x18 another item one decode4 (itemid) decodestr (message?)

    AVATAR_ORIENTED(9999),
    INCUBATOR_USE(9999),
    PLAY_SOUND_WITH_MUTE_BGM(0x1A),
    SOUL_STONE_USED(0x1B),
    INC_DEC_HP_EFFECT_EX(0x1C),
    REPEAT_EFFECT_REMOVE(9999);


    final byte code;

    ShowItemGainInChatCode(int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return code;
    }
}
