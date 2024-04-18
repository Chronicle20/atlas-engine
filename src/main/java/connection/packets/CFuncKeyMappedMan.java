package connection.packets;

import java.util.Map;

import client.keybind.MapleKeyBinding;
import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CFuncKeyMappedMan {
   public static Packet getKeymap(Map<Integer, MapleKeyBinding> keybindings) {
      final OutPacket p = OutPacket.create(SendOpcode.KEYMAP);
      p.writeByte(0);
      for (int x = 0; x < 94; x++) {
         MapleKeyBinding binding = keybindings.get(x);
         if (binding != null) {
            p.writeByte(binding.type());
            p.writeInt(binding.action());
         } else {
            p.writeByte(0);
            p.writeInt(0);
         }
      }
      return p;
   }

   public static Packet sendAutoHpPot(int itemId) {
      final OutPacket p = OutPacket.create(SendOpcode.AUTO_HP_POT);
      p.writeInt(itemId);
      return p;
   }

   public static Packet sendAutoMpPot(int itemId) {
      final OutPacket p = OutPacket.create(SendOpcode.AUTO_MP_POT);
      p.writeInt(itemId);
      return p;
   }
}
