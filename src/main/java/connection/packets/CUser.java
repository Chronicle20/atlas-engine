package connection.packets;

import java.awt.*;

import client.MapleCharacter;
import client.inventory.Equip;
import client.inventory.MaplePet;
import connection.headers.SendOpcode;
import connection.constants.ShowItemGainInChatCode;
import net.packet.InPacket;
import net.packet.OutPacket;
import net.packet.Packet;
import server.maps.MapleDragon;
import server.maps.MaplePlayerShop;

public class CUser {

   public static Packet getChatText(int cidfrom, String text, boolean gm, int show) {
      final OutPacket p = OutPacket.create(SendOpcode.CHATTEXT);
      p.writeInt(cidfrom);
      p.writeBool(gm);
      p.writeString(text);
      p.writeByte(show);
      return p;
   }

   public static Packet useChalkboard(MapleCharacter chr, boolean close) {
      final OutPacket p = OutPacket.create(SendOpcode.CHALKBOARD);
      p.writeInt(chr.getId());
      if (close) {
         p.writeByte(0);
      } else {
         p.writeByte(1);
         p.writeString(chr.getChalkboard().orElse(""));
      }
      return p;
   }

   public static Packet updatePlayerShopBox(MaplePlayerShop shop) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_CHAR_BOX);
      p.writeInt(shop.getOwner().getId());

      updatePlayerShopBoxInfo(p, shop);
      return p;
   }

   public static Packet removePlayerShopBox(MaplePlayerShop shop) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_CHAR_BOX);
      p.writeInt(shop.getOwner().getId());
      p.writeByte(0);
      return p;
   }

   public static Packet addOmokBox(MapleCharacter chr, int ammount, int type) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_CHAR_BOX);
      p.writeInt(chr.getId());
      CCommon.addAnnounceBox(p, chr.getMiniGame(), ammount, type);
      return p;
   }

   public static Packet addMatchCardBox(MapleCharacter chr, int ammount, int type) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_CHAR_BOX);
      p.writeInt(chr.getId());
      CCommon.addAnnounceBox(p, chr.getMiniGame(), ammount, type);
      return p;
   }

   public static Packet removeMinigameBox(MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_CHAR_BOX);
      p.writeInt(chr.getId());
      p.writeByte(0);
      return p;
   }

   public static Packet getScrollEffect(int chr, Equip.ScrollResult scrollSuccess, boolean legendarySpirit,
                                        boolean whiteScroll) {   // thanks to Rien dev team
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_SCROLL_EFFECT);
      p.writeInt(chr);
      p.writeBool(scrollSuccess == Equip.ScrollResult.SUCCESS);
      p.writeBool(scrollSuccess == Equip.ScrollResult.CURSE);
      p.writeBool(legendarySpirit);
      p.writeBool(whiteScroll);
      p.writeBool(false); //recoverable
      return p;
   }

   public static Packet showPet(MapleCharacter chr, MaplePet pet, boolean remove, boolean hunger) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_PET);
      p.writeInt(chr.getId());
      p.writeByte(chr.getPetIndex(pet));
      if (remove) {
         p.writeByte(0);
         p.writeByte(hunger ? 1 : 0);
      } else {
         CCommon.addPetInfo(p, pet, true);
      }
      return p;
   }

   public static Packet spawnDragon(MapleDragon dragon) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_DRAGON);
      p.writeInt(dragon.getOwner().getId());//objectid = owner id
      p.writeShort(dragon.getPosition().x);
      p.writeShort(0);
      p.writeShort(dragon.getPosition().y);
      p.writeShort(0);
      p.writeByte(dragon.getStance());
      p.writeByte(0);
      p.writeShort(dragon.getOwner().getJob().getId());
      return p;
   }

   public static Packet moveDragon(MapleDragon dragon, Point startPos, InPacket ip, long movementDataLength) {
      final OutPacket p = OutPacket.create(SendOpcode.MOVE_DRAGON);
      p.writeInt(dragon.getOwner().getId());
      p.writePos(startPos);
      CCommon.rebroadcastMovementList(p, ip, movementDataLength);
      return p;
   }

   /**
    * Sends a request to remove Mir<br>
    *
    * @param chrid - Needs the specific Character ID
    * @return The packet
    */
   public static Packet removeDragon(int chrid) {
      final OutPacket p = OutPacket.create(SendOpcode.REMOVE_DRAGON);
      p.writeInt(chrid);
      return p;
   }

   public static Packet showHpHealed(int cid, int amount) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_FOREIGN_EFFECT);
      p.writeInt(cid);
      p.writeByte(0x0A); //Type
      p.writeByte(amount);
      return p;
   }

   public static Packet showBuffeffect(int cid, int skillid, int effectid, byte direction) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_FOREIGN_EFFECT);
      p.writeInt(cid);
      p.writeByte(effectid); //buff level
      p.writeInt(skillid);
      p.writeByte(direction);
      p.writeByte(1);
      p.writeLong(0);
      return p;
   }

   public static Packet showBuffeffect(int cid, int skillid, int skilllv, int effectid,
                                       byte direction) {   // updated packet structure found thanks to Rien dev team
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_FOREIGN_EFFECT);
      p.writeInt(cid);
      p.writeByte(effectid);
      p.writeInt(skillid);
      p.writeByte(0);
      p.writeByte(skilllv);
      p.writeByte(direction);

      return p;
   }

   public static Packet showBerserk(int cid, int skilllevel, boolean Berserk) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_FOREIGN_EFFECT);
      p.writeInt(cid);
      p.writeByte(1);
      p.writeInt(1320006);
      p.writeByte(0xA9);
      p.writeByte(skilllevel);
      p.writeByte(Berserk ? 1 : 0);
      return p;
   }

   public static Packet showPetLevelUp(MapleCharacter chr, byte index) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_FOREIGN_EFFECT);
      p.writeInt(chr.getId());
      p.writeByte(4);
      p.writeByte(0);
      p.writeByte(index);
      return p;
   }

   public static Packet showForeignCardEffect(int id) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_FOREIGN_EFFECT);
      p.writeInt(id);
      p.writeByte(0x0D);
      return p;
   }

   public static Packet showForeignInfo(int cid, String path) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_FOREIGN_EFFECT);
      p.writeInt(cid);
      p.writeByte(0x17);
      p.writeString(path);
      p.writeInt(1);
      return p;
   }

   public static Packet showForeignBuybackEffect(int cid) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_FOREIGN_EFFECT);
      p.writeInt(cid);
      p.writeByte(11);
      p.writeInt(0);

      return p;
   }

   public static Packet showForeignMakerEffect(int cid, boolean makerSucceeded) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_FOREIGN_EFFECT);
      p.writeInt(cid);
      p.writeByte(16);
      p.writeInt(makerSucceeded ? 0 : 1);
      return p;
   }

   public static Packet showForeignEffect(int effect) {
      return showForeignEffect(-1, effect);
   }

   public static Packet showForeignEffect(int cid, int effect) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_FOREIGN_EFFECT);
      p.writeInt(cid);
      p.writeByte(effect);
      return p;
   }

   public static Packet showRecovery(int cid, byte amount) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_FOREIGN_EFFECT);
      p.writeInt(cid);
      p.writeByte(0x0A);
      p.writeByte(amount);
      return p;
   }

   public static Packet showOwnBuffEffect(int skillid, ShowItemGainInChatCode effect) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_ITEM_GAIN_INCHAT);
      p.writeByte(effect.getCode());
      p.writeInt(skillid);
      p.writeByte(0xA9);
      p.writeByte(1);
      return p;
   }

   public static Packet showOwnBerserk(int skilllevel, boolean Berserk) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_ITEM_GAIN_INCHAT);
      p.writeByte(ShowItemGainInChatCode.SKILL_USE.getCode());
      p.writeInt(1320006);
      p.writeByte(0xA9);
      p.writeByte(skilllevel);
      p.writeByte(Berserk ? 1 : 0);
      return p;
   }

   public static Packet showOwnPetLevelUp(byte index) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_ITEM_GAIN_INCHAT);
      p.writeByte(ShowItemGainInChatCode.PET.getCode());
      p.writeByte(0);
      p.writeByte(index); // Pet Index
      return p;
   }

   public static Packet showGainCard() {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_ITEM_GAIN_INCHAT);
      p.writeByte(ShowItemGainInChatCode.MONSTER_BOOK_CARD_GET.getCode());
      return p;
   }

   public static Packet showIntro(String path) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_ITEM_GAIN_INCHAT);
      p.writeByte(ShowItemGainInChatCode.SQUIB_EFFECT.getCode());
      p.writeString(path);
      return p;
   }

   public static Packet showInfo(String path) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_ITEM_GAIN_INCHAT);
      p.writeByte(ShowItemGainInChatCode.EXP_ITEM_CONSUMED.getCode());
      p.writeString(path);
      p.writeInt(1);
      return p;
   }

   public static Packet showBuybackEffect() {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_ITEM_GAIN_INCHAT);
      p.writeByte(ShowItemGainInChatCode.BUFF_ITEM_EFFECT.getCode());
      p.writeInt(0);

      return p;
   }

   /**
    * 0 = Levelup 6 = Exp did not drop (Safety Charms) 7 = Enter portal sound
    * 8 = Job change 9 = Quest complete 10 = Recovery 11 = Buff effect
    * 14 = Monster book pickup 15 = Equipment levelup 16 = Maker Skill Success
    * 17 = Buff effect w/ sfx 19 = Exp card [500, 200, 50] 21 = Wheel of destiny
    * 26 = Spirit Stone
    *
    * @param effect
    * @return
    */
   public static Packet showSpecialEffect(ShowItemGainInChatCode effect) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_ITEM_GAIN_INCHAT);
      p.writeByte(effect.getCode());
      return p;
   }

   public static Packet showMakerEffect(boolean makerSucceeded) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_ITEM_GAIN_INCHAT);
      p.writeByte(ShowItemGainInChatCode.ITEM_MAKER.getCode());
      p.writeInt(makerSucceeded ? 0 : 1);
      return p;
   }

   public static Packet showOwnRecovery(byte heal) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_ITEM_GAIN_INCHAT);
      p.writeByte(ShowItemGainInChatCode.INC_DEC_HP_EFFECT.getCode());
      p.writeByte(heal);
      return p;
   }

   public static Packet showWheelsLeft(int left) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_ITEM_GAIN_INCHAT);
      p.writeByte(ShowItemGainInChatCode.ITEM_LEVEL_UP.getCode());
      p.writeByte(left);
      return p;
   }

   private static void updatePlayerShopBoxInfo(OutPacket p, MaplePlayerShop shop) {
      byte[] roomInfo = shop.getShopRoomInfo();

      p.writeByte(4);
      p.writeInt(shop.getObjectId());
      p.writeString(shop.getDescription());
      p.writeByte(0);                 // pw
      p.writeByte(shop.getItemId() % 100);
      p.writeByte(roomInfo[0]);       // curPlayers
      p.writeByte(roomInfo[1]);       // maxPlayers
      p.writeByte(0);
   }

   public static Packet playPortalSound() {
      return showSpecialEffect(ShowItemGainInChatCode.PLAY_PORTAL_SE);
   }

   public static Packet showMonsterBookPickup() {
      return showSpecialEffect(ShowItemGainInChatCode.MONSTER_BOOK_CARD_GET);
   }

   public static Packet showEquipmentLevelUp() {
      return showSpecialEffect(ShowItemGainInChatCode.ITEM_LEVEL_UP);
   }

   public static Packet showItemLevelup() {
      return showSpecialEffect(ShowItemGainInChatCode.ITEM_LEVEL_UP);
   }

   public static Packet showBuffeffect(int cid, int skillid, int effectid) {
      return showBuffeffect(cid, skillid, effectid, (byte) 3);
   }
}
