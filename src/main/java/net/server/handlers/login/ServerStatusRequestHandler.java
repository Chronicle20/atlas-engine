package net.server.handlers.login;

import java.util.Optional;

import client.MapleClient;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.world.World;

public final class ServerStatusRequestHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      byte worldId = (byte) p.readShort();
      Optional<World> wserv = Server.getInstance().getWorld(worldId);
      if (wserv.isPresent()) {
         int status = wserv.get().getWorldCapacityStatus();
         c.sendPacket(CLogin.getServerStatus(status));
      } else {
         c.sendPacket(CLogin.getServerStatus(2));
      }
   }
}
