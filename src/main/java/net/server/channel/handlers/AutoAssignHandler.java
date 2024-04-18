package net.server.channel.handlers;

import client.MapleClient;
import client.processor.stat.AssignAPProcessor;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public class AutoAssignHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      AssignAPProcessor.APAutoAssignAction(p, c);
   }
}
