package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class UseWaterOfLifeHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      c.getAbstractPlayerInteraction().openNpc(1032102, "waterOfLife");
   }
}