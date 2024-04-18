package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import scripting.reactor.ReactorScriptManager;
import server.maps.MapleReactor;

public final class TouchReactorHandler extends AbstractMaplePacketHandler {

   private static void touchReactor(MapleClient c, byte mode, MapleReactor reactor) {
      if (mode != 0) {
         ReactorScriptManager.getInstance().touch(c, reactor);
      } else {
         ReactorScriptManager.getInstance().untouch(c, reactor);
      }
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int oid = p.readInt();
      byte mode = p.readByte();
      c.getPlayer().getMap().getReactorByOid(oid).ifPresent(r -> touchReactor(c, mode, r));
   }
}
