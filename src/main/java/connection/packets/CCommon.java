package connection.packets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import client.BuffDataHolder;
import client.BuffStat;
import client.GuidedBullet;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.MapleMount;
import client.MapleRing;
import client.SpeedInfusion;
import client.TemporaryStatBase;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.newyear.NewYearCardRecord;
import constants.game.ExpTable;
import constants.game.GameConstants;
import constants.inventory.ItemConstants;
import server.ItemInformationProvider;
import server.maps.MapleMiniGame;
import server.maps.MaplePlayerShop;
import tools.Pair;
import tools.StringUtil;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.LittleEndianWriter;
import tools.data.output.MaplePacketLittleEndianWriter;

public class CCommon {
   public final static long ZERO_TIME = 94354848000000000L;//00 40 E0 FD 3B 37 4F 01
   private final static long FT_UT_OFFSET = 116444736010800000L + (10000L * TimeZone.getDefault()
         .getOffset(System.currentTimeMillis())); // normalize with timezone offset suggested by Ari
   private final static long DEFAULT_TIME = 150842304000000000L;//00 80 05 BB 46 E6 17 02
   private final static long PERMANENT = 150841440000000000L; // 00 C0 9B 90 7D E5 17 02

   public static long getTime(long utcTimestamp) {
      if (utcTimestamp < 0 && utcTimestamp >= -3) {
         if (utcTimestamp == -1) {
            return DEFAULT_TIME;    //high number ll
         } else if (utcTimestamp == -2) {
            return ZERO_TIME;
         } else {
            return PERMANENT;
         }
      }

      return utcTimestamp * 10000 + FT_UT_OFFSET;
   }

   static void addExpirationTime(final MaplePacketLittleEndianWriter mplew, long time) {
      mplew.writeLong(getTime(time)); // offset expiration time issue found thanks to Thora
   }

   static void addItemInfo(final MaplePacketLittleEndianWriter mplew, Item item) {
      addItemInfo(mplew, item, false);
   }

   public static void addItemInfo(final MaplePacketLittleEndianWriter mplew, Item item, boolean zeroPosition) {
      ItemInformationProvider ii = ItemInformationProvider.getInstance();
      boolean isCash = ii.isCash(item.getItemId());
      boolean isRing = false;
      Equip equip = null;
      short pos = item.getPosition();
      byte itemType = item.getItemType();
      if (itemType == 1) {
         equip = (Equip) item;
         isRing = equip.getRingId() > -1;
      }
      if (!zeroPosition) {
         if (equip != null) {
            if (pos < 0) {
               pos *= -1;
            }
            mplew.writeShort(pos > 100 ? pos - 100 : pos);
         } else {
            mplew.write(pos);
         }
      }
      mplew.write(itemType);
      mplew.writeInt(item.getItemId());
      mplew.writeBool(isCash);
      if (isCash) {
         mplew.writeLong(item.isPet() ? item.getPetId()
               .orElse(-1) : isRing ? equip.getRingId() : item.getCashId());
      }
      addExpirationTime(mplew, item.getExpiration());
      if (item.isPet()) {
         MaplePet pet = item.getPet()
               .orElseThrow();
         mplew.writeAsciiString(StringUtil.getRightPaddedStr(pet.getName(), '\0', 13));
         mplew.write(pet.getLevel());
         mplew.writeShort(pet.getCloseness());
         mplew.write(pet.getFullness());
         addExpirationTime(mplew, item.getExpiration());
         //TODO temporarily enable pet skills. Need to programmatically ensure the user has these items.
         //0x01(item pickup), 0x02(expand pickup), 0x04(auto pickup), 0x08(unpickable), 0x10(left over pickup), 0x20(hp charge), 0x40(mp charge), 0x80(buff), 0x100(draw), 0x200(dialog)
         mplew.writeShort(0); // nPetAttribute
         mplew.writeShort(0x01 | 0x02 | 0x04 | 0x8 | 0x10);

         mplew.writeInt(100); // nRemainLife
         mplew.writeShort(0); // nAttribute
         return;
      }
      if (equip == null) {
         mplew.writeShort(item.getQuantity());
         mplew.writeMapleAsciiString(item.getOwner());
         mplew.writeShort(item.getFlag()); // flag

         if (ItemConstants.isRechargeable(item.getItemId())) {
            mplew.writeLong(0);// liSN
         }
         return;
      }
      mplew.write(equip.getUpgradeSlots()); // upgrade slots
      mplew.write(equip.getLevel()); // level
      mplew.write(0);
      mplew.writeShort(equip.getStr()); // str
      mplew.writeShort(equip.getDex()); // dex
      mplew.writeShort(equip.getInt()); // int
      mplew.writeShort(equip.getLuk()); // luk
      mplew.writeShort(equip.getHp()); // hp
      mplew.writeShort(equip.getMp()); // mp
      mplew.writeShort(equip.getWatk()); // watk
      mplew.writeShort(equip.getMatk()); // matk
      mplew.writeShort(equip.getWdef()); // wdef
      mplew.writeShort(equip.getMdef()); // mdef
      mplew.writeShort(equip.getAcc()); // accuracy
      mplew.writeShort(equip.getAvoid()); // avoid
      mplew.writeShort(equip.getHands()); // hands
      mplew.writeShort(equip.getSpeed()); // speed
      mplew.writeShort(equip.getJump()); // jump
      mplew.writeMapleAsciiString(equip.getOwner()); // owner name
      mplew.writeShort(equip.getFlag()); //Item Flags

      int itemLevel = equip.getItemLevel();

      long expNibble = ((long) ExpTable.getExpNeededForLevel(ii.getEquipLevelReq(item.getItemId())) * equip.getItemExp());
      expNibble /= ExpTable.getEquipExpNeededForLevel(itemLevel);

      mplew.write(0);
      mplew.write(itemLevel); //Item Level
      if (isCash) {
         mplew.writeInt(0);
      } else {
         mplew.writeInt((int) expNibble);
      }
      mplew.writeInt(-1);// nDurability

      mplew.write(0);
      mplew.writeShort(0);
      mplew.writeShort(0);
      mplew.writeShort(0);
      mplew.writeShort(0);
      mplew.writeShort(0);
      mplew.writeInt(0);

      if (!isCash) {
         mplew.writeLong(0);
      }
      mplew.writeLong(0);
      mplew.writeInt(-1);
   }

   public static void addCharLook(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean mega) {
      mplew.write(chr.getGender());
      mplew.write(chr.getSkinColor()
            .getId());
      mplew.writeInt(chr.getFace());
      mplew.write(mega ? 0 : 1);
      mplew.writeInt(chr.getHair());
      addCharEquips(mplew, chr);
   }

   public static void addCharStats(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
      mplew.writeInt(chr.getId()); // character id
      mplew.writeAsciiString(StringUtil.getRightPaddedStr(chr.getName(), '\0', 13));
      mplew.write(chr.getGender()); // gender (0 = male, 1 = female)
      mplew.write(chr.getSkinColor()
            .getId()); // skin color
      mplew.writeInt(chr.getFace()); // face
      mplew.writeInt(chr.getHair()); // hair

      for (int i = 0; i < 3; i++) {
         mplew.writeLong(chr.getPet(i)
               .map(MaplePet::getUniqueId)
               .orElse(0));
      }

      mplew.write(chr.getLevel()); // level
      mplew.writeShort(chr.getJob()
            .getId()); // job
      mplew.writeShort(chr.getStr()); // str
      mplew.writeShort(chr.getDex()); // dex
      mplew.writeShort(chr.getInt()); // int
      mplew.writeShort(chr.getLuk()); // luk
      mplew.writeShort(chr.getHp()); // hp (?)
      mplew.writeShort(chr.getClientMaxHp()); // maxhp
      mplew.writeShort(chr.getMp()); // mp (?)
      mplew.writeShort(chr.getClientMaxMp()); // maxmp
      mplew.writeShort(chr.getRemainingAp()); // remaining ap
      if (GameConstants.hasSPTable(chr.getJob())) {
         addRemainingSkillInfo(mplew, chr);
      } else {
         mplew.writeShort(chr.getRemainingSp()); // remaining sp
      }
      mplew.writeInt(chr.getExp()); // current exp
      mplew.writeShort(chr.getFame()); // fame
      mplew.writeInt(chr.getGachaExp()); //Gacha Exp
      mplew.writeInt(chr.getMapId()); // current map id
      mplew.write(chr.getInitialSpawnpoint()); // spawnpoint
      mplew.writeShort(0); // getSubCategory
      mplew.writeLong(0);
      mplew.writeInt(0);
      mplew.writeInt(0);
      mplew.writeInt(0);
   }

   private static void addRemainingSkillInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
      int[] remainingSp = chr.getRemainingSps();
      int effectiveLength = 0;
      for (int i = 0; i < remainingSp.length; i++) {
         if (remainingSp[i] > 0) {
            effectiveLength++;
         }
      }

      mplew.write(effectiveLength);
      for (int i = 0; i < remainingSp.length; i++) {
         if (remainingSp[i] > 0) {
            mplew.write(i + 1);
            mplew.write(remainingSp[i]);
         }
      }
   }

   private static void addCharEquips(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
      MapleInventory equip = chr.getInventory(MapleInventoryType.EQUIPPED);
      Collection<Item> ii = ItemInformationProvider.getInstance()
            .canWearEquipment(chr, equip.list());
      Map<Short, Integer> myEquip = new LinkedHashMap<>();
      Map<Short, Integer> maskedEquip = new LinkedHashMap<>();
      for (Item item : ii) {
         short pos = (byte) (item.getPosition() * -1);
         if (pos < 100 && myEquip.get(pos) == null) {
            myEquip.put(pos, item.getItemId());
         } else if (pos > 100 && pos != 111) { // don't ask. o.o
            pos -= 100;
            if (myEquip.get(pos) != null) {
               maskedEquip.put(pos, myEquip.get(pos));
            }
            myEquip.put(pos, item.getItemId());
         } else if (myEquip.get(pos) != null) {
            maskedEquip.put(pos, item.getItemId());
         }
      }
      for (Map.Entry<Short, Integer> entry : myEquip.entrySet()) {
         mplew.write(entry.getKey());
         mplew.writeInt(entry.getValue());
      }
      mplew.write(0xFF);
      for (Map.Entry<Short, Integer> entry : maskedEquip.entrySet()) {
         mplew.write(entry.getKey());
         mplew.writeInt(entry.getValue());
      }
      mplew.write(0xFF);
      Item cWeapon = equip.getItem((short) -111);
      mplew.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);
      for (int i = 0; i < 3; i++) {
         mplew.writeInt(chr.getPet(i)
               .map(Item::getItemId)
               .orElse(0));
      }
   }

   public static void encodeNewYearCard(NewYearCardRecord newyear, MaplePacketLittleEndianWriter mplew) {
      mplew.writeInt(newyear.getId());
      mplew.writeInt(newyear.getSenderId());
      mplew.writeMapleAsciiString(newyear.getSenderName());
      mplew.writeBool(newyear.isSenderCardDiscarded());
      mplew.writeLong(newyear.getDateSent());
      mplew.writeInt(newyear.getReceiverId());
      mplew.writeMapleAsciiString(newyear.getReceiverName());
      mplew.writeBool(newyear.isReceiverCardDiscarded());
      mplew.writeBool(newyear.isReceiverCardReceived());
      mplew.writeLong(newyear.getDateReceived());
      mplew.writeMapleAsciiString(newyear.getMessage());
   }

   public static void addRingLook(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean crush) {
      List<MapleRing> rings;
      if (crush) {
         rings = chr.getCrushRings();
      } else {
         rings = chr.getFriendshipRings();
      }
      boolean yes = false;
      for (MapleRing ring : rings) {
         if (ring.equipped()) {
            if (yes == false) {
               yes = true;
               mplew.write(1);
            }
            mplew.writeInt(ring.getRingId());
            mplew.writeInt(0);
            mplew.writeInt(ring.getPartnerRingId());
            mplew.writeInt(0);
            mplew.writeInt(ring.getItemId());
         }
      }
      if (yes == false) {
         mplew.write(0);
      }
   }

   public static void addMarriageRingLook(MapleClient target, final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
      Optional<MapleRing> ring = chr.getMarriageRing();

      if (ring.isEmpty() || !ring.get()
            .equipped()) {
         mplew.write(0);
         return;
      }

      mplew.write(1);
      MapleCharacter targetChr = target.getPlayer();
      if (targetChr != null && targetChr.getPartnerId() == chr.getId()) {
         mplew.writeInt(0);
         mplew.writeInt(0);
      } else {
         mplew.writeInt(chr.getId());
         mplew.writeInt(ring.get()
               .getPartnerChrId());
      }
      mplew.writeInt(ring.get()
            .getItemId());
   }

   /**
    * Adds a announcement box to an existing MaplePacketLittleEndianWriter.
    *
    * @param mplew The MaplePacketLittleEndianWriter to add an announcement box
    *              to.
    * @param shop  The shop to announce.
    */
   public static void addAnnounceBox(final MaplePacketLittleEndianWriter mplew, MaplePlayerShop shop, int availability) {
      mplew.write(4);
      mplew.writeInt(shop.getObjectId());
      mplew.writeMapleAsciiString(shop.getDescription());
      mplew.write(0);
      mplew.write(0);
      mplew.write(1);
      mplew.write(availability);
      mplew.write(0);
   }

   public static void addAnnounceBox(final MaplePacketLittleEndianWriter mplew, MapleMiniGame game, int ammount, int joinable) {
      mplew.write(game.getGameType()
            .getValue());
      mplew.writeInt(game.getObjectId()); // gameid/shopid
      mplew.writeMapleAsciiString(game.getDescription()); // desc
      mplew.writeBool(!game.getPassword()
            .isEmpty());    // password here, thanks GabrielSin
      mplew.write(game.getPieceType());
      mplew.write(ammount);
      mplew.write(2);         //player capacity
      mplew.write(joinable);
   }

   public static void rebroadcastMovementList(LittleEndianWriter lew, SeekableLittleEndianAccessor slea, long movementDataLength) {
      //movement command length is sent by client, probably not a big issue? (could be calculated on server)
      //if multiple write/reads are slow, could use (and cache?) a byte[] buffer
      for (long i = 0; i < movementDataLength; i++) {
         lew.write(slea.readByte());
      }
   }

   public static void writeLongMaskD(final MaplePacketLittleEndianWriter mplew, List<Pair<MapleDisease, Integer>> statups) {
      long firstmask = 0;
      long secondmask = 0;
      for (Pair<MapleDisease, Integer> statup : statups) {
         if (statup.getLeft()
               .isFirst()) {
            firstmask |= statup.getLeft()
                  .getValue();
         } else {
            secondmask |= statup.getLeft()
                  .getValue();
         }
      }
      mplew.writeLong(firstmask);
      mplew.writeLong(secondmask);
   }

   public static void writeLongMask(final LittleEndianWriter mplew, List<Pair<BuffStat, BuffDataHolder>> statups) {
      int[] mask = new int[4];
      for (Pair<BuffStat, BuffDataHolder> statup : statups) {
         mask[statup.left.getSet()] |= statup.left.getMask();
      }
      for (int i = 3; i >= 0; i--) {
         mplew.writeInt(mask[i]);
      }
   }

   public static void writeLongMaskFromList(final MaplePacketLittleEndianWriter mplew, List<BuffStat> statups) {
      int[] mask = new int[4];
      for (BuffStat statup : statups) {
         mask[statup.getSet()] |= statup.getMask();
      }
      for (int i = 3; i >= 0; i--) {
         mplew.writeInt(mask[i]);
      }
   }

   public static void addPetInfo(final MaplePacketLittleEndianWriter mplew, MaplePet pet, boolean showpet) {
      mplew.write(1);
      if (showpet) {
         mplew.write(0);
      }

      mplew.writeInt(pet.getItemId());
      mplew.writeMapleAsciiString(pet.getName());
      mplew.writeLong(pet.getUniqueId());
      mplew.writePos(pet.getPos());
      mplew.write(pet.getStance());
      mplew.writeInt(pet.getFh());
   }

   public static String getRightPaddedStr(String in, char padchar, int length) {
      return in + String.valueOf(padchar)
            .repeat(Math.max(0, length - in.length()));
   }

   public static List<TemporaryStatBase> getTemporaryStats(MapleCharacter character) {
      List<TemporaryStatBase> list = new ArrayList<>();
      list.add(new TemporaryStatBase(true)); // Energy Charged

      list.add(new TemporaryStatBase(true)); // Dash Speed

      list.add(new TemporaryStatBase(true)); // Dash Jump

      TemporaryStatBase mount;
      Integer bv = character.getBuffedValue(BuffStat.MONSTER_RIDING);
      if (bv == null) {
         mount = new TemporaryStatBase(false);
      } else {
         int itemId = character.getMount()
               .map(MapleMount::getItemId)
               .orElse(0);
         int skillId = character.getMount()
               .map(MapleMount::getSkillId)
               .orElse(0);
         mount = new TemporaryStatBase(itemId, skillId, false);
      }
      list.add(mount);

      list.add(new SpeedInfusion()); // Speed Infusion

      list.add(new GuidedBullet()); // Guided Bullet

      list.add(new TemporaryStatBase(true)); // Undead

      return list;
   }
}
