package net.server.channel.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import connection.packets.CCashShop;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import tools.DatabaseConnection;

public final class TransferWorldHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      p.readInt(); //cid
      int birthday = p.readInt();
      if (!CashOperationHandler.checkBirthday(c, birthday)) {
         c.sendPacket(CCashShop.showCashShopMessage((byte) 0xC4));
         c.sendPacket(CWvsContext.enableActions());
         return;
      }
      MapleCharacter chr = c.getPlayer();
      if (!YamlConfig.config.server.ALLOW_CASHSHOP_WORLD_TRANSFER || Server.getInstance().getWorldsSize() <= 1) {
         c.sendPacket(CCashShop.sendWorldTransferRules(9, c));
         return;
      }
      int worldTransferError = chr.checkWorldTransferEligibility();
      if (worldTransferError != 0) {
         c.sendPacket(CCashShop.sendWorldTransferRules(worldTransferError, c));
         return;
      }
      try (Connection con = DatabaseConnection.getConnection();
           PreparedStatement ps = con.prepareStatement("SELECT completionTime FROM worldtransfers WHERE characterid=?")) {
         ps.setInt(1, chr.getId());
         ResultSet rs = ps.executeQuery();
         while (rs.next()) {
            Timestamp completedTimestamp = rs.getTimestamp("completionTime");
            if (completedTimestamp == null) { //has pending world transfer
               c.sendPacket(CCashShop.sendWorldTransferRules(6, c));
               return;
            } else if (completedTimestamp.getTime() + YamlConfig.config.server.WORLD_TRANSFER_COOLDOWN
                  > System.currentTimeMillis()) {
               c.sendPacket(CCashShop.sendWorldTransferRules(7, c));
               return;
            }
         }
      } catch (SQLException e) {
         e.printStackTrace();
         return;
      }
      c.sendPacket(CCashShop.sendWorldTransferRules(0, c));
   }
}