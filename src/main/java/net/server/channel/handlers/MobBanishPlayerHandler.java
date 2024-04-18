package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.life.MapleLifeFactory.BanishInfo;
import server.life.MapleMonster;

public final class MobBanishPlayerHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int mobid = p.readInt();     // mob banish handling detected thanks to MedicOP

      MapleCharacter chr = c.getPlayer();
      MapleMonster mob = chr.getMap().getMonsterById(mobid);

      if (mob != null) {
         BanishInfo banishInfo = mob.getBanish();
         if (banishInfo != null) {
            chr.changeMapBanish(banishInfo.getMap(), banishInfo.getPortal(), banishInfo.getMsg());
         }
      }
   }
}