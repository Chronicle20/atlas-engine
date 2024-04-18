package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CUser;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class CloseChalkboardHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      c.getPlayer().setChalkboard(null);
      c.getPlayer().getMap().broadcastMessage(CUser.useChalkboard(c.getPlayer(), true));
   }
}
