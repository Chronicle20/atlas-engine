package net.server.channel.handlers;

import client.MapleClient;
import client.processor.action.MakerProcessor;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class MakerSkillHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MakerProcessor.makerAction(p, c);
   }
}
