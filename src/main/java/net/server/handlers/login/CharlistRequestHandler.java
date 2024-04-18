package net.server.handlers.login;

import client.MapleClient;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;

import java.util.Optional;

public final class CharlistRequestHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(InPacket p, MapleClient c) {
        int world = p.readByte();

        Optional<World> wserv = Server.getInstance().getWorld(world);
        if (wserv.isEmpty() || wserv.get().isWorldCapacityFull()) {
            c.sendPacket(CLogin.getServerStatus(2));
            return;
        }

        int channel = p.readByte() + 1;
        Optional<Channel> ch = wserv.get().getChannel(channel);
        if (ch.isEmpty()) {
            c.sendPacket(CLogin.getServerStatus(2));
            return;
        }

        c.setWorld(world);
        c.setChannel(channel);
        c.sendCharList(world);
    }
}