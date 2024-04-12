package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

public class PartyResultHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
      byte action = slea.readByte();
      int partyId = slea.readInt();
      int applierId = slea.readInt();
      System.out.printf("Action %d, Party ID: %d, Applier ID: %d\n", action, partyId, applierId);
   }
}
