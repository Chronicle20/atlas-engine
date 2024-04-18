package connection.packets;

import connection.headers.SendOpcode;
import constants.net.NPCTalkMessageType;
import net.packet.OutPacket;
import net.packet.Packet;
import tools.HexTool;

public class CScriptMan {
   /**
    * Possible values for <code>speaker</code>:<br> 0: Npc talking (left)<br>
    * 1: Npc talking (right)<br> 2: Player talking (left)<br> 3: Player talking
    * (left)<br>
    *
    * @param npc      Npcid
    * @param msgType
    * @param talk
    * @param endBytes
    * @param speaker
    * @return
    */
   public static Packet getNPCTalk(int npc, NPCTalkMessageType msgType, String talk, String endBytes, byte speaker) {
      final OutPacket p = OutPacket.create(SendOpcode.NPC_TALK);
      p.writeByte(4); // ?
      p.writeInt(npc);
      p.writeByte(msgType.getMessageType());
      p.writeByte(speaker);
      p.writeString(talk);
      p.writeBytes(HexTool.getByteArrayFromHexString(endBytes));
      return p;
   }

   public static Packet getDimensionalMirror(String talk) {
      final OutPacket p = OutPacket.create(SendOpcode.NPC_TALK);
      p.writeByte(4); // ?
      p.writeInt(9010022);
      p.writeByte(NPCTalkMessageType.ON_ASK_SLIDE_MENU.getMessageType());
      p.writeByte(0); //speaker
      p.writeInt(0);
      p.writeInt(4);
      p.writeString(talk);
      return p;
   }

   public static Packet getNPCTalkStyle(int npc, String talk, int[] styles) {
      final OutPacket p = OutPacket.create(SendOpcode.NPC_TALK);
      p.writeByte(4); // ?
      p.writeInt(npc);
      p.writeByte(NPCTalkMessageType.ON_ASK_AVATAR.getMessageType());
      p.writeByte(0); //speaker
      p.writeString(talk);
      p.writeByte(styles.length);
      for (int style : styles) {
         p.writeInt(style);
      }
      return p;
   }

   public static Packet getNPCTalkNum(int npc, String talk, int def, int min, int max) {
      final OutPacket p = OutPacket.create(SendOpcode.NPC_TALK);
      p.writeByte(4); // ?
      p.writeInt(npc);
      p.writeByte(NPCTalkMessageType.ON_ASK_NUMBER.getMessageType());
      p.writeByte(0); //speaker
      p.writeString(talk);
      p.writeInt(def);
      p.writeInt(min);
      p.writeInt(max);
      p.writeInt(0);
      return p;
   }

   public static Packet getNPCTalkText(int npc, String talk, String def) {
      final OutPacket p = OutPacket.create(SendOpcode.NPC_TALK);
      p.writeByte(4); // Doesn't matter
      p.writeInt(npc);
      p.writeByte(NPCTalkMessageType.ON_ASK_TEXT.getMessageType());
      p.writeByte(0); //speaker
      p.writeString(talk);
      p.writeString(def);//:D
      p.writeInt(0);
      return p;
   }

   // NPC Quiz packets thanks to Eric
   public static Packet OnAskQuiz(int nSpeakerTypeID, int nSpeakerTemplateID, int nResCode, String sTitle, String sProblemText,
                                  String sHintText, int nMinInput, int nMaxInput, int tRemainInitialQuiz) {
      final OutPacket p = OutPacket.create(SendOpcode.NPC_TALK);
      p.writeByte(nSpeakerTypeID);
      p.writeInt(nSpeakerTemplateID);
      p.writeByte(NPCTalkMessageType.ON_ASK_QUIZ.getMessageType());
      p.writeByte(0);
      p.writeByte(nResCode);
      if (nResCode == 0x0) {//fail has no bytes <3
         p.writeString(sTitle);
         p.writeString(sProblemText);
         p.writeString(sHintText);
         p.writeShort(nMinInput);
         p.writeShort(nMaxInput);
         p.writeInt(tRemainInitialQuiz);
      }
      return p;
   }

   public static Packet OnAskSpeedQuiz(int nSpeakerTypeID, int nSpeakerTemplateID, int nResCode, int nType, int dwAnswer,
                                       int nCorrect, int nRemain, int tRemainInitialQuiz) {
      final OutPacket p = OutPacket.create(SendOpcode.NPC_TALK);
      p.writeByte(nSpeakerTypeID);
      p.writeInt(nSpeakerTemplateID);
      p.writeByte(NPCTalkMessageType.ON_ASK_SPEED_QUIZ.getMessageType());
      p.writeByte(0);
      p.writeByte(nResCode);
      if (nResCode == 0x0) {//fail has no bytes <3
         p.writeInt(nType);
         p.writeInt(dwAnswer);
         p.writeInt(nCorrect);
         p.writeInt(nRemain);
         p.writeInt(tRemainInitialQuiz);
      }
      return p;
   }
}
