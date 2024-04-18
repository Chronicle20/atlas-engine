package net.server.channel.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleFamily;
import client.MapleFamilyEntry;
import config.YamlConfig;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.coordinator.world.MapleInviteCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator.InviteResult;
import net.server.coordinator.world.MapleInviteCoordinator.InviteType;
import net.server.coordinator.world.MapleInviteCoordinator.MapleInviteResult;
import tools.DatabaseConnection;

public final class AcceptFamilyHandler extends AbstractMaplePacketHandler {
   private final static Logger log = LoggerFactory.getLogger(AcceptFamilyHandler.class);

   private static void insertNewFamilyRecord(int characterID, int familyID, int seniorID, boolean updateChar) {
      try (Connection con = DatabaseConnection.getConnection()) {
         try (PreparedStatement ps = con.prepareStatement(
               "INSERT INTO family_character (cid, familyid, seniorid) VALUES (?, ?, ?)")) {
            ps.setInt(1, characterID);
            ps.setInt(2, familyID);
            ps.setInt(3, seniorID);
            ps.executeUpdate();
         } catch (SQLException e) {
            log.error("Could not save new family record for char id {}.", characterID);
         }
         if (updateChar) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET familyid = ? WHERE id = ?")) {
               ps.setInt(1, familyID);
               ps.setInt(2, characterID);
               ps.executeUpdate();
            } catch (SQLException e) {
               log.error("Could not update 'characters' 'familyid' record for char id {}.", characterID);
            }
         }
      } catch (SQLException e) {
         log.error("Could not get connection to DB.");
      }
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (!YamlConfig.config.server.USE_FAMILY_SYSTEM) {
         return;
      }
      MapleCharacter chr = c.getPlayer();
      int inviterId = p.readInt();
      p.readString();
      boolean accept = p.readByte() != 0;
      // String inviterName = slea.readMapleAsciiString();
      MapleCharacter inviter = c.getWorldServer().getPlayerStorage().getCharacterById(inviterId).orElse(null);
      if (inviter != null) {
         MapleInviteResult inviteResult =
               MapleInviteCoordinator.answerInvite(InviteType.FAMILY, c.getPlayer().getId(), c.getPlayer(), accept);
         if (inviteResult.result == InviteResult.NOT_FOUND) {
            return; //was never invited. (or expired on server only somehow?)
         }
         if (accept) {
            if (inviter.getFamily().isPresent()) {
               if (chr.getFamily().isEmpty()) {
                  MapleFamilyEntry newEntry =
                        new MapleFamilyEntry(inviter.getFamily().get(), chr.getId(), chr.getName(), chr.getLevel(), chr.getJob());
                  newEntry.setCharacter(chr);
                  if (!newEntry.setSenior(inviter.getFamilyEntry(), true)) {
                     inviter.sendPacket(CWvsContext.sendFamilyMessage(1, 0));
                     return;
                  } else {
                     // save
                     inviter.getFamily().get().addEntry(newEntry);
                     insertNewFamilyRecord(chr.getId(), inviter.getFamily().get().getID(), inviter.getId(), false);
                  }
               } else { //absorb target family
                  MapleFamilyEntry targetEntry = chr.getFamilyEntry();
                  MapleFamily targetFamily = targetEntry.getFamily();
                  if (targetFamily.getLeader() != targetEntry) {
                     return;
                  }
                  if (inviter.getFamily().get().getTotalGenerations() + targetFamily.getTotalGenerations()
                        <= YamlConfig.config.server.FAMILY_MAX_GENERATIONS) {
                     targetEntry.join(inviter.getFamilyEntry());
                  } else {
                     inviter.sendPacket(CWvsContext.sendFamilyMessage(76, 0));
                     chr.sendPacket(CWvsContext.sendFamilyMessage(76, 0));
                     return;
                  }
               }
            } else { // create new family
               if (chr.getFamily().isPresent() && inviter.getFamily().isPresent()
                     && chr.getFamily().get().getTotalGenerations() + inviter.getFamily().get().getTotalGenerations()
                     >= YamlConfig.config.server.FAMILY_MAX_GENERATIONS) {
                  inviter.sendPacket(CWvsContext.sendFamilyMessage(76, 0));
                  chr.sendPacket(CWvsContext.sendFamilyMessage(76, 0));
                  return;
               }
               MapleFamily newFamily = new MapleFamily(-1, c.getWorld());
               c.getWorldServer().addFamily(newFamily.getID(), newFamily);
               MapleFamilyEntry inviterEntry =
                     new MapleFamilyEntry(newFamily, inviter.getId(), inviter.getName(), inviter.getLevel(), inviter.getJob());
               inviterEntry.setCharacter(inviter);
               newFamily.setLeader(inviter.getFamilyEntry());
               newFamily.addEntry(inviterEntry);
               if (chr.getFamily().isEmpty()) { //completely new family
                  MapleFamilyEntry newEntry =
                        new MapleFamilyEntry(newFamily, chr.getId(), chr.getName(), chr.getLevel(), chr.getJob());
                  newEntry.setCharacter(chr);
                  newEntry.setSenior(inviterEntry, true);
                  // save new family
                  insertNewFamilyRecord(inviter.getId(), newFamily.getID(), 0, true);
                  insertNewFamilyRecord(chr.getId(), newFamily.getID(), inviter.getId(),
                        false); // char was already saved from setSenior() above
                  newFamily.setMessage("", true);
               } else { //new family for inviter, absorb invitee family
                  insertNewFamilyRecord(inviter.getId(), newFamily.getID(), 0, true);
                  newFamily.setMessage("", true);
                  chr.getFamilyEntry().join(inviterEntry);
               }
            }
            c.getPlayer().getFamily().get()
                  .broadcast(CWvsContext.sendFamilyJoinResponse(true, c.getPlayer().getName()), c.getPlayer().getId());
            c.sendPacket(CWvsContext.getSeniorMessage(inviter.getName()));
            c.sendPacket(CWvsContext.getFamilyInfo(chr.getFamilyEntry()));
            chr.getFamilyEntry().updateSeniorFamilyInfo(true);
         } else {
            inviter.sendPacket(CWvsContext.sendFamilyJoinResponse(false, c.getPlayer().getName()));
         }
      }
      c.sendPacket(CWvsContext.sendFamilyMessage(0, 0));
   }
}
