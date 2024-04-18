package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.ItemInformationProvider;

public final class CancelItemEffectHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int itemId = -p.readInt();
      if (ItemInformationProvider.getInstance().noCancelMouse(itemId)) {
         return;
      }
      c.getPlayer().cancelEffect(itemId);
   }
}