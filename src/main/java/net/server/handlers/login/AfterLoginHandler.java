package net.server.handlers.login;

import client.MapleClient;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.coordinator.session.MapleSessionCoordinator;

public final class AfterLoginHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      byte c2 = p.readByte();
      byte c3 = 5;
      if (p.available() > 0) {
         c3 = p.readByte();
      }
      if (c2 == 1 && c3 == 1) {
         if (c.getPin() == null || c.getPin().isEmpty()) {
            c.sendPacket(CLogin.registerPin());
         } else {
            c.sendPacket(CLogin.requestPin());
         }
      } else if (c2 == 1 && c3 == 0) {
         String pin = p.readString();
         if (c.checkPin(pin)) {
            c.sendPacket(CLogin.pinAccepted());
         } else {
            c.sendPacket(CLogin.requestPinAfterFailure());
         }
      } else if (c2 == 2 && c3 == 0) {
         String pin = p.readString();
         if (c.checkPin(pin)) {
            c.sendPacket(CLogin.registerPin());
         } else {
            c.sendPacket(CLogin.requestPinAfterFailure());
         }
      } else if (c2 == 0 && c3 == 5) {
         MapleSessionCoordinator.getInstance().closeSession(c, null);
         c.updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
      }
   }
}
