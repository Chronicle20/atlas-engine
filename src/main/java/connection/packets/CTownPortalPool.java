package connection.packets;

import java.awt.*;

import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CTownPortalPool {
   /**
    * Gets a packet to spawn a door.
    *
    * @param launched Already deployed the door.
    * @param ownerid  The door's owner ID.
    * @param pos      The position of the door.
    * @return The remove door packet.
    */
   public static Packet spawnDoor(int ownerid, Point pos, boolean launched) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_DOOR);
      p.writeBool(launched);
      p.writeInt(ownerid);
      p.writePos(pos);
      return p;
   }
}
