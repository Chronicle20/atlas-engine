package net.server.handlers.login;

import client.MapleClient;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.coordinator.session.MapleSessionCoordinator;

public final class RegisterPinHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      byte c2 = p.readByte();
      if (c2 == 0) {
         MapleSessionCoordinator.getInstance().closeSession(c, null);
         c.updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
      } else {
         String pin = p.readString();
         if (pin != null) {
            c.setPin(pin);
            c.sendPacket(CLogin.pinRegistered());

            MapleSessionCoordinator.getInstance().closeSession(c, null);
            c.updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
         }
      }
   }
}
