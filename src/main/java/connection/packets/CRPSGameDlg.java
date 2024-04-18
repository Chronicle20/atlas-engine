package connection.packets;

import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CRPSGameDlg {
   public static Packet openRPSNPC() {
      final OutPacket p = OutPacket.create(SendOpcode.RPS_GAME);
      p.writeByte(8);// open npc
      p.writeInt(9000019);
      return p;
   }

   public static Packet rpsMesoError(int mesos) {
      final OutPacket p = OutPacket.create(SendOpcode.RPS_GAME);
      p.writeByte(0x06);
      if (mesos != -1) {
         p.writeInt(mesos);
      }
      return p;
   }

   public static Packet rpsSelection(byte selection, byte answer) {
      final OutPacket p = OutPacket.create(SendOpcode.RPS_GAME);
      p.writeByte(0x0B);// 11l
      p.writeByte(selection);
      p.writeByte(answer);
      return p;
   }

   public static Packet rpsMode(byte mode) {
      final OutPacket p = OutPacket.create(SendOpcode.RPS_GAME);
      p.writeByte(mode);
      return p;
   }
}
