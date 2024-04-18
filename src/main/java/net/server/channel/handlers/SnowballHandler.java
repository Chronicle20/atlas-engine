package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.events.gm.MapleSnowball;
import server.maps.MapleMap;

public final class SnowballHandler extends AbstractMaplePacketHandler {

   public void handlePacket(InPacket p, MapleClient c) {
      //D3 00 02 00 00 A5 01
      MapleCharacter chr = c.getPlayer();
      MapleMap map = chr.getMap();
      final MapleSnowball snowball = map.getSnowball(chr.getTeam());
      final MapleSnowball othersnowball = map.getSnowball(chr.getTeam() == 0 ? (byte) 1 : 0);
      int what = p.readByte();
      //slea.skip(4);

      if (snowball == null || othersnowball == null || snowball.getSnowmanHP() == 0) {
         return;
      }
      if ((currentServerTime() - chr.getLastSnowballAttack()) < 500) {
         return;
      }
      if (chr.getTeam() != (what % 2)) {
         return;
      }

      chr.setLastSnowballAttack(currentServerTime());
      int damage = 0;
      if (what < 2 && othersnowball.getSnowmanHP() > 0) {
         damage = 10;
      } else if (what == 2 || what == 3) {
         if (Math.random() < 0.03) {
            damage = 45;
         } else {
            damage = 15;
         }
      }

      if (what >= 0 && what <= 4) {
         snowball.hit(what, damage);
      }
   }
}
