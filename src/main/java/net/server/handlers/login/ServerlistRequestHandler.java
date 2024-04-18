package net.server.handlers.login;

import java.util.List;

import client.MapleClient;
import connection.packets.CLogin;
import constants.game.GameConstants;
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

      for (World world : worlds) {
         c.sendPacket(CLogin.getServerList(world.getId(), GameConstants.WORLD_NAMES[world.getId()], world.getFlag(),
               world.getEventMessage(), world.getChannels()));
      }
      c.sendPacket(CLogin.getEndOfServerList());
      //        c.sendPacket(CLogin.selectWorld(0));//too lazy to make a check lol
      //        c.sendPacket(CLogin.sendRecommended(server.worldRecommendedList()));
   }
}