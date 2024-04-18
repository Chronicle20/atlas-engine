package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.autoban.AutobanFactory;
import client.autoban.AutobanManager;
import connection.packets.CUser;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import server.maps.MapleMap;

public final class HealOvertimeHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
      if (!chr.isLoggedinWorld()) {
         return;
      }

      AutobanManager abm = chr.getAutobanManager();
      int timestamp = Server.getInstance().getCurrentTimestamp();
      p.skip(8);

      short healHP = p.readShort();
      if (healHP != 0) {
         abm.setTimestamp(8, timestamp, 28);  // thanks Vcoc & Thora for pointing out d/c happening here
         if ((abm.getLastSpam(0) + 1500) > timestamp) {
            AutobanFactory.FAST_HP_HEALING.addPoint(abm, "Fast hp healing");
         }

         MapleMap map = chr.getMap();
         int abHeal =
               (int) (77 * map.getRecovery() * 1.5); // thanks Ari for noticing players not getting healed in sauna in certain cases
         if (healHP > abHeal) {
            AutobanFactory.HIGH_HP_HEALING.autoban(chr, "Healing: " + healHP + "; Max is " + abHeal + ".");
            return;
         }

         chr.addHP(healHP);
         chr.getMap().broadcastMessage(chr, CUser.showHpHealed(chr.getId(), healHP), false);
         abm.spam(0, timestamp);
      }
      short healMP = p.readShort();
      if (healMP != 0 && healMP < 1000) {
         abm.setTimestamp(9, timestamp, 28);
         if ((abm.getLastSpam(1) + 1500) > timestamp) {
            AutobanFactory.FAST_MP_HEALING.addPoint(abm, "Fast mp healing");
            return;     // thanks resinate for noticing mp being gained even after detection
         }
         chr.addMP(healMP);
         abm.spam(1, timestamp);
      }
   }
}
