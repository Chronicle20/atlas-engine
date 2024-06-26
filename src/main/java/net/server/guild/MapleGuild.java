package net.server.guild;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import connection.packets.CField;
import connection.packets.CUserRemote;
import connection.packets.CWvsContext;
import net.packet.Packet;
import net.server.Server;
import net.server.audit.locks.MonitoredLockType;
import net.server.audit.locks.factory.MonitoredReentrantLockFactory;
import net.server.channel.Channel;
import net.server.coordinator.matchchecker.MapleMatchCheckerCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator.InviteType;
import net.server.coordinator.world.MapleInviteCoordinator.MapleInviteResult;
import net.server.world.World;
import tools.DatabaseConnection;

public class MapleGuild {
   private static final Logger log = LoggerFactory.getLogger(MapleGuild.class);

   private final List<MapleGuildCharacter> members;
   private final Lock membersLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.GUILD, true);
   private String[] rankTitles = new String[5]; // 1 = master, 2 = jr, 5 = lowest member
   private String name, notice;
   private int id, gp, logo, logoColor, leader, capacity, logoBG, logoBGColor, signature, allianceId;
   private int world;
   private Map<Integer, List<Integer>> notifications = new LinkedHashMap<>();
   private boolean bDirty = true;

   public MapleGuild(int guildid, int world) {
      this.world = world;
      members = new ArrayList<>();
      Connection con;
      try {
         con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM guilds WHERE guildid = " + guildid);
         ResultSet rs = ps.executeQuery();
         if (!rs.next()) {
            id = -1;
            ps.close();
            rs.close();
            return;
         }
         id = guildid;
         name = rs.getString("name");
         gp = rs.getInt("GP");
         logo = rs.getInt("logo");
         logoColor = rs.getInt("logoColor");
         logoBG = rs.getInt("logoBG");
         logoBGColor = rs.getInt("logoBGColor");
         capacity = rs.getInt("capacity");
         for (int i = 1; i <= 5; i++) {
            rankTitles[i - 1] = rs.getString("rank" + i + "title");
         }
         leader = rs.getInt("leader");
         notice = rs.getString("notice");
         signature = rs.getInt("signature");
         allianceId = rs.getInt("allianceId");
         ps.close();
         rs.close();
         ps = con.prepareStatement(
               "SELECT id, name, level, job, guildrank, allianceRank FROM characters WHERE guildid = ? ORDER BY guildrank ASC, name ASC");
         ps.setInt(1, guildid);
         rs = ps.executeQuery();
         if (!rs.first()) {
            rs.close();
            ps.close();
            return;
         }
         do {
            members.add(new MapleGuildCharacter(null, rs.getInt("id"), rs.getInt("level"), rs.getString("name"), (byte) -1, world,
                  rs.getInt("job"), rs.getInt("guildrank"), guildid, false, rs.getInt("allianceRank")));
         } while (rs.next());

         ps.close();
         rs.close();
         con.close();
      } catch (SQLException se) {
         log.error("Unable to read guild information from sql: ", se);
      }
   }

   public static int createGuild(int leaderId, String name) {
      try {
         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
         ps.setString(1, name);
         ResultSet rs = ps.executeQuery();
         if (rs.first()) {
            ps.close();
            rs.close();
            return 0;
         }
         ps.close();
         rs.close();

         ps = con.prepareStatement("INSERT INTO guilds (`leader`, `name`, `signature`) VALUES (?, ?, ?)");
         ps.setInt(1, leaderId);
         ps.setString(2, name);
         ps.setInt(3, (int) System.currentTimeMillis());
         ps.execute();
         ps.close();

         ps = con.prepareStatement("SELECT guildid FROM guilds WHERE leader = ?");
         ps.setInt(1, leaderId);
         rs = ps.executeQuery();
         rs.first();
         int guildId = rs.getInt("guildid");
         rs.close();
         ps.close();

         ps = con.prepareStatement("UPDATE characters SET guildid = ? WHERE id = ?");
         ps.setInt(1, guildId);
         ps.setInt(2, leaderId);
         ps.executeUpdate();
         ps.close();

         con.close();
         return guildId;
      } catch (Exception e) {
         e.printStackTrace();
         return 0;
      }
   }

   public static MapleGuildResponse sendInvitation(MapleClient c, String targetName) {
      Optional<MapleCharacter> target = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);
      if (target.isEmpty()) {
         return MapleGuildResponse.NOT_IN_CHANNEL;
      }
      return sendInvitation(c, target.get());
   }

   private static MapleGuildResponse sendInvitation(MapleClient c, MapleCharacter target) {
      if (target.getGuildId() > 0) {
         return MapleGuildResponse.ALREADY_IN_GUILD;
      }

      MapleCharacter sender = c.getPlayer();
      if (MapleInviteCoordinator.createInvite(InviteType.GUILD, sender, sender.getGuildId(), target.getId())) {
         target.sendPacket(CWvsContext.guildInvite(sender.getGuildId(), sender.getName()));
         return null;
      } else {
         return MapleGuildResponse.MANAGING_INVITE;
      }
   }

   public static boolean answerInvitation(int targetId, String targetName, int guildId, boolean answer) {
      MapleInviteResult res = MapleInviteCoordinator.answerInvite(InviteType.GUILD, targetId, guildId, answer);

      MapleGuildResponse mgr;
      MapleCharacter sender = res.from;
      switch (res.result) {
         case ACCEPTED:
            return true;

         case DENIED:
            mgr = MapleGuildResponse.DENIED_INVITE;
            break;

         default:
            mgr = MapleGuildResponse.NOT_FOUND_INVITE;
      }

      if (mgr != null && sender != null) {
         sender.sendPacket(mgr.getPacket(targetName));
      }
      return false;
   }

   public static Set<MapleCharacter> getEligiblePlayersForGuild(MapleCharacter guildLeader) {
      Set<MapleCharacter> guildMembers = new HashSet<>();
      guildMembers.add(guildLeader);

      MapleMatchCheckerCoordinator mmce = guildLeader.getWorldServer().getMatchCheckerCoordinator();
      for (MapleCharacter chr : guildLeader.getMap().getAllPlayers()) {
         if (chr.getParty().isEmpty() && chr.getGuild().isEmpty() && mmce.getMatchConfirmationLeaderid(chr.getId()) == -1) {
            guildMembers.add(chr);
         }
      }

      return guildMembers;
   }

   public static void displayGuildRanks(MapleClient c, int npcid) {
      try {
         ResultSet rs;
         Connection con = DatabaseConnection.getConnection();
         try (PreparedStatement ps = con.prepareStatement(
               "SELECT `name`, `GP`, `logoBG`, `logoBGColor`, `logo`, `logoColor` FROM guilds ORDER BY `GP` DESC LIMIT 50", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            rs = ps.executeQuery();
            c.sendPacket(CWvsContext.showGuildRanks(npcid, rs));
         }
         rs.close();
         con.close();
      } catch (SQLException e) {
         log.error("Failed to display guild ranks.", e);
      }
   }

   public static int getIncreaseGuildCost(int size) {
      int cost = YamlConfig.config.server.EXPAND_GUILD_BASE_COST
            + Math.max(0, (size - 15) / 5) * YamlConfig.config.server.EXPAND_GUILD_TIER_COST;

      if (size > 30) {
         return Math.min(YamlConfig.config.server.EXPAND_GUILD_MAX_COST, Math.max(cost, 5000000));
      } else {
         return cost;
      }
   }

   private void buildNotifications() {
      if (!bDirty) {
         return;
      }
      Set<Integer> chs = Server.getInstance().getOpenChannels(world);
      synchronized (notifications) {
         if (notifications.keySet().size() != chs.size()) {
            notifications.clear();
            for (Integer ch : chs) {
               notifications.put(ch, new LinkedList<>());
            }
         } else {
            for (List<Integer> l : notifications.values()) {
               l.clear();
            }
         }
      }

      membersLock.lock();
      try {
         for (MapleGuildCharacter mgc : members) {
            if (!mgc.isOnline()) {
               continue;
            }

            List<Integer> chl;
            synchronized (notifications) {
               chl = notifications.get(mgc.getChannel());
            }
            if (chl != null) {
               chl.add(mgc.getId());
            }
            //Unable to connect to Channel... error was here
         }
      } finally {
         membersLock.unlock();
      }

      bDirty = false;
   }

   public void writeToDB(boolean bDisband) {
      try {
         Connection con = DatabaseConnection.getConnection();

         if (!bDisband) {
            StringBuilder builder = new StringBuilder();
            builder.append("UPDATE guilds SET GP = ?, logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ?, ");
            for (int i = 0; i < 5; i++) {
               builder.append("rank").append(i + 1).append("title = ?, ");
            }
            builder.append("capacity = ?, notice = ? WHERE guildid = ?");
            try (PreparedStatement ps = con.prepareStatement(builder.toString())) {
               ps.setInt(1, gp);
               ps.setInt(2, logo);
               ps.setInt(3, logoColor);
               ps.setInt(4, logoBG);
               ps.setInt(5, logoBGColor);
               for (int i = 6; i < 11; i++) {
                  ps.setString(i, rankTitles[i - 6]);
               }
               ps.setInt(11, capacity);
               ps.setString(12, notice);
               ps.setInt(13, this.id);
               ps.execute();
            }
         } else {
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?");
            ps.setInt(1, this.id);
            ps.execute();
            ps.close();
            ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
            ps.setInt(1, this.id);
            ps.execute();
            ps.close();

            membersLock.lock();
            try {
               this.broadcast(CWvsContext.guildDisband(this.id));
            } finally {
               membersLock.unlock();
            }
         }

         con.close();
      } catch (SQLException se) {
         se.printStackTrace();
      }
   }

   public int getId() {
      return id;
   }

   public int getLeaderId() {
      return leader;
   }

   public int setLeaderId(int charId) {
      return leader = charId;
   }

   public int getGP() {
      return gp;
   }

   public int getLogo() {
      return logo;
   }

   public void setLogo(int l) {
      logo = l;
   }

   public int getLogoColor() {
      return logoColor;
   }

   public void setLogoColor(int c) {
      logoColor = c;
   }

   public int getLogoBG() {
      return logoBG;
   }

   public void setLogoBG(int bg) {
      logoBG = bg;
   }

   public int getLogoBGColor() {
      return logoBGColor;
   }

   public void setLogoBGColor(int c) {
      logoBGColor = c;
   }

   public String getNotice() {
      if (notice == null) {
         return "";
      }
      return notice;
   }

   public String getName() {
      return name;
   }

   public List<MapleGuildCharacter> getMembers() {
      membersLock.lock();
      try {
         return new ArrayList<>(members);
      } finally {
         membersLock.unlock();
      }
   }

   public int getCapacity() {
      return capacity;
   }

   public int getSignature() {
      return signature;
   }

   public void broadcastNameChanged() {
      getMembers().stream()
            .map(MapleGuildCharacter::getId)
            .map(id -> Server.getInstance().getWorld(world)
                  .map(World::getPlayerStorage)
                  .flatMap(w -> w.getCharacterById(id)))
            .flatMap(Optional::stream)
            .filter(MapleCharacter::isLoggedinWorld)
            .forEach(c -> c.getMap().broadcastMessage(c, CUserRemote.guildNameChanged(c.getId(), getName())));
   }

   public void broadcastEmblemChanged() {
      getMembers().stream()
            .map(MapleGuildCharacter::getId)
            .map(id -> Server.getInstance().getWorld(world)
                  .map(World::getPlayerStorage)
                  .flatMap(w -> w.getCharacterById(id)))
            .flatMap(Optional::stream)
            .filter(MapleCharacter::isLoggedinWorld)
            .forEach(c -> c.getMap().broadcastMessage(c, CUserRemote.guildMarkChanged(c.getId(), this)));
   }

   public void broadcastInfoChanged() {
      getMembers().stream()
            .map(MapleGuildCharacter::getId)
            .map(id -> Server.getInstance().getWorld(world)
                  .map(World::getPlayerStorage)
                  .flatMap(w -> w.getCharacterById(id)))
            .flatMap(Optional::stream)
            .filter(MapleCharacter::isLoggedinWorld)
            .forEach(c -> c.getMap().broadcastMessage(c, CWvsContext.showGuildInfo(c)));
   }

   public void broadcast(Packet packet) {
      broadcast(packet, -1, BCOp.NONE);
   }

   public void broadcast(Packet packet, int exception) {
      broadcast(packet, exception, BCOp.NONE);
   }

   private void broadcast(Packet packet, int exceptionId, BCOp bcop) {
      membersLock.lock(); // membersLock awareness thanks to ProjectNano dev team
      try {
         synchronized (notifications) {
            if (bDirty) {
               buildNotifications();
            }
            try {
               for (Integer b : Server.getInstance().getOpenChannels(world)) {
                  if (!notifications.get(b).isEmpty()) {
                     if (bcop == BCOp.DISBAND) {
                        Server.getInstance().getWorld(world)
                              .ifPresent(w -> w.setGuildAndRank(notifications.get(b), 0, 5, exceptionId));
                     } else if (bcop == BCOp.EMBLEMCHANGE) {
                        Server.getInstance().getWorld(world)
                              .ifPresent(w -> w.changeEmblem(this.id, notifications.get(b), new MapleGuildSummary(this)));
                     } else {
                        Server.getInstance().getWorld(world)
                              .ifPresent(w -> w.sendPacket(notifications.get(b), packet, exceptionId));
                     }
                  }
               }
            } catch (Exception re) {
               log.error("Failed to contact channel(s) for broadcast.", re);
            }
         }
      } finally {
         membersLock.unlock();
      }
   }

   public void guildMessage(Packet serverNotice) {
      membersLock.lock();
      try {
         //TODO why is this only to the first member?
         for (MapleGuildCharacter mgc : members) {
            for (Channel cs : Server.getInstance().getChannelsFromWorld(world)) {
               Optional<MapleCharacter> character = cs.getPlayerStorage().getCharacterById(mgc.getId());
               if (character.isPresent()) {
                  character.get().sendPacket(serverNotice);
                  break;
               }
            }
         }
      } finally {
         membersLock.unlock();
      }
   }

   public void dropMessage(String message) {
      dropMessage(5, message);
   }

   public void dropMessage(int type, String message) {
      membersLock.lock();
      try {
         for (MapleGuildCharacter mgc : members) {
            if (mgc.getCharacter() != null) {
               mgc.getCharacter().dropMessage(type, message);
            }
         }
      } finally {
         membersLock.unlock();
      }
   }

   public void broadcastMessage(Packet packet) {
      Server.getInstance().guildMessage(id, packet);
   }

   public final void setOnline(int cid, boolean online, int channel) {
      membersLock.lock();
      try {
         boolean bBroadcast = true;
         for (MapleGuildCharacter mgc : members) {
            if (mgc.getId() == cid) {
               if (mgc.isOnline() && online) {
                  bBroadcast = false;
               }
               mgc.setOnline(online);
               mgc.setChannel(channel);
               break;
            }
         }
         if (bBroadcast) {
            this.broadcast(CWvsContext.guildMemberOnline(id, cid, online), cid);
         }
         bDirty = true;
      } finally {
         membersLock.unlock();
      }
   }

   public void guildChat(String name, int cid, String message) {
      membersLock.lock();
      try {
         this.broadcast(CField.multiChat(name, message, 2), cid);
      } finally {
         membersLock.unlock();
      }
   }

   public String getRankTitle(int rank) {
      return rankTitles[rank - 1];
   }

   public int addGuildMember(MapleGuildCharacter mgc, MapleCharacter chr) {
      membersLock.lock();
      try {
         if (members.size() >= capacity) {
            return 0;
         }
         for (int i = members.size() - 1; i >= 0; i--) {
            if (members.get(i).getGuildRank() < 5 || members.get(i).getName().compareTo(mgc.getName()) < 0) {
               mgc.setCharacter(chr);
               members.add(i + 1, mgc);
               bDirty = true;
               break;
            }
         }

         this.broadcast(CWvsContext.newGuildMember(mgc));
         return 1;
      } finally {
         membersLock.unlock();
      }
   }

   public void leaveGuild(MapleGuildCharacter mgc) {
      membersLock.lock();
      try {
         this.broadcast(CWvsContext.memberLeft(mgc, false));
         members.remove(mgc);
         bDirty = true;
      } finally {
         membersLock.unlock();
      }
   }

   public void expelMember(MapleGuildCharacter initiator, String name, int cid) {
      membersLock.lock();
      try {
         java.util.Iterator<MapleGuildCharacter> itr = members.iterator();
         MapleGuildCharacter mgc;
         while (itr.hasNext()) {
            mgc = itr.next();
            if (mgc.getId() == cid && initiator.getGuildRank() < mgc.getGuildRank()) {
               this.broadcast(CWvsContext.memberLeft(mgc, true));
               itr.remove();
               bDirty = true;
               try {
                  if (mgc.isOnline()) {
                     Server.getInstance().getWorld(mgc.getWorld()).ifPresent(w -> w.setGuildAndRank(cid, 0, 5));
                  } else {
                     try {
                        Connection con = DatabaseConnection.getConnection();
                        try (PreparedStatement ps = con.prepareStatement(
                              "INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)")) {
                           ps.setString(1, mgc.getName());
                           ps.setString(2, initiator.getName());
                           ps.setString(3, "You have been expelled from the guild.");
                           ps.setLong(4, System.currentTimeMillis());
                           ps.executeUpdate();
                        }

                        con.close();
                     } catch (SQLException e) {
                        log.error("ExpelMember - MapleGuild ", e);
                     }
                     Server.getInstance().getWorld(mgc.getWorld())
                           .ifPresent(w -> w.setOfflineGuildStatus((short) 0, (byte) 5, cid));
                  }
               } catch (Exception re) {
                  re.printStackTrace();
                  return;
               }
               return;
            }
         }
         log.warn("Unable to find member with name {} and id {}", name, cid);
      } finally {
         membersLock.unlock();
      }
   }

   public void changeRank(int cid, int newRank) {
      membersLock.lock();
      try {
         for (MapleGuildCharacter mgc : members) {
            if (cid == mgc.getId()) {
               changeRank(mgc, newRank);
               return;
            }
         }
      } finally {
         membersLock.unlock();
      }
   }

   public void changeRank(MapleGuildCharacter mgc, int newRank) {
      try {
         if (mgc.isOnline()) {
            Server.getInstance().getWorld(mgc.getWorld()).ifPresent(w -> w.setGuildAndRank(mgc.getId(), this.id, newRank));
            mgc.setGuildRank(newRank);
         } else {
            Server.getInstance().getWorld(mgc.getWorld())
                  .ifPresent(w -> w.setOfflineGuildStatus((short) this.id, (byte) newRank, mgc.getId()));
            mgc.setOfflineGuildRank(newRank);
         }
      } catch (Exception re) {
         re.printStackTrace();
         return;
      }

      membersLock.lock();
      try {
         this.broadcast(CWvsContext.changeRank(mgc));
      } finally {
         membersLock.unlock();
      }
   }

   public void setGuildNotice(String notice) {
      this.notice = notice;
      this.writeToDB(false);

      membersLock.lock();
      try {
         this.broadcast(CWvsContext.guildNotice(this.id, notice));
      } finally {
         membersLock.unlock();
      }
   }

   public void memberLevelJobUpdate(MapleGuildCharacter mgc) {
      membersLock.lock();
      try {
         for (MapleGuildCharacter member : members) {
            if (mgc.equals(member)) {
               member.setJobId(mgc.getJobId());
               member.setLevel(mgc.getLevel());
               this.broadcast(CWvsContext.guildMemberLevelJobUpdate(mgc));
               break;
            }
         }
      } finally {
         membersLock.unlock();
      }
   }

   @Override
   public int hashCode() {
      int hash = 3;
      hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
      hash = 89 * hash + this.id;
      return hash;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof MapleGuildCharacter o)) {
         return false;
      }
      return (o.getId() == id && o.getName().equals(name));
   }

   public void changeRankTitle(String[] ranks) {
      System.arraycopy(ranks, 0, rankTitles, 0, 5);

      membersLock.lock();
      try {
         this.broadcast(CWvsContext.rankTitleChange(this.id, ranks));
      } finally {
         membersLock.unlock();
      }

      this.writeToDB(false);
   }

   public void disbandGuild() {
      if (allianceId > 0) {
         if (!MapleAlliance.removeGuildFromAlliance(allianceId, id, world)) {
            MapleAlliance.disbandAlliance(allianceId);
         }
      }

      membersLock.lock();
      try {
         this.writeToDB(true);
         this.broadcast(null, -1, BCOp.DISBAND);
      } finally {
         membersLock.unlock();
      }
   }

   public void setGuildEmblem(short bg, byte bgcolor, short logo, byte logocolor) {
      this.logoBG = bg;
      this.logoBGColor = bgcolor;
      this.logo = logo;
      this.logoColor = logocolor;
      this.writeToDB(false);

      membersLock.lock();
      try {
         this.broadcast(null, -1, BCOp.EMBLEMCHANGE);
      } finally {
         membersLock.unlock();
      }
   }

   public Optional<MapleGuildCharacter> getMGC(int cid) {
      membersLock.lock();
      try {
         return members.stream().filter(mgc -> mgc.getId() == cid).findFirst();
      } finally {
         membersLock.unlock();
      }
   }

   public boolean increaseCapacity() {
      if (capacity > 99) {
         return false;
      }
      capacity += 5;
      this.writeToDB(false);

      membersLock.lock();
      try {
         this.broadcast(CWvsContext.guildCapacityChange(this.id, this.capacity));
      } finally {
         membersLock.unlock();
      }

      return true;
   }

   public void gainGP(int amount) {
      this.gp += amount;
      this.writeToDB(false);
      this.guildMessage(CWvsContext.updateGP(this.id, this.gp));
      this.guildMessage(CWvsContext.getGPMessage(amount));
   }

   public void removeGP(int amount) {
      this.gp -= amount;
      this.writeToDB(false);
      this.guildMessage(CWvsContext.updateGP(this.id, this.gp));
   }

   public int getAllianceId() {
      return allianceId;
   }

   public void setAllianceId(int aid) {
      this.allianceId = aid;
      try {
         Connection con = DatabaseConnection.getConnection();
         try (PreparedStatement ps = con.prepareStatement("UPDATE guilds SET allianceId = ? WHERE guildid = ?")) {
            ps.setInt(1, aid);
            ps.setInt(2, id);
            ps.executeUpdate();
         }

         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   public void resetAllianceGuildPlayersRank() {
      try {
         membersLock.lock();
         try {
            members.stream().filter(MapleGuildCharacter::isOnline).forEach(mgc -> mgc.setAllianceRank(5));
         } finally {
            membersLock.unlock();
         }

         Connection con = DatabaseConnection.getConnection();
         try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET allianceRank = ? WHERE guildid = ?")) {
            ps.setInt(1, 5);
            ps.setInt(2, id);
            ps.executeUpdate();
         }

         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   private enum BCOp {
      NONE, DISBAND, EMBLEMCHANGE
   }
}
