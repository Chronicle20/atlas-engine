package connection.packets;

import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CFieldBattlefield {
   public static Packet sheepRanchInfo(byte wolf, byte sheep) {
      final OutPacket p = OutPacket.create(SendOpcode.SHEEP_RANCH_INFO);
      p.writeByte(wolf);
      p.writeByte(sheep);
      return p;
   }

   public static Packet sheepRanchClothes(int id, byte clothes) {
      final OutPacket p = OutPacket.create(SendOpcode.SHEEP_RANCH_CLOTHES);
      p.writeInt(id); //Character id
      p.writeByte(clothes); //0 = sheep, 1 = wolf, 2 = Spectator (wolf without wool)
      return p;
   }
}
