/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.server.handlers.login;

import java.net.InetAddress;
import java.net.UnknownHostException;

import client.MapleClient;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.coordinator.session.MapleSessionCoordinator;
import net.server.coordinator.session.MapleSessionCoordinator.AntiMulticlientResult;
import net.server.world.World;

public final class CharSelectedHandler extends AbstractMaplePacketHandler {

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
      int charId = p.readInt();

      String hwid = "";
      //TODO
//      c.updateHwid(hwid);

//      AntiMulticlientResult res = MapleSessionCoordinator.getInstance().attemptGameSession(c, c.getAccID(), hwid);
//      if (res != AntiMulticlientResult.SUCCESS) {
//         c.sendPacket(CLogin.getAfterLoginError(parseAntiMulticlientError(res)));
//         return;
//      }

      if (c.hasBannedMac() || c.hasBannedHWID()) {
         MapleSessionCoordinator.getInstance().closeSession(c, true);
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

      String[] socket = server.getInetSocket(c.getWorld(), c.getChannel());
      if (socket == null) {
         c.sendPacket(CLogin.getAfterLoginError(10));
         return;
      }

      server.unregisterLoginState(c);
      c.setCharacterOnSessionTransitionState(charId);

      try {
         c.sendPacket(CLogin.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
      } catch (UnknownHostException | NumberFormatException e) {
         e.printStackTrace();
      }
   }
}