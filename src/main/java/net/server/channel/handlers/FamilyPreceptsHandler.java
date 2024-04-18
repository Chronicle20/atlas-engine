package net.server.channel.handlers;

import java.util.Optional;

import client.MapleClient;
import client.MapleFamily;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public class FamilyPreceptsHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      Optional<MapleFamily> family = c.getPlayer().getFamily();
      if (family.isEmpty()) {
         return;
      }
      if (family.get().getLeader().getChr() != c.getPlayer()) {
         return; //only the leader can set the precepts
      }
      String newPrecepts = p.readString();
      if (newPrecepts.length() > 200) {
         return;
      }
      family.get().setMessage(newPrecepts, true);
      //family.broadcastFamilyInfoUpdate(); //probably don't need to broadcast for this?
      c.sendPacket(CWvsContext.getFamilyInfo(c.getPlayer().getFamilyEntry()));
   }
}
