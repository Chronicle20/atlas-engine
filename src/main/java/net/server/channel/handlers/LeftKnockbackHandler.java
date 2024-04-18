package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CFieldSnowBall;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public class LeftKnockbackHandler extends AbstractMaplePacketHandler {
   public void handlePacket(InPacket p, final MapleClient c) {
      c.sendPacket(CFieldSnowBall.leftKnockBack());
      c.sendPacket(CWvsContext.enableActions());
   }
}
