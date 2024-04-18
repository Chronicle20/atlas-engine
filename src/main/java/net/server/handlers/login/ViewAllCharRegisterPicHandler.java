package net.server.handlers.login;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleClient;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.coordinator.session.Hwid;
import net.server.coordinator.session.MapleSessionCoordinator;
import net.server.coordinator.session.MapleSessionCoordinator.AntiMulticlientResult;
import net.server.world.World;
import tools.Randomizer;

public final class ViewAllCharRegisterPicHandler extends AbstractMaplePacketHandler {
   private static final Logger log = LoggerFactory.getLogger(ViewAllCharRegisterPicHandler.class);

   private static int parseAntiMulticlientError(AntiMulticlientResult res) {
      return switch (res) {
         case REMOTE_PROCESSING -> 10;
         case REMOTE_LOGGEDIN -> 7;
         case REMOTE_NO_MATCH -> 17;
         case COORDINATOR_ERROR -> 8;
         default -> 9;
      };
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      p.readByte();
      int charId = p.readInt();
      p.readInt(); // please don't let the client choose which world they should login

      String mac = p.readString();
      String hostString = p.readString();

      final Hwid hwid;
      try {
         hwid = Hwid.fromHostString(hostString);
      } catch (IllegalArgumentException e) {
         log.warn("Invalid host string: {}", hostString, e);
         c.sendPacket(CLogin.getAfterLoginError(17));
         return;
      }

      c.updateMacs(mac);
      c.updateHwid(hwid);

      if (c.hasBannedMac() || c.hasBannedHWID()) {
         MapleSessionCoordinator.getInstance().closeSession(c, true);
         return;
      }

      AntiMulticlientResult res = MapleSessionCoordinator.getInstance().attemptGameSession(c, c.getAccID(), hwid);
      if (res != AntiMulticlientResult.SUCCESS) {
         c.sendPacket(CLogin.getAfterLoginError(parseAntiMulticlientError(res)));
         return;
      }

      Server server = Server.getInstance();
      if (!server.haveCharacterEntry(c.getAccID(), charId)) {
         MapleSessionCoordinator.getInstance().closeSession(c, true);
         return;
      }

      c.setWorld(server.getCharacterWorld(charId).orElseThrow());
      World wserv = c.getWorldServer();
      if (wserv == null || wserv.isWorldCapacityFull()) {
         c.sendPacket(CLogin.getAfterLoginError(10));
         return;
      }

      int channel = Randomizer.rand(1, server.getWorld(c.getWorld()).orElseThrow().getChannelsSize());
      c.setChannel(channel);

      String pic = p.readString();
      c.setPic(pic);

      String[] socket = server.getInetSocket(c.getWorld(), channel);
      if (socket == null) {
         c.sendPacket(CLogin.getAfterLoginError(10));
         return;
      }

      server.unregisterLoginState(c);
      c.setCharacterOnSessionTransitionState(charId);

      try {
         c.sendPacket(CLogin.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
      } catch (UnknownHostException e) {
         e.printStackTrace();
      }
   }
}
