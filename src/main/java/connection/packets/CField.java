package connection.packets;

import java.util.List;
import java.util.Map;
import java.util.Set;

import client.MapleCharacter;
import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import server.MTSItemInfo;
import tools.Pair;

public class CField {
   /**
    * Gets a "block" packet (ie. the cash shop is unavailable, etc)
    * <p>
    * Possible values for <code>type</code>:<br> 1: You cannot move that
    * channel. Please try again later.<br> 2: You cannot go into the cash shop.
    * Please try again later.<br> 3: The Item-Trading Shop is currently
    * unavailable. Please try again later.<br> 4: You cannot go into the trade
    * shop, due to limitation of user count.<br> 5: You do not meet the minimum
    * level requirement to access the Trade Shop.<br>
    *
    * @param type The type
    * @return The "block" packet.
    */
   public static Packet blockedMessage2(int type) {
      final OutPacket p = OutPacket.create(SendOpcode.BLOCKED_SERVER);
      p.writeByte(type);
      return p;
   }

   public static Packet showForcedEquip(int team) {
      final OutPacket p = OutPacket.create(SendOpcode.FORCED_MAP_EQUIP);
      if (team > -1) {
         p.writeByte(team);   // 00 = red, 01 = blue
      }
      return p;
   }

   /**
    * mode: 0 buddychat; 1 partychat; 2 guildchat
    *
    * @param name
    * @param chattext
    * @param mode
    * @return
    */
   public static Packet multiChat(String name, String chattext, int mode) {
      final OutPacket p = OutPacket.create(SendOpcode.MULTICHAT);
      p.writeByte(mode);
      p.writeString(name);
      p.writeString(chattext);
      return p;
   }

   public static Packet getWhisper(String sender, int channel, String text) {
      final OutPacket p = OutPacket.create(SendOpcode.WHISPER);
      p.writeByte(0x12);
      p.writeString(sender);
      p.writeShort(channel - 1); // I guess this is the channel
      p.writeString(text);
      return p;
   }

   /**
    * @param target name of the target character
    * @param reply  error code: 0x0 = cannot find char, 0x1 = success
    * @return the MaplePacket
    */
   public static Packet getWhisperReply(String target, byte reply) {
      final OutPacket p = OutPacket.create(SendOpcode.WHISPER);
      p.writeByte(0x0A); // whisper?
      p.writeString(target);
      p.writeByte(reply);
      return p;
   }

   /**
    * @param target
    * @param mapid
    * @param MTSmapCSchannel 0: MTS 1: Map 2: CS 3: Different Channel
    * @return
    */
   public static Packet getFindReply(String target, int mapid, int MTSmapCSchannel) {
      final OutPacket p = OutPacket.create(SendOpcode.WHISPER);
      p.writeByte(9);
      p.writeString(target);
      p.writeByte(MTSmapCSchannel); // 0: mts 1: map 2: cs
      p.writeInt(mapid); // -1 if mts, cs
      if (MTSmapCSchannel == 1) {
         p.writeBytes(new byte[8]);
      }
      return p;
   }

   /**
    * @param target
    * @param mapid
    * @param MTSmapCSchannel 0: MTS 1: Map 2: CS 3: Different Channel
    * @return
    */
   public static Packet getBuddyFindReply(String target, int mapid, int MTSmapCSchannel) {
      final OutPacket p = OutPacket.create(SendOpcode.WHISPER);
      p.writeByte(72);
      p.writeString(target);
      p.writeByte(MTSmapCSchannel); // 0: mts 1: map 2: cs
      p.writeInt(mapid); // -1 if mts, cs
      if (MTSmapCSchannel == 1) {
         p.writeBytes(new byte[8]);
      }
      return p;
   }

   public static Packet OnCoupleMessage(String fiance, String text, boolean spouse) {
      final OutPacket p = OutPacket.create(SendOpcode.SPOUSE_CHAT);
      p.writeByte(spouse ? 5 : 4); // v2 = CInPacket::Decode1(a1) - 4;
      if (spouse) { // if ( v2 ) {
         p.writeString(fiance);
      }
      p.writeByte(spouse ? 5 : 1);
      p.writeString(text);
      return p;
   }

   public static Packet showBossHP(int oid, int currHP, int maxHP, byte tagColor, byte tagBgColor) {
      final OutPacket p = OutPacket.create(SendOpcode.FIELD_EFFECT);
      p.writeByte(5);
      p.writeInt(oid);
      p.writeInt(currHP);
      p.writeInt(maxHP);
      p.writeByte(tagColor);
      p.writeByte(tagBgColor);
      return p;
   }

   public static Packet customShowBossHP(byte call, int oid, long currHP, long maxHP, byte tagColor, byte tagBgColor) {
      Pair<Integer, Integer> customHP = normalizedCustomMaxHP(currHP, maxHP);

      final OutPacket p = OutPacket.create(SendOpcode.FIELD_EFFECT);
      p.writeByte(call);
      p.writeInt(oid);
      p.writeInt(customHP.left);
      p.writeInt(customHP.right);
      p.writeByte(tagColor);
      p.writeByte(tagBgColor);
      return p;
   }

   public static Packet environmentChange(String env, int mode) {
      final OutPacket p = OutPacket.create(SendOpcode.FIELD_EFFECT);
      p.writeByte(mode);
      p.writeString(env);
      return p;
   }

   public static Packet mapEffect(String path) {
      final OutPacket p = OutPacket.create(SendOpcode.FIELD_EFFECT);
      p.writeByte(3);
      p.writeString(path);
      return p;
   }

   public static Packet mapSound(String path) {
      final OutPacket p = OutPacket.create(SendOpcode.FIELD_EFFECT);
      p.writeByte(4);
      p.writeString(path);
      return p;
   }

   public static Packet sendDojoAnimation(byte firstByte, String animation) {
      final OutPacket p = OutPacket.create(SendOpcode.FIELD_EFFECT);
      p.writeByte(firstByte);
      p.writeString(animation);
      return p;
   }

   /**
    * @param type  - (0:Light&Long 1:Heavy&Short)
    * @param delay - seconds
    * @return
    */
   public static Packet trembleEffect(int type, int delay) {
      final OutPacket p = OutPacket.create(SendOpcode.FIELD_EFFECT);
      p.writeByte(1);
      p.writeByte(type);
      p.writeInt(delay);
      return p;
   }

   public static Packet environmentMove(String env, int mode) {
      final OutPacket p = OutPacket.create(SendOpcode.FIELD_OBSTACLE_ONOFF);
      p.writeString(env);
      p.writeInt(mode);   // 0: stop and back to start, 1: move

      return p;
   }

   public static Packet environmentMoveList(Set<Map.Entry<String, Integer>> envList) {
      final OutPacket p = OutPacket.create(SendOpcode.FIELD_OBSTACLE_ONOFF_LIST);
      p.writeInt(envList.size());

      for (Map.Entry<String, Integer> envMove : envList) {
         p.writeString(envMove.getKey());
         p.writeInt(envMove.getValue());
      }

      return p;
   }

   public static Packet environmentMoveReset() {
      final OutPacket p = OutPacket.create(SendOpcode.FIELD_OBSTACLE_ALL_RESET);
      return p;
   }

   public static Packet startMapEffect(String msg, int itemid, boolean active) {
      final OutPacket p = OutPacket.create(SendOpcode.BLOW_WEATHER);
      p.writeByte(active ? 0 : 1);
      p.writeInt(itemid);
      if (active) {
         p.writeString(msg);
      }
      return p;
   }

   public static Packet removeMapEffect() {
      final OutPacket p = OutPacket.create(SendOpcode.BLOW_WEATHER);
      p.writeByte(0);
      p.writeInt(0);
      return p;
   }

   public static Packet hpqMessage(String text) {
      final OutPacket p = OutPacket.create(SendOpcode.BLOW_WEATHER); // not 100% sure
      p.writeByte(0);
      p.writeInt(5120016);
      p.writeFixedString(text);
      return p;
   }

   /**
    * Gets a gm effect packet (ie. hide, banned, etc.)
    * <p>
    * Possible values for <code>type</code>:<br> 0x04: You have successfully
    * blocked access.<br>
    * 0x05: The unblocking has been successful.<br> 0x06 with Mode 0: You have
    * successfully removed the name from the ranks.<br> 0x06 with Mode 1: You
    * have entered an invalid character name.<br> 0x10: GM Hide, mode
    * determines whether or not it is on.<br> 0x1E: Mode 0: Failed to send
    * warning Mode 1: Sent warning<br> 0x13 with Mode 0: + mapid 0x13 with Mode
    * 1: + ch (FF = Unable to find merchant)
    *
    * @param type The type
    * @param mode The mode
    * @return The gm effect packet
    */
   public static Packet getGMEffect(int type, byte mode) {
      final OutPacket p = OutPacket.create(SendOpcode.ADMIN_RESULT);
      p.writeByte(type);
      p.writeByte(mode);
      return p;
   }

   public static Packet findMerchantResponse(boolean map, int extra) {
      final OutPacket p = OutPacket.create(SendOpcode.ADMIN_RESULT);
      p.writeByte(0x13);
      p.writeByte(map ? 0 : 1); //00 = mapid, 01 = ch
      if (map) {
         p.writeInt(extra);
      } else {
         p.writeByte(extra); //-1 = unable to find
      }
      p.writeByte(0);
      return p;
   }

   public static Packet disableMinimap() {
      final OutPacket p = OutPacket.create(SendOpcode.ADMIN_RESULT);
      p.writeShort(0x1C);
      return p;
   }

   public static Packet showOXQuiz(int questionSet, int questionId, boolean askQuestion) {
      final OutPacket p = OutPacket.create(SendOpcode.OX_QUIZ);
      p.writeByte(askQuestion ? 1 : 0);
      p.writeByte(questionSet);
      p.writeShort(questionId);
      return p;
   }

   public static Packet showEventInstructions() {
      final OutPacket p = OutPacket.create(SendOpcode.GMEVENT_INSTRUCTIONS);
      p.writeByte(0);
      return p;
   }

   public static Packet getClock(int time) { // time in seconds
      final OutPacket p = OutPacket.create(SendOpcode.CLOCK);
      p.writeByte(
            2); // clock type. if you send 3 here you have to send another byte (which does not matter at all) before the timestamp
      p.writeInt(time);
      return p;
   }

   public static Packet getClockTime(int hour, int min, int sec) { // Current Time
      final OutPacket p = OutPacket.create(SendOpcode.CLOCK);
      p.writeByte(1); //Clock-Type
      p.writeByte(hour);
      p.writeByte(min);
      p.writeByte(sec);
      return p;
   }

   public static Packet crogBoatPacket(boolean type) {
      final OutPacket p = OutPacket.create(SendOpcode.CONTI_MOVE);
      p.writeByte(10);
      p.writeByte(type ? 4 : 5);
      return p;
   }

   public static Packet boatPacket(boolean type) {
      final OutPacket p = OutPacket.create(SendOpcode.CONTI_STATE);
      p.writeByte(type ? 1 : 2);
      p.writeByte(0);
      return p;
   }

   public static Packet removeClock() {
      final OutPacket p = OutPacket.create(SendOpcode.STOP_CLOCK);
      p.writeByte(0);
      return p;
   }

   public static Packet showAriantScoreBoard() {
      final OutPacket p = OutPacket.create(SendOpcode.ARIANT_ARENA_SHOW_RESULT);
      return p;
   }

   public static Packet pyramidGauge(int gauge) {
      final OutPacket p = OutPacket.create(SendOpcode.PYRAMID_GAUGE);
      p.writeInt(gauge);
      return p;
   }

   public static Packet pyramidScore(byte score, int exp) {
      final OutPacket p = OutPacket.create(SendOpcode.PYRAMID_SCORE);
      p.writeByte(score);
      p.writeInt(exp);
      return p;
   }

   private static Packet MassacreResult(byte nRank, int nIncExp) {
      //CField_MassacreResult__OnMassacreResult @ 0x005617C5
      final OutPacket p = OutPacket.create(SendOpcode.PYRAMID_SCORE); //MASSACRERESULT | 0x009E
      p.writeByte(nRank); //(0 - S) (1 - A) (2 - B) (3 - C) (4 - D) ( Else - Crash )
      p.writeInt(nIncExp);
      return p;
   }

   public static Packet showMTSCash(MapleCharacter character) {
      final OutPacket p = OutPacket.create(SendOpcode.MTS_OPERATION2);
      p.writeInt(character.getCashShop().getCash(4));
      p.writeInt(character.getCashShop().getCash(2));
      return p;
   }

   public static Packet sendMTS(List<MTSItemInfo> items, int tab, int type, int page, int pages) {
      final OutPacket p = OutPacket.create(SendOpcode.MTS_OPERATION);
      p.writeByte(0x15); //operation
      p.writeInt(pages * 16); //testing, change to 10 if fails
      p.writeInt(items.size()); //number of items
      p.writeInt(tab);
      p.writeInt(type);
      p.writeInt(page);
      p.writeByte(1);
      p.writeByte(1);
      for (MTSItemInfo item : items) {
         CCommon.addItemInfo(p, item.getItem(), true);
         p.writeInt(item.getID()); //id
         p.writeInt(item.getTaxes()); //this + below = price
         p.writeInt(item.getPrice()); //price
         p.writeInt(0);
         p.writeLong(CCommon.getTime(item.getEndingDate()));
         p.writeString(item.getSeller()); //account name (what was nexon thinking?)
         p.writeString(item.getSeller()); //char name
         for (int j = 0; j < 28; j++) {
            p.writeByte(0);
         }
      }
      p.writeByte(1);
      return p;
   }

   public static Packet MTSWantedListingOver(int nx, int items) {
      final OutPacket p = OutPacket.create(SendOpcode.MTS_OPERATION);
      p.writeByte(0x3D);
      p.writeInt(nx);
      p.writeInt(items);
      return p;
   }

   public static Packet MTSConfirmSell() {
      final OutPacket p = OutPacket.create(SendOpcode.MTS_OPERATION);
      p.writeByte(0x1D);
      return p;
   }

   public static Packet MTSConfirmBuy() {
      final OutPacket p = OutPacket.create(SendOpcode.MTS_OPERATION);
      p.writeByte(0x33);
      return p;
   }

   public static Packet MTSFailBuy() {
      final OutPacket p = OutPacket.create(SendOpcode.MTS_OPERATION);
      p.writeByte(0x34);
      p.writeByte(0x42);
      return p;
   }

   public static Packet MTSConfirmTransfer(int quantity, int pos) {
      final OutPacket p = OutPacket.create(SendOpcode.MTS_OPERATION);
      p.writeByte(0x27);
      p.writeInt(quantity);
      p.writeInt(pos);
      return p;
   }

   public static Packet notYetSoldInv(List<MTSItemInfo> items) {
      final OutPacket p = OutPacket.create(SendOpcode.MTS_OPERATION);
      p.writeByte(0x23);
      p.writeInt(items.size());
      if (!items.isEmpty()) {
         for (MTSItemInfo item : items) {
            CCommon.addItemInfo(p, item.getItem(), true);
            p.writeInt(item.getID()); //id
            p.writeInt(item.getTaxes()); //this + below = price
            p.writeInt(item.getPrice()); //price
            p.writeInt(0);
            p.writeLong(CCommon.getTime(item.getEndingDate()));
            p.writeString(item.getSeller()); //account name (what was nexon thinking?)
            p.writeString(item.getSeller()); //char name
            for (int i = 0; i < 28; i++) {
               p.writeByte(0);
            }
         }
      } else {
         p.writeInt(0);
      }
      return p;
   }

   public static Packet transferInventory(List<MTSItemInfo> items) {
      final OutPacket p = OutPacket.create(SendOpcode.MTS_OPERATION);
      p.writeByte(0x21);
      p.writeInt(items.size());
      if (!items.isEmpty()) {
         for (MTSItemInfo item : items) {
            CCommon.addItemInfo(p, item.getItem(), true);
            p.writeInt(item.getID()); //id
            p.writeInt(item.getTaxes()); //taxes
            p.writeInt(item.getPrice()); //price
            p.writeInt(0);
            p.writeLong(CCommon.getTime(item.getEndingDate()));
            p.writeString(item.getSeller()); //account name (what was nexon thinking?)
            p.writeString(item.getSeller()); //char name
            for (int i = 0; i < 28; i++) {
               p.writeByte(0);
            }
         }
      }
      p.writeByte(0xD0 + items.size());
      p.writeBytes(new byte[]{-1, -1, -1, 0});
      return p;
   }

   public static Packet sendMapleLifeCharacterInfo() {
      final OutPacket p = OutPacket.create(SendOpcode.MAPLELIFE_RESULT);
      p.writeInt(0);
      return p;
   }

   public static Packet sendMapleLifeNameError() {
      final OutPacket p = OutPacket.create(SendOpcode.MAPLELIFE_RESULT);
      p.writeInt(2);
      p.writeInt(3);
      p.writeByte(0);
      return p;
   }

   public static Packet sendMapleLifeError(int code) {
      final OutPacket p = OutPacket.create(SendOpcode.MAPLELIFE_ERROR);
      p.writeByte(0);
      p.writeInt(code);
      return p;
   }

   public static Packet sendHammerData(int hammerUsed) {
      final OutPacket p = OutPacket.create(SendOpcode.VICIOUS_HAMMER);
      p.writeByte(0x39);
      p.writeInt(0);
      p.writeInt(hammerUsed);
      return p;
   }

   public static Packet sendHammerMessage() {
      final OutPacket p = OutPacket.create(SendOpcode.VICIOUS_HAMMER);
      p.writeByte(0x3D);
      p.writeInt(0);
      return p;
   }

   public static Packet sendVegaScroll(int op) {
      final OutPacket p = OutPacket.create(SendOpcode.VEGA_SCROLL);
      p.writeByte(op);
      return p;
   }

   public static Packet musicChange(String song) {
      return environmentChange(song, 6);
   }

   public static Packet showEffect(String effect) {
      return environmentChange(effect, 3);
   }

   public static Packet playSound(String sound) {
      return environmentChange(sound, 4);
   }

   private static Pair<Integer, Integer> normalizedCustomMaxHP(long currHP, long maxHP) {
      int sendHP, sendMaxHP;

      if (maxHP <= Integer.MAX_VALUE) {
         sendHP = (int) currHP;
         sendMaxHP = (int) maxHP;
      } else {
         float f = ((float) currHP) / maxHP;

         sendHP = (int) (Integer.MAX_VALUE * f);
         sendMaxHP = Integer.MAX_VALUE;
      }

      return new Pair<>(sendHP, sendMaxHP);
   }
}
