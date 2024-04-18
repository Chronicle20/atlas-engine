package net.server.handlers.login;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import client.MapleClient;
import config.YamlConfig;
import connection.constants.LoginStatusCode;
import connection.models.WorldInformation;
import connection.packets.CLogin;
import net.MaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.world.World;
import tools.DatabaseConnection;
import tools.HexTool;

public final class LoginPasswordHandler implements MaplePacketHandler {

   private static String hashpwSHA512(String pwd) throws NoSuchAlgorithmException, UnsupportedEncodingException {
      MessageDigest digester = MessageDigest.getInstance("SHA-512");
      digester.update(pwd.getBytes(StandardCharsets.UTF_8), 0, pwd.length());
      return HexTool.toString(digester.digest()).replace(" ", "").toLowerCase();
   }

   private static void login(MapleClient c) {
      c.sendPacket(CLogin.getAuthSuccess(c));//why the fk did I do c.getAccountName()?
      Server.getInstance().registerLoginState(c);

      List<World> worlds = Server.getInstance().getWorlds();
      c.requestedServerlist(worlds.size());
      worlds.stream()
            .map(WorldInformation::fromWorld)
            .map(CLogin::getWorldInformation)
            .forEach(c::sendPacket);
      c.sendPacket(CLogin.getEndOfWorldInformation());
      c.sendPacket(CLogin.selectWorld(0));
      c.sendPacket(CLogin.sendRecommended(Server.getInstance().worldRecommendedList()));
   }

   private static void disposeSql(Connection con, PreparedStatement ps) {
      try {
         if (con != null) {
            con.close();
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      String remoteHost = c.getRemoteAddress();
      if (remoteHost.contentEquals("null")) {
         c.sendPacket(CLogin.getLoginFailed(
               LoginStatusCode.WRONG_GATEWAY));          // thanks Alchemist for noting remoteHost could be null
         return;
      }

      if (YamlConfig.config.server.USE_IP_VALIDATION) {    // thanks Alex-0000 (CanIGetaPR) for suggesting IP validation as a server flag
         if (remoteHost.startsWith("127.")) {
            if (!YamlConfig.config.server.LOCALSERVER) { // thanks Mills for noting HOST can also have a field named "localhost"
               c.sendPacket(CLogin.getLoginFailed(
                     LoginStatusCode.UNABLE_TO_LOG_ON_AS_MASTER));  // cannot login as localhost if it's not a local server
               return;
            }
         } else {
            if (YamlConfig.config.server.LOCALSERVER) {
               c.sendPacket(CLogin.getLoginFailed(
                     LoginStatusCode.UNABLE_TO_LOG_ON_AS_MASTER));  // cannot login as non-localhost if it's a local server
               return;
            }
         }
      }

      String login = p.readString();
      String pwd = p.readString();
      c.setAccountName(login);

      p.skip(6);   // localhost masked the initial part with zeroes...
      byte[] hwidNibbles = p.readBytes(4);
      String nibbleHwid = HexTool.toCompressedString(hwidNibbles);
      LoginStatusCode loginok = c.login(login, pwd, nibbleHwid);

      Connection con = null;
      PreparedStatement ps = null;

      if (YamlConfig.config.server.AUTOMATIC_REGISTER && loginok == LoginStatusCode.NOT_REGISTERED_ID) {
         try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO accounts (name, password, birthday, tempban) VALUES (?, ?, ?, ?);",
                  Statement.RETURN_GENERATED_KEYS); //Jayd: Added birthday, tempban
            ps.setString(1, login);
            ps.setString(2, YamlConfig.config.server.BCRYPT_MIGRATION ? BCrypt.hashpw(pwd, BCrypt.gensalt(12)) : hashpwSHA512(pwd));
            ps.setString(3, "2018-06-20"); //Jayd's idea: was added to solve the MySQL 5.7 strict checking (birthday)
            ps.setString(4, "2018-06-20"); //Jayd's idea: was added to solve the MySQL 5.7 strict checking (tempban)
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            c.setAccID(rs.getInt(1));
            rs.close();
         } catch (SQLException | NoSuchAlgorithmException | UnsupportedEncodingException e) {
            c.setAccID(-1);
            e.printStackTrace();
         } finally {
            disposeSql(con, ps);
            loginok = c.login(login, pwd, nibbleHwid);
         }
      }

      if (YamlConfig.config.server.BCRYPT_MIGRATION && (loginok.getCode()
            <= -10)) { // -10 means migration to bcrypt, -23 means TOS wasn't accepted
         try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE accounts SET password = ? WHERE name = ?;");
            ps.setString(1, BCrypt.hashpw(pwd, BCrypt.gensalt(12)));
            ps.setString(2, login);
            ps.executeUpdate();
         } catch (SQLException e) {
            e.printStackTrace();
         } finally {
            disposeSql(con, ps);
            loginok = (loginok == LoginStatusCode.BCRYPT_MIGRATION) ? LoginStatusCode.OK : LoginStatusCode.LICENSE_AGREEMENT;
         }
      }

      if (c.hasBannedIP() || c.hasBannedMac()) {
         c.sendPacket(CLogin.getLoginFailed(LoginStatusCode.ID_DELETED_OR_BLOCKED));
         return;
      }
      Calendar tempban = c.getTempBanCalendarFromDB();
      if (tempban != null) {
         if (tempban.getTimeInMillis() > Calendar.getInstance().getTimeInMillis()) {
            c.sendPacket(CLogin.getTempBan(tempban.getTimeInMillis(), c.getGReason()));
            return;
         }
      }
      if (loginok == LoginStatusCode.ID_DELETED_OR_BLOCKED) {
         c.sendPacket(CLogin.getPermBan(c.getGReason()));//crashes but idc :D
         return;
      } else if (loginok.isError()) {
         c.sendPacket(CLogin.getLoginFailed(loginok));
         return;
      }
      if (c.finishLogin() == 0) {
         c.checkChar(c.getAccID());
         login(c);
      } else {
         c.sendPacket(CLogin.getLoginFailed(LoginStatusCode.ALREADY_LOGGED_IN));
      }
   }

   @Override
   public boolean validateState(MapleClient c) {
      return !c.isLoggedIn();
   }
}
