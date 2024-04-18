package net.server.channel.handlers;

import client.MapleClient;
import client.autoban.AutobanFactory;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;

public final class ChangeChannelHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int channel = p.readByte() + 1;
      p.readInt();
      c.getPlayer().getAutobanManager().setTimestamp(6, Server.getInstance().getCurrentTimestamp(), 3);
      if (c.getChannel() == channel) {
         AutobanFactory.GENERAL.alert(c.getPlayer(), "CCing to same channel.");
         c.disconnect(false, false);
         return;
      } else if (c.getPlayer().getCashShop().isOpened() || c.getPlayer().getMiniGame() != null
            || c.getPlayer().getPlayerShop() != null) {
         return;
      }

      c.changeChannel(channel);
   }
}