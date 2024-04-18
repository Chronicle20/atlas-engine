package connection.packets;

import java.util.List;

import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import tools.Pair;

public class CUserLocal {
   public static Packet dojoWarpUp() {
      final OutPacket p = OutPacket.create(SendOpcode.DOJO_WARP_UP);
      p.writeByte(0);
      p.writeByte(6);
      return p;
   }

   public static Packet addQuestTimeLimit(final short quest, final int time) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_QUEST_INFO);
      p.writeByte(6);
      p.writeShort(1);//Size but meh, when will there be 2 at the same time? And it won't even replace the old one :)
      p.writeShort(quest);
      p.writeInt(time);
      return p;
   }

   public static Packet removeQuestTimeLimit(final short quest) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_QUEST_INFO);
      p.writeByte(7);
      p.writeShort(1);//Position
      p.writeShort(quest);
      return p;
   }

   public static Packet updateQuestFinish(short quest, int npc, short nextquest) { //Check
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_QUEST_INFO); //0xF2 in v95
      p.writeByte(8);//0x0A in v95
      p.writeShort(quest);
      p.writeInt(npc);
      p.writeShort(nextquest);
      return p;
   }

   public static Packet questError(short quest) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_QUEST_INFO);
      p.writeByte(0x0A);
      p.writeShort(quest);
      return p;
   }

   public static Packet questFailure(byte type) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_QUEST_INFO);
      p.writeByte(type);//0x0B = No meso, 0x0D = Worn by character, 0x0E = Not having the item ?
      return p;
   }

   public static Packet questExpire(short quest) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_QUEST_INFO);
      p.writeByte(0x0F);
      p.writeShort(quest);
      return p;
   }

   /**
    * Sends a player hint.
    *
    * @param hint   The hint it's going to send.
    * @param width  How tall the box is going to be.
    * @param height How long the box is going to be.
    * @return The player hint packet.
    */
   public static Packet sendHint(String hint, int width, int height) {
      if (width < 1) {
         width = hint.length() * 10;
         if (width < 40) {
            width = 40;
         }
      }
      if (height < 5) {
         height = 5;
      }
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_HINT);
      p.writeString(hint);
      p.writeShort(width);
      p.writeShort(height);
      p.writeByte(1);
      return p;
   }

   // MAKER_RESULT packets thanks to Arnah (Vertisy)
   public static Packet makerResult(boolean success, int itemMade, int itemCount, int mesos, List<Pair<Integer, Integer>> itemsLost,
                                    int catalystID, List<Integer> INCBuffGems) {
      final OutPacket p = OutPacket.create(SendOpcode.MAKER_RESULT);
      p.writeInt(success ? 0 : 1); // 0 = success, 1 = fail
      p.writeInt(1); // 1 or 2 doesn't matter, same methods
      p.writeBool(!success);
      if (success) {
         p.writeInt(itemMade);
         p.writeInt(itemCount);
      }
      p.writeInt(itemsLost.size()); // Loop
      for (Pair<Integer, Integer> item : itemsLost) {
         p.writeInt(item.getLeft());
         p.writeInt(item.getRight());
      }
      p.writeInt(INCBuffGems.size());
      for (Integer gem : INCBuffGems) {
         p.writeInt(gem);
      }
      if (catalystID != -1) {
         p.writeByte(1); // stimulator
         p.writeInt(catalystID);
      } else {
         p.writeByte(0);
      }

      p.writeInt(mesos);
      return p;
   }

   public static Packet makerResultCrystal(int itemIdGained, int itemIdLost) {
      final OutPacket p = OutPacket.create(SendOpcode.MAKER_RESULT);
      p.writeInt(0); // Always successful!
      p.writeInt(3); // Monster Crystal
      p.writeInt(itemIdGained);
      p.writeInt(itemIdLost);
      return p;
   }

   public static Packet makerResultDesynth(int itemId, int mesos, List<Pair<Integer, Integer>> itemsGained) {
      final OutPacket p = OutPacket.create(SendOpcode.MAKER_RESULT);
      p.writeInt(0); // Always successful!
      p.writeInt(4); // Mode Desynth
      p.writeInt(itemId); // Item desynthed
      p.writeInt(itemsGained.size()); // Loop of items gained, (int, int)
      for (Pair<Integer, Integer> item : itemsGained) {
         p.writeInt(item.getLeft());
         p.writeInt(item.getRight());
      }
      p.writeInt(mesos); // Mesos spent.
      return p;
   }

   public static Packet makerEnableActions() {
      final OutPacket p = OutPacket.create(SendOpcode.MAKER_RESULT);
      p.writeInt(0); // Always successful!
      p.writeInt(0); // Monster Crystal
      p.writeInt(0);
      p.writeInt(0);
      return p;
   }

   /**
    * Sends a UI utility. 0x01 - Equipment Inventory. 0x02 - Stat Window. 0x03
    * - Skill Window. 0x05 - Keyboard Settings. 0x06 - Quest window. 0x09 -
    * Monsterbook Window. 0x0A - Char Info 0x0B - Guild BBS 0x12 - Monster
    * Carnival Window 0x16 - Party Search. 0x17 - Item Creation Window. 0x1A -
    * My Ranking O.O 0x1B - Family Window 0x1C - Family Pedigree 0x1D - GM
    * Story Board /funny shet 0x1E - Envelop saying you got mail from an admin.
    * lmfao 0x1F - Medal Window 0x20 - Maple Event (???) 0x21 - Invalid Pointer
    * Crash
    *
    * @param ui
    * @return
    */
   public static Packet openUI(byte ui) {
      final OutPacket p = OutPacket.create(SendOpcode.OPEN_UI);
      p.writeByte(ui);
      return p;
   }

   public static Packet lockUI(boolean enable) {
      final OutPacket p = OutPacket.create(SendOpcode.LOCK_UI);
      p.writeByte(enable ? 1 : 0);
      return p;
   }

   public static Packet disableUI(boolean enable) {
      final OutPacket p = OutPacket.create(SendOpcode.DISABLE_UI);
      p.writeByte(enable ? 1 : 0);
      return p;
   }

   public static Packet spawnGuide(boolean spawn) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_GUIDE);
      if (spawn) {
         p.writeByte(1);
      } else {
         p.writeByte(0);
      }
      return p;
   }

   public static Packet talkGuide(String talk) {
      final OutPacket p = OutPacket.create(SendOpcode.TALK_GUIDE);
      p.writeByte(0);
      p.writeString(talk);
      p.writeBytes(new byte[]{(byte) 0xC8, 0, 0, 0, (byte) 0xA0, (byte) 0x0F, 0, 0});
      return p;
   }

   public static Packet guideHint(int hint) {
      final OutPacket p = OutPacket.create(SendOpcode.TALK_GUIDE);
      p.writeByte(1);
      p.writeInt(hint);
      p.writeInt(7000);
      return p;
   }

   public static Packet showCombo(int count) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_COMBO);
      p.writeInt(count);
      return p;
   }

   public static Packet skillCooldown(int sid, int time) {
      final OutPacket p = OutPacket.create(SendOpcode.COOLDOWN);
      p.writeInt(sid);
      p.writeShort(time);//Int in v97
      return p;
   }
}
