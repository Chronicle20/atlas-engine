package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class CharInfoRequestHandler extends AbstractMaplePacketHandler {

   private static void showInfo(MapleClient c, MapleCharacter target) {
      if (c.getPlayer().getId() != target.getId()) {
         target.exportExcludedItems(c);
      }
      c.sendPacket(CWvsContext.charInfo(target));
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int updateTime = p.readInt();
      int characterId = p.readInt();
      byte petInfo = p.readByte();
      c.getPlayer().getMap().getCharacterByOid(characterId).ifPresent(t -> showInfo(c, t));
   }
}
