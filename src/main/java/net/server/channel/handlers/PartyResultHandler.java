package net.server.channel.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public class PartyResultHandler extends AbstractMaplePacketHandler {
   private final static Logger log = LoggerFactory.getLogger(PartyResultHandler.class);

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      byte action = p.readByte();
      int partyId = p.readInt();
      int applierId = p.readInt();
      log.debug("Action {}, Party ID: {}, Applier ID: {}", action, partyId, applierId);
   }
}
