package net.server.channel.handlers;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import connection.packets.CUserRemote;
import connection.packets.CWvsContext;
import constants.game.GameConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.coordinator.matchchecker.MatchCheckerListenerFactory.MatchCheckerType;
import net.server.guild.MapleGuild;
import net.server.guild.MapleGuildResponse;
import net.server.world.MapleParty;
import net.server.world.World;

public final class GuildOperationHandler extends AbstractMaplePacketHandler {
   private static final Logger log = LoggerFactory.getLogger(GuildOperationHandler.class);

   private boolean isGuildNameAcceptable(String name) {
      if (name.length() < 3 || name.length() > 12) {
         return false;
      }
      for (int i = 0; i < name.length(); i++) {
         if (!Character.isLowerCase(name.charAt(i)) && !Character.isUpperCase(name.charAt(i))) {
            return false;
         }
      }
      return true;
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter mc = c.getPlayer();
      byte type = p.readByte();
      int allianceId;
      switch (type) {
         case 0x00:
            //c.sendPacket(MaplePacketCreator.showGuildInfo(mc));
            break;
         case 0x02:
            if (mc.getGuildId() > 0) {
               mc.dropMessage(1, "You cannot create a new Guild while in one.");
               return;
            }
            if (mc.getMeso() < YamlConfig.config.server.CREATE_GUILD_COST) {
               mc.dropMessage(1, "You do not have " + GameConstants.numberWithCommas(YamlConfig.config.server.CREATE_GUILD_COST)
                     + " mesos to create a Guild.");
               return;
            }
            String guildName = p.readString();
            if (!isGuildNameAcceptable(guildName)) {
               mc.dropMessage(1, "The Guild name you have chosen is not accepted.");
               return;
            }

            Set<MapleCharacter> eligibleMembers = new HashSet<>(MapleGuild.getEligiblePlayersForGuild(mc));
            if (eligibleMembers.size() < YamlConfig.config.server.CREATE_GUILD_MIN_PARTNERS) {
               if (mc.getMap().getAllPlayers().size() < YamlConfig.config.server.CREATE_GUILD_MIN_PARTNERS) {
                  // thanks NovaStory for noticing message in need of smoother info
                  mc.dropMessage(1,
                        "Your Guild doesn't have enough cofounders present here and therefore cannot be created at this time.");
               } else {
                  // players may be unaware of not belonging on a party in order to become eligible, thanks Hair (Legalize) for pointing this out
                  mc.dropMessage(1, "Please make sure everyone you are trying to invite is neither on a guild nor on a party.");
               }

               return;
            }

            if (!MapleParty.createParty(mc, true)) {
               mc.dropMessage(1, "You cannot create a new Guild while in a party.");
               return;
            }

            Set<Integer> eligibleCids = new HashSet<>();
            for (MapleCharacter chr : eligibleMembers) {
               eligibleCids.add(chr.getId());
            }

            c.getWorldServer().getMatchCheckerCoordinator()
                  .createMatchConfirmation(MatchCheckerType.GUILD_CREATION, c.getWorld(), mc.getId(), eligibleCids, guildName);
            break;
         case 0x05:
            if (mc.getGuildId() <= 0 || mc.getGuildRank() > 2) {
               return;
            }

            String targetName = p.readString();
            MapleGuildResponse mgr = MapleGuild.sendInvitation(c, targetName);
            if (mgr != null) {
               c.sendPacket(mgr.getPacket(targetName));
            } else {
            } // already sent invitation, do nothing

            break;
         case 0x06:
            if (mc.getGuildId() > 0) {
               log.error("[Hack] {} attempted to join a guild when s/he is already in one.", mc.getName());
               return;
            }
            int gid = p.readInt();
            int cid = p.readInt();
            if (cid != mc.getId()) {
               log.error("[Hack] {} attempted to join a guild with a different character id.", mc.getName());
               return;
            }

            if (!MapleGuild.answerInvitation(cid, mc.getName(), gid, true)) {
               return;
            }

            mc.getMGC().orElseThrow().setGuildId(gid); // joins the guild
            mc.getMGC().orElseThrow().setGuildRank(5); // start at lowest rank
            mc.getMGC().orElseThrow().setAllianceRank(5);

            int s = Server.getInstance().addGuildMember(mc.getMGC().orElseThrow(), mc);
            if (s == 0) {
               mc.dropMessage(1, "The guild you are trying to join is already full.");
               mc.getMGC().ifPresent(mgc -> mgc.setGuildId(0));
               return;
            }

            c.sendPacket(CWvsContext.showGuildInfo(mc));

            allianceId = mc.getGuild().map(MapleGuild::getAllianceId).orElse(0);
            if (allianceId > 0) {
               Server.getInstance().getAlliance(allianceId).ifPresent(a -> a.updateAlliancePackets(mc));
            }

            mc.saveGuildStatus(); // update database
            mc.getMap().broadcastMessage(mc,
                  CUserRemote.guildNameChanged(mc.getId(), mc.getGuild().map(MapleGuild::getName).orElse("")));
            mc.getMap().broadcastMessage(mc, CUserRemote.guildMarkChanged(mc.getId(), mc.getGuild().orElseThrow()));
            break;
         case 0x07:
            cid = p.readInt();
            String name = p.readString();
            if (cid != mc.getId() || !name.equals(mc.getName()) || mc.getGuildId() <= 0) {
               log.error("[Hack] {} tried to quit guild under the name \"{}\" and current guild id of {}.", mc.getName(), name,
                     mc.getGuildId());
               return;
            }

            allianceId = mc.getGuild().map(MapleGuild::getAllianceId).orElse(0);

            c.sendPacket(CWvsContext.updateGP(mc.getGuildId(), 0));
            Server.getInstance().leaveGuild(mc.getMGC().orElseThrow());

            c.sendPacket(CWvsContext.showGuildInfo(null));
            if (allianceId > 0) {
               Server.getInstance().getAlliance(allianceId).ifPresent(a -> a.updateAlliancePackets(mc));
            }

            mc.getMGC().orElseThrow().setGuildId(0);
            mc.getMGC().orElseThrow().setGuildRank(5);
            mc.saveGuildStatus();
            mc.getMap().broadcastMessage(mc, CUserRemote.guildNameChanged(mc.getId(), ""));
            break;
         case 0x08:
            allianceId = mc.getGuild().map(MapleGuild::getAllianceId).orElse(0);

            cid = p.readInt();
            name = p.readString();
            if (mc.getGuildRank() > 2 || mc.getGuildId() <= 0) {
               log.error("[Hack] {} is trying to expel without rank 1 or 2.", mc.getName());
               return;
            }

            Server.getInstance().expelMember(mc.getMGC().orElseThrow(), name, cid);
            if (allianceId > 0) {
               Server.getInstance().getAlliance(allianceId).ifPresent(a -> a.updateAlliancePackets(mc));
            }
            break;
         case 0x0d:
            if (mc.getGuildId() <= 0 || mc.getGuildRank() != 1) {
               log.error("[Hack] {} tried to change guild rank titles when s/he does not have permission.", mc.getName());
               return;
            }
            String[] ranks = new String[5];
            for (int i = 0; i < 5; i++) {
               ranks[i] = p.readString();
            }

            Server.getInstance().changeRankTitle(mc.getGuildId(), ranks);
            break;
         case 0x0e:
            cid = p.readInt();
            byte newRank = p.readByte();
            if (mc.getGuildRank() > 2 || (newRank <= 2 && mc.getGuildRank() != 1) || mc.getGuildId() <= 0) {
               log.error("[Hack] {} is trying to change rank outside of his/her permissions.", mc.getName());
               return;
            }
            if (newRank <= 1 || newRank > 5) {
               return;
            }
            Server.getInstance().changeRank(mc.getGuildId(), cid, newRank);
            break;
         case 0x0f:
            if (mc.getGuildId() <= 0 || mc.getGuildRank() != 1 || mc.getMapId() != 200000301) {
               log.error("[Hack] {} tried to change guild emblem without being the guild leader.", mc.getName());
               return;
            }
            if (mc.getMeso() < YamlConfig.config.server.CHANGE_EMBLEM_COST) {
               c.sendPacket(CWvsContext.serverNotice(1,
                     "You do not have " + GameConstants.numberWithCommas(YamlConfig.config.server.CHANGE_EMBLEM_COST)
                           + " mesos to change the Guild emblem."));
               return;
            }
            short bg = p.readShort();
            byte bgcolor = p.readByte();
            short logo = p.readShort();
            byte logocolor = p.readByte();
            Server.getInstance().setGuildEmblem(mc.getGuildId(), bg, bgcolor, logo, logocolor);

            if (mc.getGuild().isPresent() && mc.getGuild().map(MapleGuild::getAllianceId).orElse(0) > 0) {
               mc.getAlliance().ifPresent(
                     a -> Server.getInstance().allianceMessage(a.getId(), CWvsContext.getGuildAlliances(a, c.getWorld()), -1, -1));
            }

            mc.gainMeso(-YamlConfig.config.server.CHANGE_EMBLEM_COST, true, false, true);
            mc.getGuild().ifPresent(MapleGuild::broadcastNameChanged);
            mc.getGuild().ifPresent(MapleGuild::broadcastEmblemChanged);
            break;
         case 0x10:
            if (mc.getGuildId() <= 0 || mc.getGuildRank() > 2) {
               if (mc.getGuildId() <= 0) {
                  log.error("[Hack] {} tried to change guild notice while not in a guild.", mc.getName());
               }
               return;
            }
            String notice = p.readString();
            if (notice.length() > 100) {
               return;
            }
            Server.getInstance().setGuildNotice(mc.getGuildId(), notice);
            break;
         case 0x1E:
            p.readInt();
            World wserv = c.getWorldServer();

            if (mc.getParty().isPresent()) {
               wserv.getMatchCheckerCoordinator().dismissMatchConfirmation(mc.getId());
               return;
            }

            int leaderid = wserv.getMatchCheckerCoordinator().getMatchConfirmationLeaderid(mc.getId());
            if (leaderid != -1) {
               boolean result = p.readByte() != 0;
               if (result && wserv.getMatchCheckerCoordinator().isMatchConfirmationActive(mc.getId())) {
                  wserv.getPlayerStorage().getCharacterById(leaderid).flatMap(MapleCharacter::getPartyId)
                        .ifPresent(id -> MapleParty.joinParty(mc, id, true));
               }

               wserv.getMatchCheckerCoordinator().answerMatchConfirmation(mc.getId(), result);
            }

            break;
         default:
            log.debug("Unhandled GUILD_OPERATION packet: {}", p);
      }
   }
}
