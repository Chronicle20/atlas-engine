package connection.packets;

import java.util.LinkedHashMap;
import java.util.Map;

import client.MapleCharacter;
import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CFieldAriantArena {
   public static Packet updateAriantPQRanking(Map<MapleCharacter, Integer> playerScore) {
      final OutPacket p = OutPacket.create(SendOpcode.ARIANT_ARENA_USER_SCORE);
      p.writeByte(playerScore.size());
      for (Map.Entry<MapleCharacter, Integer> e : playerScore.entrySet()) {
         p.writeString(e.getKey().getName());
         p.writeInt(e.getValue());
      }
      return p;
   }

   public static Packet updateAriantPQRanking(final MapleCharacter chr, final int score) {
      return updateAriantPQRanking(new LinkedHashMap<>() {{
         put(chr, score);
      }});
   }
}
