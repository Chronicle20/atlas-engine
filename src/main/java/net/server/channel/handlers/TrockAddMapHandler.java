package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.maps.FieldLimit;

public final class TrockAddMapHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
      byte type = p.readByte();
      boolean vip = p.readByte() == 1;
      if (type == 0x00) {
         int mapId = p.readInt();
         if (vip) {
            chr.deleteFromVipTrocks(mapId);
         } else {
            chr.deleteFromTrocks(mapId);
         }
         c.sendPacket(CWvsContext.trockRefreshMapList(chr, true, vip));
      } else if (type == 0x01) {
         if (!FieldLimit.CANNOTVIPROCK.check(chr.getMap().getFieldLimit())) {
            if (vip) {
               chr.addVipTrockMap();
            } else {
               chr.addTrockMap();
            }

            c.sendPacket(CWvsContext.trockRefreshMapList(chr, false, vip));
         } else {
            chr.message("You may not save this map.");
         }
      }
   }
}
