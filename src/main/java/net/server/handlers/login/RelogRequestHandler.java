package net.server.handlers.login;

import client.MapleClient;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class RelogRequestHandler extends AbstractMaplePacketHandler {
   @Override
   public boolean validateState(MapleClient c) {
      return !c.isLoggedIn();
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      c.sendPacket(CLogin.getRelogResponse());
   }
}
