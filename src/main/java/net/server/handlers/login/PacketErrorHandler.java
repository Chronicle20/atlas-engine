package net.server.handlers.login;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import tools.HexTool;

public class PacketErrorHandler extends AbstractMaplePacketHandler {

   private static final Logger log = LoggerFactory.getLogger(AbstractMaplePacketHandler.class);

   @Override
   public final void handlePacket(InPacket p, MapleClient c) {
      short type = p.readShort();
      int errortype = p.readInt(); // example error 38
      if (errortype == 0) { // i don't wanna log error code 0 stuffs, (usually some bounceback to login)
         return;
      }
      short data_length = p.readShort();
      p.skip(4);
      short opcodeheader = p.readShort();
      log.debug("Error Type: {}", errortype);
      log.debug("Data Length: {}", data_length);
      log.debug("Character: {} Map: {} - Account: {}", c.getPlayer() == null ? "" : c.getPlayer().getName(),
            c.getPlayer() == null ? "" : c.getPlayer().getMap().getId(), c.getAccountName());
      log.debug("Opcode: {}", opcodeheader);
      log.debug("{}", HexTool.toString(p.readBytes(p.available())));
   }

   @Override
   public boolean validateState(MapleClient c) {
      return true;
   }
}
