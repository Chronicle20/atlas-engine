package connection.packets;

import client.MapleCharacter;
import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CFieldMonsterCarnival {
   public static Packet startMonsterCarnival(MapleCharacter chr, int team, int oposition) {
      final OutPacket p = OutPacket.create(SendOpcode.MONSTER_CARNIVAL_START);
      p.writeByte(team); // team
      p.writeShort(chr.getCP()); // Obtained CP - Used CP
      p.writeShort(chr.getTotalCP()); // Total Obtained CP
      p.writeShort(chr.getMonsterCarnival()
            .getCP(team)); // Obtained CP - Used CP of the team
      p.writeShort(chr.getMonsterCarnival()
            .getTotalCP(team)); // Total Obtained CP of the team
      p.writeShort(chr.getMonsterCarnival()
            .getCP(oposition)); // Obtained CP - Used CP of the team
      p.writeShort(chr.getMonsterCarnival()
            .getTotalCP(oposition)); // Total Obtained CP of the team
      p.writeShort(0); // Probably useless nexon shit
      p.writeLong(0); // Probably useless nexon shit
      return p;
   }

   public static Packet CPUpdate(boolean party, int curCP, int totalCP, int team) { // CPQ
      OutPacket p;
      if (!party) {
         p = OutPacket.create(SendOpcode.MONSTER_CARNIVAL_OBTAINED_CP);
      } else {
         p = OutPacket.create(SendOpcode.MONSTER_CARNIVAL_PARTY_CP);
         p.writeByte(team); // team?
      }
      p.writeShort(curCP);
      p.writeShort(totalCP);
      return p;
   }

   public static Packet playerSummoned(String name, int tab, int number) {
      final OutPacket p = OutPacket.create(SendOpcode.MONSTER_CARNIVAL_SUMMON);
      p.writeByte(tab);
      p.writeByte(number);
      p.writeString(name);
      return p;
   }

   public static Packet CPQMessage(byte message) {
      final OutPacket p = OutPacket.create(SendOpcode.MONSTER_CARNIVAL_MESSAGE);
      p.writeByte(message); // Message
      return p;
   }

   public static Packet playerDiedMessage(String name, int lostCP, int team) { // CPQ
      final OutPacket p = OutPacket.create(SendOpcode.MONSTER_CARNIVAL_DIED);
      p.writeByte(team); // team
      p.writeString(name);
      p.writeByte(lostCP);
      return p;
   }
}
