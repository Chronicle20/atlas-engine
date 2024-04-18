package net.server.handlers;

import client.MapleClient;
import net.MaplePacketHandler;
import net.packet.InPacket;

public final class LoginRequiringNoOpHandler implements MaplePacketHandler {
   private static LoginRequiringNoOpHandler instance = new LoginRequiringNoOpHandler();

   public static LoginRequiringNoOpHandler getInstance() {
      return instance;
   }

   public void handlePacket(InPacket p, MapleClient c) {
   }

   public boolean validateState(MapleClient c) {
      return c.isLoggedIn();
   }
}
