package net.server.channel.handlers;

import client.MapleClient;
import client.autoban.AutobanFactory;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public class UseGachaExpHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {

      if (c.tryacquireClient()) {
         try {
            if (c.getPlayer().getGachaExp() <= 0) {
               AutobanFactory.GACHA_EXP.autoban(c.getPlayer(), "Player tried to redeem GachaEXP, but had none to redeem.");
            }
            c.getPlayer().gainGachaExp();
         } finally {
            c.releaseClient();
         }
      }

      c.sendPacket(CWvsContext.enableActions());
   }
}
