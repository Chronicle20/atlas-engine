package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class MonsterBookCoverHandler extends AbstractMaplePacketHandler {
   public void handlePacket(InPacket p, MapleClient c) {
      int id = p.readInt();
      if (id == 0 || id / 10000 == 238) {
         c.getPlayer().setMonsterBookCover(id);
         c.sendPacket(CWvsContext.changeCover(id));
      }
   }
}
