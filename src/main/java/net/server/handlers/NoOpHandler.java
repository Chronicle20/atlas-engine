package net.server.handlers;

import client.MapleClient;
import net.MaplePacketHandler;
import net.packet.InPacket;

public class NoOpHandler implements MaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
   }

   @Override
   public boolean validateState(MapleClient c) {
      return true;
   }
}
