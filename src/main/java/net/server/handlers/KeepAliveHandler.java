package net.server.handlers;

import client.MapleClient;
import net.MaplePacketHandler;
import net.packet.InPacket;

public class KeepAliveHandler implements MaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      c.pongReceived();
   }

   @Override
   public boolean validateState(MapleClient c) {
      return true;
   }
}
