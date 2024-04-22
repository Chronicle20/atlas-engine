package client;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import javax.script.ScriptEngine;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import buddy.BuddyProcessor;
import client.inventory.MapleInventoryType;
import config.YamlConfig;
import connection.constants.LoginStatusCode;
import connection.packets.CCashShop;
import connection.packets.CClientSocket;
import connection.packets.CLogin;
import connection.packets.CUserLocal;
import connection.packets.CWvsContext;
import constants.game.GameConstants;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import net.MaplePacketHandler;
import net.PacketProcessor;
import net.netty.InvalidPacketHeaderException;
import net.packet.InPacket;
import net.packet.Packet;
import net.packet.logging.MonitoredChrLogger;
import net.server.Server;
import net.server.audit.locks.MonitoredLockType;
import net.server.audit.locks.factory.MonitoredReentrantLockFactory;
import net.server.channel.Channel;
import net.server.coordinator.login.MapleLoginBypassCoordinator;
import net.server.coordinator.session.Hwid;
import net.server.coordinator.session.MapleSessionCoordinator;
import net.server.coordinator.session.MapleSessionCoordinator.AntiMulticlientResult;
import net.server.guild.MapleGuild;
import net.server.world.MapleMessenger;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import net.server.world.PartyOperation;
import net.server.world.World;
import scripting.AbstractPlayerInteraction;
import scripting.event.EventManager;
import scripting.npc.NPCConversationManager;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestActionManager;
import scripting.quest.QuestScriptManager;
import server.ThreadManager;
import server.TimerManager;
import server.life.MapleMonster;
import server.maps.FieldLimit;
import server.maps.MapleMap;
import server.maps.MapleMiniDungeonInfo;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.HexTool;
import tools.LogHelper;

public class MapleClient extends ChannelInboundHandlerAdapter {
   private static final Logger log = LoggerFactory.getLogger(MapleClient.class);

   public static final int LOGIN_NOTLOGGEDIN = 0;
   public static final int LOGIN_SERVER_TRANSITION = 1;
   public static final int LOGIN_LOGGEDIN = 2;

   private final Type type;
   private final long sessionId;
   private final PacketProcessor packetProcessor;
   private final Semaphore actionsSemaphore = new Semaphore(7);
   private final Lock lock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.CLIENT, true);
   private final Lock encoderLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.CLIENT_ENCODER, true);
   private final Lock announcerLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.CLIENT_ANNOUNCER, true);
   private Hwid hwid;
   private String remoteAddress;
   private volatile boolean inTransition;

   private io.netty.channel.Channel ioChannel;
   private MapleCharacter player;
   private int channel = 1;
   private int accId = -4;
   private boolean loggedIn = false;
   private boolean serverTransition = false;
   private Calendar birthday = null;
   private String accountName = null;
   private int world;
   private long lastPong;
   private int gmlevel;
   private Set<String> macs = new HashSet<>();
   private Map<String, ScriptEngine> engines = new HashMap<>();
   private byte characterSlots = 3;
   private byte loginattempt = 0;
   private String pin = "";
   private int pinattempt = 0;
   private String pic = "";
   private int picattempt = 0;
   private byte csattempt = 0;
   private byte gender = -1;
   private boolean disconnecting = false;
   // thanks Masterrulax & try2hack for pointing out a bottleneck issue with shared locks, shavit for noticing an opportunity for improvement
   private Calendar tempBanCalendar;
   private int votePoints;
   private int voteTime = -1;
   private int visibleWorlds;
   private long lastNpcClick;
   private long lastPacket = System.currentTimeMillis();
   private int lang = 0;

   public MapleClient(Type type, long sessionId, String remoteAddress, PacketProcessor packetProcessor, int world, int channel) {
      this.type = type;
      this.sessionId = sessionId;
      this.remoteAddress = remoteAddress;
      this.packetProcessor = packetProcessor;
      this.world = world;
      this.channel = channel;
   }

   public static MapleClient createLoginClient(long sessionId, String remoteAddress, PacketProcessor packetProcessor,
                                               int world, int channel) {
      return new MapleClient(Type.LOGIN, sessionId, remoteAddress, packetProcessor, world, channel);
   }

   public static MapleClient createChannelClient(long sessionId, String remoteAddress, PacketProcessor packetProcessor,
                                                 int world, int channel) {
      return new MapleClient(Type.CHANNEL, sessionId, remoteAddress, packetProcessor, world, channel);
   }

   public static MapleClient createMock() {
      return new MapleClient(null, -1, null, null, -123, -123);
   }

   @Override
   public void channelActive(ChannelHandlerContext ctx) {
      final io.netty.channel.Channel channel = ctx.channel();
      if (!Server.getInstance().isOnline()) {
         channel.close();
         return;
      }

      this.remoteAddress = getRemoteAddress(channel);
      this.ioChannel = channel;
   }

   private static String getRemoteAddress(io.netty.channel.Channel channel) {
      String remoteAddress = "null";
      try {
         remoteAddress = ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
      } catch (NullPointerException npe) {
         log.warn("Unable to get remote address for client", npe);
      }

      return remoteAddress;
   }

   @Override
   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (!(msg instanceof InPacket packet)) {
         log.warn("Received invalid message: {}", msg);
         return;
      }

      short opcode = packet.readShort();
      Optional<MaplePacketHandler> handler = packetProcessor.getHandler(opcode);

      if (handler.isPresent() && handler.get().validateState(this)) {
         try {
            MonitoredChrLogger.logPacketIfMonitored(this, opcode, packet.getBytes());
            handler.get().handlePacket(packet, this);
         } catch (final Throwable t) {
            final String chrInfo = player != null ? player.getName() + " on map " + player.getMapId() : "?";
            log.warn("Error in packet handler {}. Chr {}, account {}. Packet: {}", handler.getClass().getSimpleName(),
                  chrInfo, getAccountName(), packet, t);
            //client.sendPacket(PacketCreator.enableActions());//bugs sometimes
         }
      }

      updateLastPacket();
   }

   @Override
   public void userEventTriggered(ChannelHandlerContext ctx, Object event) {
      if (event instanceof IdleStateEvent idleEvent) {
         checkIfIdle(idleEvent);
      }
   }

   @Override
   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      if (player != null) {
         log.warn("Exception caught by {}", player, cause);
      }

      if (cause instanceof InvalidPacketHeaderException) {
         MapleSessionCoordinator.getInstance().closeSession(this, true);
      } else if (cause instanceof IOException) {
         closeMapleSession();
      }
   }

   @Override
   public void channelInactive(ChannelHandlerContext ctx) {
      closeMapleSession();
   }

   private void closeMapleSession() {
      switch (type) {
         case LOGIN -> MapleSessionCoordinator.getInstance().closeLoginSession(this);
         case CHANNEL -> MapleSessionCoordinator.getInstance().closeSession(this, null);
      }

      try {
         // client freeze issues on session transition states found thanks to yolinlin, Omo Oppa, Nozphex
         if (!inTransition) {
            disconnect(false, false);
         }
      } catch (Throwable t) {
         log.warn("Account stuck", t);
      } finally {
         closeSession();
      }
   }

   public void checkIfIdle(final IdleStateEvent event) {
      final long pingedAt = System.currentTimeMillis();
      sendPacket(CClientSocket.getPing());
      TimerManager.getInstance().schedule(() -> {
         try {
            if (lastPong < pingedAt) {
               if (ioChannel.isActive()) {
                  log.info("Disconnected {} due to idling. Reason: {}", remoteAddress, event.state());
                  updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
                  disconnectSession();
               }
            }
         } catch (NullPointerException e) {
            e.printStackTrace();
         }
      }, SECONDS.toMillis(15));
   }

   public void disconnectSession() {
      ioChannel.disconnect();
   }

   private static boolean checkHash(String hash, String type, String password) {
      try {
         MessageDigest digester = MessageDigest.getInstance(type);
         digester.update(password.getBytes(StandardCharsets.UTF_8), 0, password.length());
         return HexTool.toString(digester.digest())
               .replace(" ", "")
               .toLowerCase()
               .equals(hash);
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException("Encoding the string failed", e);
      }
   }

   public void updateLastPacket() {
      lastPacket = System.currentTimeMillis();
   }

   public long getLastPacket() {
      return lastPacket;
   }

   public EventManager getEventManager(String event) {
      return getChannelServer().getEventSM()
            .getEventManager(event);
   }

   public MapleCharacter getPlayer() {
      return player;
   }

   public void setPlayer(MapleCharacter player) {
      this.player = player;
   }

   public AbstractPlayerInteraction getAbstractPlayerInteraction() {
      return new AbstractPlayerInteraction(this);
   }

   public void sendCharList(int server) {
      this.sendPacket(CLogin.getCharList(this, server, 0));
   }

   public List<MapleCharacter> loadCharacters(int serverId) {
      return loadCharactersInternal(serverId).stream()
            .map(CharNameAndId::getId)
            .map(id -> MapleCharacter.loadCharFromDB(id, this, false))
            .flatMap(Optional::stream)
            .limit(15)
            .collect(Collectors.toList());
   }

   private List<CharNameAndId> loadCharactersInternal(int worldId) {
      PreparedStatement ps;
      List<CharNameAndId> chars = new ArrayList<>(15);
      try {
         Connection con = DatabaseConnection.getConnection();
         ps = con.prepareStatement("SELECT id, name FROM characters WHERE accountid = ? AND world = ?");
         ps.setInt(1, this.getAccID());
         ps.setInt(2, worldId);
         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               chars.add(new CharNameAndId(rs.getString("name"), rs.getInt("id")));
            }
         }
         ps.close();
         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
      return chars;
   }

   public boolean isLoggedIn() {
      return loggedIn;
   }

   public boolean hasBannedIP() {
      boolean ret = false;
      try {
         Connection con = DatabaseConnection.getConnection();
         try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')")) {
            ps.setString(1, remoteAddress);
            try (ResultSet rs = ps.executeQuery()) {
               rs.next();
               if (rs.getInt(1) > 0) {
                  ret = true;
               }
            }
         }
         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
      return ret;
   }

   public int getVoteTime() {
      if (voteTime != -1) {
         return voteTime;
      }
      try {
         Connection con = DatabaseConnection.getConnection();
         try (PreparedStatement ps = con.prepareStatement("SELECT date FROM bit_votingrecords WHERE UPPER(account) = UPPER(?)")) {
            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery()) {
               if (!rs.next()) {
                  return -1;
               }
               voteTime = rs.getInt("date");
            }
         }
         con.close();
      } catch (SQLException e) {
         FilePrinter.printError("hasVotedAlready.txt", e);
         return -1;
      }
      return voteTime;
   }

   public void resetVoteTime() {
      voteTime = -1;
   }

   public boolean hasVotedAlready() {
      Date currentDate = new Date();
      int timeNow = (int) (currentDate.getTime() / 1000);
      int difference = (timeNow - getVoteTime());
      return difference < 86400 && difference > 0;
   }

   public boolean hasBannedHWID() {
      if (hwid == null) {
         return false;
      }

      boolean ret = false;
      PreparedStatement ps = null;
      Connection con = null;
      try {
         con = DatabaseConnection.getConnection();
         ps = con.prepareStatement("SELECT COUNT(*) FROM hwidbans WHERE hwid LIKE ?");
         ps.setString(1, hwid.hwid());
         ResultSet rs = ps.executeQuery();
         if (rs != null && rs.next()) {
            if (rs.getInt(1) > 0) {
               ret = true;
            }
         }
      } catch (SQLException e) {
         e.printStackTrace();
      } finally {
         try {
            if (ps != null && !ps.isClosed()) {
               ps.close();
            }

            if (con != null && !con.isClosed()) {
               con.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }

      return ret;
   }

   public boolean hasBannedMac() {
      if (macs.isEmpty()) {
         return false;
      }
      boolean ret = false;
      int i;
      try {
         StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM macbans WHERE mac IN (");
         for (i = 0; i < macs.size(); i++) {
            sql.append("?");
            if (i != macs.size() - 1) {
               sql.append(", ");
            }
         }
         sql.append(")");

         try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            i = 0;
            for (String mac : macs) {
               i++;
               ps.setString(i, mac);
            }
            try (ResultSet rs = ps.executeQuery()) {
               rs.next();
               if (rs.getInt(1) > 0) {
                  ret = true;
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return ret;
   }

   private void loadHWIDIfNescessary() throws SQLException {
      if (hwid == null) {
         try (Connection con = DatabaseConnection.getConnection();
              PreparedStatement ps = con.prepareStatement("SELECT hwid FROM accounts WHERE id = ?")) {
            ps.setInt(1, accId);
            try (ResultSet rs = ps.executeQuery()) {
               if (rs.next()) {
                  hwid = new Hwid(rs.getString("hwid"));
               }
            }
         }
      }
   }

   // TODO: Recode to close statements...
   private void loadMacsIfNescessary() throws SQLException {
      if (macs.isEmpty()) {
         try (Connection con = DatabaseConnection.getConnection();
              PreparedStatement ps = con.prepareStatement("SELECT macs FROM accounts WHERE id = ?")) {
            ps.setInt(1, accId);
            try (ResultSet rs = ps.executeQuery()) {
               if (rs.next()) {
                  for (String mac : rs.getString("macs").split(", ")) {
                     if (!mac.isEmpty()) {
                        macs.add(mac);
                     }
                  }
               }
            }
         }
      }
   }

   public void banHWID() {
      PreparedStatement ps = null;
      Connection con = null;
      try {
         loadHWIDIfNescessary();

         con = DatabaseConnection.getConnection();
         ps = con.prepareStatement("INSERT INTO hwidbans (hwid) VALUES (?)");
         ps.setString(1, hwid.hwid());
         ps.executeUpdate();
      } catch (SQLException e) {
         e.printStackTrace();
      } finally {
         try {
            if (ps != null && !ps.isClosed()) {
               ps.close();
            }
            if (con != null && !con.isClosed()) {
               con.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
   }

   public void banMacs() {
      Connection con;
      try {
         loadMacsIfNescessary();

         con = DatabaseConnection.getConnection();
         List<String> filtered = new LinkedList<>();
         try (PreparedStatement ps = con.prepareStatement("SELECT filter FROM macfilters"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               filtered.add(rs.getString("filter"));
            }
         }
         try (PreparedStatement ps = con.prepareStatement("INSERT INTO macbans (mac, aid) VALUES (?, ?)")) {
            for (String mac : macs) {
               boolean matched = filtered.stream().anyMatch(mac::matches);
               if (!matched) {
                  ps.setString(1, mac);
                  ps.setString(2, String.valueOf(getAccID()));
                  ps.executeUpdate();
               }
            }
         }

         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   public int finishLogin() {
      encoderLock.lock();
      try {
         if (getLoginState() > LOGIN_NOTLOGGEDIN) { // 0 = LOGIN_NOTLOGGEDIN, 1= LOGIN_SERVER_TRANSITION, 2 = LOGIN_LOGGEDIN
            loggedIn = false;
            return 7;
         }
         updateLoginState(MapleClient.LOGIN_LOGGEDIN);
      } finally {
         encoderLock.unlock();
      }

      return 0;
   }

   public String getPin() {
      return pin;
   }

   public void setPin(String pin) {
      this.pin = pin;
      try {
         try (Connection con = DatabaseConnection.getConnection();
              PreparedStatement ps = con.prepareStatement("UPDATE accounts SET pin = ? WHERE id = ?")) {
            ps.setString(1, pin);
            ps.setInt(2, accId);
            ps.executeUpdate();
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   public boolean checkPin(String other) {
      if (!(YamlConfig.config.server.ENABLE_PIN && !canBypassPin())) {
         return true;
      }

      pinattempt++;
      if (pinattempt > 5) {
         MapleSessionCoordinator.getInstance()
               .closeSession(this, false);
      }
      if (pin.equals(other)) {
         pinattempt = 0;
         MapleLoginBypassCoordinator.getInstance()
               .registerLoginBypassEntry(hwid, accId, false);
         return true;
      }
      return false;
   }

   public String getPic() {
      return pic;
   }

   public void setPic(String pic) {
      this.pic = pic;
      try {
         try (Connection con = DatabaseConnection.getConnection();
              PreparedStatement ps = con.prepareStatement("UPDATE accounts SET pic = ? WHERE id = ?")) {
            ps.setString(1, pic);
            ps.setInt(2, accId);
            ps.executeUpdate();
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   public boolean checkPic(String other) {
      if (!(YamlConfig.config.server.ENABLE_PIC && !canBypassPic())) {
         return true;
      }

      picattempt++;
      if (picattempt > 5) {
         MapleSessionCoordinator.getInstance()
               .closeSession(this, false);
      }
      if (pic.equals(other)) {    // thanks ryantpayton (HeavenClient) for noticing null pics being checked here
         picattempt = 0;
         MapleLoginBypassCoordinator.getInstance()
               .registerLoginBypassEntry(hwid, accId, true);
         return true;
      }
      return false;
   }

   public LoginStatusCode login(String login, String pwd, String nibbleHwid) {
      LoginStatusCode loginok = LoginStatusCode.NOT_REGISTERED_ID;

      loginattempt++;
      if (loginattempt > 4) {
         loggedIn = false;
         MapleSessionCoordinator.getInstance()
               .closeSession(this, false);
         return LoginStatusCode.SYSTEM_ERROR_1;   // thanks Survival_Project for finding out an issue with AUTOMATIC_REGISTER here
      }

      Connection con = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      try {
         con = DatabaseConnection.getConnection();
         ps = con.prepareStatement(
               "SELECT id, password, gender, banned, pin, pic, characterslots, tos, language FROM accounts WHERE name = ?");
         ps.setString(1, login);
         rs = ps.executeQuery();
         accId = -2;
         if (rs.next()) {
            accId = rs.getInt("id");
            if (accId <= 0) {
               FilePrinter.printError(FilePrinter.LOGIN_EXCEPTION, "Tried to login with accid " + accId);
               return LoginStatusCode.PROCESSING_REQUEST;
            }

            boolean banned = (rs.getByte("banned") == 1);
            gmlevel = 0;
            pin = rs.getString("pin");
            pic = rs.getString("pic");
            gender = rs.getByte("gender");
            characterSlots = rs.getByte("characterslots");
            lang = rs.getInt("language");
            String passhash = rs.getString("password");
            byte tos = rs.getByte("tos");

            ps.close();
            rs.close();

            if (banned) {
               return LoginStatusCode.ID_DELETED_OR_BLOCKED;
            }

            if (getLoginState() > LOGIN_NOTLOGGEDIN) { // already loggedin
               loggedIn = false;
               loginok = LoginStatusCode.ALREADY_LOGGED_IN;
            } else if (passhash.charAt(0) == '$' && passhash.charAt(1) == '2' && BCrypt.checkpw(pwd, passhash)) {
               loginok = LoginStatusCode.OK;
            } else if (pwd.equals(passhash) || checkHash(passhash, "SHA-1", pwd) || checkHash(passhash, "SHA-512", pwd)) {
               // thanks GabrielSin for detecting some no-bcrypt inconsistencies here
               loginok = !YamlConfig.config.server.BCRYPT_MIGRATION ? LoginStatusCode.OK : LoginStatusCode.BCRYPT_MIGRATION;
            } else {
               loggedIn = false;
               loginok = LoginStatusCode.INCORRECT_PASSWORD;
            }
         } else {
            accId = -3;
         }
      } catch (SQLException e) {
         e.printStackTrace();
      } finally {
         try {
            if (ps != null && !ps.isClosed()) {
               ps.close();
            }
            if (rs != null && !rs.isClosed()) {
               rs.close();
            }
            if (con != null && !con.isClosed()) {
               con.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }

      if (loginok == LoginStatusCode.OK || loginok == LoginStatusCode.INCORRECT_PASSWORD) {
         AntiMulticlientResult res = MapleSessionCoordinator.getInstance()
               .attemptLoginSession(this, hwid, accId, loginok == LoginStatusCode.INCORRECT_PASSWORD);

         switch (res) {
            case SUCCESS:
               if (loginok == LoginStatusCode.OK) {
                  loginattempt = 0;
               }

               return loginok;

            case REMOTE_LOGGEDIN:
               return LoginStatusCode.WRONG_GATEWAY_2;

            case REMOTE_REACHED_LIMIT:
               return LoginStatusCode.UNABLE_TO_LOG_ON_AS_MASTER;

            case REMOTE_PROCESSING, MANY_ACCOUNT_ATTEMPTS:
               return LoginStatusCode.CANNOT_PROCESS_SO_MANY_CONNECTIONS;

            default:
               return LoginStatusCode.SYSTEM_ERROR_2;
         }
      } else {
         return loginok;
      }
   }

   public Calendar getTempBanCalendarFromDB() {
      Connection con = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      final Calendar lTempban = Calendar.getInstance();
      try {
         con = DatabaseConnection.getConnection();
         ps = con.prepareStatement("SELECT `tempban` FROM accounts WHERE id = ?");
         ps.setInt(1, getAccID());
         rs = ps.executeQuery();
         if (!rs.next()) {
            return null;
         }

         Timestamp blubb = rs.getTimestamp("tempban");

         if (blubb == null || rs.getString("tempban")
               .equals("2018-06-20 00:00:00.0")) { // 0000-00-00 or 2018-06-20 (default set in LoginPasswordHandler)
            return null;
         }
         lTempban.setTimeInMillis(rs.getTimestamp("tempban")
               .getTime());
         tempBanCalendar = lTempban;
         return lTempban;
      } catch (SQLException e) {
         e.printStackTrace();
      } finally {
         try {
            if (ps != null) {
               ps.close();
            }
            if (rs != null) {
               rs.close();
            }
            if (con != null && !con.isClosed()) {
               con.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
      return null;//why oh why!?!
   }

   public Calendar getTempBanCalendar() {
      return tempBanCalendar;
   }

   public boolean hasBeenBanned() {
      return tempBanCalendar != null;
   }

   public void updateHwid(Hwid hwid) {
      this.hwid = hwid;

      try (Connection con = DatabaseConnection.getConnection();
           PreparedStatement ps = con.prepareStatement("UPDATE accounts SET hwid = ? WHERE id = ?")) {
         ps.setString(1, hwid.hwid());
         ps.setInt(2, accId);
         ps.executeUpdate();
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   public void updateMacs(String macData) {
      macs.addAll(Arrays.asList(macData.split(", ")));
      StringBuilder newMacData = new StringBuilder();
      Iterator<String> iter = macs.iterator();
      PreparedStatement ps = null;
      while (iter.hasNext()) {
         String cur = iter.next();
         newMacData.append(cur);
         if (iter.hasNext()) {
            newMacData.append(", ");
         }
      }
      Connection con = null;
      try {
         con = DatabaseConnection.getConnection();
         ps = con.prepareStatement("UPDATE accounts SET macs = ? WHERE id = ?");
         ps.setString(1, newMacData.toString());
         ps.setInt(2, accId);
         ps.executeUpdate();
         ps.close();
      } catch (SQLException e) {
         e.printStackTrace();
      } finally {
         try {
            if (ps != null && !ps.isClosed()) {
               ps.close();
            }
            if (con != null && !con.isClosed()) {
               con.close();
            }
         } catch (SQLException ex) {
            ex.printStackTrace();
         }
      }
   }

   public int getAccID() {
      return accId;
   }

   public void setAccID(int id) {
      this.accId = id;
   }

   public void updateLoginState(int newstate) {
      // rules out possibility of multiple account entries
      if (newstate == LOGIN_LOGGEDIN) {
         MapleSessionCoordinator.getInstance().updateOnlineClient(this);
      }

      try {
         Connection con = DatabaseConnection.getConnection();
         try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = ?, lastlogin = ? WHERE id = ?")) {
            // using sql currenttime here could potentially break the login, thanks Arnah for pointing this out

            ps.setInt(1, newstate);
            ps.setTimestamp(2, new java.sql.Timestamp(Server.getInstance()
                  .getCurrentTime()));
            ps.setInt(3, getAccID());
            ps.executeUpdate();
         }
         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }

      if (newstate == LOGIN_NOTLOGGEDIN) {
         loggedIn = false;
         serverTransition = false;
         setAccID(0);
      } else {
         serverTransition = (newstate == LOGIN_SERVER_TRANSITION);
         loggedIn = !serverTransition;
      }
   }

   public int getLoginState() {  // 0 = LOGIN_NOTLOGGEDIN, 1= LOGIN_SERVER_TRANSITION, 2 = LOGIN_LOGGEDIN
      try {
         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT loggedin, lastlogin, birthday FROM accounts WHERE id = ?");
         ps.setInt(1, getAccID());
         ResultSet rs = ps.executeQuery();
         if (!rs.next()) {
            rs.close();
            ps.close();
            throw new RuntimeException("getLoginState - MapleClient AccID: " + getAccID());
         }

         birthday = Calendar.getInstance();
         try {
            birthday.setTime(rs.getDate("birthday"));
         } catch (SQLException e) {
         }

         int state = rs.getInt("loggedin");
         if (state == LOGIN_SERVER_TRANSITION) {
            if (rs.getTimestamp("lastlogin")
                  .getTime() + 30000 < Server.getInstance()
                  .getCurrentTime()) {
               int accountId = accId;
               state = LOGIN_NOTLOGGEDIN;
               updateLoginState(
                     MapleClient.LOGIN_NOTLOGGEDIN);   // ACCID = 0, issue found thanks to Tochi & K u ssss o & Thora & Omo Oppa
               this.setAccID(accountId);
            }
         }
         rs.close();
         ps.close();
         if (state == LOGIN_LOGGEDIN) {
            loggedIn = true;
         } else if (state == LOGIN_SERVER_TRANSITION) {
            ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE id = ?");
            ps.setInt(1, getAccID());
            ps.executeUpdate();
            ps.close();
         } else {
            loggedIn = false;
         }

         con.close();
         return state;
      } catch (SQLException e) {
         loggedIn = false;
         e.printStackTrace();
         throw new RuntimeException("login state");
      }
   }

   public boolean checkBirthDate(Calendar date) {
      return date.get(Calendar.YEAR) == birthday.get(Calendar.YEAR) && date.get(Calendar.MONTH) == birthday.get(Calendar.MONTH)
            && date.get(Calendar.DAY_OF_MONTH) == birthday.get(Calendar.DAY_OF_MONTH);
   }

   private void removePartyPlayer(World wserv) {
      MapleMap map = player.getMap();
      final Optional<MapleParty> party = player.getParty();
      final int idz = player.getId();

      if (party.isPresent()) {
         final MaplePartyCharacter chrp = new MaplePartyCharacter(player);
         chrp.setOnline(false);
         wserv.updateParty(party.get()
               .getId(), PartyOperation.LOG_ONOFF, chrp);
         if (party.get()
               .getLeader()
               .getId() == idz && map != null) {
            MaplePartyCharacter lchr = null;
            for (MaplePartyCharacter pchr : party.get().getMembers()) {
               if (pchr != null && pchr.getId() != idz && (lchr == null || lchr.getLevel() <= pchr.getLevel())
                     && map.getCharacterById(pchr.getId())
                     .isPresent()) {
                  lchr = pchr;
               }
            }
            if (lchr != null) {
               wserv.updateParty(party.get()
                     .getId(), PartyOperation.CHANGE_LEADER, lchr);
            }
         }
      }
   }

   private void removePlayer(World wserv, boolean serverTransition) {
      try {
         player.setDisconnectedFromChannelWorld();
         player.notifyMapTransferToPartner(-1);
         player.removeIncomingInvites();
         player.cancelAllBuffs(true);

         player.closePlayerInteractions();
         player.closePartySearchInteractions();

         if (!serverTransition) {    // thanks MedicOP for detecting an issue with party leader change on changing channels
            removePartyPlayer(wserv);

            player.getEventInstance()
                  .ifPresent(ei -> ei.playerDisconnected(player));

            if (player.getMonsterCarnival() != null) {
               player.getMonsterCarnival()
                     .playerDisconnected(getPlayer().getId());
            }

            if (player.getAriantColiseum() != null) {
               player.getAriantColiseum()
                     .playerDisconnected(getPlayer());
            }
         }

         if (player.getMap() != null) {
            int mapId = player.getMapId();
            player.getMap()
                  .removePlayer(player);
            if (GameConstants.isDojo(mapId)) {
               this.getChannelServer()
                     .freeDojoSectionIfEmpty(mapId);
            }
         }
      } catch (final Throwable t) {
         FilePrinter.printError(FilePrinter.ACCOUNT_STUCK, t);
      }
   }

   public final void disconnect(final boolean shutdown, final boolean cashshop) {
      if (canDisconnect()) {
         ThreadManager.getInstance()
               .newTask(() -> disconnectInternal(shutdown, cashshop));
      }
   }

   public final void forceDisconnect() {
      if (canDisconnect()) {
         disconnectInternal(true, false);
      }
   }

   private synchronized boolean canDisconnect() {
      if (disconnecting) {
         return false;
      }

      disconnecting = true;
      return true;
   }

   private void disconnectInternal(boolean shutdown, boolean cashshop) {//once per MapleClient instance
      if (player != null && player.isLoggedin() && player.getClient() != null) {
         int messengerId = player.getMessenger()
               .map(MapleMessenger::getId)
               .orElse(0);
         //final int fid = player.getFamilyId();

         player.cancelMagicDoor();

         final World wserv = getWorldServer();   // obviously wserv is NOT null if this player was online on it
         try {
            removePlayer(wserv, this.serverTransition);

            if (!(channel == -1 || shutdown)) {
               if (!cashshop) {
                  if (!this.serverTransition) { // meaning not changing channels
                     if (messengerId > 0) {
                        wserv.leaveMessenger(messengerId, player.getName());
                     }
                                                        /*
                                                        if (fid > 0) {
                                                                final MapleFamily family = worlda.getFamily(fid);
                                                                family.
                                                        }
                                                        */

                     player.forfeitExpirableQuests();    //This is for those quests that you have to stay logged in for a certain amount of time

                     Optional<MapleGuild> guild = player.getGuild();
                     if (guild.isPresent()) {
                        Server.getInstance()
                              .setGuildMemberOnline(player, false, player.getClient()
                                    .getChannel());
                        player.sendPacket(CWvsContext.showGuildInfo(player));
                     }
                     BuddyProcessor.getInstance().notifyLogoff(player.getWorld(), player.getId());
                  }
               } else {
                  if (!this.serverTransition) { // if dc inside of cash shop.
                     BuddyProcessor.getInstance().notifyLogoff(player.getWorld(), player.getId());
                  }
               }
            }
         } catch (final Exception e) {
            FilePrinter.printError(FilePrinter.ACCOUNT_STUCK, e);
         } finally {
            if (!this.serverTransition) {
               player.getMGC()
                     .ifPresent(mgc -> mgc.setCharacter(null));

               wserv.removePlayer(player);
               //getChannelServer().removePlayer(player); already being done

               player.saveCooldowns();
               player.cancelAllDebuffs();
               player.saveCharToDB(true);

               player.logOff();
               if (YamlConfig.config.server.INSTANT_NAME_CHANGE) {
                  player.doPendingNameChange();
               }
               clear();
            } else {
               getChannelServer().removePlayer(player);

               player.saveCooldowns();
               player.cancelAllDebuffs();
               player.saveCharToDB();
            }
         }
      }

      MapleSessionCoordinator.getInstance().closeSession(this, false);

      if (!serverTransition && isLoggedIn()) {
         updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
         clear();
      } else {
         if (!Server.getInstance()
               .hasCharacteridInTransition(this)) {
            updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
         }
         engines = null;
      }
   }

   private void clear() {
      // player hard reference removal thanks to Steve (kaito1410)
      if (this.player != null) {
         this.player.empty(true); // clears schedules and stuff
      }

      Server.getInstance()
            .unregisterLoginState(this);

      this.accountName = null;
      this.macs = null;
      this.hwid = null;
      this.birthday = null;
      this.engines = null;
      this.player = null;
   }

   public void setCharacterOnSessionTransitionState(int cid) {
      this.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
      this.inTransition = true;
      Server.getInstance().setCharacteridInTransition(this, cid);
   }

   public int getChannel() {
      return channel;
   }

   public void setChannel(int channel) {
      this.channel = channel;
   }

   public Channel getChannelServer() {
      return Server.getInstance()
            .getChannel(world, channel)
            .orElseThrow();
   }

   public World getWorldServer() {
      return Server.getInstance()
            .getWorld(world)
            .orElseThrow();
   }

   public Optional<Channel> getChannelServer(byte channel) {
      return Server.getInstance()
            .getChannel(world, channel);
   }

   public boolean deleteCharacter(int cid, int senderAccId) {
      Optional<MapleCharacter> chr = MapleCharacter.loadCharFromDB(cid, this, false);
      if (chr.isEmpty()) {
         return false;
      }

      Integer partyid = chr.get()
            .getWorldServer()
            .getCharacterPartyid(cid);
      if (partyid != null) {
         this.setPlayer(chr.get());

         chr.get()
               .getWorldServer()
               .getParty(partyid)
               .ifPresent(p -> chr.get()
                     .setParty(p));
         chr.get()
               .getMPC();
         chr.get()
               .leaveParty();

         this.setPlayer(null);
      }

      return MapleCharacter.deleteCharFromDB(chr.get(), senderAccId);
   }

   public String getAccountName() {
      return accountName;
   }

   public void setAccountName(String a) {
      this.accountName = a;
   }

   public int getWorld() {
      return world;
   }

   public void setWorld(int world) {
      this.world = world;
   }

   public void pongReceived() {
      lastPong = System.currentTimeMillis();
   }

   public Hwid getHwid() {
      return hwid;
   }

   public void setHwid(Hwid hwid) {
      this.hwid = hwid;
   }

   public String getRemoteAddress() {
      return remoteAddress;
   }

   public Set<String> getMacs() {
      return Collections.unmodifiableSet(macs);
   }

   public int getGMLevel() {
      return gmlevel;
   }

   public void setGMLevel(int level) {
      gmlevel = level;
   }

   public void setScriptEngine(String name, ScriptEngine e) {
      engines.put(name, e);
   }

   public Optional<ScriptEngine> getScriptEngine(String name) {
      return Optional.ofNullable(engines.get(name));
   }

   public void removeScriptEngine(String name) {
      engines.remove(name);
   }

   public NPCConversationManager getCM() {
      return NPCScriptManager.getInstance()
            .getCM(this);
   }

   public QuestActionManager getQM() {
      return QuestScriptManager.getInstance()
            .getQM(this);
   }

   public boolean acceptToS() {
      boolean disconnectForBeingAFaggot = false;
      if (accountName == null) {
         return true;
      }
      try {
         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT `tos` FROM accounts WHERE id = ?");
         ps.setInt(1, accId);
         ResultSet rs = ps.executeQuery();

         if (rs.next()) {
            if (rs.getByte("tos") == 1) {
               disconnectForBeingAFaggot = true;
            }
         }
         ps.close();
         rs.close();
         ps = con.prepareStatement("UPDATE accounts SET tos = 1 WHERE id = ?");
         ps.setInt(1, accId);
         ps.executeUpdate();
         ps.close();
         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
      return disconnectForBeingAFaggot;
   }

   public void checkChar(int accid) {  /// issue with multiple chars from same account login found by shavit, resinate
      if (!YamlConfig.config.server.USE_CHARACTER_ACCOUNT_CHECK) {
         return;
      }

      for (World w : Server.getInstance().getWorlds()) {
         for (MapleCharacter chr : w.getPlayerStorage()
               .getAllCharacters()) {
            if (accid == chr.getAccountID()) {
               log.error("Player: {} has been removed from {}. Possible Dupe attempt.", chr.getName(),
                     GameConstants.WORLD_NAMES[w.getId()]);
               chr.getClient().forceDisconnect();
               w.getPlayerStorage().removePlayer(chr.getId());
            }
         }
      }
   }

   public int getVotePoints() {
      int points = 0;
      try {
         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT `votepoints` FROM accounts WHERE id = ?");
         ps.setInt(1, accId);
         ResultSet rs = ps.executeQuery();

         if (rs.next()) {
            points = rs.getInt("votepoints");
         }
         ps.close();
         rs.close();

         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
      votePoints = points;
      return votePoints;
   }

   public void addVotePoints(int points) {
      votePoints += points;
      saveVotePoints();
   }

   public void useVotePoints(int points) {
      if (points > votePoints) {
         //Should not happen, should probably log this
         return;
      }
      votePoints -= points;
      saveVotePoints();
      LogHelper.logLeaf(player, false, Integer.toString(points));
   }

   private void saveVotePoints() {
      try {
         Connection con = DatabaseConnection.getConnection();
         try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET votepoints = ? WHERE id = ?")) {
            ps.setInt(1, votePoints);
            ps.setInt(2, accId);
            ps.executeUpdate();
         }

         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   public void lockClient() {
      lock.lock();
   }

   public void unlockClient() {
      lock.unlock();
   }

   public boolean tryacquireClient() {
      if (actionsSemaphore.tryAcquire()) {
         lockClient();
         return true;
      } else {
         return false;
      }
   }

   public void releaseClient() {
      unlockClient();
      actionsSemaphore.release();
   }

   public boolean tryacquireEncoder() {
      if (actionsSemaphore.tryAcquire()) {
         encoderLock.lock();
         return true;
      } else {
         return false;
      }
   }

   public void unlockEncoder() {
      encoderLock.unlock();
      actionsSemaphore.release();
   }

   public short getAvailableCharacterSlots() {
      return (short) Math.max(0, characterSlots - Server.getInstance()
            .getAccountCharacterCount(accId));
   }

   public short getAvailableCharacterWorldSlots() {
      return (short) Math.max(0, characterSlots - Server.getInstance()
            .getAccountWorldCharacterCount(accId, world));
   }

   public short getAvailableCharacterWorldSlots(int world) {
      return (short) Math.max(0, characterSlots - Server.getInstance()
            .getAccountWorldCharacterCount(accId, world));
   }

   public short getCharacterSlots() {
      return characterSlots;
   }

   public void setCharacterSlots(byte slots) {
      characterSlots = slots;
   }

   public boolean canGainCharacterSlot() {
      return characterSlots < 15;
   }

   public synchronized boolean gainCharacterSlot() {
      if (canGainCharacterSlot()) {
         Connection con;
         try {
            con = DatabaseConnection.getConnection();

            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET characterslots = ? WHERE id = ?")) {
               ps.setInt(1, this.characterSlots += 1);
               ps.setInt(2, accId);
               ps.executeUpdate();
            }

            con.close();
         } catch (SQLException e) {
            e.printStackTrace();
         }
         return true;
      }
      return false;
   }

   public final byte getGReason() {
      Connection con = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      try {
         con = DatabaseConnection.getConnection();
         ps = con.prepareStatement("SELECT `greason` FROM `accounts` WHERE id = ?");
         ps.setInt(1, accId);
         rs = ps.executeQuery();
         if (rs.next()) {
            return rs.getByte("greason");
         }
      } catch (SQLException e) {
         e.printStackTrace();
      } finally {
         try {
            if (ps != null) {
               ps.close();
            }
            if (rs != null) {
               rs.close();
            }
            if (con != null) {
               con.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
      return 0;
   }

   public byte getGender() {
      return gender;
   }

   public void setGender(byte m) {
      this.gender = m;
      Connection con;
      try {
         con = DatabaseConnection.getConnection();
         try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET gender = ? WHERE id = ?")) {
            ps.setByte(1, gender);
            ps.setInt(2, accId);
            ps.executeUpdate();
         }

         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   private void announceDisableServerMessage() {
      if (!this.getWorldServer()
            .registerDisabledServerMessage(player.getId())) {
         sendPacket(CWvsContext.serverMessage(""));
      }
   }

   public void announceServerMessage() {
      sendPacket(CWvsContext.serverMessage(this.getChannelServer()
            .getServerMessage()));
   }

   public synchronized void announceBossHpBar(MapleMonster mm, final int mobHash, Packet packet) {
      long timeNow = System.currentTimeMillis();
      int targetHash = player.getTargetHpBarHash();

      if (mobHash != targetHash) {
         if (timeNow - player.getTargetHpBarTime() >= 5 * 1000) {
            // is there a way to INTERRUPT this annoying thread running on the client that drops the boss bar after some time at every attack?
            announceDisableServerMessage();
            sendPacket(packet);

            player.setTargetHpBarHash(mobHash);
            player.setTargetHpBarTime(timeNow);
         }
      } else {
         announceDisableServerMessage();
         sendPacket(packet);

         player.setTargetHpBarTime(timeNow);
      }
   }

   public void sendPacket(Packet packet) {
      announcerLock.lock();
      try {
         ioChannel.writeAndFlush(packet);
      } finally {
         announcerLock.unlock();
      }
   }

   public void announceHint(String msg, int length) {
      sendPacket(CUserLocal.sendHint(msg, length, 10));
      sendPacket(CWvsContext.enableActions());
   }

   public void changeChannel(int channel) {
      Server server = Server.getInstance();
      if (player.isBanned()) {
         disconnect(false, false);
         return;
      }
      if (!player.isAlive() || FieldLimit.CANNOTMIGRATE.check(player.getMap()
            .getFieldLimit())) {
         sendPacket(CWvsContext.enableActions());
         return;
      } else if (MapleMiniDungeonInfo.isDungeonMap(player.getMapId())) {
         sendPacket(CWvsContext.serverNotice(5,
               "Changing channels or entering Cash Shop or MTS are disabled when inside a Mini-Dungeon."));
         sendPacket(CWvsContext.enableActions());
         return;
      }

      String[] socket = Server.getInstance()
            .getInetSocket(getWorld(), channel);
      if (socket == null) {
         sendPacket(CWvsContext.serverNotice(1, "Channel " + channel + " is currently disabled. Try another channel."));
         sendPacket(CWvsContext.enableActions());
         return;
      }

      player.closePlayerInteractions();
      player.closePartySearchInteractions();

      player.unregisterChairBuff();
      server.getPlayerBuffStorage()
            .addBuffsToStorage(player.getId(), player.getAllBuffs());
      server.getPlayerBuffStorage()
            .addDiseasesToStorage(player.getId(), player.getAllDiseases());
      player.setDisconnectedFromChannelWorld();
      player.notifyMapTransferToPartner(-1);
      player.removeIncomingInvites();
      player.cancelAllBuffs(true);
      player.cancelAllDebuffs();
      player.cancelBuffExpireTask();
      player.cancelDiseaseExpireTask();
      player.cancelSkillCooldownTask();
      player.cancelQuestExpirationTask();
      //Cancelling magicdoor? Nope
      //Cancelling mounts? Noty

      player.getInventory(MapleInventoryType.EQUIPPED)
            .checked(false); //test
      player.getMap()
            .removePlayer(player);
      player.clearBanishPlayerData();
      player.getClient()
            .getChannelServer()
            .removePlayer(player);

      player.saveCharToDB();

      player.setSessionTransitionState();
      try {
         sendPacket(CClientSocket.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public long getSessionId() {
      return this.sessionId;
   }

   public boolean canRequestCharlist() {
      return lastNpcClick + 877 < Server.getInstance()
            .getCurrentTime();
   }

   public boolean canClickNPC() {
      return lastNpcClick + 500 < Server.getInstance()
            .getCurrentTime();
   }

   public void setClickedNPC() {
      lastNpcClick = Server.getInstance()
            .getCurrentTime();
   }

   public void removeClickedNPC() {
      lastNpcClick = 0;
   }

   public int getVisibleWorlds() {
      return visibleWorlds;
   }

   public void requestedServerlist(int worlds) {
      visibleWorlds = worlds;
      setClickedNPC();
   }

   public void closePlayerScriptInteractions() {
      this.removeClickedNPC();
      NPCScriptManager.getInstance()
            .dispose(this);
      QuestScriptManager.getInstance()
            .dispose(this);
   }

   public boolean attemptCsCoupon() {
      if (csattempt > 2) {
         resetCsCoupon();
         return false;
      }

      csattempt++;
      return true;
   }

   public void resetCsCoupon() {
      csattempt = 0;
   }

   public void enableCSActions() {
      sendPacket(CCashShop.enableCSUse(player));
   }

   public boolean canBypassPin() {
      return MapleLoginBypassCoordinator.getInstance()
            .canLoginBypass(hwid, accId, false);
   }

   public boolean canBypassPic() {
      return MapleLoginBypassCoordinator.getInstance()
            .canLoginBypass(hwid, accId, true);
   }

   public int getLanguage() {
      return lang;
   }

   public void setLanguage(int lingua) {
      this.lang = lingua;
   }

   public void closeSession() {
      ioChannel.close();
   }

   public enum Type {
      LOGIN,
      CHANNEL
   }

   private static class CharNameAndId {

      public String name;
      public int id;

      public CharNameAndId(String name, int id) {
         super();
         this.name = name;
         this.id = id;
      }

      public int getId() {
         return id;
      }
   }
}