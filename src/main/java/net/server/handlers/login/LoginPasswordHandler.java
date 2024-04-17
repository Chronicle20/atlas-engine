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
package net.server.handlers.login;

import client.MapleClient;
import config.YamlConfig;
import connection.constants.LoginStatusCode;
import connection.packets.CLogin;
import constants.game.GameConstants;
import net.MaplePacketHandler;
import net.server.Server;
import net.server.coordinator.session.MapleSessionCoordinator;
import net.server.world.World;
import org.apache.mina.core.session.IoSession;
import org.mindrot.jbcrypt.BCrypt;

import tools.DatabaseConnection;
import tools.HexTool;
import tools.data.input.SeekableLittleEndianAccessor;

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

public final class LoginPasswordHandler implements MaplePacketHandler {

    private static String hashpwSHA512(String pwd) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digester = MessageDigest.getInstance("SHA-512");
        digester.update(pwd.getBytes(StandardCharsets.UTF_8), 0, pwd.length());
        return HexTool.toString(digester.digest()).replace(" ", "").toLowerCase();
    }

    private static String getRemoteIp(IoSession session) {
        return MapleSessionCoordinator.getSessionRemoteAddress(session);
    }

    private static void login(MapleClient c) {
        c.announce(CLogin.getAuthSuccess(c));//why the fk did I do c.getAccountName()?
        Server.getInstance().registerLoginState(c);

        List<World> worlds = Server.getInstance().getWorlds();
        c.requestedServerlist(worlds.size());
        for (World world : worlds) {
            c.announce(CLogin.getServerList(world.getId(), GameConstants.WORLD_NAMES[world.getId()], world.getFlag(), world.getEventMessage(), world.getChannels()));
        }
        c.announce(CLogin.getEndOfServerList());
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
    public boolean validateState(MapleClient c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String remoteHost = getRemoteIp(c.getSession());
        if (!remoteHost.contentEquals("null")) {
            if (YamlConfig.config.server.USE_IP_VALIDATION) {    // thanks Alex-0000 (CanIGetaPR) for suggesting IP validation as a server flag
                if (remoteHost.startsWith("127.")) {
                    if (!YamlConfig.config.server.LOCALSERVER) { // thanks Mills for noting HOST can also have a field named "localhost"
                        c.announce(CLogin.getLoginFailed(LoginStatusCode.UNABLE_TO_LOG_ON_AS_MASTER));  // cannot login as localhost if it's not a local server
                        return;
                    }
                } else {
                    if (YamlConfig.config.server.LOCALSERVER) {
                        c.announce(CLogin.getLoginFailed(LoginStatusCode.UNABLE_TO_LOG_ON_AS_MASTER));  // cannot login as non-localhost if it's a local server
                        return;
                    }
                }
            }
        } else {
            c.announce(CLogin.getLoginFailed(LoginStatusCode.WRONG_GATEWAY));          // thanks Alchemist for noting remoteHost could be null
            return;
        }

        String login = slea.readMapleAsciiString();
        String pwd = slea.readMapleAsciiString();
        c.setAccountName(login);

        slea.skip(6);   // localhost masked the initial part with zeroes...
        byte[] hwidNibbles = slea.read(4);
        String nibbleHwid = HexTool.toCompressedString(hwidNibbles);
        LoginStatusCode loginok = c.login(login, pwd, nibbleHwid);

        Connection con = null;
        PreparedStatement ps = null;

        if (YamlConfig.config.server.AUTOMATIC_REGISTER && loginok == LoginStatusCode.NOT_REGISTERED_ID) {
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("INSERT INTO accounts (name, password, birthday, tempban) VALUES (?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS); //Jayd: Added birthday, tempban
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

        if (YamlConfig.config.server.BCRYPT_MIGRATION && (loginok.getCode() <= -10)) { // -10 means migration to bcrypt, -23 means TOS wasn't accepted
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
            c.announce(CLogin.getLoginFailed(LoginStatusCode.ID_DELETED_OR_BLOCKED));
            return;
        }
        Calendar tempban = c.getTempBanCalendarFromDB();
        if (tempban != null) {
            if (tempban.getTimeInMillis() > Calendar.getInstance().getTimeInMillis()) {
                c.announce(CLogin.getTempBan(tempban.getTimeInMillis(), c.getGReason()));
                return;
            }
        }
        if (loginok == LoginStatusCode.ID_DELETED_OR_BLOCKED) {
            c.announce(CLogin.getPermBan(c.getGReason()));//crashes but idc :D
            return;
        } else if (loginok.isError()) {
            c.announce(CLogin.getLoginFailed(loginok));
            return;
        }
        if (c.finishLogin() == 0) {
            c.checkChar(c.getAccID());
            login(c);
        } else {
            c.announce(CLogin.getLoginFailed(LoginStatusCode.ALREADY_LOGGED_IN));
        }
    }
}
