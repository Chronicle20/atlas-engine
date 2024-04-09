/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleFamily;
import client.MapleFamilyEntry;
import config.YamlConfig;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.server.coordinator.world.MapleInviteCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator.InviteResult;
import net.server.coordinator.world.MapleInviteCoordinator.InviteType;
import net.server.coordinator.world.MapleInviteCoordinator.MapleInviteResult;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.data.input.SeekableLittleEndianAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Jay Estrella
 * @author Ubaware
 */
public final class AcceptFamilyHandler extends AbstractMaplePacketHandler {

    private static void insertNewFamilyRecord(int characterID, int familyID, int seniorID, boolean updateChar) {
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO family_character (cid, familyid, seniorid) VALUES (?, ?, ?)")) {
                ps.setInt(1, characterID);
                ps.setInt(2, familyID);
                ps.setInt(3, seniorID);
                ps.executeUpdate();
            } catch (SQLException e) {
                FilePrinter.printError(FilePrinter.FAMILY_ERROR, e, "Could not save new family record for char id " + characterID + ".");
                e.printStackTrace();
            }
            if (updateChar) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET familyid = ? WHERE id = ?")) {
                    ps.setInt(1, familyID);
                    ps.setInt(2, characterID);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    FilePrinter.printError(FilePrinter.FAMILY_ERROR, e, "Could not update 'characters' 'familyid' record for char id " + characterID + ".");
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            FilePrinter.printError(FilePrinter.FAMILY_ERROR, e, "Could not get connection to DB.");
            e.printStackTrace();
        }
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!YamlConfig.config.server.USE_FAMILY_SYSTEM) {
            return;
        }
        MapleCharacter chr = c.getPlayer();
        int inviterId = slea.readInt();
        slea.readMapleAsciiString();
        boolean accept = slea.readByte() != 0;
        // String inviterName = slea.readMapleAsciiString();
        MapleCharacter inviter = c.getWorldServer().getPlayerStorage().getCharacterById(inviterId).orElse(null);
        if (inviter != null) {
            MapleInviteResult inviteResult = MapleInviteCoordinator.answerInvite(InviteType.FAMILY, c.getPlayer().getId(), c.getPlayer(), accept);
            if (inviteResult.result == InviteResult.NOT_FOUND) {
                return; //was never invited. (or expired on server only somehow?)
            }
            if (accept) {
                if (inviter.getFamily().isPresent()) {
                    if (chr.getFamily().isEmpty()) {
                        MapleFamilyEntry newEntry = new MapleFamilyEntry(inviter.getFamily().get(), chr.getId(), chr.getName(), chr.getLevel(), chr.getJob());
                        newEntry.setCharacter(chr);
                        if (!newEntry.setSenior(inviter.getFamilyEntry(), true)) {
                            inviter.announce(CWvsContext.sendFamilyMessage(1, 0));
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
                        if (inviter.getFamily().get().getTotalGenerations() + targetFamily.getTotalGenerations() <= YamlConfig.config.server.FAMILY_MAX_GENERATIONS) {
                            targetEntry.join(inviter.getFamilyEntry());
                        } else {
                            inviter.announce(CWvsContext.sendFamilyMessage(76, 0));
                            chr.announce(CWvsContext.sendFamilyMessage(76, 0));
                            return;
                        }
                    }
                } else { // create new family
                    if (chr.getFamily().isPresent() && inviter.getFamily().isPresent() && chr.getFamily().get().getTotalGenerations() + inviter.getFamily().get().getTotalGenerations() >= YamlConfig.config.server.FAMILY_MAX_GENERATIONS) {
                        inviter.announce(CWvsContext.sendFamilyMessage(76, 0));
                        chr.announce(CWvsContext.sendFamilyMessage(76, 0));
                        return;
                    }
                    MapleFamily newFamily = new MapleFamily(-1, c.getWorld());
                    c.getWorldServer().addFamily(newFamily.getID(), newFamily);
                    MapleFamilyEntry inviterEntry = new MapleFamilyEntry(newFamily, inviter.getId(), inviter.getName(), inviter.getLevel(), inviter.getJob());
                    inviterEntry.setCharacter(inviter);
                    newFamily.setLeader(inviter.getFamilyEntry());
                    newFamily.addEntry(inviterEntry);
                    if (chr.getFamily().isEmpty()) { //completely new family
                        MapleFamilyEntry newEntry = new MapleFamilyEntry(newFamily, chr.getId(), chr.getName(), chr.getLevel(), chr.getJob());
                        newEntry.setCharacter(chr);
                        newEntry.setSenior(inviterEntry, true);
                        // save new family
                        insertNewFamilyRecord(inviter.getId(), newFamily.getID(), 0, true);
                        insertNewFamilyRecord(chr.getId(), newFamily.getID(), inviter.getId(), false); // char was already saved from setSenior() above
                        newFamily.setMessage("", true);
                    } else { //new family for inviter, absorb invitee family
                        insertNewFamilyRecord(inviter.getId(), newFamily.getID(), 0, true);
                        newFamily.setMessage("", true);
                        chr.getFamilyEntry().join(inviterEntry);
                    }
                }
                c.getPlayer().getFamily().get().broadcast(CWvsContext.sendFamilyJoinResponse(true, c.getPlayer().getName()), c.getPlayer().getId());
                c.announce(CWvsContext.getSeniorMessage(inviter.getName()));
                c.announce(CWvsContext.getFamilyInfo(chr.getFamilyEntry()));
                chr.getFamilyEntry().updateSeniorFamilyInfo(true);
            } else {
                inviter.announce(CWvsContext.sendFamilyJoinResponse(false, c.getPlayer().getName()));
            }
        }
        c.announce(CWvsContext.sendFamilyMessage(0, 0));
    }
}
