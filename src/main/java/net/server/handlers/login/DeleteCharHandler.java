package net.server.handlers.login;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import client.MapleClient;
import client.MapleFamily;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import tools.DatabaseConnection;
import tools.FilePrinter;

public final class DeleteCharHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      String pic = p.readString();
      int cid = p.readInt();
      if (c.checkPic(pic)) {
         //check for family, guild leader, pending marriage, world transfer
         try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
               "SELECT `world`, `guildid`, `guildrank`, `familyId` FROM characters WHERE id = ?");
              PreparedStatement ps2 = con.prepareStatement(
                    "SELECT COUNT(*) as rowcount FROM worldtransfers WHERE `characterid` = ? AND completionTime IS NULL")) {
            ps.setInt(1, cid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
               throw new SQLException("Character record does not exist.");
            }
            int world = rs.getInt("world");
            int guildId = rs.getInt("guildid");
            int guildRank = rs.getInt("guildrank");
            int familyId = rs.getInt("familyId");
            if (guildId != 0 && guildRank <= 1) {
               c.sendPacket(CLogin.deleteCharResponse(cid, 0x16));
               return;
            } else if (familyId != -1) {
               MapleFamily family = Server.getInstance().getWorld(world).orElseThrow().getFamily(familyId);
               if (family != null && family.getTotalMembers() > 1) {
                  c.sendPacket(CLogin.deleteCharResponse(cid, 0x1D));
                  return;
               }
            }
            rs.close();
            ps2.setInt(1, cid);
            rs = ps2.executeQuery();
            rs.next();
            if (rs.getInt("rowcount") > 0) {
               c.sendPacket(CLogin.deleteCharResponse(cid, 0x1A));
               return;
            }
         } catch (SQLException e) {
            e.printStackTrace();
            c.sendPacket(CLogin.deleteCharResponse(cid, 0x09));
            return;
         }
         if (c.deleteCharacter(cid, c.getAccID())) {
            FilePrinter.print(FilePrinter.DELETED_CHAR + c.getAccountName() + ".txt", c.getAccountName() + " deleted CID: " + cid);
            c.sendPacket(CLogin.deleteCharResponse(cid, 0));
         } else {
            c.sendPacket(CLogin.deleteCharResponse(cid, 0x09));
         }
      } else {
         c.sendPacket(CLogin.deleteCharResponse(cid, 0x14));
      }
   }
}
