package connection.packets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import client.GuidedBullet;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.MapleMount;
import client.MapleRing;
import client.SpeedInfusion;
import client.TemporaryStatBase;
import client.TemporaryStatType;
import client.TemporaryStatValue;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.newyear.NewYearCardRecord;
import constants.game.ExpTable;
import constants.game.GameConstants;
import constants.inventory.ItemConstants;
import net.packet.InPacket;
import net.packet.OutPacket;
import server.ItemInformationProvider;
import server.maps.MapleMiniGame;
import server.maps.MaplePlayerShop;
import tools.Pair;
import tools.StringUtil;

public class CCommon {
   public final static long ZERO_TIME = 94354848000000000L;//00 40 E0 FD 3B 37 4F 01
   private final static long FT_UT_OFFSET =
         116444736010800000L + (10000L * TimeZone.getDefault().getOffset(System.currentTimeMillis()));
   // normalize with timezone offset suggested by Ari
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

   static void addExpirationTime(OutPacket p, long time) {
      p.writeLong(getTime(time)); // offset expiration time issue found thanks to Thora
   }

   static void addItemInfo(OutPacket p, Item item) {
      addItemInfo(p, item, false);
   }

   public static void addItemInfo(OutPacket p, Item item, boolean zeroPosition) {
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
            p.writeShort(pos > 100 ? pos - 100 : pos);
         } else {
            p.writeByte(pos);
         }
      }
      p.writeByte(itemType);
      p.writeInt(item.getItemId());
      p.writeBool(isCash);
      if (isCash) {
         p.writeLong(item.isPet() ? item.getPetId().orElse(-1) : isRing ? equip.getRingId() : item.getCashId());
      }
      addExpirationTime(p, item.getExpiration());
      if (item.isPet()) {
         MaplePet pet = item.getPet().orElseThrow();
         p.writeFixedString(StringUtil.getRightPaddedStr(pet.getName(), '\0', 13));
         p.writeByte(pet.getLevel());
         p.writeShort(pet.getCloseness());
         p.writeByte(pet.getFullness());
         addExpirationTime(p, item.getExpiration());
         //TODO temporarily enable pet skills. Need to programmatically ensure the user has these items.
         //0x01(item pickup), 0x02(expand pickup), 0x04(auto pickup), 0x08(unpickable), 0x10(left over pickup), 0x20(hp charge), 0x40(mp charge), 0x80(buff), 0x100(draw), 0x200(dialog)
         p.writeShort(0); // nPetAttribute
         p.writeShort(0x01 | 0x02 | 0x04 | 0x8 | 0x10);

         p.writeInt(100); // nRemainLife
         p.writeShort(0); // nAttribute
         return;
      }
      if (equip == null) {
         p.writeShort(item.getQuantity());
         p.writeString(item.getOwner());
         p.writeShort(item.getFlag()); // flag

         if (ItemConstants.isRechargeable(item.getItemId())) {
            p.writeLong(0);// liSN
         }
         return;
      }
      p.writeByte(equip.getUpgradeSlots()); // upgrade slots
      p.writeByte(equip.getLevel()); // level
      p.writeByte(0);
      p.writeShort(equip.getStr()); // str
      p.writeShort(equip.getDex()); // dex
      p.writeShort(equip.getInt()); // int
      p.writeShort(equip.getLuk()); // luk
      p.writeShort(equip.getHp()); // hp
      p.writeShort(equip.getMp()); // mp
      p.writeShort(equip.getWatk()); // watk
      p.writeShort(equip.getMatk()); // matk
      p.writeShort(equip.getWdef()); // wdef
      p.writeShort(equip.getMdef()); // mdef
      p.writeShort(equip.getAcc()); // accuracy
      p.writeShort(equip.getAvoid()); // avoid
      p.writeShort(equip.getHands()); // hands
      p.writeShort(equip.getSpeed()); // speed
      p.writeShort(equip.getJump()); // jump
      p.writeString(equip.getOwner()); // owner name
      p.writeShort(equip.getFlag()); //Item Flags

      int itemLevel = equip.getItemLevel();

      long expNibble = ((long) ExpTable.getExpNeededForLevel(ii.getEquipLevelReq(item.getItemId())) * equip.getItemExp());
      expNibble /= ExpTable.getEquipExpNeededForLevel(itemLevel);

      p.writeByte(0);
      p.writeByte(itemLevel); //Item Level
      if (isCash) {
         p.writeInt(0);
      } else {
         p.writeInt((int) expNibble);
      }
      p.writeInt(-1);// nDurability

      p.writeByte(0);
      p.writeShort(0);
      p.writeShort(0);
      p.writeShort(0);
      p.writeShort(0);
      p.writeShort(0);
      p.writeInt(0);

      if (!isCash) {
         p.writeLong(0);
      }
      p.writeLong(0);
      p.writeInt(-1);
   }

   public static void addCharLook(OutPacket p, MapleCharacter chr, boolean mega) {
      p.writeByte(chr.getGender());
      p.writeByte(chr.getSkinColor().getId());
      p.writeInt(chr.getFace());
      p.writeByte(mega ? 0 : 1);
      p.writeInt(chr.getHair());
      addCharEquips(p, chr);
   }

   public static void addCharStats(OutPacket p, MapleCharacter chr) {
      p.writeInt(chr.getId()); // character id
      p.writeFixedString(StringUtil.getRightPaddedStr(chr.getName(), '\0', 13));
      p.writeByte(chr.getGender()); // gender (0 = male, 1 = female)
      p.writeByte(chr.getSkinColor().getId()); // skin color
      p.writeInt(chr.getFace()); // face
      p.writeInt(chr.getHair()); // hair

      for (int i = 0; i < 3; i++) {
         p.writeLong(chr.getPet(i).map(MaplePet::getUniqueId).orElse(0));
      }

      p.writeByte(chr.getLevel()); // level
      p.writeShort(chr.getJob().getId()); // job
      p.writeShort(chr.getStr()); // str
      p.writeShort(chr.getDex()); // dex
      p.writeShort(chr.getInt()); // int
      p.writeShort(chr.getLuk()); // luk
      p.writeShort(chr.getHp()); // hp (?)
      p.writeShort(chr.getClientMaxHp()); // maxhp
      p.writeShort(chr.getMp()); // mp (?)
      p.writeShort(chr.getClientMaxMp()); // maxmp
      p.writeShort(chr.getRemainingAp()); // remaining ap
      if (GameConstants.hasSPTable(chr.getJob())) {
         addRemainingSkillInfo(p, chr);
      } else {
         p.writeShort(chr.getRemainingSp()); // remaining sp
      }
      p.writeInt(chr.getExp()); // current exp
      p.writeShort(chr.getFame()); // fame
      p.writeInt(chr.getGachaExp()); //Gacha Exp
      p.writeInt(chr.getMapId()); // current map id
      p.writeByte(chr.getInitialSpawnpoint()); // spawnpoint
      p.writeShort(0); // getSubCategory
      p.writeLong(0);
      p.writeInt(0);
      p.writeInt(0);
      p.writeInt(0);
   }

   private static void addRemainingSkillInfo(OutPacket p, MapleCharacter chr) {
      int[] remainingSp = chr.getRemainingSps();
      int effectiveLength = 0;
      for (int j : remainingSp) {
         if (j > 0) {
            effectiveLength++;
         }
      }

      p.writeByte(effectiveLength);
      for (int i = 0; i < remainingSp.length; i++) {
         if (remainingSp[i] > 0) {
            p.writeByte(i + 1);
            p.writeByte(remainingSp[i]);
         }
      }
   }

   private static void addCharEquips(OutPacket p, MapleCharacter chr) {
      MapleInventory equip = chr.getInventory(MapleInventoryType.EQUIPPED);
      Collection<Item> ii = ItemInformationProvider.getInstance().canWearEquipment(chr, equip.list());
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
         p.writeByte(entry.getKey());
         p.writeInt(entry.getValue());
      }
      p.writeByte(0xFF);
      for (Map.Entry<Short, Integer> entry : maskedEquip.entrySet()) {
         p.writeByte(entry.getKey());
         p.writeInt(entry.getValue());
      }
      p.writeByte(0xFF);
      Item cWeapon = equip.getItem((short) -111);
      p.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);
      for (int i = 0; i < 3; i++) {
         p.writeInt(chr.getPet(i).map(Item::getItemId).orElse(0));
      }
   }

   public static void encodeNewYearCard(NewYearCardRecord newyear, OutPacket p) {
      p.writeInt(newyear.getId());
      p.writeInt(newyear.getSenderId());
      p.writeString(newyear.getSenderName());
      p.writeBool(newyear.isSenderCardDiscarded());
      p.writeLong(newyear.getDateSent());
      p.writeInt(newyear.getReceiverId());
      p.writeString(newyear.getReceiverName());
      p.writeBool(newyear.isReceiverCardDiscarded());
      p.writeBool(newyear.isReceiverCardReceived());
      p.writeLong(newyear.getDateReceived());
      p.writeString(newyear.getMessage());
   }

   public static void addRingLook(OutPacket p, MapleCharacter chr, boolean crush) {
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
               p.writeByte(1);
            }
            p.writeInt(ring.getRingId());
            p.writeInt(0);
            p.writeInt(ring.getPartnerRingId());
            p.writeInt(0);
            p.writeInt(ring.getItemId());
         }
      }
      if (yes == false) {
         p.writeByte(0);
      }
   }

   public static void addMarriageRingLook(MapleClient target, OutPacket p, MapleCharacter chr) {
      Optional<MapleRing> ring = chr.getMarriageRing();

      if (ring.isEmpty() || !ring.get().equipped()) {
         p.writeByte(0);
         return;
      }

      p.writeByte(1);
      MapleCharacter targetChr = target.getPlayer();
      if (targetChr != null && targetChr.getPartnerId() == chr.getId()) {
         p.writeInt(0);
         p.writeInt(0);
      } else {
         p.writeInt(chr.getId());
         p.writeInt(ring.get().getPartnerChrId());
      }
      p.writeInt(ring.get().getItemId());
   }

   /**
    * Adds a announcement box to an existing MaplePacketLittleEndianWriter.
    *
    * @param p    The MaplePacketLittleEndianWriter to add an announcement box
    *             to.
    * @param shop The shop to sendPacket.
    */
   public static void addAnnounceBox(OutPacket p, MaplePlayerShop shop, int availability) {
      p.writeByte(4);
      p.writeInt(shop.getObjectId());
      p.writeString(shop.getDescription());
      p.writeByte(0);
      p.writeByte(0);
      p.writeByte(1);
      p.writeByte(availability);
      p.writeByte(0);
   }

   public static void addAnnounceBox(OutPacket p, MapleMiniGame game, int ammount, int joinable) {
      p.writeByte(game.getGameType().getValue());
      p.writeInt(game.getObjectId()); // gameid/shopid
      p.writeString(game.getDescription()); // desc
      p.writeBool(!game.getPassword().isEmpty());    // password here, thanks GabrielSin
      p.writeByte(game.getPieceType());
      p.writeByte(ammount);
      p.writeByte(2);         //player capacity
      p.writeByte(joinable);
   }

   public static void rebroadcastMovementList(OutPacket op, InPacket ip, long movementDataLength) {
      //movement command length is sent by client, probably not a big issue? (could be calculated on server)
      //if multiple write/reads are slow, could use (and cache?) a byte[] buffer
      for (long i = 0; i < movementDataLength; i++) {
         op.writeByte(ip.readByte());
      }
   }

   public static void writeLongMaskD(OutPacket p, List<Pair<MapleDisease, Integer>> statups) {
      long firstmask = 0;
      long secondmask = 0;
      for (Pair<MapleDisease, Integer> statup : statups) {
         if (statup.getLeft().isFirst()) {
            firstmask |= statup.getLeft().getValue();
         } else {
            secondmask |= statup.getLeft().getValue();
         }
      }
      p.writeLong(firstmask);
      p.writeLong(secondmask);
   }

   public static void writeLongMask(OutPacket p, List<Pair<TemporaryStatType, TemporaryStatValue>> statups) {
      int[] mask = new int[4];
      for (Pair<TemporaryStatType, TemporaryStatValue> statup : statups) {
         mask[statup.left.getSet()] |= statup.left.getMask();
      }
      for (int i = 3; i >= 0; i--) {
         p.writeInt(mask[i]);
      }
   }

   public static void writeLongMaskFromList(OutPacket p, List<TemporaryStatType> statups) {
      int[] mask = new int[4];
      for (TemporaryStatType statup : statups) {
         mask[statup.getSet()] |= statup.getMask();
      }
      for (int i = 3; i >= 0; i--) {
         p.writeInt(mask[i]);
      }
   }

   public static void addPetInfo(OutPacket p, MaplePet pet, boolean showpet) {
      p.writeByte(1);
      if (showpet) {
         p.writeByte(0);
      }

      p.writeInt(pet.getItemId());
      p.writeString(pet.getName());
      p.writeLong(pet.getUniqueId());
      p.writePos(pet.getPos());
      p.writeByte(pet.getStance());
      p.writeInt(pet.getFh());
   }

   public static String getRightPaddedStr(String in, char padchar, int length) {
      return in + String.valueOf(padchar).repeat(Math.max(0, length - in.length()));
   }

   public static List<TemporaryStatBase> getTemporaryStats(MapleCharacter character) {
      List<TemporaryStatBase> list = new ArrayList<>();
      list.add(new TemporaryStatBase(true)); // Energy Charged

      list.add(new TemporaryStatBase(true)); // Dash Speed

      list.add(new TemporaryStatBase(true)); // Dash Jump

      TemporaryStatBase mount;
      Integer bv = character.getBuffedValue(TemporaryStatType.MONSTER_RIDING);
      if (bv == null) {
         mount = new TemporaryStatBase(false);
      } else {
         int itemId = character.getMount().map(MapleMount::getItemId).orElse(0);
         int skillId = character.getMount().map(MapleMount::getSkillId).orElse(0);
         mount = new TemporaryStatBase(itemId, skillId, false);
      }
      list.add(mount);

      list.add(new SpeedInfusion()); // Speed Infusion

      list.add(new GuidedBullet()); // Guided Bullet

      list.add(new TemporaryStatBase(true)); // Undead

      return list;
   }
}
