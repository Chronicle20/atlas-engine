package connection.packets;

import java.util.Set;

import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import tools.Pair;

public class CNpcTemplate {
   public static Packet setNPCScriptable(Set<Pair<Integer, String>> scriptNpcDescriptions) {
      final OutPacket p = OutPacket.create(SendOpcode.SET_NPC_SCRIPTABLE);
      p.writeByte(scriptNpcDescriptions.size());
      for (Pair<Integer, String> pr : scriptNpcDescriptions) {
         p.writeInt(pr.getLeft());
         p.writeString(pr.getRight());
         p.writeInt(0); // start time
         p.writeInt(Integer.MAX_VALUE); // end time
      }
      return p;
   }
}
