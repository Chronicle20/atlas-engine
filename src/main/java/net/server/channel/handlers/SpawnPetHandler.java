package net.server.channel.handlers;

import client.MapleClient;
import client.processor.action.SpawnPetProcessor;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class SpawnPetHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int updateTime = p.readInt();
      short slot = p.readShort();
      boolean lead = p.readBool();
      SpawnPetProcessor.processSpawnPet(c, slot, lead);
   }
}
