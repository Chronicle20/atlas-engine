package net.server.handlers.login;

import client.MapleClient;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.coordinator.session.MapleSessionCoordinator;

public class SetGenderHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (c.getGender() == 10) { //Packet shouldn't come if Gender isn't 10.
         byte confirmed = p.readByte();
         if (confirmed == 0x01) {
            c.setGender(p.readByte());
            c.sendPacket(CLogin.getAuthSuccess(c));

            Server.getInstance().registerLoginState(c);
         } else {
            MapleSessionCoordinator.getInstance().closeSession(c, null);
            c.updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
         }
      }
   }
}
