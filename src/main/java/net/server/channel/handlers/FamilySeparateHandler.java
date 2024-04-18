package net.server.channel.handlers;

import java.util.Optional;

import client.MapleClient;
import client.MapleFamily;
import client.MapleFamilyEntry;
import config.YamlConfig;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public class FamilySeparateHandler extends AbstractMaplePacketHandler {

   private static int separateRepCost(MapleFamilyEntry junior) {
      int level = junior.getLevel();
      int ret = level / 20;
      ret += 10;
      ret *= level;
      ret *= 2;
      return ret;
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (!YamlConfig.config.server.USE_FAMILY_SYSTEM) {
         return;
      }
      Optional<MapleFamily> oldFamily = c.getPlayer().getFamily();
      if (oldFamily.isEmpty()) {
         return;
      }
      MapleFamilyEntry forkOn;
      boolean isSenior;
      if (p.available() > 0) { //packet 0x95 doesn't send id, since there is only one senior
         forkOn = c.getPlayer().getFamily().map(f -> f.getEntryByID(p.readInt())).orElse(null);
         if (!c.getPlayer().getFamilyEntry().isJunior(forkOn)) {
            return; //packet editing?
         }
         isSenior = true;
      } else {
         forkOn = c.getPlayer().getFamilyEntry();
         isSenior = false;
      }
      if (forkOn == null) {
         return;
      }

      MapleFamilyEntry senior = forkOn.getSenior();
      if (senior == null) {
         return;
      }
      int levelDiff = Math.abs(c.getPlayer().getLevel() - senior.getLevel());
      int cost = 2500 * levelDiff;
      cost += levelDiff * levelDiff;
      if (c.getPlayer().getMeso() < cost) {
         c.sendPacket(CWvsContext.sendFamilyMessage(isSenior ? 81 : 80, cost));
         return;
      }
      c.getPlayer().gainMeso(-cost);
      int repCost = separateRepCost(forkOn);
      senior.gainReputation(-repCost, false);
      if (senior.getSenior() != null) {
         senior.getSenior().gainReputation(-(repCost / 2), false);
      }
      forkOn.announceToSenior(CWvsContext.serverNotice(5, forkOn.getName() + " has left the family."), true);
      forkOn.fork();
      c.sendPacket(CWvsContext.getFamilyInfo(forkOn)); //pedigree info will be requested from the client if the window is open
      forkOn.updateSeniorFamilyInfo(true);
      c.sendPacket(CWvsContext.sendFamilyMessage(1, 0));
   }
}
