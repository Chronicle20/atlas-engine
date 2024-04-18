package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CField;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class UseHammerHandler extends AbstractMaplePacketHandler {
   public void handlePacket(InPacket p, MapleClient c) {
      c.sendPacket(CField.sendHammerMessage());
   }
}
