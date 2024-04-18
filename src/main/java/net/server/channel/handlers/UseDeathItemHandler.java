package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CUserRemote;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class UseDeathItemHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int itemId = p.readInt();
      c.getPlayer().setItemEffect(itemId);
      c.sendPacket(CUserRemote.itemEffect(c.getPlayer().getId(), itemId));
   }
}
