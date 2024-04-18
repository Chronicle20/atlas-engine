package net.server.coordinator.session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import net.server.Server;
import net.server.audit.locks.MonitoredLockType;
import net.server.audit.locks.factory.MonitoredReentrantLockFactory;
import net.server.coordinator.login.LoginStorage;
import tools.DatabaseConnection;

public class MapleSessionCoordinator {
   private static final Logger log = LoggerFactory.getLogger(MapleSessionCoordinator.class);
   private final static MapleSessionCoordinator instance = new MapleSessionCoordinator();
   private final SessionInitialization sessionInit = new SessionInitialization();
   private final LoginStorage loginStorage = new LoginStorage();
   private final Map<Integer, MapleClient> onlineClients = new HashMap<>();
   private final Set<Hwid> onlineRemoteHwids = new HashSet<>();
   private final Map<String, MapleClient> loginRemoteHosts = new HashMap<>();
   private final HostHwidCache hostHwidCache = new HostHwidCache();
   private final Set<String> pooledRemoteHosts = new HashSet<>();
   private final ConcurrentHashMap<String, String> cachedHostHwids = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<String, Long> cachedHostTimeout = new ConcurrentHashMap<>();
   private final List<ReentrantLock> poolLock = new ArrayList<>(100);

   private MapleSessionCoordinator() {
      for (int i = 0; i < 100; i++) {
         poolLock.add(MonitoredReentrantLockFactory.createLock(MonitoredLockType.SERVER_LOGIN_COORD));
      }
   }

   public static MapleSessionCoordinator getInstance() {
      return instance;
   }

   private static long hwidExpirationUpdate(int relevance) {
      int degree = 1, i = relevance, subdegree;
      while ((subdegree = 5 * degree) <= i) {
         i -= subdegree;
         degree++;
      }

      degree--;
      int baseTime, subdegreeTime;
      if (degree > 2) {
         subdegreeTime = 10;
      } else {
         subdegreeTime = 1 + (3 * degree);
      }

      baseTime = switch (degree) {
         case 0 -> 2;       // 2 hours
         case 1 -> 24;      // 1 day
         case 2 -> 168;     // 7 days
         default -> 1680;    // 70 days
      };

      return 3600000L * (baseTime + subdegreeTime);
   }

   private static void updateAccessAccount(Connection con, String remoteHwid, int accountId, int loginRelevance) throws
         SQLException {
      java.sql.Timestamp nextTimestamp =
            new java.sql.Timestamp(Server.getInstance().getCurrentTime() + hwidExpirationUpdate(loginRelevance));
      if (loginRelevance < Byte.MAX_VALUE) {
         loginRelevance++;
      }

      try (PreparedStatement ps = con.prepareStatement(
            "UPDATE hwidaccounts SET relevance = ?, expiresat = ? WHERE accountid = ? AND hwid LIKE ?")) {
         ps.setInt(1, loginRelevance);
         ps.setTimestamp(2, nextTimestamp);
         ps.setInt(3, accountId);
         ps.setString(4, remoteHwid);

         ps.executeUpdate();
      }
   }

   private static void registerAccessAccount(Connection con, String remoteHwid, int accountId) throws SQLException {
      try (PreparedStatement ps = con.prepareStatement("INSERT INTO hwidaccounts (accountid, hwid, expiresat) VALUES (?, ?, ?)")) {
         ps.setInt(1, accountId);
         ps.setString(2, remoteHwid);
         ps.setTimestamp(3, new java.sql.Timestamp(Server.getInstance().getCurrentTime() + hwidExpirationUpdate(0)));

         ps.executeUpdate();
      }
   }

   private static void associateHwidAccountIfAbsent(Hwid hwid, int accountId) {
      try (Connection con = DatabaseConnection.getConnection()) {
         List<Hwid> hwids = SessionDAO.getHwidsForAccount(con, accountId);

         boolean containsRemoteHwid = hwids.stream().anyMatch(accountHwid -> accountHwid.equals(hwid));
         if (containsRemoteHwid) {
            return;
         }

         if (hwids.size() < YamlConfig.config.server.MAX_ALLOWED_ACCOUNT_HWID) {
            Instant expiry = HwidAssociationExpiry.getHwidAccountExpiry(0);
            SessionDAO.registerAccountAccess(con, accountId, hwid, expiry);
         }
      } catch (SQLException ex) {
         log.warn("Failed to associate hwid {} with account id {}", hwid, accountId, ex);
      }
   }

   private static boolean attemptAccessAccount(String nibbleHwid, int accountId, boolean routineCheck) {
      try {

         try (Connection con = DatabaseConnection.getConnection();
              PreparedStatement ps = con.prepareStatement("SELECT SQL_CACHE * FROM hwidaccounts WHERE accountid = ?")) {
            int hwidCount = 0;
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                  String rsHwid = rs.getString("hwid");
                  if (rsHwid.endsWith(nibbleHwid)) {
                     if (!routineCheck) {
                        // better update HWID relevance as soon as the login is authenticated

                        int loginRelevance = rs.getInt("relevance");
                        updateAccessAccount(con, rsHwid, accountId, loginRelevance);
                     }

                     return true;
                  }

                  hwidCount++;
               }
            }

            if (hwidCount < YamlConfig.config.server.MAX_ALLOWED_ACCOUNT_HWID) {
               return true;
            }
         }
      } catch (SQLException ex) {
         ex.printStackTrace();
      }

      return false;
   }

   public static String getSessionRemoteHost(MapleClient client) {
      Hwid hwid = client.getHwid();

      if (hwid != null) {
         return client.getRemoteAddress() + "-" + hwid.hwid();
      } else {
         return client.getRemoteAddress();
      }
   }

   private static MapleClient fetchInTransitionSessionClient(MapleClient client) {
      Hwid hwid = MapleSessionCoordinator.getInstance().getGameSessionHwid(client);
      if (hwid == null) {   // maybe this session was currently in-transition?
         return null;
      }

      MapleClient fakeClient = MapleClient.createMock();
      fakeClient.setHwid(hwid);
      Server.getInstance().freeCharacteridInTransition(client).ifPresent(chrId -> {
         fakeClient.setAccID(MapleCharacter.loadCharFromDB(chrId, client, false).get().getAccountID());
      });

      return fakeClient;
   }

   private static boolean attemptAccountAccess(int accountId, Hwid hwid, boolean routineCheck) {
      try (Connection con = DatabaseConnection.getConnection()) {
         List<HwidRelevance> hwidRelevances = SessionDAO.getHwidRelevance(con, accountId);
         for (HwidRelevance hwidRelevance : hwidRelevances) {
            if (hwidRelevance.hwid().endsWith(hwid.hwid())) {
               if (!routineCheck) {
                  // better update HWID relevance as soon as the login is authenticated
                  Instant expiry = HwidAssociationExpiry.getHwidAccountExpiry(hwidRelevance.relevance());
                  SessionDAO.updateAccountAccess(con, hwid, accountId, expiry, hwidRelevance.getIncrementedRelevance());
               }

               return true;
            }
         }

         if (hwidRelevances.size() < YamlConfig.config.server.MAX_ALLOWED_ACCOUNT_HWID) {
            return true;
         }
      } catch (SQLException e) {
         log.warn("Failed to update account access. Account id: {}, nibbleHwid: {}", accountId, hwid, e);
      }

      return false;
   }

   public void updateOnlineClient(MapleClient client) {
      if (client != null) {
         int accountId = client.getAccID();
         disconnectClientIfOnline(accountId);
         onlineClients.put(accountId, client);
      }
   }

   private void disconnectClientIfOnline(int accountId) {
      MapleClient ingameClient = onlineClients.get(accountId);
      if (ingameClient
            != null) {     // thanks MedicOP for finding out a loss of loggedin account uniqueness when using the CMS "Unstuck" feature
         ingameClient.forceDisconnect();
      }
   }

   private Lock getCoodinatorLock(String remoteHost) {
      return poolLock.get(Math.abs(remoteHost.hashCode()) % 100);
   }

   public boolean canStartLoginSession(MapleClient client) {
      if (!YamlConfig.config.server.DETERRED_MULTICLIENT) {
         return true;
      }

      String remoteHost = getSessionRemoteHost(client);
      Lock lock = getCoodinatorLock(remoteHost);

      try {
         int tries = 0;
         while (true) {
            if (lock.tryLock()) {
               try {
                  if (pooledRemoteHosts.contains(remoteHost)) {
                     return false;
                  }

                  pooledRemoteHosts.add(remoteHost);
               } finally {
                  lock.unlock();
               }

               break;
            } else {
               if (tries == 2) {
                  return true;
               }
               tries++;

               Thread.sleep(1777);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
         return true;
      }

      try {
         final HostHwid knownHwid = hostHwidCache.getEntry(remoteHost);
         if (knownHwid != null && onlineRemoteHwids.contains(knownHwid.hwid())) {
            return false;
         } else if (loginRemoteHosts.containsKey(remoteHost)) {
            return false;
         }

         loginRemoteHosts.put(remoteHost, client);
         return true;
      } finally {
         sessionInit.finalize(remoteHost);
      }
   }

   public void closeLoginSession(MapleClient client) {
      clearLoginRemoteHost(client);

      Hwid nibbleHwid = client.getHwid();
      client.setHwid(null);
      if (nibbleHwid != null) {
         onlineRemoteHwids.remove(nibbleHwid);

         if (client != null) {
            MapleClient loggedClient = onlineClients.get(client.getAccID());

            // do not remove an online game session here, only login session
            if (loggedClient != null && loggedClient.getSessionId() == client.getSessionId()) {
               onlineClients.remove(client.getAccID());
            }
         }
      }
   }

   private void clearLoginRemoteHost(MapleClient client) {
      String remoteHost = getSessionRemoteHost(client);
      loginRemoteHosts.remove(client.getRemoteAddress());
      loginRemoteHosts.remove(remoteHost);
   }

   public AntiMulticlientResult attemptLoginSession(MapleClient client, Hwid hwid, int accountId,
                                                    boolean routineCheck) {
      if (!YamlConfig.config.server.DETERRED_MULTICLIENT) {
         client.setHwid(hwid);
         return AntiMulticlientResult.SUCCESS;
      }

      String remoteHost = getSessionRemoteHost(client);
      InitializationResult initResult = sessionInit.initialize(remoteHost);
      if (initResult != InitializationResult.SUCCESS) {
         return initResult.getAntiMulticlientResult();
      }

      try {
         if (!loginStorage.registerLogin(accountId)) {
            return AntiMulticlientResult.MANY_ACCOUNT_ATTEMPTS;
         } else if (routineCheck && !attemptAccountAccess(accountId, hwid, routineCheck)) {
            return AntiMulticlientResult.REMOTE_REACHED_LIMIT;
         } else if (onlineRemoteHwids.contains(hwid)) {
            return AntiMulticlientResult.REMOTE_LOGGEDIN;
         } else if (!attemptAccountAccess(accountId, hwid, routineCheck)) {
            return AntiMulticlientResult.REMOTE_REACHED_LIMIT;
         }

         client.setHwid(hwid);
         onlineRemoteHwids.add(hwid);

         return AntiMulticlientResult.SUCCESS;
      } finally {
         sessionInit.finalize(remoteHost);
      }
   }

   public AntiMulticlientResult attemptGameSession(MapleClient client, int accountId, Hwid hwid) {
      final String remoteHost = getSessionRemoteHost(client);
      if (!YamlConfig.config.server.DETERRED_MULTICLIENT) {
         hostHwidCache.addEntry(remoteHost, hwid);
         hostHwidCache.addEntry(client.getRemoteAddress(), hwid);  // no HWID information on the loggedin newcomer session...
         return AntiMulticlientResult.SUCCESS;
      }

      final InitializationResult initResult = sessionInit.initialize(remoteHost);
      if (initResult != InitializationResult.SUCCESS) {
         return initResult.getAntiMulticlientResult();
      }

      try {
         Hwid clientHwid = client.getHwid(); // thanks Paxum for noticing account stuck after PIC failure
         if (clientHwid == null) {
            return AntiMulticlientResult.REMOTE_NO_MATCH;
         }

         onlineRemoteHwids.remove(clientHwid);

         if (!hwid.equals(clientHwid)) {
            return AntiMulticlientResult.REMOTE_NO_MATCH;
         } else if (onlineRemoteHwids.contains(hwid)) {
            return AntiMulticlientResult.REMOTE_LOGGEDIN;
         }

         // assumption: after a SUCCESSFUL login attempt, the incoming client WILL receive a new IoSession from the game server

         // updated session CLIENT_HWID attribute will be set when the player log in the game
         onlineRemoteHwids.add(hwid);
         hostHwidCache.addEntry(remoteHost, hwid);
         hostHwidCache.addEntry(client.getRemoteAddress(), hwid);
         associateHwidAccountIfAbsent(hwid, accountId);

         return AntiMulticlientResult.SUCCESS;
      } finally {
         sessionInit.finalize(remoteHost);
      }
   }

   public void closeSession(MapleClient client, Boolean immediately) {
      if (client == null) {
         client = fetchInTransitionSessionClient(client);
      }

      final Hwid hwid = client.getHwid();
      client.setHwid(null); // making sure to clean up calls to this function on login phase
      if (hwid != null) {
         onlineRemoteHwids.remove(hwid);
      }

      final boolean isGameSession = hwid != null;
      if (isGameSession) {
         onlineClients.remove(client.getAccID());
      } else {
         MapleClient loggedClient = onlineClients.get(client.getAccID());

         // do not remove an online game session here, only login session
         if (loggedClient != null && loggedClient.getSessionId() == client.getSessionId()) {
            onlineClients.remove(client.getAccID());
         }
      }

      if (immediately != null && immediately) {
         client.closeSession();
      }
   }

   public Hwid pickLoginSessionHwid(MapleClient client) {
      String remoteHost = client.getRemoteAddress();
      return hostHwidCache.removeEntryAndGetItsHwid(remoteHost);
   }

   public Hwid getGameSessionHwid(MapleClient client) {
      String remoteHost = getSessionRemoteHost(client);
      return hostHwidCache.getEntryHwid(remoteHost);
   }

   private void associateRemoteHostHwid(String remoteHost, String remoteHwid) {
      cachedHostHwids.put(remoteHost, remoteHwid);
      cachedHostTimeout.put(remoteHost, Server.getInstance().getCurrentTime() + 604800000);   // 1 week-time entry
   }

   public void runUpdateHwidHistory() {
      try {
         try (Connection con = DatabaseConnection.getConnection();
              PreparedStatement ps = con.prepareStatement("DELETE FROM hwidaccounts WHERE expiresat < CURRENT_TIMESTAMP")) {
            ps.execute();
         }
      } catch (SQLException ex) {
         ex.printStackTrace();
      }

      long timeNow = Server.getInstance().getCurrentTime();
      List<String> toRemove = new LinkedList<>();
      for (Entry<String, Long> cht : cachedHostTimeout.entrySet()) {
         if (cht.getValue() < timeNow) {
            toRemove.add(cht.getKey());
         }
      }

      if (!toRemove.isEmpty()) {
         for (String s : toRemove) {
            cachedHostHwids.remove(s);
            cachedHostTimeout.remove(s);
         }
      }
   }

   public void runUpdateLoginHistory() {
      loginStorage.updateLoginHistory();
   }

   public void printSessionTrace() {
      if (!onlineClients.isEmpty()) {
         List<Entry<Integer, MapleClient>> elist = new ArrayList<>(onlineClients.entrySet());
         elist.sort(Entry.comparingByKey());

         log.debug("Current online clients: ");
         for (Entry<Integer, MapleClient> e : elist) {
            log.debug("  {}", e.getKey());
         }
      }

      if (!onlineRemoteHwids.isEmpty()) {
         List<Hwid> hwids = new ArrayList<>(onlineRemoteHwids);
         hwids.sort(Comparator.comparing(Hwid::hwid));

         log.debug("Current online HWIDs: {}", hwids.stream()
               .map(Hwid::hwid)
               .collect(Collectors.joining(" ")));
      }

      if (!loginRemoteHosts.isEmpty()) {
         List<Entry<String, MapleClient>> elist = new ArrayList<>(loginRemoteHosts.entrySet());
         elist.sort(Entry.comparingByKey());

         log.debug("Current login sessions: {}", loginRemoteHosts.entrySet().stream()
               .sorted(Entry.comparingByKey())
               .map(entry -> "(" + entry.getKey() + ", client: " + entry.getValue())
               .collect(Collectors.joining(", ")));
      }
   }

   public void printSessionTrace(MapleClient c) {
      StringBuilder str = new StringBuilder("Opened server sessions:\r\n\r\n");

      if (!onlineClients.isEmpty()) {
         List<Entry<Integer, MapleClient>> elist = new ArrayList<>(onlineClients.entrySet());
         elist.sort(Entry.comparingByKey());

         str.append("Current online clients:\r\n");
         str.append(elist.stream()
               .map(Entry::getKey)
               .reduce(new StringBuilder(),
                     (sb, key) -> sb.append("  ").append(key).append("\r\n"),
                     StringBuilder::append));
      }

      if (!onlineRemoteHwids.isEmpty()) {
         List<Hwid> hwids = new ArrayList<>(onlineRemoteHwids);
         hwids.sort(Comparator.comparing(Hwid::hwid));

         str.append("Current online HWIDs:\r\n");
         for (Hwid s : hwids) {
            str.append("  ").append(s).append("\r\n");
         }
      }

      if (!loginRemoteHosts.isEmpty()) {
         List<Entry<String, MapleClient>> elist = new ArrayList<>(loginRemoteHosts.entrySet());
         elist.sort(Entry.comparingByKey());

         str.append("Current login sessions:\r\n");
         str.append(elist.stream()
               .reduce(new StringBuilder(),
                     (sb, e) -> sb.append("  ").append(e.getKey()).append(", IP: ").append(e.getValue()).append("\r\n"),
                     StringBuilder::append));
      }

      c.getAbstractPlayerInteraction().npcTalk(2140000, str.toString());
   }

   public enum AntiMulticlientResult {
      SUCCESS,
      REMOTE_LOGGEDIN,
      REMOTE_REACHED_LIMIT,
      REMOTE_PROCESSING,
      REMOTE_NO_MATCH,
      MANY_ACCOUNT_ATTEMPTS,
      COORDINATOR_ERROR
   }
}
