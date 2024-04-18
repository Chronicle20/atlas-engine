package connection.packets;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import buddy.BuddyConstants;
import buddy.BuddyListEntry;
import client.BuddyRequestInfo;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.MapleFamilyEntitlement;
import client.MapleFamilyEntry;
import client.MapleMount;
import client.MapleQuestStatus;
import client.MapleStat;
import client.MonsterBook;
import client.SkillMacro;
import client.TemporaryStatType;
import client.TemporaryStatValue;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.ModifyInventory;
import client.newyear.NewYearCardRecord;
import connection.constants.BuddylistErrorMode;
import connection.constants.BuddylistMode;
import connection.constants.PartyOperationMode;
import connection.headers.SendOpcode;
import connection.constants.ShowStatusInfoMessageType;
import constants.game.GameConstants;
import constants.inventory.ItemConstants;
import door.DoorProcessor;
import net.packet.ByteBufOutPacket;
import net.packet.OutPacket;
import net.packet.Packet;
import net.server.Server;
import net.server.guild.MapleAlliance;
import net.server.guild.MapleGuild;
import net.server.guild.MapleGuildCharacter;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import net.server.world.PartyOperation;
import server.ItemInformationProvider;
import server.life.MobSkill;
import server.maps.AbstractMapleMapObject;
import server.maps.MapleDoor;
import server.maps.MapleDoorObject;
import server.maps.MapleHiredMerchant;
import server.maps.MaplePlayerShop;
import server.maps.MaplePlayerShopItem;
import tools.Pair;

public class CWvsContext {
   public static Packet modifyInventory(boolean updateTick, final List<ModifyInventory> mods) {
      final OutPacket p = OutPacket.create(SendOpcode.INVENTORY_OPERATION);
      p.writeBool(updateTick);
      p.writeByte(mods.size());
      //p.writeByte(0); v104 :)
      int addMovement = -1;
      for (ModifyInventory mod : mods) {
         p.writeByte(mod.getMode());
         p.writeByte(mod.getInventoryType());
         p.writeShort(mod.getMode() == 2 ? mod.getOldPosition() : mod.getPosition());
         switch (mod.getMode()) {
            case 0: {//add item
               CCommon.addItemInfo(p, mod.getItem(), true);
               break;
            }
            case 1: {//update quantity
               p.writeShort(mod.getQuantity());
               break;
            }
            case 2: {//move
               p.writeShort(mod.getPosition());
               if (mod.getPosition() < 0 || mod.getOldPosition() < 0) {
                  addMovement = mod.getOldPosition() < 0 ? 1 : 2;
               }
               break;
            }
            case 3: {//remove
               if (mod.getPosition() < 0) {
                  addMovement = 2;
               }
               break;
            }
         }
         mod.clear();
      }
      if (addMovement > -1) {
         p.writeByte(addMovement);
      }
      return p;
   }

   public static Packet updateInventorySlotLimit(int type, int newLimit) {
      final OutPacket p = OutPacket.create(SendOpcode.INVENTORY_GROW);
      p.writeByte(type);
      p.writeByte(newLimit);
      return p;
   }

   /**
    * Gets an update for specified stats.
    *
    * @param stats         The list of stats to update.
    * @param enableActions Allows actions after the update.
    * @param chr           The update target.
    * @return The stat update packet.
    */
   public static Packet updatePlayerStats(List<Pair<MapleStat, Integer>> stats, boolean enableActions, MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.STAT_CHANGED);
      p.writeByte(enableActions ? 1 : 0);
      int updateMask = 0;
      for (Pair<MapleStat, Integer> statupdate : stats) {
         updateMask |= statupdate.getLeft().getValue();
      }

      Comparator<Pair<MapleStat, Integer>> comparator = (o1, o2) -> {
         int val1 = o1.getLeft().getValue();
         int val2 = o2.getLeft().getValue();
         return (Integer.compare(val1, val2));
      };

      p.writeInt(updateMask);
      stats.stream().sorted(comparator).forEach(stat -> updatePlayerStat(p, chr, stat));
      return p;
   }

   public static Packet petStatUpdate(MapleCharacter chr) {
      // this actually does nothing... packet structure and stats needs to be uncovered

      final OutPacket p = OutPacket.create(SendOpcode.STAT_CHANGED);
      int mask = 0;
      mask |= MapleStat.PETSN.getValue();
      mask |= MapleStat.PETSN2.getValue();
      mask |= MapleStat.PETSN3.getValue();
      p.writeByte(0);
      p.writeInt(mask);
      MaplePet[] pets = chr.getPets();
      for (int i = 0; i < 3; i++) {
         if (pets[i] != null) {
            p.writeLong(pets[i].getUniqueId());
         } else {
            p.writeLong(0);
         }
      }
      p.writeByte(0);
      return p;
   }

   /**
    * It is important that statups is in the correct order (see declaration
    * order in TemporaryStatType) since this method doesn't do automagical
    * reordering.
    *
    * @param buffid
    * @param bufflength
    * @param statups
    * @return
    */
   //1F 00 00 00 00 00 03 00 00 40 00 00 00 E0 00 00 00 00 00 00 00 00 E0 01 8E AA 4F 00 00 C2 EB 0B E0 01 8E AA 4F 00 00 C2 EB 0B 0C 00 8E AA 4F 00 00 C2 EB 0B 44 02 8E AA 4F 00 00 C2 EB 0B 44 02 8E AA 4F 00 00 C2 EB 0B 00 00 E0 7A 1D 00 8E AA 4F 00 00 00 00 00 00 00 00 03
   public static Packet giveBuff(MapleCharacter character, int buffid, int bufflength,
                                 List<Pair<TemporaryStatType, TemporaryStatValue>> statups) {
      final OutPacket p = OutPacket.create(SendOpcode.GIVE_BUFF);
      CCommon.writeLongMask(p, statups);
      for (Pair<TemporaryStatType, TemporaryStatValue> statup : statups) {
         p.writeShort(statup.getRight().value());
         p.writeInt(buffid);
         p.writeInt(bufflength);
      }
      p.writeByte(0); // bDefenseAtt
      p.writeByte(0); // bDefenseState
      CCommon.getTemporaryStats(character).forEach(ts -> ts.EncodeForClient(p));
      p.writeShort(0);// tDelay
      return p;
   }

   public static Packet giveDebuff(List<Pair<MapleDisease, Integer>> statups, MobSkill skill) {
      final OutPacket p = OutPacket.create(SendOpcode.GIVE_BUFF);
      CCommon.writeLongMaskD(p, statups);
      for (Pair<MapleDisease, Integer> statup : statups) {
         p.writeShort(statup.getRight().shortValue());
         p.writeShort(skill.getSkillId());
         p.writeShort(skill.getSkillLevel());
         p.writeInt((int) skill.getDuration());
      }
      p.writeShort(0); // ??? wk charges have 600 here o.o
      p.writeShort(900);//Delay
      p.writeByte(1);
      return p;
   }

   public static Packet cancelBuff(List<TemporaryStatType> statups) {
      final OutPacket p = OutPacket.create(SendOpcode.CANCEL_BUFF);
      CCommon.writeLongMaskFromList(p, statups);
      p.writeByte(1);//?
      return p;
   }

   public static Packet cancelDebuff(long mask) {
      final OutPacket p = OutPacket.create(SendOpcode.CANCEL_BUFF);
      p.writeLong(0);
      p.writeLong(mask);
      p.writeByte(0);
      return p;
   }

   public static Packet aranGodlyStats() {
      final OutPacket p = OutPacket.create(SendOpcode.FORCED_STAT_SET);
      p.writeBytes(
            new byte[]{(byte) 0x1F, (byte) 0x0F, 0, 0, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0xFF, 0, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0x78, (byte) 0x8C});
      return p;
   }

   public static Packet resetForcedStats() {
      final OutPacket p = OutPacket.create(SendOpcode.FORCED_STAT_RESET);
      return p;
   }

   public static Packet updateSkill(int skillid, int level, int masterlevel, long expiration) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_SKILLS);
      p.writeByte(1);
      p.writeShort(1);
      p.writeInt(skillid);
      p.writeInt(level);
      p.writeInt(masterlevel);
      CCommon.addExpirationTime(p, expiration);
      p.writeByte(4);
      return p;
   }

   public static Packet giveFameResponse(int mode, String charname, int newfame) {
      final OutPacket p = OutPacket.create(SendOpcode.FAME_RESPONSE);
      p.writeByte(0);
      p.writeString(charname);
      p.writeByte(mode);
      p.writeShort(newfame);
      p.writeShort(0);
      return p;
   }

   /**
    * status can be: <br> 0: ok, use giveFameResponse<br> 1: the username is
    * incorrectly entered<br> 2: users under level 15 are unable to toggle with
    * fame.<br> 3: can't raise or drop fame anymore today.<br> 4: can't raise
    * or drop fame for this character for this month anymore.<br> 5: received
    * fame, use receiveFame()<br> 6: level of fame neither has been raised nor
    * dropped due to an unexpected error
    *
    * @param status
    * @return
    */
   public static Packet giveFameErrorResponse(int status) {
      final OutPacket p = OutPacket.create(SendOpcode.FAME_RESPONSE);
      p.writeByte(status);
      return p;
   }

   public static Packet receiveFame(int mode, String charnameFrom) {
      final OutPacket p = OutPacket.create(SendOpcode.FAME_RESPONSE);
      p.writeByte(5);
      p.writeString(charnameFrom);
      p.writeByte(mode);
      return p;
   }

   /**
    * Gets a packet telling the client to show an EXP increase.
    *
    * @param gain   The amount of EXP gained.
    * @param inChat In the chat box?
    * @param white  White text or yellow?
    * @return The exp gained packet.
    */
   public static Packet getShowExpGain(int gain, int equip, int party, boolean inChat, boolean white) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_INCREASE_EXP.getMessageType());
      p.writeBool(white);
      p.writeInt(gain);
      p.writeBool(inChat);
      p.writeInt(0); // bonus event exp
      p.writeByte(0); // third monster kill event
      p.writeByte(0); // RIP byte, this is always a 0
      p.writeInt(0); //wedding bonus
      if (0 > 0) {
         p.writeByte(0); // nPlayTimeHour
      }
      if (inChat) { // quest bonus rate stuff
         p.writeByte(0); // nQuestBonusRate
         if (0 > 0) {
            p.writeByte(0); // nQuestBonusRemainCount
         }
      }

      p.writeByte(0); //0 = party bonus, 100 = 1x Bonus EXP, 200 = 2x Bonus EXP
      p.writeInt(party); // party bonus
      p.writeInt(equip); //equip bonus
      p.writeInt(0); //Internet Cafe Bonus
      p.writeInt(0); //Rainbow Week Bonus
      p.writeInt(0); // nPartyExpRingExp
      p.writeInt(0); // nCakePieEventBonus
      return p;
   }

   /**
    * Gets a packet telling the client to show a fame gain.
    *
    * @param gain How many fame gained.
    * @return The meso gain packet.
    */
   public static Packet getShowFameGain(int gain) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_INCREASE_FAME.getMessageType());
      p.writeInt(gain);
      return p;
   }

   /**
    * Gets a packet telling the client to show a meso gain.
    *
    * @param gain How many mesos gained.
    * @return The meso gain packet.
    */
   public static Packet getShowMesoGain(int gain) {
      return getShowMesoGain(gain, false);
   }

   /**
    * Gets a packet telling the client to show a meso gain.
    *
    * @param gain   How many mesos gained.
    * @param inChat Show in the chat window?
    * @return The meso gain packet.
    */
   public static Packet getShowMesoGain(int gain, boolean inChat) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      if (!inChat) {
         //TODO does this actually work?
         p.writeByte(ShowStatusInfoMessageType.ON_DROP_PICK_UP.getMessageType());
         p.writeShort(1); //v83
      } else {
         p.writeByte(ShowStatusInfoMessageType.ON_INCREASE_MONEY.getMessageType());
      }
      p.writeInt(gain);
      p.writeShort(0);
      return p;
   }

   /**
    * Gets a packet telling the client to show an item gain.
    *
    * @param itemId   The ID of the item gained.
    * @param quantity The number of items gained.
    * @param inChat   Show in the chat window?
    * @return The item gain packet.
    */
   public static Packet getShowItemGain(int itemId, short quantity, boolean inChat) {
      final OutPacket p;
      if (inChat) {
         p = OutPacket.create(SendOpcode.SHOW_ITEM_GAIN_INCHAT);
         p.writeByte(3);
         p.writeByte(1);
         p.writeInt(itemId);
         p.writeInt(quantity);
      } else {
         p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
         p.writeShort(ShowStatusInfoMessageType.ON_DROP_PICK_UP.getMessageType());
         p.writeInt(itemId);
         p.writeInt(quantity);
         p.writeInt(0);
         p.writeInt(0);
      }
      return p;
   }

   /**
    * @param quest
    * @return
    */
   public static Packet forfeitQuest(short quest) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_QUEST_RECORD.getMessageType());
      p.writeShort(quest);
      p.writeByte(0);
      return p;
   }

   /**
    * @param quest
    * @return
    */
   public static Packet completeQuest(short quest, long time) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_QUEST_RECORD.getMessageType());
      p.writeShort(quest);
      p.writeByte(2);
      p.writeLong(CCommon.getTime(time));
      return p;
   }

   /**
    * @param quest
    * @param npc
    * @return
    */

   public static Packet updateQuestInfo(short quest, int npc) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_QUEST_INFO);
      p.writeByte(8); //0x0A in v95
      p.writeShort(quest);
      p.writeInt(npc);
      p.writeInt(0);
      return p;
   }

   public static Packet updateQuest(MapleCharacter chr, MapleQuestStatus qs, boolean infoUpdate) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_QUEST_RECORD.getMessageType());

      if (infoUpdate) {
         MapleQuestStatus iqs = chr.getQuest(qs.getInfoNumber());
         p.writeShort(iqs.getQuestID());
         p.writeByte(1);
         p.writeString(iqs.getProgressData());
      } else {
         p.writeShort(qs.getQuest().getId());
         p.writeByte(qs.getStatus().getId());
         p.writeString(qs.getProgressData());
      }
      p.writeLong(0);
      return p;
   }

   public static Packet getShowInventoryStatus(int mode) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_DROP_PICK_UP.getMessageType());
      p.writeByte(mode);
      p.writeInt(0);
      p.writeInt(0);
      return p;
   }

   public static Packet updateAreaInfo(int area, String info) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_QUEST_RECORD_EX.getMessageType()); //0x0B in v95
      p.writeShort(area);//infoNumber
      p.writeString(info);
      return p;
   }

   public static Packet getGPMessage(int gpChange) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_INCREASE_GUILD_POINT.getMessageType());
      p.writeInt(gpChange);
      return p;
   }

   public static Packet getItemMessage(int itemid) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_GIVE_BUFF.getMessageType());
      p.writeInt(itemid);
      return p;
   }

   public static Packet showInfoText(String text) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_SYSTEM.getMessageType());
      p.writeString(text);
      return p;
   }

   public static Packet getDojoInfo(String info) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_QUEST_RECORD_EX.getMessageType());
      p.writeBytes(new byte[]{(byte) 0xB7, 4});//QUEST ID f5
      p.writeString(info);
      return p;
   }

   public static Packet getDojoInfoMessage(String message) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_SYSTEM.getMessageType());
      p.writeString(message);
      return p;
   }

   public static Packet updateDojoStats(MapleCharacter chr, int belt) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_QUEST_RECORD_EX.getMessageType());
      p.writeBytes(new byte[]{(byte) 0xB7, 4}); //?
      p.writeString("pt=" + chr.getDojoPoints() + ";belt=" + belt + ";tuto=" + (chr.getFinishedDojoTutorial() ? "1" : "0"));
      return p;
   }

   public static Packet itemExpired(int itemid) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_CASH_ITEM_EXPIRE.getMessageType());
      p.writeInt(itemid);
      return p;
   }

   public static Packet bunnyPacket() {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_STATUS_INFO);
      p.writeByte(ShowStatusInfoMessageType.ON_SYSTEM.getMessageType());
      p.writeFixedString("Protect the Moon Bunny!!!");
      return p;
   }

   public static Packet noteSendMsg() {
      final OutPacket p = OutPacket.create(SendOpcode.MEMO_RESULT);
      p.writeByte(4);
      return p;
   }

   /*
    *  0 = Player online, use whisper
    *  1 = Check player's name
    *  2 = Receiver inbox full
    */
   public static Packet noteError(byte error) {
      final OutPacket p = OutPacket.create(SendOpcode.MEMO_RESULT);
      p.writeByte(5);
      p.writeByte(error);
      return p;
   }

   public static Packet showNotes(ResultSet notes, int count) throws SQLException {
      final OutPacket p = OutPacket.create(SendOpcode.MEMO_RESULT);
      p.writeByte(3);
      p.writeByte(count);
      for (int i = 0; i < count; i++) {
         p.writeInt(notes.getInt("id"));
         p.writeString(notes.getString("from") + " ");//Stupid nexon forgot space lol
         p.writeString(notes.getString("message"));
         p.writeLong(CCommon.getTime(notes.getLong("timestamp")));
         p.writeByte(notes.getByte("fame"));//FAME :D
         notes.next();
      }
      return p;
   }

   public static Packet trockRefreshMapList(MapleCharacter chr, boolean delete, boolean vip) {
      final OutPacket p = OutPacket.create(SendOpcode.MAP_TRANSFER_RESULT);
      p.writeByte(delete ? 2 : 3);
      if (vip) {
         p.writeByte(1);
         List<Integer> map = chr.getVipTrockMaps();
         for (int i = 0; i < 10; i++) {
            p.writeInt(map.get(i));
         }
      } else {
         p.writeByte(0);
         List<Integer> map = chr.getTrockMaps();
         for (int i = 0; i < 5; i++) {
            p.writeInt(map.get(i));
         }
      }
      return p;
   }

   public static Packet enableReport() { // thanks to snow
      final OutPacket p = OutPacket.create(SendOpcode.CLAIM_STATUS_CHANGED);
      p.writeByte(1);
      return p;
   }

   public static Packet updateMount(int charid, MapleMount mount, boolean levelup) {
      final OutPacket p = OutPacket.create(SendOpcode.SET_TAMING_MOB_INFO);
      p.writeInt(charid);
      p.writeInt(mount.getLevel());
      p.writeInt(mount.getExp());
      p.writeInt(mount.getTiredness());
      p.writeByte(levelup ? (byte) 1 : (byte) 0);
      return p;
   }

   public static Packet getShowQuestCompletion(int id) {
      final OutPacket p = OutPacket.create(SendOpcode.QUEST_CLEAR);
      p.writeShort(id);
      return p;
   }

   public static Packet hiredMerchantBox() {
      final OutPacket p = OutPacket.create(SendOpcode.ENTRUSTED_SHOP_CHECK_RESULT); // header.
      p.writeByte(0x07);
      return p;
   }

   public static Packet retrieveFirstMessage() {
      final OutPacket p = OutPacket.create(SendOpcode.ENTRUSTED_SHOP_CHECK_RESULT); // header.
      p.writeByte(0x09);
      return p;
   }

   public static Packet remoteChannelChange(byte ch) {
      final OutPacket p = OutPacket.create(SendOpcode.ENTRUSTED_SHOP_CHECK_RESULT); // header.
      p.writeByte(0x10);
      p.writeInt(0);//No idea yet
      p.writeByte(ch);
      return p;
   }

   public static Packet skillBookResult(MapleCharacter chr, int skillid, int maxlevel, boolean canuse, boolean success) {
      final OutPacket p = OutPacket.create(SendOpcode.SKILL_LEARN_ITEM_RESULT);
      p.writeInt(chr.getId());
      p.writeByte(1);
      p.writeInt(skillid);
      p.writeInt(maxlevel);
      p.writeByte(canuse ? 1 : 0);
      p.writeByte(success ? 1 : 0);
      return p;
   }

   public static Packet finishedSort(int inv) {
      final OutPacket p = OutPacket.create(SendOpcode.GATHER_ITEM_RESULT);
      p.writeByte(0);
      p.writeByte(inv);
      return p;
   }

   public static Packet finishedSort2(int inv) {
      final OutPacket p = OutPacket.create(SendOpcode.SORT_ITEM_RESULT);
      p.writeByte(0);
      p.writeByte(inv);
      return p;
   }

   /**
    * Sends a report response
    * <p>
    * Possible values for <code>mode</code>:<br> 0: You have succesfully
    * reported the user.<br> 1: Unable to locate the user.<br> 2: You may only
    * report users 10 times a day.<br> 3: You have been reported to the GM's by
    * a user.<br> 4: Your request did not go through for unknown reasons.
    * Please try again later.<br>
    *
    * @param mode The mode
    * @return Report Reponse packet
    */
   public static Packet reportResponse(byte mode) {
      final OutPacket p = OutPacket.create(SendOpcode.SUE_CHARACTER_RESULT);
      p.writeByte(mode);
      return p;
   }

   public static Packet sendMesoLimit() {
      final OutPacket p = OutPacket.create(SendOpcode.TRADE_MONEY_LIMIT); //Players under level 15 can only trade 1m per day
      return p;
   }

   public static Packet updateGender(MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.SET_GENDER);
      p.writeByte(chr.getGender());
      return p;
   }

   public static Packet BBSThreadList(ResultSet rs, int start) throws SQLException {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_BBS_PACKET);
      p.writeByte(0x06);
      if (!rs.last()) {
         p.writeByte(0);
         p.writeInt(0);
         p.writeInt(0);
         return p;
      }
      int threadCount = rs.getRow();
      if (rs.getInt("localthreadid") == 0) { //has a notice
         p.writeByte(1);
         addThread(p, rs);
         threadCount--; //one thread didn't count (because it's a notice)
      } else {
         p.writeByte(0);
      }
      if (!rs.absolute(start + 1)) { //seek to the thread before where we start
         rs.first(); //uh, we're trying to start at a place past possible
         start = 0;
      }
      p.writeInt(threadCount);
      p.writeInt(Math.min(10, threadCount - start));
      for (int i = 0; i < Math.min(10, threadCount - start); i++) {
         addThread(p, rs);
         rs.next();
      }
      return p;
   }

   public static Packet showThread(int localthreadid, ResultSet threadRS, ResultSet repliesRS) throws SQLException,
         RuntimeException {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_BBS_PACKET);
      p.writeByte(0x07);
      p.writeInt(localthreadid);
      p.writeInt(threadRS.getInt("postercid"));
      p.writeLong(CCommon.getTime(threadRS.getLong("timestamp")));
      p.writeString(threadRS.getString("name"));
      p.writeString(threadRS.getString("startpost"));
      p.writeInt(threadRS.getInt("icon"));
      if (repliesRS != null) {
         int replyCount = threadRS.getInt("replycount");
         p.writeInt(replyCount);
         int i;
         for (i = 0; i < replyCount && repliesRS.next(); i++) {
            p.writeInt(repliesRS.getInt("replyid"));
            p.writeInt(repliesRS.getInt("postercid"));
            p.writeLong(CCommon.getTime(repliesRS.getLong("timestamp")));
            p.writeString(repliesRS.getString("content"));
         }
         if (i != replyCount || repliesRS.next()) {
            throw new RuntimeException(String.valueOf(threadRS.getInt("threadid")));
         }
      } else {
         p.writeInt(0);
      }
      return p;
   }

   /**
    * @param chr
    * @return
    */
   public static Packet charInfo(MapleCharacter chr) {
      //3D 00 0A 43 01 00 02 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
      final OutPacket p = OutPacket.create(SendOpcode.CHAR_INFO);
      p.writeInt(chr.getId());
      p.writeByte(chr.getLevel());
      p.writeShort(chr.getJob().getId());
      p.writeShort(chr.getFame());
      p.writeByte(chr.getMarriageRing().isPresent() ? 1 : 0);
      String guildName = "";
      String allianceName = "";
      if (chr.getGuildId() > 0) {
         guildName = Server.getInstance().getGuild(chr.getGuildId()).map(MapleGuild::getName).orElse("");

         allianceName = chr.getGuild().map(MapleGuild::getAllianceId).flatMap(id -> Server.getInstance().getAlliance(id))
               .map(MapleAlliance::getName).orElse("");
      }
      p.writeString(guildName);
      p.writeString(allianceName);  // does not seem to work

      p.writeInt(0);
      p.writeInt(0);
      p.writeByte(0); // pMedalInfo

      boolean hasSummonedPets = Arrays.stream(chr.getPets()).filter(Objects::nonNull).anyMatch(MaplePet::isSummoned);
      p.writeBool(hasSummonedPets);

      // TODO this needs some refinement. I am not sure how it works with zero pets present.
      MaplePet[] pets = chr.getPets();
      Item inv = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -114);
      int petCount = 0;
      for (int i = 0; i < 3; i++) {
         if (pets[i] != null && pets[i].isSummoned()) {
            //                p.writeByte(pets[i].getUniqueId());
            p.writeInt(pets[i].getItemId()); // dwTemplateID
            p.writeString(pets[i].getName()); // sName
            p.writeByte(pets[i].getLevel()); // nLevel
            p.writeShort(pets[i].getCloseness()); // nTameness
            p.writeByte(pets[i].getFullness()); // nRepleteness
            //TODO temporarily enable pet skills. Need to programmatically ensure the user has these items.
            p.writeShort(0x01 | 0x02 | 0x04 | 0x8 | 0x10); // usPetSkill
            p.writeInt(inv != null ? inv.getItemId() : 0);
            petCount++;
         }
         if (i + 1 < 3 && pets[i + 1] != null) {
            p.writeByte(1);
         }
      }
      if (petCount > 0) {
         p.writeByte(0); //end of pets
      }

      Item mount;     //mounts can potentially crash the client if the player's level is not properly checked
      if (chr.getMount().isPresent() && (mount = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -18)) != null
            && ItemInformationProvider.getInstance().getEquipLevelReq(mount.getItemId()) <= chr.getLevel()) {
         p.writeByte(chr.getMount().map(MapleMount::getId).orElse(0));
         p.writeInt(chr.getMount().map(MapleMount::getLevel).orElse(0));
         p.writeInt(chr.getMount().map(MapleMount::getExp).orElse(0));
         p.writeInt(chr.getMount().map(MapleMount::getTiredness).orElse(0));
      } else {
         p.writeByte(0);
      }
      p.writeByte(chr.getCashShop().getWishList().size());
      for (int sn : chr.getCashShop().getWishList()) {
         p.writeInt(sn);
      }

      MonsterBook book = chr.getMonsterBook();
      p.writeInt(book.getBookLevel());
      p.writeInt(book.getNormalCard());
      p.writeInt(book.getSpecialCard());
      p.writeInt(book.getTotalCards());
      p.writeInt(chr.getMonsterBookCover() > 0 ? ItemInformationProvider.getInstance().getCardMobId(chr.getMonsterBookCover()) : 0);

      Item medal = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -49);
      if (medal != null) {
         p.writeInt(medal.getItemId());
      } else {
         p.writeInt(0);
      }
      ArrayList<Short> medalQuests = new ArrayList<>();
      List<MapleQuestStatus> completed = chr.getCompletedQuests();
      for (MapleQuestStatus qs : completed) {
         if (qs.getQuest().getId() >= 29000) { // && q.getQuest().getId() <= 29923
            medalQuests.add(qs.getQuest().getId());
         }
      }

      Collections.sort(medalQuests);
      p.writeShort(medalQuests.size());
      for (Short s : medalQuests) {
         p.writeShort(s);
      }

      List<Integer> chairs = new ArrayList<>();
      for (Item item : chr.getInventory(MapleInventoryType.SETUP).list()) {
         if (ItemConstants.isChair(item.getItemId())) {
            chairs.add(item.getItemId());
         }
      }
      p.writeInt(chairs.size());
      for (int itemid : chairs) {
         p.writeInt(itemid);
      }
      return p;
   }

   public static Packet partyCreated(MapleParty party, MaplePartyCharacter partyCharacter) {
      final OutPacket p = OutPacket.create(SendOpcode.PARTY_OPERATION);
      p.writeByte(8);
      p.writeInt(party.getId());

      Optional<MapleDoor> door = DoorProcessor.getInstance().getPartyDoors(partyCharacter.getWorld(), party.getMemberIds()).stream()
            .filter(d -> d.ownerId() == partyCharacter.getId()).findFirst();
      if (door.isEmpty()) {
         writeEmptyDoor(p);
         return p;
      }

      Optional<MapleDoorObject> mdo =
            DoorProcessor.getInstance().getDoorMapObject(door.get(), door.get()::targetId, door.get()::targetDoorId);
      if (mdo.isEmpty()) {
         writeEmptyDoor(p);
         return p;
      }

      p.writeInt(mdo.get().getTo().getId());
      p.writeInt(mdo.get().getFrom().getId());
      p.writeInt(mdo.get().getPosition().x);
      p.writeInt(mdo.get().getPosition().y);
      return p;
   }

   public static Packet partyInvite(MapleCharacter from) {
      final OutPacket p = OutPacket.create(SendOpcode.PARTY_OPERATION);
      p.writeByte(PartyOperationMode.INVITE.getMode());
      p.writeInt(from.getPartyId().orElse(-1));
      p.writeString(from.getName());
      p.writeInt(from.getLevel());
      p.writeInt(from.getJob().getId());
      p.writeByte(0);
      return p;
   }

   public static Packet partySearchInvite(MapleCharacter from) {
      final OutPacket p = OutPacket.create(SendOpcode.PARTY_OPERATION);
      p.writeByte(PartyOperationMode.INVITE.getMode());
      p.writeInt(from.getPartyId().orElse(-1));
      p.writeString("PS: " + from.getName());
      p.writeInt(from.getLevel());
      p.writeInt(from.getJob().getId());
      p.writeByte(0);
      return p;
   }

   /**
    * 10: A beginner can't create a party. 1/5/6/11/14/19: Your request for a
    * party didn't work due to an unexpected error. 12: Quit as leader of the
    * party. 13: You have yet to join a party.
    * 16: Already have joined a party. 17: The party you're trying to join is
    * already in full capacity. 19: Unable to find the requested character in
    * this channel. 25: Cannot kick another user in this map. 28/29: Leadership
    * can only be given to a party member in the vicinity. 30: Change leadership
    * only on same channel.
    *
    * @param message
    * @return
    */
   public static Packet partyStatusMessage(int message) {
      final OutPacket p = OutPacket.create(SendOpcode.PARTY_OPERATION);
      p.writeByte(message);
      return p;
   }

   /**
    * 21: Player is blocking any party invitations, 22: Player is taking care of
    * another invitation, 23: Player have denied request to the party.
    *
    * @param message
    * @param charname
    * @return
    */
   public static Packet partyStatusMessage(int message, String charname) {
      final OutPacket p = OutPacket.create(SendOpcode.PARTY_OPERATION);
      p.writeByte(message);
      p.writeString(charname);
      return p;
   }

   public static Packet updateParty(int forChannel, MapleParty party, PartyOperation op, MaplePartyCharacter target) {
      final OutPacket p = OutPacket.create(SendOpcode.PARTY_OPERATION);
      switch (op) {
         case DISBAND:
         case EXPEL:
         case LEAVE:
            p.writeByte(0x0C);
            p.writeInt(party.getId());
            p.writeInt(target.getId());
            if (op == PartyOperation.DISBAND) {
               p.writeByte(0);
               p.writeInt(party.getId());
            } else {
               p.writeByte(1);
               if (op == PartyOperation.EXPEL) {
                  p.writeByte(1);
               } else {
                  p.writeByte(0);
               }
               p.writeString(target.getName());
               addPartyStatus(forChannel, party, p, false);
            }
            break;
         case JOIN:
            p.writeByte(0xF);
            p.writeInt(party.getId());
            p.writeString(target.getName());
            addPartyStatus(forChannel, party, p, false);
            break;
         case SILENT_UPDATE:
         case LOG_ONOFF:
            p.writeByte(0x7);
            p.writeInt(party.getId());
            addPartyStatus(forChannel, party, p, false);
            break;
         case CHANGE_LEADER:
            p.writeByte(0x1B);
            p.writeInt(target.getId());
            p.writeByte(0);
            break;
      }
      return p;
   }

   public static Packet partyPortal(int townId, int targetId, int skillId, Point position) {
      final OutPacket p = OutPacket.create(SendOpcode.PARTY_OPERATION);
      p.writeShort(0x28);
      p.writeByte(0);
      p.writeInt(townId);
      p.writeInt(targetId);
      p.writePos(position);
      return p;
   }

   public static Packet updateBuddylist(Collection<BuddyListEntry> buddylist) {
      final OutPacket p = OutPacket.create(SendOpcode.BUDDYLIST);
      p.writeByte(BuddylistMode.RESET.getMode());
      p.writeByte((byte) buddylist.stream().filter(BuddyListEntry::visible).count());
      for (BuddyListEntry buddy : buddylist) {
         if (buddy.visible()) {
            p.writeInt(buddy.characterId());
            p.writeFixedString(CCommon.getRightPaddedStr(buddy.name(), '\0', 13));
            p.writeByte(0); // opposite status
            p.writeInt(buddy.channel() - 1);
            p.writeFixedString(CCommon.getRightPaddedStr(buddy.group(), '\0', 17));
         }
      }
      for (BuddyListEntry buddy : buddylist) {
         if (buddy.visible()) {
            p.writeInt(0); // InShop
         }
      }
      return p;
   }

   public static Packet buddylistMessage(BuddylistErrorMode message) {
      final OutPacket p = OutPacket.create(SendOpcode.BUDDYLIST);
      p.writeByte(message.getMode());
      return p;
   }

   public static Packet requestBuddylistAdd(int characterId, BuddyRequestInfo requestInfo) {
      final OutPacket p = OutPacket.create(SendOpcode.BUDDYLIST);
      p.writeByte(BuddylistMode.REQUEST_BUDDY_ADD.getMode());
      p.writeInt(requestInfo.characterId());
      p.writeString(requestInfo.characterName());
      p.writeInt(requestInfo.level());
      p.writeInt(requestInfo.jobId());

      p.writeInt(characterId);
      p.writeFixedString(CCommon.getRightPaddedStr(requestInfo.characterName(), '\0', 13));
      p.writeByte(0); // nFlag ? could be 3/4 in a is_online check
      p.writeInt(requestInfo.channelId() - 1); // nChannelID
      p.writeFixedString(CCommon.getRightPaddedStr(BuddyConstants.DEFAULT_GROUP, '\0', 17));
      p.writeByte(0); // InShop
      return p;
   }

   public static Packet updateBuddyChannel(int characterid, int channel) {
      final OutPacket p = OutPacket.create(SendOpcode.BUDDYLIST);
      p.writeByte(BuddylistMode.UPDATE_BUDDY_CHANNEL.getMode());
      p.writeInt(characterid);
      p.writeByte(0);
      p.writeInt(channel);
      return p;
   }

   public static Packet updateBuddyCapacity(int capacity) {
      final OutPacket p = OutPacket.create(SendOpcode.BUDDYLIST);
      p.writeByte(BuddylistMode.UPDATE_BUDDY_CAPACITY.getMode());
      p.writeByte(capacity);
      return p;
   }

   public static Packet showGuildInfo(MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x1A); //signature for showing guild info
      if (chr == null) { //show empty guild (used for leaving, expelled)
         p.writeByte(0);
         return p;
      }
      Optional<MapleGuild> g = chr.getMGC().flatMap(mgc -> chr.getClient().getWorldServer().getGuild(mgc));
      if (g.isEmpty()) { //failed to read from DB - don't show a guild
         p.writeByte(0);
         return p;
      }
      p.writeByte(1); //bInGuild
      p.writeInt(g.get().getId());
      p.writeString(g.get().getName());
      for (int i = 1; i <= 5; i++) {
         p.writeString(g.get().getRankTitle(i));
      }
      Collection<MapleGuildCharacter> members = g.get().getMembers();
      p.writeByte(members.size()); //then it is the size of all the members
      for (MapleGuildCharacter mgc : members) {//and each of their character ids o_O
         p.writeInt(mgc.getId());
      }
      for (MapleGuildCharacter mgc : members) {
         p.writeFixedString(CCommon.getRightPaddedStr(mgc.getName(), '\0', 13));
         p.writeInt(mgc.getJobId());
         p.writeInt(mgc.getLevel());
         p.writeInt(mgc.getGuildRank());
         p.writeInt(mgc.isOnline() ? 1 : 0);
         p.writeInt(g.get().getSignature());
         p.writeInt(mgc.getAllianceRank());
      }
      p.writeInt(g.get().getCapacity());
      p.writeShort(g.get().getLogoBG());
      p.writeByte(g.get().getLogoBGColor());
      p.writeShort(g.get().getLogo());
      p.writeByte(g.get().getLogoColor());
      p.writeString(g.get().getNotice());
      p.writeInt(g.get().getGP());
      p.writeInt(g.get().getAllianceId());
      return p;
   }

   public static Packet guildMemberOnline(int gid, int cid, boolean bOnline) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x3d);
      p.writeInt(gid);
      p.writeInt(cid);
      p.writeByte(bOnline ? 1 : 0);
      return p;
   }

   public static Packet guildInvite(int gid, String charName) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x05);
      p.writeInt(gid);
      p.writeString(charName);
      return p;
   }

   public static Packet createGuildMessage(String masterName, String guildName) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x3);
      p.writeInt(0);
      p.writeString(masterName);
      p.writeString(guildName);
      return p;
   }

   /**
    * Gets a Heracle/guild message packet.
    * <p>
    * Possible values for <code>code</code>:<br> 28: guild name already in use<br>
    * 31: problem in locating players during agreement<br> 33/40: already joined a guild<br>
    * 35: Cannot make guild<br> 36: problem in player agreement<br> 38: problem during forming guild<br>
    * 41: max number of players in joining guild<br> 42: character can't be found this channel<br>
    * 45/48: character not in guild<br> 52: problem in disbanding guild<br> 56: admin cannot make guild<br>
    * 57: problem in increasing guild size<br>
    *
    * @param code The response code.
    * @return The guild message packet.
    */
   public static Packet genericGuildMessage(byte code) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(code);
      return p;
   }

   /**
    * Gets a guild message packet appended with target name.
    * <p>
    * 53: player not accepting guild invites<br>
    * 54: player already managing an invite<br> 55: player denied an invite<br>
    *
    * @param code       The response code.
    * @param targetName The initial player target of the invitation.
    * @return The guild message packet.
    */
   public static Packet responseGuildMessage(byte code, String targetName) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(code);
      p.writeString(targetName);
      return p;
   }

   public static Packet newGuildMember(MapleGuildCharacter mgc) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x27);
      p.writeInt(mgc.getGuildId());
      p.writeInt(mgc.getId());
      p.writeFixedString(CCommon.getRightPaddedStr(mgc.getName(), '\0', 13));
      p.writeInt(mgc.getJobId());
      p.writeInt(mgc.getLevel());
      p.writeInt(mgc.getGuildRank()); //should be always 5 but whatevs
      p.writeInt(mgc.isOnline() ? 1 : 0); //should always be 1 too
      p.writeInt(1); //? could be guild signature, but doesn't seem to matter
      p.writeInt(3);
      return p;
   }

   //someone leaving, mode == 0x2c for leaving, 0x2f for expelled
   public static Packet memberLeft(MapleGuildCharacter mgc, boolean bExpelled) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(bExpelled ? 0x2f : 0x2c);
      p.writeInt(mgc.getGuildId());
      p.writeInt(mgc.getId());
      p.writeString(mgc.getName());
      return p;
   }

   //rank change
   public static Packet changeRank(MapleGuildCharacter mgc) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x40);
      p.writeInt(mgc.getGuildId());
      p.writeInt(mgc.getId());
      p.writeByte(mgc.getGuildRank());
      return p;
   }

   public static Packet guildNotice(int gid, String notice) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x44);
      p.writeInt(gid);
      p.writeString(notice);
      return p;
   }

   public static Packet guildMemberLevelJobUpdate(MapleGuildCharacter mgc) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x3C);
      p.writeInt(mgc.getGuildId());
      p.writeInt(mgc.getId());
      p.writeInt(mgc.getLevel());
      p.writeInt(mgc.getJobId());
      return p;
   }

   public static Packet rankTitleChange(int gid, String[] ranks) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x3E);
      p.writeInt(gid);
      for (int i = 0; i < 5; i++) {
         p.writeString(ranks[i]);
      }
      return p;
   }

   public static Packet guildDisband(int gid) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x32);
      p.writeInt(gid);
      p.writeByte(1);
      return p;
   }

   public static Packet guildQuestWaitingNotice(byte channel, int waitingPos) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x4C);
      p.writeByte(channel - 1);
      p.writeByte(waitingPos);
      return p;
   }

   public static Packet guildEmblemChange(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x42);
      p.writeInt(gid);
      p.writeShort(bg);
      p.writeByte(bgcolor);
      p.writeShort(logo);
      p.writeByte(logocolor);
      return p;
   }

   public static Packet guildCapacityChange(int gid, int capacity) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x3A);
      p.writeInt(gid);
      p.writeByte(capacity);
      return p;
   }

   public static Packet showGuildRanks(int npcid, ResultSet rs) throws SQLException {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x49);
      p.writeInt(npcid);
      if (!rs.last()) { //no guilds o.o
         p.writeInt(0);
         return p;
      }
      p.writeInt(rs.getRow()); //number of entries
      rs.beforeFirst();
      while (rs.next()) {
         p.writeString(rs.getString("name"));
         p.writeInt(rs.getInt("GP"));
         p.writeInt(rs.getInt("logo"));
         p.writeInt(rs.getInt("logoColor"));
         p.writeInt(rs.getInt("logoBG"));
         p.writeInt(rs.getInt("logoBGColor"));
      }
      return p;
   }

   public static Packet showPlayerRanks(int npcid, List<Pair<String, Integer>> worldRanking) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x49);
      p.writeInt(npcid);
      if (worldRanking.isEmpty()) {
         p.writeInt(0);
         return p;
      }
      p.writeInt(worldRanking.size());
      for (Pair<String, Integer> wr : worldRanking) {
         p.writeString(wr.getLeft());
         p.writeInt(wr.getRight());
         p.writeInt(0);
         p.writeInt(0);
         p.writeInt(0);
         p.writeInt(0);
      }
      return p;
   }

   public static Packet updateGP(int gid, int GP) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
      p.writeByte(0x48);
      p.writeInt(gid);
      p.writeInt(GP);
      return p;
   }

   public static Packet getAllianceInfo(MapleAlliance alliance) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x0C);
      p.writeByte(1);
      p.writeInt(alliance.getId());
      p.writeString(alliance.getName());
      for (int i = 1; i <= 5; i++) {
         p.writeString(alliance.getRankTitle(i));
      }
      p.writeByte(alliance.getGuilds().size());
      p.writeInt(alliance.getCapacity()); // probably capacity
      for (Integer guild : alliance.getGuilds()) {
         p.writeInt(guild);
      }
      p.writeString(alliance.getNotice());
      return p;
   }

   public static Packet updateAllianceInfo(MapleAlliance alliance, int world) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x0F);
      p.writeInt(alliance.getId());
      p.writeString(alliance.getName());
      for (int i = 1; i <= 5; i++) {
         p.writeString(alliance.getRankTitle(i));
      }
      p.writeByte(alliance.getGuilds().size());
      for (Integer guild : alliance.getGuilds()) {
         p.writeInt(guild);
      }
      p.writeInt(alliance.getCapacity()); // probably capacity
      p.writeShort(0);
      alliance.getGuilds().stream().map(id -> Server.getInstance().getGuild(id, world)).flatMap(Optional::stream)
            .forEach(g -> getGuildInfo(p, g));
      return p;
   }

   public static Packet getGuildAlliances(MapleAlliance alliance, int worldId) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x0D);
      p.writeInt(alliance.getGuilds().size());
      alliance.getGuilds().stream().map(id -> Server.getInstance().getGuild(id, worldId)).flatMap(Optional::stream)
            .forEach(g -> getGuildInfo(p, g));
      return p;
   }

   public static Packet addGuildToAlliance(MapleAlliance alliance, int newGuild, MapleClient c) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x12);
      p.writeInt(alliance.getId());
      p.writeString(alliance.getName());
      for (int i = 1; i <= 5; i++) {
         p.writeString(alliance.getRankTitle(i));
      }
      p.writeByte(alliance.getGuilds().size());
      for (Integer guild : alliance.getGuilds()) {
         p.writeInt(guild);
      }
      p.writeInt(alliance.getCapacity());
      p.writeString(alliance.getNotice());
      p.writeInt(newGuild);
      getGuildInfo(p, Server.getInstance().getGuild(newGuild, c.getWorld(), null).orElseThrow());
      return p;
   }

   public static Packet allianceMemberOnline(MapleCharacter mc, boolean online) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x0E);
      p.writeInt(mc.getGuild().map(MapleGuild::getAllianceId).orElse(0));
      p.writeInt(mc.getGuildId());
      p.writeInt(mc.getId());
      p.writeByte(online ? 1 : 0);
      return p;
   }

   public static Packet allianceNotice(int id, String notice) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x1C);
      p.writeInt(id);
      p.writeString(notice);
      return p;
   }

   public static Packet changeAllianceRankTitle(int alliance, String[] ranks) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x1A);
      p.writeInt(alliance);
      for (int i = 0; i < 5; i++) {
         p.writeString(ranks[i]);
      }
      return p;
   }

   public static Packet updateAllianceJobLevel(MapleCharacter mc) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x18);
      p.writeInt(mc.getGuild().map(MapleGuild::getAllianceId).orElse(0));
      p.writeInt(mc.getGuildId());
      p.writeInt(mc.getId());
      p.writeInt(mc.getLevel());
      p.writeInt(mc.getJob().getId());
      return p;
   }

   public static Packet removeGuildFromAlliance(MapleAlliance alliance, int expelledGuild, int worldId) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x10);
      p.writeInt(alliance.getId());
      p.writeString(alliance.getName());
      for (int i = 1; i <= 5; i++) {
         p.writeString(alliance.getRankTitle(i));
      }
      p.writeByte(alliance.getGuilds().size());
      for (Integer guild : alliance.getGuilds()) {
         p.writeInt(guild);
      }
      p.writeInt(alliance.getCapacity());
      p.writeString(alliance.getNotice());
      p.writeInt(expelledGuild);
      getGuildInfo(p, Server.getInstance().getGuild(expelledGuild, worldId, null).orElseThrow());
      p.writeByte(0x01);
      return p;
   }

   public static Packet disbandAlliance(int alliance) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x1D);
      p.writeInt(alliance);
      return p;
   }

   public static Packet allianceInvite(int allianceid, MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x03);
      p.writeInt(allianceid);
      p.writeString(chr.getName());
      p.writeShort(0);
      return p;
   }

   public static Packet sendShowInfo(int allianceid, int playerid) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x02);
      p.writeInt(allianceid);
      p.writeInt(playerid);
      return p;
   }

   private static Packet sendInvitation(int allianceid, int playerid, final String guildname) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x05);
      p.writeInt(allianceid);
      p.writeInt(playerid);
      p.writeString(guildname);
      return p;
   }

   private static Packet sendChangeGuild(int allianceid, int playerid, int guildid, int option) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x07);
      p.writeInt(allianceid);
      p.writeInt(guildid);
      p.writeInt(playerid);
      p.writeByte(option);
      return p;
   }

   private static Packet sendChangeLeader(int allianceid, int playerid, int victim) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x08);
      p.writeInt(allianceid);
      p.writeInt(playerid);
      p.writeInt(victim);
      return p;
   }

   private static Packet sendChangeRank(int allianceid, int playerid, int int1, byte byte1) {
      final OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
      p.writeByte(0x09);
      p.writeInt(allianceid);
      p.writeInt(playerid);
      p.writeInt(int1);
      p.writeInt(byte1);
      return p;
   }

   /**
    * Gets a packet to spawn a portal.
    *
    * @param townId   The ID of the town the portal goes to.
    * @param targetId The ID of the target.
    * @param pos      Where to put the portal.
    * @return The portal spawn packet.
    */
   public static Packet spawnPortal(int townId, int targetId, int skillId, Point pos) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_PORTAL);
      p.writeInt(townId);
      p.writeInt(targetId);
      p.writePos(pos);
      return p;
   }

   /**
    * Gets a packet to remove a door.
    *
    * @param ownerid The door's owner ID.
    * @param town
    * @return The remove door packet.
    */
   public static Packet removeDoor(int ownerid, boolean town) {
      final OutPacket p;
      if (town) {
         p = OutPacket.create(SendOpcode.SPAWN_PORTAL);
         p.writeInt(999999999);
         p.writeInt(999999999);
      } else {
         p = OutPacket.create(SendOpcode.REMOVE_DOOR);
         p.writeByte(0);
         p.writeInt(ownerid);
      }
      return p;
   }

   /**
    * Gets a server message packet.
    * <p>
    * Possible values for <code>type</code>:<br> 0: [Notice]<br> 1: Popup<br>
    * 2: Megaphone<br> 3: Super Megaphone<br> 4: Scrolling message at top<br>
    * 5: Pink Text<br> 6: Lightblue Text<br> 7: BroadCasting NPC
    *
    * @param type          The type of the notice.
    * @param channel       The channel this notice was sent on.
    * @param message       The message to convey.
    * @param servermessage Is this a scrolling ticker?
    * @return The server notice packet.
    */
   static Packet serverMessage(int type, int channel, String message, boolean servermessage, boolean megaEar, int npc) {
      final OutPacket p = OutPacket.create(SendOpcode.SERVERMESSAGE);
      p.writeByte(type);
      if (servermessage) {
         p.writeByte(1);
      }
      p.writeString(message);
      if (type == 3) {
         p.writeByte(channel - 1); // channel
         p.writeBool(megaEar);
      } else if (type == 6) {
         p.writeInt(0);
      } else if (type == 7) { // npc
         p.writeInt(npc);
      }
      return p;
   }

   /**
    * Sends the Gachapon green message when a user uses a gachapon ticket.
    *
    * @param item
    * @param town
    * @param player
    * @return
    */
   public static Packet gachaponMessage(Item item, String town, MapleCharacter player) {
      final OutPacket p = OutPacket.create(SendOpcode.SERVERMESSAGE);
      p.writeByte(0x0B);
      p.writeString(player.getName() + " : got a(n)");
      p.writeInt(0); //random?
      p.writeString(town);
      CCommon.addItemInfo(p, item, true);
      return p;
   }

   public static Packet itemMegaphone(String msg, boolean whisper, int channel, Item item) {
      final OutPacket p = OutPacket.create(SendOpcode.SERVERMESSAGE);
      p.writeByte(8);
      p.writeString(msg);
      p.writeByte(channel - 1);
      p.writeByte(whisper ? 1 : 0);
      if (item == null) {
         p.writeByte(0);
      } else {
         p.writeByte(item.getPosition());
         CCommon.addItemInfo(p, item, true);
      }
      return p;
   }

   public static Packet getMultiMegaphone(String[] messages, int channel, boolean showEar) {
      final OutPacket p = OutPacket.create(SendOpcode.SERVERMESSAGE);
      p.writeByte(0x0A);
      if (messages[0] != null) {
         p.writeString(messages[0]);
      }
      p.writeByte(messages.length);
      for (int i = 1; i < messages.length; i++) {
         if (messages[i] != null) {
            p.writeString(messages[i]);
         }
      }
      for (int i = 0; i < 10; i++) {
         p.writeByte(channel - 1);
      }
      p.writeByte(showEar ? 1 : 0);
      p.writeByte(1);
      return p;
   }

   public static Packet incubatorResult() {//lol
      final OutPacket p = OutPacket.create(SendOpcode.INCUBATOR_RESULT);
      p.skip(6);
      return p;
   }

   public static Packet owlOfMinerva(MapleClient c, int itemid,
                                     List<Pair<MaplePlayerShopItem, AbstractMapleMapObject>> hmsAvailable) {
      byte itemType = ItemConstants.getInventoryType(itemid).getType();

      final OutPacket p = OutPacket.create(SendOpcode.SHOP_SCANNER_RESULT); // header.
      p.writeByte(6);
      p.writeInt(0);
      p.writeInt(itemid);
      p.writeInt(hmsAvailable.size());
      for (Pair<MaplePlayerShopItem, AbstractMapleMapObject> hme : hmsAvailable) {
         MaplePlayerShopItem item = hme.getLeft();
         AbstractMapleMapObject mo = hme.getRight();

         if (mo instanceof MaplePlayerShop ps) {
            MapleCharacter owner = ps.getOwner();

            p.writeString(owner.getName());
            p.writeInt(owner.getMapId());
            p.writeString(ps.getDescription());
            p.writeInt(item.getBundles());
            p.writeInt(item.getItem().getQuantity());
            p.writeInt(item.getPrice());
            p.writeInt(owner.getId());
            p.writeByte(owner.getClient().getChannel() - 1);
         } else {
            MapleHiredMerchant hm = (MapleHiredMerchant) mo;

            p.writeString(hm.getOwner());
            p.writeInt(hm.getMapId());
            p.writeString(hm.getDescription());
            p.writeInt(item.getBundles());
            p.writeInt(item.getItem().getQuantity());
            p.writeInt(item.getPrice());
            p.writeInt(hm.getOwnerId());
            p.writeByte(hm.getChannel() - 1);
         }

         p.writeByte(itemType);
         if (itemType == MapleInventoryType.EQUIP.getType()) {
            CCommon.addItemInfo(p, item.getItem(), true);
         }
      }
      return p;
   }

   public static Packet getOwlOpen(List<Integer> owlLeaderboards) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOP_SCANNER_RESULT);
      p.writeByte(7);
      p.writeByte(owlLeaderboards.size());
      for (Integer i : owlLeaderboards) {
         p.writeInt(i);
      }

      return p;
   }

   // 0: Success
   // 1: The room is already closed.
   // 2: You can't enter the room due to full capacity.
   // 3: Other requests are being fulfilled this minute.
   // 4: You can't do it while you're dead.
   // 7: You are not allowed to trade other items at this point.
   // 17: You may not enter this store.
   // 18: The owner of the store is currently undergoing store maintenance. Please try again in a bit.
   // 23: This can only be used inside the Free Market.
   // default: This character is unable to do it.
   public static Packet getOwlMessage(int msg) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOP_LINK_RESULT);
      p.writeByte(msg); // depending on the byte sent, a different message is sent.
      return p;
   }

   public static Packet sendYellowTip(String tip) {
      final OutPacket p = OutPacket.create(SendOpcode.SET_WEEK_EVENT_MESSAGE);
      p.writeByte(0xFF);
      p.writeString(tip);
      p.writeShort(0);
      return p;
   }

   public static Packet catchMessage(int message) { // not done, I guess
      final OutPacket p = OutPacket.create(SendOpcode.BRIDLE_MOB_CATCH_FAIL);
      p.writeByte(message); // 1 = too strong, 2 = Elemental Rock
      p.writeInt(0);//Maybe itemid?
      p.writeInt(0);
      return p;
   }

   public static Packet addCard(boolean full, int cardid, int level) {
      final OutPacket p = OutPacket.create(SendOpcode.MONSTER_BOOK_SET_CARD);
      p.writeByte(full ? 0 : 1);
      p.writeInt(cardid);
      p.writeInt(level);
      return p;
   }

   public static Packet changeCover(int cardid) {
      final OutPacket p = OutPacket.create(SendOpcode.MONSTER_BOOK_SET_COVER);
      p.writeInt(cardid);
      return p;
   }

   public static Packet getEnergy(String info, int amount) {
      final OutPacket p = OutPacket.create(SendOpcode.SESSION_VALUE);
      p.writeString(info);
      p.writeString(Integer.toString(amount));
      return p;
   }

   public static Packet showPedigree(MapleFamilyEntry entry) {
      final OutPacket p = OutPacket.create(SendOpcode.FAMILY_CHART_RESULT);
      p.writeInt(entry.getChrId()); //ID of viewed player's pedigree, can't be leader?
      List<MapleFamilyEntry> superJuniors = new ArrayList<>(4);
      boolean hasOtherJunior = false;
      int entryCount = 2; //2 guaranteed, leader and self
      entryCount += Math.min(2, entry.getTotalSeniors());
      //needed since MaplePacketLittleEndianWriter doesn't have any seek functionality
      if (entry.getSenior() != null) {
         if (entry.getSenior().getJuniorCount() == 2) {
            entryCount++;
            hasOtherJunior = true;
         }
      }
      for (MapleFamilyEntry junior : entry.getJuniors()) {
         if (junior == null) {
            continue;
         }
         entryCount++;
         for (MapleFamilyEntry superJunior : junior.getJuniors()) {
            if (superJunior == null) {
               continue;
            }
            entryCount++;
            superJuniors.add(superJunior);
         }
      }
      //write entries
      boolean missingEntries =
            entryCount == 2; //pedigree requires at least 3 entries to show leader, might only have 2 if leader's juniors leave
      if (missingEntries) {
         entryCount++;
      }
      p.writeInt(entryCount); //player count
      addPedigreeEntry(p, entry.getFamily().getLeader());
      if (entry.getSenior() != null) {
         if (entry.getSenior().getSenior() != null) {
            addPedigreeEntry(p, entry.getSenior().getSenior());
         }
         addPedigreeEntry(p, entry.getSenior());
      }
      addPedigreeEntry(p, entry);
      if (hasOtherJunior) { //must be sent after own entry
         entry.getSenior().getOtherJunior(entry).ifPresent(oj -> addPedigreeEntry(p, oj));
      }
      if (missingEntries) {
         addPedigreeEntry(p, entry);
      }
      for (MapleFamilyEntry junior : entry.getJuniors()) {
         if (junior == null) {
            continue;
         }
         addPedigreeEntry(p, junior);
         for (MapleFamilyEntry superJunior : junior.getJuniors()) {
            if (superJunior != null) {
               addPedigreeEntry(p, superJunior);
            }
         }
      }
      p.writeInt(2 + superJuniors.size()); //member info count
      // 0 = total seniors, -1 = total members, otherwise junior count of ID
      p.writeInt(-1);
      p.writeInt(entry.getFamily().getTotalMembers());
      p.writeInt(0);
      p.writeInt(entry.getTotalSeniors()); //client subtracts provided seniors
      for (MapleFamilyEntry superJunior : superJuniors) {
         p.writeInt(superJunior.getChrId());
         p.writeInt(superJunior.getTotalJuniors());
      }
      p.writeInt(0); //another loop count (entitlements used)
      //p.writeInt(1); //entitlement index
      //p.writeInt(2); //times used
      p.writeShort(entry.getJuniorCount() >= 2 ? 0 : 2); //0 disables Add button (only if viewing own pedigree)
      return p;
   }

   public static Packet getFamilyInfo(MapleFamilyEntry f) {
      if (f == null) {
         return getEmptyFamilyInfo();
      }
      final OutPacket p = OutPacket.create(SendOpcode.FAMILY_INFO_RESULT);
      p.writeInt(f.getReputation()); // cur rep left
      p.writeInt(f.getTotalReputation()); // tot rep left
      p.writeInt(f.getTodaysRep()); // todays rep
      p.writeShort(f.getJuniorCount()); // juniors added
      p.writeShort(2); // juniors allowed
      p.writeShort(0); //Unknown
      p.writeInt(f.getFamily().getLeader().getChrId()); // Leader ID (Allows setting message)
      p.writeString(f.getFamily().getName());
      p.writeString(f.getFamily().getMessage()); //family message
      p.writeInt(MapleFamilyEntitlement.values().length); //Entitlement info count
      for (MapleFamilyEntitlement entitlement : MapleFamilyEntitlement.values()) {
         p.writeInt(entitlement.ordinal()); //ID
         p.writeInt(f.isEntitlementUsed(entitlement) ? 1 : 0); //Used count
      }
      return p;
   }

   private static Packet getEmptyFamilyInfo() {
      final OutPacket p = OutPacket.create(SendOpcode.FAMILY_INFO_RESULT);
      p.writeInt(0); // cur rep left
      p.writeInt(0); // tot rep left
      p.writeInt(0); // todays rep
      p.writeShort(0); // juniors added
      p.writeShort(2); // juniors allowed
      p.writeShort(0); //Unknown
      p.writeInt(0); // Leader ID (Allows setting message)
      p.writeString("");
      p.writeString(""); //family message
      p.writeInt(0);
      return p;
   }

   /**
    * Family Result Message
    * <p>
    * Possible values for <code>type</code>:<br>
    * 64: You cannot add this character as a junior.
    * 65: The name could not be found or is not online.
    * 66: You belong to the same family.
    * 67: You do not belong to the same family.<br>
    * 69: The character you wish to add as\r\na Junior must be in the same
    * map.<br>
    * 70: This character is already a Junior of another character.<br>
    * 71: The Junior you wish to add\r\nmust be at a lower rank.<br>
    * 72: The gap between you and your\r\njunior must be within 20 levels.<br>
    * 73: Another character has requested to add this character.\r\nPlease try
    * again later.<br>
    * 74: Another character has requested a summon.\r\nPlease try again
    * later.<br>
    * 75: The summons has failed. Your current location or state does not allow
    * a summons.<br>
    * 76: The family cannot extend more than 1000 generations from above and
    * below.<br>
    * 77: The Junior you wish to add\r\nmust be over Level 10.<br>
    * 78: You cannot add a Junior \r\nthat has requested to change worlds.<br>
    * 79: You cannot add a Junior \r\nsince you've requested to change
    * worlds.<br>
    * 80: Separation is not possible due to insufficient Mesos.\r\nYou will
    * need %d Mesos to\r\nseparate with a Senior.<br>
    * 81: Separation is not possible due to insufficient Mesos.\r\nYou will
    * need %d Mesos to\r\nseparate with a Junior.<br>
    * 82: The Entitlement does not apply because your level does not match the
    * corresponding area.<br>
    *
    * @param type The type
    * @return Family Result packet
    */
   public static Packet sendFamilyMessage(int type, int mesos) {
      final OutPacket p = OutPacket.create(SendOpcode.FAMILY_RESULT);
      p.writeInt(type);
      p.writeInt(mesos);
      return p;
   }

   public static Packet sendFamilyInvite(int playerId, String inviter) {
      final OutPacket p = OutPacket.create(SendOpcode.FAMILY_JOIN_REQUEST);
      p.writeInt(playerId);
      p.writeString(inviter);
      return p;
   }

   public static Packet sendFamilyJoinResponse(boolean accepted, String added) {
      final OutPacket p = OutPacket.create(SendOpcode.FAMILY_JOIN_REQUEST_RESULT);
      p.writeByte(accepted ? 1 : 0);
      p.writeString(added);
      return p;
   }

   public static Packet getSeniorMessage(String name) {
      final OutPacket p = OutPacket.create(SendOpcode.FAMILY_JOIN_ACCEPTED);
      p.writeString(name);
      p.writeInt(0);
      return p;
   }

   public static Packet loadFamily(MapleCharacter player) {
      final OutPacket p = OutPacket.create(SendOpcode.FAMILY_PRIVILEGE_LIST);
      p.writeInt(MapleFamilyEntitlement.values().length);
      for (int i = 0; i < MapleFamilyEntitlement.values().length; i++) {
         MapleFamilyEntitlement entitlement = MapleFamilyEntitlement.values()[i];
         p.writeByte(i <= 1 ? 1 : 2); //type
         p.writeInt(entitlement.getRepCost());
         p.writeInt(entitlement.getUsageLimit());
         p.writeString(entitlement.getName());
         p.writeString(entitlement.getDescription());
      }
      return p;
   }

   public static Packet sendGainRep(int gain, String from) {
      final OutPacket p = OutPacket.create(SendOpcode.FAMILY_REP_GAIN);
      p.writeInt(gain);
      p.writeString(from);
      return p;
   }

   public static Packet sendFamilyLoginNotice(String name, boolean loggedIn) {
      final OutPacket p = OutPacket.create(SendOpcode.FAMILY_NOTIFY_LOGIN_OR_LOGOUT);
      p.writeBool(loggedIn);
      p.writeString(name);
      return p;
   }

   public static Packet sendFamilySummonRequest(String familyName, String from) {
      final OutPacket p = OutPacket.create(SendOpcode.FAMILY_SUMMON_REQUEST);
      p.writeString(from);
      p.writeString(familyName);
      return p;
   }

   /**
    * Sends a "levelup" packet to the guild or family.
    * <p>
    * Possible values for <code>type</code>:<br> 0: <Family> ? has reached Lv.
    * ?.<br> - The Reps you have received from ? will be reduced in half. 1:
    * <Family> ? has reached Lv. ?.<br> 2: <Guild> ? has reached Lv. ?.<br>
    *
    * @param type The type
    * @return The "levelup" packet.
    */
   public static Packet levelUpMessage(int type, int level, String charname) {
      final OutPacket p = OutPacket.create(SendOpcode.NOTIFY_LEVELUP);
      p.writeByte(type);
      p.writeInt(level);
      p.writeString(charname);

      return p;
   }

   /**
    * Sends a "married" packet to the guild or family.
    * <p>
    * Possible values for <code>type</code>:<br> 0: <Guild ? is now married.
    * Please congratulate them.<br> 1: <Family ? is now married. Please
    * congratulate them.<br>
    *
    * @param type The type
    * @return The "married" packet.
    */
   public static Packet marriageMessage(int type, String charname) {
      final OutPacket p = OutPacket.create(SendOpcode.NOTIFY_MARRIAGE);
      p.writeByte(type);  // 0: guild, 1: family
      p.writeString("> " + charname); //To fix the stupid packet lol

      return p;
   }

   /**
    * Sends a "job advance" packet to the guild or family.
    * <p>
    * Possible values for <code>type</code>:<br> 0: <Guild ? has advanced to
    * a(an) ?.<br> 1: <Family ? has advanced to a(an) ?.<br>
    *
    * @param type The type
    * @return The "job advance" packet.
    */
   public static Packet jobMessage(int type, int job, String charname) {
      final OutPacket p = OutPacket.create(SendOpcode.NOTIFY_JOB_CHANGE);
      p.writeByte(type);
      p.writeInt(job); //Why fking int?
      p.writeString("> " + charname); //To fix the stupid packet lol

      return p;
   }

   /**
    * Sends a Avatar Super Megaphone packet.
    *
    * @param chr     The character name.
    * @param medal   The medal text.
    * @param channel Which channel.
    * @param itemId  Which item used.
    * @param message The message sent.
    * @param ear     Whether or not the ear is shown for whisper.
    * @return
    */
   public static Packet getAvatarMega(MapleCharacter chr, String medal, int channel, int itemId, List<String> message,
                                      boolean ear) {
      final OutPacket p = OutPacket.create(SendOpcode.SET_AVATAR_MEGAPHONE);
      p.writeInt(itemId);
      p.writeString(medal + chr.getName());
      for (String s : message) {
         p.writeString(s);
      }
      p.writeInt(channel - 1); // channel
      p.writeBool(ear);
      CCommon.addCharLook(p, chr, true);
      return p;
   }

   /*
    * Sends a packet to remove the tiger megaphone
    * @return
    */
   public static Packet byeAvatarMega() {
      final OutPacket p = OutPacket.create(SendOpcode.CLEAR_AVATAR_MEGAPHONE);
      p.writeByte(1);
      return p;
   }

   public static Packet showNameChangeCancel(boolean success) {
      final OutPacket p = OutPacket.create(SendOpcode.CANCEL_NAME_CHANGE_RESULT);
      p.writeBool(success);
      if (!success) {
         p.writeByte(0);
      }
      //p.writeString("Custom message."); //only if ^ != 0
      return p;
   }

   public static Packet showWorldTransferCancel(boolean success) {
      final OutPacket p = OutPacket.create(SendOpcode.CANCEL_TRANSFER_WORLD_RESULT);
      p.writeBool(success);
      if (!success) {
         p.writeByte(0);
      }
      //p.writeString("Custom message."); //only if ^ != 0
      return p;
   }

   public static Packet sendPolice() {
      final OutPacket p = OutPacket.create(SendOpcode.FAKE_GM_NOTICE);
      p.writeByte(0);//doesn't even matter what value
      return p;
   }

   public static Packet onNewYearCardRes(MapleCharacter user, NewYearCardRecord newyear, int mode, int msg) {
      final OutPacket p = OutPacket.create(SendOpcode.NEW_YEAR_CARD_RES);
      p.writeByte(mode);
      switch (mode) {
         case 4: // Successfully sent a New Year Card\r\n to %s.
         case 6: // Successfully received a New Year Card.
            CCommon.encodeNewYearCard(newyear, p);
            break;

         case 8: // Successfully deleted a New Year Card.
            p.writeInt(newyear.getId());
            break;

         case 5: // Nexon's stupid and makes 4 modes do the same operation..
         case 7:
         case 9:
         case 0xB:
            // 0x10: You have no free slot to store card.\r\ntry later on please.
            // 0x11: You have no card to send.
            // 0x12: Wrong inventory information !
            // 0x13: Cannot find such character !
            // 0x14: Incoherent Data !
            // 0x15: An error occured during DB operation.
            // 0x16: An unknown error occured !
            // 0xF: You cannot send a card to yourself !
            p.writeByte(msg);
            break;

         case 0xA:   // GetUnreceivedList_Done
            int nSN = 1;
            p.writeInt(nSN);
            if ((nSN - 1) <= 98 && nSN > 0) {//lol nexon are you kidding
               for (int i = 0; i < nSN; i++) {
                  p.writeInt(newyear.getId());
                  p.writeInt(newyear.getSenderId());
                  p.writeString(newyear.getSenderName());
               }
            }
            break;

         case 0xC:   // NotiArrived
            p.writeInt(newyear.getId());
            p.writeString(newyear.getSenderName());
            break;

         case 0xD:   // BroadCast_AddCardInfo
            p.writeInt(newyear.getId());
            p.writeInt(user.getId());
            break;

         case 0xE:   // BroadCast_RemoveCardInfo
            p.writeInt(newyear.getId());
            break;
      }
      return p;
   }

   public static Packet setExtraPendantSlot(boolean toggleExtraSlot) {
      final OutPacket p = OutPacket.create(SendOpcode.SET_EXTRA_PENDANT_SLOT);
      p.writeBool(toggleExtraSlot);
      return p;
   }

   public static Packet earnTitleMessage(String msg) {
      final OutPacket p = OutPacket.create(SendOpcode.SCRIPT_PROGRESS_MESSAGE);
      p.writeString(msg);
      return p;
   }

   public static Packet sendPolice(String text) {
      final OutPacket p = OutPacket.create(SendOpcode.DATA_CRC_CHECK_FAILED);
      p.writeString(text);
      return p;
   }

   public static Packet getMacros(SkillMacro[] macros) {
      final OutPacket p = OutPacket.create(SendOpcode.MACRO_SYS_DATA_INIT);

      List<SkillMacro> macroList = Arrays.stream(macros).filter(Objects::nonNull).toList();
      p.writeByte(macroList.size());
      macroList.stream().map(CWvsContext::writeMacro).map(Packet::getBytes).forEach(p::writeBytes);
      return p;
   }

   public static Packet writeMacro(SkillMacro macro) {
      final OutPacket p = new ByteBufOutPacket();
      p.writeString(macro.name());
      p.writeByte(macro.shout());
      p.writeInt(macro.skill1());
      p.writeInt(macro.skill2());
      p.writeInt(macro.skill3());
      return p;
   }

   private static void addPedigreeEntry(OutPacket p, MapleFamilyEntry entry) {
      MapleCharacter chr = entry.getChr();
      boolean isOnline = chr != null;
      p.writeInt(entry.getChrId()); //ID
      p.writeInt(entry.getSenior() != null ? entry.getSenior().getChrId() : 0); //parent ID
      p.writeShort(entry.getJob().getId()); //job id
      p.writeByte(entry.getLevel()); //level
      p.writeBool(isOnline); //isOnline
      p.writeInt(entry.getReputation()); //current rep
      p.writeInt(entry.getTotalReputation()); //total rep
      p.writeInt(entry.getRepsToSenior()); //reps recorded to senior
      p.writeInt(entry.getTodaysRep());
      p.writeInt(isOnline ? ((chr.isAwayFromWorld() || chr.getCashShop().isOpened()) ? -1 : chr.getClient().getChannel() - 1) : 0);
      p.writeInt(isOnline ? (int) (chr.getLoggedInTime() / 60000) : 0); //time online in minutes
      p.writeString(entry.getName()); //name
   }

   private static void addPartyStatus(int forchannel, MapleParty party, OutPacket p, boolean leaving) {
      List<MaplePartyCharacter> partymembers = new ArrayList<>(party.getMembers());
      while (partymembers.size() < 6) {
         partymembers.add(new MaplePartyCharacter());
      }
      for (MaplePartyCharacter partychar : partymembers) {
         p.writeInt(partychar.getId());
      }
      for (MaplePartyCharacter partychar : partymembers) {
         p.writeFixedString(CCommon.getRightPaddedStr(partychar.getName(), '\0', 13));
      }
      for (MaplePartyCharacter partychar : partymembers) {
         p.writeInt(partychar.getJobId());
      }
      for (MaplePartyCharacter partychar : partymembers) {
         p.writeInt(partychar.getLevel());
      }
      for (MaplePartyCharacter partychar : partymembers) {
         if (partychar.isOnline()) {
            p.writeInt(partychar.getChannel() - 1);
         } else {
            p.writeInt(-2);
         }
      }
      p.writeInt(party.getLeader().getId());
      for (MaplePartyCharacter partychar : partymembers) {
         if (partychar.getChannel() == forchannel) {
            p.writeInt(partychar.getMapId());
         } else {
            p.writeInt(0);
         }
      }

      List<MapleDoor> doors = DoorProcessor.getInstance().getPartyDoors(party.getLeader().getWorld(), party.getMemberIds());

      for (MaplePartyCharacter partychar : partymembers) {
         if (partychar.getChannel() != forchannel || leaving) {
            writeEmptyDoor(p);
            continue;
         }

         if (doors.isEmpty()) {
            writeEmptyDoor(p);
            continue;
         }

         Optional<MapleDoor> door = doors.stream().filter(d -> d.ownerId() == partychar.getId()).findFirst();
         if (door.isEmpty()) {
            writeEmptyDoor(p);
            continue;
         }

         Optional<MapleDoorObject> mdo =
               DoorProcessor.getInstance().getDoorMapObject(door.get(), door.get()::townId, door.get()::townDoorId);
         if (mdo.isEmpty()) {
            writeEmptyDoor(p);
            continue;
         }

         p.writeInt(mdo.get().getTown().getId());
         p.writeInt(mdo.get().getArea().getId());
         p.writeInt(mdo.get().toPosition().x);
         p.writeInt(mdo.get().toPosition().y);
      }
   }

   private static void writeEmptyDoor(OutPacket p) {
      p.writeInt(999999999);
      p.writeInt(999999999);
      p.writeInt(0);
      p.writeInt(0);
   }

   private static void getGuildInfo(OutPacket p, MapleGuild guild) {
      p.writeInt(guild.getId());
      p.writeString(guild.getName());
      for (int i = 1; i <= 5; i++) {
         p.writeString(guild.getRankTitle(i));
      }
      Collection<MapleGuildCharacter> members = guild.getMembers();
      p.writeByte(members.size());
      for (MapleGuildCharacter mgc : members) {
         p.writeInt(mgc.getId());
      }
      for (MapleGuildCharacter mgc : members) {
         p.writeFixedString(CCommon.getRightPaddedStr(mgc.getName(), '\0', 13));
         p.writeInt(mgc.getJobId());
         p.writeInt(mgc.getLevel());
         p.writeInt(mgc.getGuildRank());
         p.writeInt(mgc.isOnline() ? 1 : 0);
         p.writeInt(guild.getSignature());
         p.writeInt(mgc.getAllianceRank());
      }
      p.writeInt(guild.getCapacity());
      p.writeShort(guild.getLogoBG());
      p.writeByte(guild.getLogoBGColor());
      p.writeShort(guild.getLogo());
      p.writeByte(guild.getLogoColor());
      p.writeString(guild.getNotice());
      p.writeInt(guild.getGP());
      p.writeInt(guild.getAllianceId());
   }

   private static void updatePlayerStat(OutPacket p, MapleCharacter chr, Pair<MapleStat, Integer> statupdate) {
      switch (statupdate.getLeft()) {
         case SKIN:
         case LEVEL:
            p.writeByte(statupdate.getRight().byteValue());
            break;
         case JOB:
         case STR:
         case DEX:
         case INT:
         case LUK:
         case HP:
         case MAXHP:
         case MP:
         case MAXMP:
         case AVAILABLEAP:
         case FAME:
            p.writeShort(statupdate.getRight().shortValue());
            break;
         case AVAILABLESP:
            if (GameConstants.hasExtendedSPTable(chr.getJob())) {
               //TODO Evan
               //                    p.writeByte(chr.getRemainingSpSize());
               //                    for(int i = 0; i < chr.getRemainingSps().length; i++){
               //                        if(chr.getRemainingSpBySkill(i) > 0){
               //                            p.writeByte(i);
               //                            p.writeByte(chr.getRemainingSpBySkill(i));
               //                        }
               //                    }
            } else {
               p.writeShort(statupdate.getRight().shortValue());
            }
            break;
         case FACE:
         case HAIR:
         case EXP:
         case MESO:
         case GACHAEXP:
            p.writeInt(statupdate.getRight());
            break;
         case PETSN, PETSN2, PETSN3:
            p.writeLong(0);
            break;
      }
   }

   /**
    * Gets a server message packet.
    *
    * @param message The message to convey.
    * @return The server message packet.
    */
   public static Packet serverMessage(String message) {
      return serverMessage(4, (byte) 0, message, true, false, 0);
   }

   /**
    * Gets a server notice packet.
    * <p>
    * Possible values for <code>type</code>:<br> 0: [Notice]<br> 1: Popup<br>
    * 2: Megaphone<br> 3: Super Megaphone<br> 4: Scrolling message at top<br>
    * 5: Pink Text<br> 6: Lightblue Text
    *
    * @param type    The type of the notice.
    * @param message The message to convey.
    * @return The server notice packet.
    */
   public static Packet serverNotice(int type, String message) {
      return serverMessage(type, (byte) 0, message, false, false, 0);
   }

   /**
    * Gets a server notice packet.
    * <p>
    * Possible values for <code>type</code>:<br> 0: [Notice]<br> 1: Popup<br>
    * 2: Megaphone<br> 3: Super Megaphone<br> 4: Scrolling message at top<br>
    * 5: Pink Text<br> 6: Lightblue Text
    *
    * @param type    The type of the notice.
    * @param message The message to convey.
    * @return The server notice packet.
    */
   public static Packet serverNotice(int type, String message, int npc) {
      return serverMessage(type, 0, message, false, false, npc);
   }

   public static Packet serverNotice(int type, int channel, String message) {
      return serverMessage(type, channel, message, false, false, 0);
   }

   public static Packet serverNotice(int type, int channel, String message, boolean smegaEar) {
      return serverMessage(type, channel, message, false, smegaEar, 0);
   }

   /**
    * Gets a packet telling the client to show a item gain.
    *
    * @param itemId   The ID of the item gained.
    * @param quantity How many items gained.
    * @return The item gain packet.
    */
   public static Packet getShowItemGain(int itemId, short quantity) {
      return getShowItemGain(itemId, quantity, false);
   }

   public static Packet onNewYearCardRes(MapleCharacter user, int cardId, int mode, int msg) {
      NewYearCardRecord newyear = user.getNewYearRecord(cardId).orElseThrow();
      return onNewYearCardRes(user, newyear, mode, msg);
   }

   public static void addThread(OutPacket p, ResultSet rs) throws SQLException {
      p.writeInt(rs.getInt("localthreadid"));
      p.writeInt(rs.getInt("postercid"));
      p.writeString(rs.getString("name"));
      p.writeLong(CCommon.getTime(rs.getLong("timestamp")));
      p.writeInt(rs.getInt("icon"));
      p.writeInt(rs.getInt("replycount"));
   }

   public static Packet getInventoryFull() {
      return modifyInventory(true, Collections.emptyList());
   }

   public static Packet getShowInventoryFull() {
      return getShowInventoryStatus(0xff);
   }

   public static Packet showItemUnavailable() {
      return getShowInventoryStatus(0xfe);
   }

   /**
    * Gets an empty stat update.
    *
    * @return The empty stat update packet.
    */
   public static Packet enableActions() {
      return updatePlayerStats(Collections.emptyList(), true, null);
   }
}
