package net.server.channel.handlers;

import java.util.Optional;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.coordinator.world.MapleInviteCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator.InviteResult;
import net.server.coordinator.world.MapleInviteCoordinator.InviteType;
import net.server.coordinator.world.MapleInviteCoordinator.MapleInviteResult;
import net.server.world.MapleParty;
import net.server.world.PartyOperation;

public final class PartyOperationHandler extends AbstractMaplePacketHandler {

   private static void changePartyLeader(MapleClient client, int newLeader) {
      client.getPlayer().getParty().ifPresent(p -> changePartyLeader(client, p, newLeader));
   }

   private static void changePartyLeader(MapleClient client, MapleParty party, int characterId) {
      party.getMemberById(characterId)
            .ifPresent(l -> client.getWorldServer().updateParty(party.getId(), PartyOperation.CHANGE_LEADER, l));
   }

   private static void expelFromParty(MapleClient client, int characterId) {
      client.getPlayer().getParty().ifPresent(p -> MapleParty.expelFromParty(p, client, characterId));
   }

   private static void inviteToParty(MapleClient client, String name) {
      MapleCharacter character = client.getPlayer();
      MapleCharacter invited = client.getWorldServer().getPlayerStorage().getCharacterByName(name).orElse(null);
      if (invited == null) {
         client.sendPacket(CWvsContext.partyStatusMessage(19));
         return;
      }

      if (invited.getLevel() < 10 && (!YamlConfig.config.server.USE_PARTY_FOR_STARTERS
            || character.getLevel() >= 10)) { //min requirement is level 10
         client.sendPacket(CWvsContext.serverNotice(5, "The player you have invited does not meet the requirements."));
         return;
      }
      if (YamlConfig.config.server.USE_PARTY_FOR_STARTERS && invited.getLevel() >= 10
            && character.getLevel() < 10) {    //trying to invite high level
         client.sendPacket(CWvsContext.serverNotice(5, "The player you have invited does not meet the requirements."));
         return;
      }

      if (invited.getParty().isPresent()) {
         client.sendPacket(CWvsContext.partyStatusMessage(16));
         return;
      }

      Optional<MapleParty> party = character.getParty();
      if (party.isEmpty()) {
         if (!MapleParty.createParty(character, false)) {
            return;
         }

         party = character.getParty();
      }

      if (party.isEmpty()) {
         client.sendPacket(CWvsContext.partyStatusMessage(1));
         return;
      }

      if (party.get().getMembers().size() >= 6) {
         client.sendPacket(CWvsContext.partyStatusMessage(17));
         return;
      }

      if (MapleInviteCoordinator.createInvite(InviteType.PARTY, character, party.get().getId(), invited.getId())) {
         invited.sendPacket(CWvsContext.partyInvite(character));
      } else {
         client.sendPacket(CWvsContext.partyStatusMessage(22, invited.getName()));
      }
   }

   private static void joinParty(MapleClient client, int partyId) {
      MapleCharacter character = client.getPlayer();
      MapleInviteResult inviteRes = MapleInviteCoordinator.answerInvite(InviteType.PARTY, character.getId(), partyId, true);
      InviteResult res = inviteRes.result;
      if (res == InviteResult.ACCEPTED) {
         MapleParty.joinParty(character, partyId, false);
      } else {
         client.sendPacket(CWvsContext.serverNotice(5, "You couldn't join the party due to an expired invitation request."));
      }
   }

   private static void leaveOrDisbandParty(MapleClient client) {
      client.getPlayer().getParty().ifPresent(p -> leaveOrDisbandParty(client, p));
   }

   private static void leaveOrDisbandParty(MapleClient client, MapleParty party) {
      MapleCharacter character = client.getPlayer();
      MapleParty.leaveParty(party, client);
      character.updatePartySearchAvailability(true);
      character.partyOperationUpdate(party, character.getPartyMembersOnline());
   }

   private static void createParty(MapleClient client) {
      MapleParty.createParty(client.getPlayer(), false);
   }

   @Override
   public void handlePacket(InPacket p, MapleClient client) {
      int operation = p.readByte();
      switch (operation) {
         case 1 -> createParty(client);
         case 2 -> leaveOrDisbandParty(client);
         case 3 -> {
            int partyId = p.readInt();
            joinParty(client, partyId);
         }
         case 4 -> {
            String name = p.readString();
            inviteToParty(client, name);
         }
         case 5 -> {
            int characterId = p.readInt();
            expelFromParty(client, characterId);
         }
         case 6 -> {
            int newLeader = p.readInt();
            changePartyLeader(client, newLeader);
         }
      }
   }
}