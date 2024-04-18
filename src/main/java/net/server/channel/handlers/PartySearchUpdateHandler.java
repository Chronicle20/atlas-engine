package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class PartySearchUpdateHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(InPacket p, MapleClient c) {
        c.getWorldServer().getPartySearchCoordinator().unregisterPartyLeader(c.getPlayer());
    }
}