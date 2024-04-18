package net.server.channel.handlers;

import client.MapleClient;
import client.processor.stat.AssignSPProcessor;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class DistributeSPHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      p.readInt();
      int skillid = p.readInt();
      AssignSPProcessor.SPAssignAction(c, skillid);
   }
}