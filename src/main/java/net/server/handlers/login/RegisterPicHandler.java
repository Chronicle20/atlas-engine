package net.server.handlers.login;

import client.MapleClient;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import net.server.coordinator.session.MapleSessionCoordinator;
import net.server.coordinator.session.MapleSessionCoordinator.AntiMulticlientResult;
import net.server.world.World;
import org.apache.mina.core.session.IoSession;
import tools.data.input.SeekableLittleEndianAccessor;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class RegisterPicHandler extends AbstractMaplePacketHandler {

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
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        int charId = slea.readInt();

        String macs = slea.readMapleAsciiString();
        String hwid = slea.readMapleAsciiString();

        if (!hwid.matches("[0-9A-F]{12}_[0-9A-F]{8}")) {
            c.announce(CLogin.getAfterLoginError(17));
            return;
        }

        c.updateMacs(macs);
        c.updateHWID(hwid);

        IoSession session = c.getSession();
        AntiMulticlientResult res = MapleSessionCoordinator.getInstance().attemptGameSession(session, c.getAccID(), hwid);
        if (res != AntiMulticlientResult.SUCCESS) {
            c.announce(CLogin.getAfterLoginError(parseAntiMulticlientError(res)));
            return;
        }

        if (c.hasBannedMac() || c.hasBannedHWID()) {
            MapleSessionCoordinator.getInstance().closeSession(c.getSession(), true);
            return;
        }

        Server server = Server.getInstance();
        if (!server.haveCharacterEntry(c.getAccID(), charId)) {
            MapleSessionCoordinator.getInstance().closeSession(c.getSession(), true);
            return;
        }

        String pic = slea.readMapleAsciiString();
        if (c.getPic() == null || c.getPic().equals("")) {
            c.setPic(pic);

            c.setWorld(server.getCharacterWorld(charId).orElseThrow());
            World wserv = c.getWorldServer();
            if (wserv == null || wserv.isWorldCapacityFull()) {
                c.announce(CLogin.getAfterLoginError(10));
                return;
            }

            String[] socket = server.getInetSocket(c.getWorld(), c.getChannel());
            if (socket == null) {
                c.announce(CLogin.getAfterLoginError(10));
                return;
            }

            server.unregisterLoginState(c);
            c.setCharacterOnSessionTransitionState(charId);

            try {
                c.announce(CLogin.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } else {
            MapleSessionCoordinator.getInstance().closeSession(c.getSession(), true);
        }
    }
}