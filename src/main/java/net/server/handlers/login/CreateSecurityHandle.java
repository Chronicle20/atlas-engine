package net.server.handlers.login;

import java.util.Random;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.packet.OutPacket;
import net.packet.Packet;

public class CreateSecurityHandle extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      c.sendPacket(writePacket());
   }

   @Override
   public boolean validateState(MapleClient c) {
      return true;
   }

   private Packet writePacket() {
      String[] LoginScreen = {"MapLogin", "MapLogin1"};
      final OutPacket op = OutPacket.create(connection.constants.SendOpcode.LOGIN_AUTH);
      int index = new Random().nextInt(2);
      op.writeString(LoginScreen[index]);
      return op;
   }
}
