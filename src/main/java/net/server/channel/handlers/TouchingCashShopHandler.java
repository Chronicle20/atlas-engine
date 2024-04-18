package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CCashShop;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class TouchingCashShopHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      c.sendPacket(CCashShop.showCash(c.getPlayer()));
   }
}
