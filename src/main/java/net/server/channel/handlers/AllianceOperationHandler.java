package net.server.channel.handlers;

import java.util.Optional;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.guild.MapleAlliance;
import net.server.guild.MapleGuild;
import net.server.guild.MapleGuildCharacter;
import net.server.world.World;

public final class AllianceOperationHandler extends AbstractMaplePacketHandler {

   private static void expelGuild(MapleClient c, MapleAlliance alliance, MapleCharacter chr, int guildid, int allianceid) {
      if (chr.getGuild().map(MapleGuild::getAllianceId).orElse(0) == 0
            || chr.getGuild().map(MapleGuild::getAllianceId).orElse(0) != allianceid) {
         return;
      }

      Server.getInstance()
            .allianceMessage(alliance.getId(), CWvsContext.removeGuildFromAlliance(alliance, guildid, c.getWorld()), -1, -1);
      Server.getInstance().removeGuildFromAlliance(alliance.getId(), guildid);

      Server.getInstance().allianceMessage(alliance.getId(), CWvsContext.getGuildAlliances(alliance, c.getWorld()), -1, -1);
      Server.getInstance()
            .allianceMessage(alliance.getId(), CWvsContext.allianceNotice(alliance.getId(), alliance.getNotice()), -1, -1);
      Server.getInstance().guildMessage(guildid, CWvsContext.disbandAlliance(allianceid));

      String guildName = Server.getInstance().getGuild(guildid).map(MapleGuild::getName).orElse("");
      alliance.dropMessage("[" + guildName + "] guild has been expelled from the union.");
   }

   private static void sendInvite(MapleClient c, MapleAlliance alliance, MapleCharacter chr, String guildName) {
      if (alliance.getGuilds().size() == alliance.getCapacity()) {
         chr.dropMessage(5, "Your alliance cannot comport any more guilds at the moment.");
      } else {
         MapleAlliance.sendInvitation(c, guildName, alliance.getId());
      }
   }

   @Override
   public void handlePacket(InPacket p, MapleClient client) {
      MapleCharacter chr = client.getPlayer();

      if (chr.getGuild().isEmpty()) {
         client.sendPacket(CWvsContext.enableActions());
         return;
      }

      Optional<MapleAlliance> alliance = Optional.empty();
      if (chr.getGuild().map(MapleGuild::getAllianceId).orElse(0) > 0) {
         alliance = chr.getAlliance();
      }

      byte b = p.readByte();
      if (alliance.isEmpty()) {
         if (b != 4) {
            client.sendPacket(CWvsContext.enableActions());
            return;
         }
      } else {
         if (b == 4) {
            chr.dropMessage(5, "Your guild is already registered on a guild alliance.");
            client.sendPacket(CWvsContext.enableActions());
            return;
         }

         if (chr.getMGC().orElseThrow().getAllianceRank() > 2 || !alliance.get().getGuilds().contains(chr.getGuildId())) {
            client.sendPacket(CWvsContext.enableActions());
            return;
         }
      }

      // "alliance" is only null at case 0x04
      switch (b) {
         case 0x01:
            chr.getGuild().map(MapleGuild::getAllianceId)
                  .ifPresent(id -> Server.getInstance().allianceMessage(id, CWvsContext.sendShowInfo(id, chr.getId()), -1, -1));
            break;
         case 0x02: { // Leave Alliance
            if (chr.getGuild().map(MapleGuild::getAllianceId).orElse(0) == 0 || chr.getGuildId() < 1 || chr.getGuildRank() != 1) {
               return;
            }
            chr.getGuild().map(MapleGuild::getAllianceId)
                  .ifPresent(id -> MapleAlliance.removeGuildFromAlliance(id, chr.getGuildId(), chr.getWorld()));
            break;
         }
         case 0x03: // Send Invite
            String guildName = p.readString();
            sendInvite(client, alliance.get(), chr, guildName);
            break;
         case 0x04: { // Accept Invite
            Optional<MapleGuild> guild = chr.getGuild();
            if (guild.map(MapleGuild::getAllianceId).orElse(0) != 0 || chr.getGuildRank() != 1 || chr.getGuildId() < 1) {
               return;
            }

            int allianceid = p.readInt();
            //slea.readMapleAsciiString();  //recruiter's guild name

            alliance = Server.getInstance().getAlliance(allianceid);
            if (alliance.isEmpty()) {
               return;
            }

            if (!MapleAlliance.answerInvitation(client.getPlayer().getId(), guild.map(MapleGuild::getName).orElse(""),
                  alliance.get().getId(), true)) {
               return;
            }

            if (alliance.get().getGuilds().size() == alliance.get().getCapacity()) {
               chr.dropMessage(5, "Your alliance cannot comport any more guilds at the moment.");
               return;
            }

            int guildid = chr.getGuildId();

            Server.getInstance().addGuildtoAlliance(alliance.get().getId(), guildid);
            Server.getInstance().resetAllianceGuildPlayersRank(guildid);

            chr.getMGC().orElseThrow().setAllianceRank(2);
            Server.getInstance().getGuild(chr.getGuildId()).ifPresent(g -> g.getMGC(chr.getId()).orElseThrow().setAllianceRank(2));

            chr.saveGuildStatus();

            Server.getInstance()
                  .allianceMessage(alliance.get().getId(), CWvsContext.addGuildToAlliance(alliance.get(), guildid, client), -1, -1);
            Server.getInstance()
                  .allianceMessage(alliance.get().getId(), CWvsContext.updateAllianceInfo(alliance.get(), client.getWorld()), -1,
                        -1);
            Server.getInstance().allianceMessage(alliance.get().getId(),
                  CWvsContext.allianceNotice(alliance.get().getId(), alliance.get().getNotice()), -1, -1);
            guild.get().dropMessage("Your guild has joined the [" + alliance.get().getName() + "] union.");

            break;
         }
         case 0x06: { // Expel Guild
            int guildid = p.readInt();
            int allianceid = p.readInt();
            expelGuild(client, alliance.get(), chr, guildid, allianceid);
            break;
         }
         case 0x07: { // Change Alliance Leader
            if (chr.getGuild().map(MapleGuild::getAllianceId).orElse(0) == 0 || chr.getGuildId() < 1) {
               return;
            }
            int victimid = p.readInt();
            Optional<MapleCharacter> player = Server.getInstance().getWorld(client.getWorld()).map(World::getPlayerStorage)
                  .flatMap(s -> s.getCharacterById(victimid));
            if (player.isEmpty() || player.get().getAllianceRank() != 2) {
               return;
            }

            //Server.getInstance().allianceMessage(alliance.getId(), sendChangeLeader(chr.getGuild().getAllianceId(), chr.getId(), slea.readInt()), -1, -1);
            changeLeaderAllianceRank(alliance.get(), player.get());
            break;
         }
         case 0x08:
            String[] ranks = new String[5];
            for (int i = 0; i < 5; i++) {
               ranks[i] = p.readString();
            }
            Server.getInstance().setAllianceRanks(alliance.get().getId(), ranks);
            Server.getInstance()
                  .allianceMessage(alliance.get().getId(), CWvsContext.changeAllianceRankTitle(alliance.get().getId(), ranks), -1,
                        -1);
            break;
         case 0x09: {
            int int1 = p.readInt();
            byte byte1 = p.readByte();

            //Server.getInstance().allianceMessage(alliance.getId(), sendChangeRank(chr.getGuild().getAllianceId(), chr.getId(), int1, byte1), -1, -1);
            Optional<MapleCharacter> player = Server.getInstance().getWorld(client.getWorld()).map(World::getPlayerStorage)
                  .flatMap(s -> s.getCharacterById(int1));
            if (player.isEmpty()) {
               return;
            }
            changePlayerAllianceRank(alliance.get(), player.get(), (byte1 > 0));
            break;
         }
         case 0x0A:
            String notice = p.readString();
            Server.getInstance().setAllianceNotice(alliance.get().getId(), notice);
            Server.getInstance()
                  .allianceMessage(alliance.get().getId(), CWvsContext.allianceNotice(alliance.get().getId(), notice), -1, -1);

            alliance.get().dropMessage(5, "* Alliance Notice : " + notice);
            break;
         default:
            chr.dropMessage("Feature not available");
      }

      alliance.get().saveToDB();
   }

   private void changeLeaderAllianceRank(MapleAlliance alliance, MapleCharacter newLeader) {
      Optional<MapleCharacter> oldLeader = alliance.getLeader().map(MapleGuildCharacter::getId)
            .flatMap(id -> newLeader.getWorldServer().getPlayerStorage().getCharacterById(id));
      if (oldLeader.isEmpty()) {
         return;
      }

      oldLeader.get().getMGC().orElseThrow().setAllianceRank(2);
      oldLeader.get().saveGuildStatus();

      newLeader.getMGC().orElseThrow().setAllianceRank(1);
      newLeader.saveGuildStatus();

      Server.getInstance().allianceMessage(alliance.getId(), CWvsContext.getGuildAlliances(alliance, newLeader.getWorld()), -1, -1);
      alliance.dropMessage("'" + newLeader.getName() + "' has been appointed as the new head of this Alliance.");
   }

   private void changePlayerAllianceRank(MapleAlliance alliance, MapleCharacter chr, boolean raise) {
      int newRank = chr.getAllianceRank() + (raise ? -1 : 1);
      if (newRank < 3 || newRank > 5) {
         return;
      }

      chr.getMGC().orElseThrow().setAllianceRank(newRank);
      chr.saveGuildStatus();

      Server.getInstance().allianceMessage(alliance.getId(), CWvsContext.getGuildAlliances(alliance, chr.getWorld()), -1, -1);
      alliance.dropMessage(
            "'" + chr.getName() + "' has been reassigned to '" + alliance.getRankTitle(newRank) + "' in this Alliance.");
   }
}
