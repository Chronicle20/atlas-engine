package connection.packets;

import client.keybind.MapleQuickslotBinding;
import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CQuickslotKeyMappedMan {
   public static Packet QuickslotMappedInit(MapleQuickslotBinding pQuickslot) {
      final OutPacket p = OutPacket.create(SendOpcode.QUICKSLOT_INIT);
      pQuickslot.Encode(p);
      return p;
   }
}
