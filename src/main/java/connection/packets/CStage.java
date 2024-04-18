package connection.packets;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import buddy.BuddyProcessor;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.MapleRing;
import client.Skill;
import client.SkillEntry;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.newyear.NewYearCardRecord;
import connection.headers.SendOpcode;
import constants.game.GameConstants;
import net.packet.OutPacket;
import net.packet.Packet;
import net.server.PlayerCoolDownValueHolder;
import net.server.Server;
import server.maps.MapleMap;
import tools.Randomizer;
import tools.StringUtil;

public class CStage {
   /**
    * Gets character info for a character.
    *
    * @param chr The character to get info about.
    * @return The character info packet.
    */
   public static Packet getCharInfo(MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.SET_FIELD);
      p.writeShort(0);// decode opt, loop with 2 decode 4s
      p.writeInt(chr.getClient()
            .getChannel() - 1);
      p.writeByte(0);
      p.writeInt(0);
      p.writeByte(1); // sNotifierMessage
      p.writeByte(1); // bCharacterData
      p.writeShort(0); // nNotifierCheck
      for (int i = 0; i < 3; i++) {
         p.writeInt(Randomizer.nextInt());
      }
      addCharacterInfo(p, chr);
      setLogutGiftConfig(chr, p);
      p.writeLong(CCommon.getTime(System.currentTimeMillis()));
      return p;
   }

   /**
    * Gets a packet telling the client to change maps.
    *
    * @param to         The <code>MapleMap</code> to warp to.
    * @param spawnPoint The spawn portal number to spawn at.
    * @param chr        The character warping to <code>to</code>
    * @return The map change packet.
    */
   public static Packet getWarpToMap(MapleMap to, int spawnPoint, MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.SET_FIELD);
      p.writeShort(0);// decode opt, loop with 2 decode 4s
      for (int i = 0; i < 0; i++) {
         p.writeInt(i + 1); // dwType
         p.writeInt(0); // idk?
      }
      p.writeInt(chr.getClient()
            .getChannel() - 1);
      p.writeByte(0);
      p.writeInt(0);
      p.writeByte(0); // sNotifierMessage
      p.writeByte(0); // bCharacterData
      p.writeShort(0); // nNotifierCheck
      if (0 > 0) {
         p.writeString("");
         for (int j = 0; j < 0; j++) {
            p.writeString("");
         }
      }
      if (0 != 0) { // bCharacterData

      } else {
         p.writeByte(0); // revive
         p.writeInt(to.getId());
         p.writeByte(spawnPoint);
         p.writeShort(chr.getHp());
      }
      p.writeLong(CCommon.getTime(Server.getInstance()
            .getCurrentTime()));
      return p;
   }

   public static Packet getWarpToMap(MapleMap to, int spawnPoint, Point spawnPosition, MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.SET_FIELD);
      p.writeShort(0);// decode opt, loop with 2 decode 4s
      p.writeInt(chr.getClient()
            .getChannel() - 1);
      p.writeByte(0);
      p.writeInt(0);
      p.writeByte(0); // sNotifierMessage
      p.writeByte(0); // bCharacterData
      p.writeShort(0); // nNotifierCheck
      p.writeByte(0);// revive
      p.writeInt(to.getId());
      p.writeByte(spawnPoint);
      p.writeShort(chr.getHp());
      p.writeLong(CCommon.getTime(Server.getInstance()
            .getCurrentTime()));
      return p;
   }

   public static Packet openCashShop(MapleClient c, boolean mts) throws Exception {
      final OutPacket p = OutPacket.create(mts ? SendOpcode.SET_ITC : SendOpcode.SET_CASH_SHOP);

      addCharacterInfo(p, c.getPlayer());

      p.writeString(c.getAccountName());
      if (mts) {
         p.writeBytes(
               new byte[]{(byte) 0x88, 19, 0, 0, 7, 0, 0, 0, (byte) 0xF4, 1, 0, 0, (byte) 0x18, 0, 0, 0, (byte) 0xA8, 0, 0, 0, (byte) 0x70, (byte) 0xAA, (byte) 0xA7, (byte) 0xC5, (byte) 0x4E, (byte) 0xC1, (byte) 0xCA, 1});
      } else {

         p.writeShort(0);
         //            java.util.List<CashShop.SpecialCashItem> lsci = CashShop.CashItemFactory.getSpecialCashItems();
         //            p.writeShort(lsci.size());//Guess what
         //            for (CashShop.SpecialCashItem sci : lsci) {
         //                p.writeInt(sci.getSN());
         //                p.writeInt(sci.getModifier());
         //                p.write(sci.getInfo());
         //            }

         p.writeShort(0);
         //TODO this is something
         //            int skipped1 = 0;
         //            p.writeShort(skipped1);
         //            for (int i = 0; i < skipped1; i++) {
         //                p.writeInt(0);
         //                p.writeAsciiString("");
         //            }

         p.writeByte(0);
         p.skip(1080);
         p.writeShort(0); // CCashShop::DecodeStock
         p.writeShort(0); // CCashShop::DecodeLimitedGoods
         p.writeByte(0);
      }
      return p;
   }

   private static void addCharacterInfo(OutPacket p, MapleCharacter chr) {
      p.writeLong(-1); // dbcharFlag
      p.writeByte(0); // something about SN, I believe this is size of list
      CCommon.addCharStats(p, chr);
      p.writeByte(BuddyProcessor.getInstance().getBuddyList(chr.getWorld(), chr.getId()).capacity());

      if (chr.getLinkedName() == null) {
         p.writeByte(0);
      } else {
         p.writeByte(1);
         p.writeString(chr.getLinkedName());
      }

      p.writeInt(chr.getMeso());

      p.writeInt(chr.getId());
      p.writeInt(chr.getDama());
      p.writeInt(0);

      addInventoryInfo(p, chr);
      addSkillInfo(p, chr);
      addQuestInfo(p, chr);
      addMiniGameInfo(p, chr);
      addRingInfo(p, chr);
      addTeleportInfo(p, chr);
      p.writeShort(0);
      addMonsterBookInfo(p, chr);
      p.writeShort(0); // quest info
      p.writeShort(0); // not sure here decode 2, loop decode 4 decode 2
   }

   private static void addNewYearInfo(OutPacket p, MapleCharacter chr) {
      Set<NewYearCardRecord> received = chr.getReceivedNewYearRecords();

      p.writeShort(received.size());
      for (NewYearCardRecord nyc : received) {
         CCommon.encodeNewYearCard(nyc, p);
      }
   }

   private static void addTeleportInfo(OutPacket p, MapleCharacter chr) {
      final List<Integer> tele = chr.getTrockMaps();
      final List<Integer> viptele = chr.getVipTrockMaps();
      for (int i = 0; i < 5; i++) {
         p.writeInt(tele.get(i));
      }
      for (int i = 0; i < 10; i++) {
         p.writeInt(viptele.get(i));
      }
   }

   private static void addMiniGameInfo(OutPacket p, MapleCharacter chr) {
      p.writeShort(0);
                /*for (int m = size; m > 0; m--) {//nexon does this :P
                 p.writeInt(0);
                 p.writeInt(0);
                 p.writeInt(0);
                 p.writeInt(0);
                 p.writeInt(0);
                 }*/
   }

   private static void addAreaInfo(OutPacket p, MapleCharacter chr) {
      Map<Short, String> areaInfos = chr.getAreaInfos();
      p.writeShort(areaInfos.size());
      for (Short area : areaInfos.keySet()) {
         p.writeShort(area);
         p.writeString(areaInfos.get(area));
      }
   }

   private static void addQuestInfo(OutPacket p, MapleCharacter chr) {
      List<MapleQuestStatus> started = chr.getStartedQuests();
      int startedSize = 0;
      for (MapleQuestStatus qs : started) {
         if (qs.getInfoNumber() > 0) {
            startedSize++;
         }
         startedSize++;
      }
      p.writeShort(startedSize);
      for (MapleQuestStatus qs : started) {
         p.writeShort(qs.getQuest()
               .getId());
         p.writeString(qs.getProgressData());

         short infoNumber = qs.getInfoNumber();
         if (infoNumber > 0) {
            MapleQuestStatus iqs = chr.getQuest(infoNumber);
            p.writeShort(infoNumber);
            p.writeString(iqs.getProgressData());
         }
      }
      p.writeShort(0);
      List<MapleQuestStatus> completed = chr.getCompletedQuests();
      p.writeShort(completed.size());
      for (MapleQuestStatus qs : completed) {
         p.writeShort(qs.getQuest()
               .getId());
         p.writeLong(CCommon.getTime(qs.getCompletionTime()));
      }
   }

   private static void addInventoryInfo(OutPacket p, MapleCharacter chr) {
      for (byte i = 1; i <= 5; i++) {
         byte limit = MapleInventoryType.getByType(i)
               .map(chr::getInventory)
               .map(MapleInventory::getSlotLimit)
               .orElse((byte) 0);
         p.writeByte(limit);
      }
      p.writeLong(CCommon.getTime(-2));
      MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
      Collection<Item> equippedC = iv.list();
      List<Item> equipped = new ArrayList<>(equippedC.size());
      List<Item> equippedCash = new ArrayList<>(equippedC.size());
      for (Item item : equippedC) {
         if (item.getPosition() <= -100) {
            equippedCash.add(item);
         } else {
            equipped.add(item);
         }
      }
      for (Item item : equipped) {    // equipped doesn't actually need sorting, thanks Pllsz
         CCommon.addItemInfo(p, item);
      }
      p.writeShort(0); // start of equip cash
      for (Item item : equippedCash) {
         CCommon.addItemInfo(p, item);
      }
      p.writeShort(0); // start of equip inventory
      for (Item item : chr.getInventory(MapleInventoryType.EQUIP)
            .list()) {
         CCommon.addItemInfo(p, item);
      }
      p.writeInt(0);
      for (Item item : chr.getInventory(MapleInventoryType.USE)
            .list()) {
         CCommon.addItemInfo(p, item);
      }
      p.writeByte(0);
      for (Item item : chr.getInventory(MapleInventoryType.SETUP)
            .list()) {
         CCommon.addItemInfo(p, item);
      }
      p.writeByte(0);
      for (Item item : chr.getInventory(MapleInventoryType.ETC)
            .list()) {
         CCommon.addItemInfo(p, item);
      }
      p.writeByte(0);
      for (Item item : chr.getInventory(MapleInventoryType.CASH)
            .list()) {
         CCommon.addItemInfo(p, item);
      }
      p.writeByte(0);
   }

   private static void addSkillInfo(OutPacket p, MapleCharacter chr) {
      Map<Skill, SkillEntry> skills = chr.getSkills();
      int skillsSize = skills.size();
      // We don't want to include any hidden skill in this, so subtract them from the size list and ignore them.
      for (Map.Entry<Skill, SkillEntry> skill : skills.entrySet()) {
         if (GameConstants.isHiddenSkills(skill.getKey()
               .id())) {
            skillsSize--;
         }
      }
      p.writeShort(skillsSize);
      for (Map.Entry<Skill, SkillEntry> skill : skills.entrySet()) {
         if (GameConstants.isHiddenSkills(skill.getKey()
               .id())) {
            continue;
         }
         p.writeInt(skill.getKey()
               .id());
         p.writeInt(skill.getValue().skillLevel());
         CCommon.addExpirationTime(p, skill.getValue().expiration());
         if (skill.getKey()
               .isFourthJob()) {
            p.writeInt(skill.getValue().masterLevel());
         }
      }
      p.writeShort(chr.getAllCooldowns()
            .size());
      for (PlayerCoolDownValueHolder cooling : chr.getAllCooldowns()) {
         p.writeInt(cooling.skillId);
         int timeLeft = (int) (cooling.length + cooling.startTime - System.currentTimeMillis());
         p.writeShort(timeLeft / 1000);
      }
   }

   private static void addMonsterBookInfo(OutPacket p, MapleCharacter chr) {
      p.writeInt(chr.getMonsterBookCover()); // cover

      p.writeByte(0);
      Map<Integer, Integer> cards = chr.getMonsterBook()
            .getCards();
      p.writeShort(cards.size());
      for (Map.Entry<Integer, Integer> all : cards.entrySet()) {
         p.writeShort(all.getKey() % 10000); // Id
         p.writeByte(all.getValue()); // Level
      }
   }

   private static void setLogutGiftConfig(MapleCharacter chr, OutPacket p) {
      p.writeInt(0);// bPredictQuit
      for (int i = 0; i < 3; i++) {
         p.writeInt(0);//
      }
   }

   private static void addRingInfo(OutPacket p, MapleCharacter chr) {
      p.writeShort(chr.getCrushRings()
            .size());
      for (MapleRing ring : chr.getCrushRings()) {
         p.writeInt(ring.getPartnerChrId());
         p.writeFixedString(CCommon.getRightPaddedStr(ring.getPartnerName(), '\0', 13));
         p.writeInt(ring.getRingId());
         p.writeInt(0);
         p.writeInt(ring.getPartnerRingId());
         p.writeInt(0);
      }
      p.writeShort(chr.getFriendshipRings()
            .size());
      for (MapleRing ring : chr.getFriendshipRings()) {
         p.writeInt(ring.getPartnerChrId());
         p.writeFixedString(CCommon.getRightPaddedStr(ring.getPartnerName(), '\0', 13));
         p.writeInt(ring.getRingId());
         p.writeInt(0);
         p.writeInt(ring.getPartnerRingId());
         p.writeInt(0);
         p.writeInt(ring.getItemId());
      }

      if (chr.getPartnerId() > 0) {
         p.writeShort(1);
         p.writeInt(chr.getRelationshipId());
         p.writeInt(chr.getGender() == 0 ? chr.getId() : chr.getPartnerId());
         p.writeInt(chr.getGender() == 0 ? chr.getPartnerId() : chr.getId());

         Optional<MapleRing> marriageRing = chr.getMarriageRing();
         p.writeShort((marriageRing.isPresent()) ? 3 : 1);
         p.writeInt(marriageRing.map(MapleRing::getItemId)
               .orElse(1112803));
         p.writeInt(marriageRing.map(MapleRing::getItemId)
               .orElse(1112803));
         String spouse = MapleCharacter.getNameById(chr.getPartnerId())
               .orElseThrow();
         p.writeFixedString(StringUtil.getRightPaddedStr(chr.getGender() == 0 ? chr.getName() : spouse, '\0', 13));
         p.writeFixedString(StringUtil.getRightPaddedStr(chr.getGender() == 0 ? spouse : chr.getName(), '\0', 13));
      } else {
         p.writeShort(0);
      }
   }
}
