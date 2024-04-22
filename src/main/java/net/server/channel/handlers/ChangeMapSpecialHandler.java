package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.MapleTrade;
import server.MapleTrade.TradeResult;
import server.maps.MaplePortal;

public final class ChangeMapSpecialHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      byte type = p.readByte();
      String startwp = p.readString();
      short xRange = p.readShort();
      short yRange = p.readShort();

      MaplePortal portal = c.getPlayer().getMap().getPortal(startwp);
      if (portal == null || c.getPlayer().portalDelay() > currentServerTime() || c.getPlayer().getBlockedPortals()
            .contains(portal.getScriptName())) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }
      if (c.getPlayer().isChangingMaps() || c.getPlayer().isBanned()) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }
      if (c.getPlayer().getTrade() != null) {
         MapleTrade.cancelTrade(c.getPlayer(), TradeResult.UNSUCCESSFUL_ANOTHER_MAP);
      }
      portal.enterPortal(c);
   }
}
