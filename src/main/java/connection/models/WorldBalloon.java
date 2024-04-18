package connection.models;

import net.packet.OutPacket;

public record WorldBalloon(int x, int y, String message) {
   public void encode(OutPacket p) {
      p.writeShort(x);
      p.writeShort(y);
      p.writeString(message);
   }
}
