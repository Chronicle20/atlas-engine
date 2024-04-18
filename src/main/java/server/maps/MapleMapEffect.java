package server.maps;

import client.MapleClient;
import connection.packets.CField;
import net.packet.Packet;

public class MapleMapEffect {
   private final String msg;
   private final int itemId;
   private final boolean active = true;

   public MapleMapEffect(String msg, int itemId) {
      this.msg = msg;
      this.itemId = itemId;
   }

   public final Packet makeDestroyData() {
      return CField.removeMapEffect();
   }

   public final Packet makeStartData() {
      return CField.startMapEffect(msg, itemId, active);
   }

   public void sendStartData(MapleClient client) {
      client.sendPacket(makeStartData());
   }
}
