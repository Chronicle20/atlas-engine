package net.server.channel.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import tools.DatabaseConnection;

public final class ReportHandler extends AbstractMaplePacketHandler {
   public void handlePacket(InPacket p, MapleClient c) {
      int type = p.readByte(); //01 = Conversation claim 00 = illegal program
      String victim = p.readString();
      int reason = p.readByte();
      String description = p.readString();
      if (type == 0) {
         if (c.getPlayer().getPossibleReports() > 0) {
            if (c.getPlayer().getMeso() > 299) {
               c.getPlayer().decreaseReports();
               c.getPlayer().gainMeso(-300, true);
            } else {
               c.sendPacket(CWvsContext.reportResponse((byte) 4));
               return;
            }
         } else {
            c.sendPacket(CWvsContext.reportResponse((byte) 2));
            return;
         }
         Server.getInstance()
               .broadcastGMMessage(c.getWorld(), CWvsContext.serverNotice(6, victim + " was reported for: " + description));
         addReport(c.getPlayer().getId(), MapleCharacter.getIdByName(victim), 0, description, null);
      } else if (type == 1) {
         String chatlog = p.readString();
         if (chatlog == null) {
            return;
         }
         if (c.getPlayer().getPossibleReports() > 0) {
            if (c.getPlayer().getMeso() > 299) {
               c.getPlayer().decreaseReports();
               c.getPlayer().gainMeso(-300, true);
            } else {
               c.sendPacket(CWvsContext.reportResponse((byte) 4));
               return;
            }
         }
         Server.getInstance()
               .broadcastGMMessage(c.getWorld(), CWvsContext.serverNotice(6, victim + " was reported for: " + description));
         addReport(c.getPlayer().getId(), MapleCharacter.getIdByName(victim), reason, description, chatlog);
      } else {
         Server.getInstance().broadcastGMMessage(c.getWorld(), CWvsContext.serverNotice(6,
               c.getPlayer().getName() + " is probably packet editing. Got unknown report type, which is impossible."));
      }
   }

   public void addReport(int reporterid, int victimid, int reason, String description, String chatlog) {
      Calendar calendar = Calendar.getInstance();
      Timestamp currentTimestamp = new java.sql.Timestamp(calendar.getTime().getTime());
      Connection con;
      try {
         con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(
               "INSERT INTO reports (`reporttime`, `reporterid`, `victimid`, `reason`, `chatlog`, `description`) VALUES (?, ?, ?, ?, ?, ?)");
         ps.setString(1, currentTimestamp.toString());
         ps.setInt(2, reporterid);
         ps.setInt(3, victimid);
         ps.setInt(4, reason);
         ps.setString(5, chatlog);
         ps.setString(6, description);
         ps.addBatch();
         ps.executeBatch();
         ps.close();
         con.close();
      } catch (SQLException ex) {
         ex.printStackTrace();
      }
   }
}
