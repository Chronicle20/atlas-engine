package connection.packets;

import java.awt.*;

import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CMessageBoxPool {
   public static Packet sendCannotSpawnKite() {
      return OutPacket.create(SendOpcode.CANNOT_SPAWN_KITE);
   }

   public static Packet spawnKite(int oid, int itemid, String name, String msg, Point pos, int ft) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_KITE);
      p.writeInt(oid);
      p.writeInt(itemid);
      p.writeString(msg);
      p.writeString(name);
      p.writeShort(pos.x);
      p.writeShort(ft);
      return p;
   }

   public static Packet removeKite(int objectid, int animationType) {    // thanks to Arnah (Vertisy)
      final OutPacket p = OutPacket.create(SendOpcode.REMOVE_KITE);
      p.writeByte(animationType); // 0 is 10/10, 1 just vanishes
      p.writeInt(objectid);
      return p;
   }
}
