package net;

import client.MapleClient;
import net.server.Server;

public abstract class AbstractMaplePacketHandler implements MaplePacketHandler {
    protected static long currentServerTime() {
        return Server.getInstance().getCurrentTime();
    }

    @Override
    public boolean validateState(MapleClient c) {
        return c.isLoggedIn();
    }
}