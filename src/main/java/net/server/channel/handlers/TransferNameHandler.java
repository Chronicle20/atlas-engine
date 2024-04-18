package net.server.channel.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import connection.packets.CCashShop;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import tools.DatabaseConnection;

public final class TransferNameHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      p.readInt(); //cid
      int birthday = p.readInt();
      if (!CashOperationHandler.checkBirthday(c, birthday)) {
         c.sendPacket(CCashShop.showCashShopMessage((byte) 0xC4));
         c.sendPacket(CWvsContext.enableActions());
         return;
      }
      if (!YamlConfig.config.server.ALLOW_CASHSHOP_NAME_CHANGE) {
         c.sendPacket(CCashShop.sendNameTransferRules(4));
         return;
      }
      MapleCharacter chr = c.getPlayer();
      if (chr.getLevel() < 10) {
         c.sendPacket(CCashShop.sendNameTransferRules(4));
         return;
      } else if (c.getTempBanCalendar() != null
            && c.getTempBanCalendar().getTimeInMillis() + (30L * 24 * 60 * 60 * 1000) < Calendar.getInstance().getTimeInMillis()) {
         c.sendPacket(CCashShop.sendNameTransferRules(2));
         return;
      }
      //sql queries
      try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
            "SELECT completionTime FROM namechanges WHERE characterid=?")) { //double check, just in case
         ps.setInt(1, chr.getId());
         ResultSet rs = ps.executeQuery();
         while (rs.next()) {
            Timestamp completedTimestamp = rs.getTimestamp("completionTime");
            if (completedTimestamp == null) { //has pending name request
               c.sendPacket(CCashShop.sendNameTransferRules(1));
               return;
            } else if (completedTimestamp.getTime() + YamlConfig.config.server.NAME_CHANGE_COOLDOWN > System.currentTimeMillis()) {
               c.sendPacket(CCashShop.sendNameTransferRules(3));
               return;
            }
         }
      } catch (SQLException e) {
         e.printStackTrace();
         return;
      }
      c.sendPacket(CCashShop.sendNameTransferRules(0));
   }
}