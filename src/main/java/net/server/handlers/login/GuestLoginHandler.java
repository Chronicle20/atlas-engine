package net.server.handlers.login;

import client.MapleClient;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class GuestLoginHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      c.sendPacket(CLogin.sendGuestTOS());
      new LoginPasswordHandler().handlePacket(p, c);
   }
}
