package net.server.handlers.login;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class ServerStatusRequestHandler extends AbstractMaplePacketHandler {
   private static final Logger log = LoggerFactory.getLogger(ServerStatusRequestHandler.class);

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      // Original intent of this packet has changed.
      log.debug("Account {} has returned to world selection.", c.getAccountName());
   }
}
