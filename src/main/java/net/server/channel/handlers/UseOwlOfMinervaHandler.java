package net.server.channel.handlers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import client.MapleClient;
import connection.packets.CWvsContext;
import constants.game.GameConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.world.OwlSearchResult;

public final class UseOwlOfMinervaHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      List<OwlSearchResult> owlSearched = c.getWorldServer().getOwlSearchedItems();
      List<Integer> owlLeaderboards;

      if (owlSearched.size() < 5) {
         owlLeaderboards = Arrays.stream(GameConstants.OWL_DATA).boxed().collect(Collectors.toList());
      } else {
         owlLeaderboards = owlSearched.stream().sorted(Comparator.comparingInt(OwlSearchResult::count).reversed())
               .limit(Math.min(owlSearched.size(), 10)).map(OwlSearchResult::itemId).collect(Collectors.toList());
      }

      c.sendPacket(CWvsContext.getOwlOpen(owlLeaderboards));
   }
}