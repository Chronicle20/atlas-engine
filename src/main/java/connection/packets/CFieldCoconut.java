package connection.packets;

import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CFieldCoconut {
   public static Packet hitCoconut(boolean spawn, int id, int type) {
      final OutPacket p = OutPacket.create(SendOpcode.COCONUT_HIT);
      if (spawn) {
         p.writeShort(-1);
         p.writeShort(5000);
         p.writeByte(0);
      } else {
         p.writeShort(id);
         p.writeShort(1000);//delay till you can attack again!
         p.writeByte(type); // What action to do for the coconut.
      }
      return p;
   }

   public static Packet coconutScore(int team1, int team2) {
      final OutPacket p = OutPacket.create(SendOpcode.COCONUT_SCORE);
      p.writeShort(team1);
      p.writeShort(team2);
      return p;
   }
}
