package net.server.channel.handlers;

import client.MapleClient;
import client.processor.npc.StorageProcessor;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class StorageHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      StorageProcessor.storageAction(p, c);
   }
}