package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CMapLoadable;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.maps.MapleDoorObject;
import server.maps.MapleMapObject;

public final class DoorHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int ownerid = p.readInt();
      p.readByte(); // specifies if backwarp or not, 1 town to target, 0 target to town

      MapleCharacter chr = c.getPlayer();
      if (chr.isChangingMaps() || chr.isBanned()) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }

      for (MapleMapObject obj : chr.getMap().getMapObjects()) {
         if (obj instanceof MapleDoorObject door) {
            if (door.getOwnerId() == ownerid) {
               door.warp(chr);
               return;
            }
         }
      }

      c.sendPacket(CMapLoadable.blockedMessage(6));
      c.sendPacket(CWvsContext.enableActions());
   }
}
