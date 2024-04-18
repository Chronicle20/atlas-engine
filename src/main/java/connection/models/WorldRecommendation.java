package connection.models;

import net.packet.OutPacket;

public record WorldRecommendation(int id, String message) {
   public void encode(OutPacket p) {
      p.writeInt(id);
      p.writeString(message);
   }
}
