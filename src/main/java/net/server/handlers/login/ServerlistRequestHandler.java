package net.server.handlers.login;

import java.util.List;

import client.MapleClient;
import connection.models.WorldInformation;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.world.World;

public final class ServerlistRequestHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      Server server = Server.getInstance();
      List<World> worlds = server.getWorlds();
      c.requestedServerlist(worlds.size());
      worlds.stream()
            .map(WorldInformation::fromWorld)
            .map(CLogin::getWorldInformation)
            .forEach(c::sendPacket);
      c.sendPacket(CLogin.getEndOfWorldInformation());
      c.sendPacket(CLogin.selectWorld(0));//too lazy to make a check lol
      c.sendPacket(CLogin.sendRecommended(server.worldRecommendedList()));
   }
}