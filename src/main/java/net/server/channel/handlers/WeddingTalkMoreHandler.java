package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import scripting.event.EventInstanceManager;
import tools.packets.Wedding;

public final class WeddingTalkMoreHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      EventInstanceManager eim = c.getPlayer().getEventInstance().orElse(null);
      if (eim != null && !(c.getPlayer().getId() == eim.getIntProperty("groomId") || c.getPlayer().getId() == eim.getIntProperty(
            "brideId"))) {
         eim.gridInsert(c.getPlayer(), 1);
         c.getPlayer().dropMessage(5,
               "High Priest John: Your blessings have been added to their love. What a noble act for a lovely couple!");
      }

      c.sendPacket(Wedding.OnWeddingProgress(true, 0, 0, (byte) 3));
      c.sendPacket(CWvsContext.enableActions());
   }
}