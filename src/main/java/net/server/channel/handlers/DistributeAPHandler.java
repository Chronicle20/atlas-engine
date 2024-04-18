package net.server.channel.handlers;

import client.MapleClient;
import client.processor.stat.AssignAPProcessor;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class DistributeAPHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      p.readInt();
      int num = p.readInt();

      AssignAPProcessor.APAssignAction(c, num);
   }
}
