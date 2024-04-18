package net.server.handlers.login;

import client.MapleClient;
import connection.constants.LoginStatusCode;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class AcceptToSHandler extends AbstractMaplePacketHandler {

   @Override
   public boolean validateState(MapleClient c) {
      return !c.isLoggedIn();
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (p.available() == 0 || p.readByte() != 1 || c.acceptToS()) {
         c.disconnect(false, false);//Client dc's but just because I am cool I do this (:
         return;
      }
      if (c.finishLogin() == 0) {
         c.sendPacket(CLogin.getAuthSuccess(c));
      } else {
         c.sendPacket(CLogin.getLoginFailed(LoginStatusCode.SYSTEM_ERROR_3));//shouldn't happen XD
      }
   }
}
