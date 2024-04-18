package net.server.channel.handlers;

import java.util.Optional;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.coordinator.world.MapleInviteCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator.InviteResult;
import net.server.coordinator.world.MapleInviteCoordinator.InviteType;
import net.server.world.MapleParty;

public final class DenyPartyRequestHandler extends AbstractMaplePacketHandler {

   private static void denyPartyRequest(MapleCharacter character, MapleCharacter characterFrom) {
      if (MapleInviteCoordinator.answerInvite(InviteType.PARTY, character.getId(), characterFrom.getPartyId().orElse(-1),
            false).result == InviteResult.DENIED) {
         character.updatePartySearchAvailability(!character.hasParty());
         characterFrom.sendPacket(CWvsContext.partyStatusMessage(23, character.getName()));
      }
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      byte action = p.readByte();
      int partyId = p.readInt();

      Optional<MapleParty> party = Server.getInstance().getWorld(c.getWorld()).flatMap(w -> w.getParty(partyId));
      if (party.isEmpty()) {
         return;
      }

      if (action == 0x1B) {
         acceptInvite(c, party.get());
      } else if (action != 0x16) {
         denyInvite(c, party.get());
      }
   }

   private void denyInvite(MapleClient c, MapleParty party) {
      c.getChannelServer().getPlayerStorage().getCharacterById(party.getLeaderId())
            .ifPresent(characterFrom -> denyPartyRequest(c.getPlayer(), characterFrom));
   }

   private void acceptInvite(MapleClient c, MapleParty party) {
      MapleCharacter character = c.getPlayer();
      MapleInviteCoordinator.MapleInviteResult inviteRes =
            MapleInviteCoordinator.answerInvite(InviteType.PARTY, character.getId(), party.getId(), true);
      InviteResult res = inviteRes.result;
      if (res == InviteResult.ACCEPTED) {
         MapleParty.joinParty(character, party.getId(), false);
      } else {
         c.sendPacket(CWvsContext.serverNotice(5, "You couldn't join the party due to an expired invitation request."));
      }
   }
}
