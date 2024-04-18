package connection.packets;

import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CFieldWitchtower {
   public static Packet updateWitchTowerScore(int score) {
      final OutPacket p = OutPacket.create(SendOpcode.WITCH_TOWER_SCORE_UPDATE);
      p.writeByte(score);
      return p;
   }
}
