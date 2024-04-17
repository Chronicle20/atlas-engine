package ban;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import tools.DatabaseConnection;

public class BanProcessor {
   private static BanProcessor instance = null;

   public static synchronized BanProcessor getInstance() {
      if (instance == null) {
         instance = new BanProcessor();
      }

      return instance;
   }


   private BanProcessor() {
   }

   public boolean ban(String id, String reason, boolean accountId) {
      PreparedStatement ps = null;
      ResultSet rs = null;
      Connection con = null;
      try {
         con = DatabaseConnection.getConnection();

         if (id.matches("/[0-9]{1,3}\\..*")) {
            ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
            ps.setString(1, id);
            ps.executeUpdate();
            ps.close();
            return true;
         }
         if (accountId) {
            ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
         } else {
            ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
         }

         boolean ret = false;
         ps.setString(1, id);
         rs = ps.executeQuery();
         if (rs.next()) {

            try (Connection con2 = DatabaseConnection.getConnection();
                 PreparedStatement psb = con2.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?")) {
               psb.setString(1, reason);
               psb.setInt(2, rs.getInt(1));
               psb.executeUpdate();
            }
            ret = true;
         }

         rs.close();
         ps.close();
         con.close();
         return ret;
      } catch (SQLException ex) {
         ex.printStackTrace();
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
      return false;
   }
}
