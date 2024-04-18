package net.server.handlers.login;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class CheckCharNameHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(InPacket p, MapleClient c) {
        String name = p.readString();
        c.sendPacket(CLogin.charNameResponse(name, !MapleCharacter.canCreateChar(name)));
    }
}
