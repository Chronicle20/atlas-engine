package net.server.channel.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import client.MapleClient;
import connection.packets.CCashShop;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import tools.DatabaseConnection;

public final class NoteActionHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int action = p.readByte();
      if (action == 0 && c.getPlayer().getCashShop().getAvailableNotes() > 0) {
         String charname = p.readString();
         String message = p.readString();
         if (c.getPlayer().getCashShop().isOpened()) {
            c.sendPacket(CCashShop.showCashInventory(c));
         }

         c.getPlayer().sendNote(charname, message, (byte) 1);
         c.getPlayer().getCashShop().decreaseNotes();
      } else if (action == 1) {
         int num = p.readByte();
         p.readByte();
         p.readByte();
         int fame = 0;
         for (int i = 0; i < num; i++) {
            int id = p.readInt();
            p.readByte(); //Fame, but we read it from the database :)
            PreparedStatement ps;
            try {
               Connection con = DatabaseConnection.getConnection();
               ps = con.prepareStatement("SELECT `fame` FROM notes WHERE id=? AND deleted=0");
               ps.setInt(1, id);
               ResultSet rs = ps.executeQuery();
               if (rs.next()) {
                  fame += rs.getInt("fame");
               }
               rs.close();

               ps = con.prepareStatement("UPDATE notes SET `deleted` = 1 WHERE id = ?");
               ps.setInt(1, id);
               ps.executeUpdate();
               ps.close();
               con.close();
            } catch (SQLException e) {
               e.printStackTrace();
            }
         }
         if (fame > 0) {
            c.getPlayer().gainFame(fame);
         }
      }
   }
}
