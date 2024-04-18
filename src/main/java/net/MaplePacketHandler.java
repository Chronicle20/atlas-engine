package net;

import client.MapleClient;
import net.packet.InPacket;

public interface MaplePacketHandler {
   void handlePacket(InPacket p, MapleClient c);

   boolean validateState(MapleClient c);
}
