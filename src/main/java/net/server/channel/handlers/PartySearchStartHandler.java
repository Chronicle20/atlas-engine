package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public class PartySearchStartHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient client) {
      int min = p.readInt();
      int max = p.readInt();

      if (min > max) {
         client.getPlayer().dropMessage(1, "The min. value is higher than the max!");
         client.sendPacket(CWvsContext.enableActions());
         return;
      }

      if (max - min > 30) {
         client.getPlayer().dropMessage(1, "You can only search for party members within a range of 30 levels.");
         client.sendPacket(CWvsContext.enableActions());
         return;
      }

      if (client.getPlayer().getLevel() < min || client.getPlayer().getLevel() > max) {
         client.getPlayer().dropMessage(1, "The range of level for search has to include your own level.");
         client.sendPacket(CWvsContext.enableActions());
         return;
      }

      p.readInt(); // members
      int jobs = p.readInt();

      client.getPlayer().getParty().filter(_ -> client.getPlayer().isPartyLeader()).ifPresent(
            _ -> client.getWorldServer().getPartySearchCoordinator().registerPartyLeader(client.getPlayer(), min, max, jobs));
   }
}