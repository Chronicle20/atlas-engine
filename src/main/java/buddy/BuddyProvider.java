package buddy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.BuddyRequestInfo;
import tools.DatabaseConnection;

public class BuddyProvider {
   private final static Logger log = LoggerFactory.getLogger(BuddyProvider.class);

   protected static BuddyList loadBuddyList(BuddyCharacterKey key) {
      Map<Integer, BuddyListEntry> buddies = new HashMap<>();
      Deque<BuddyRequestInfo> pendingRequests = new LinkedList<>();
      int capacity = BuddyConstants.DEFAULT_CAPACITY;
      try {

         Connection con = DatabaseConnection.getConnection();
         CharacterIdNameBuddyCapacity ret;
         try (PreparedStatement ps = con.prepareStatement("SELECT id, name, buddyCapacity FROM characters WHERE id = ?")) {
            ps.setInt(1, key.characterId());
            try (ResultSet rs = ps.executeQuery()) {
               ret = null;
               if (rs.next()) {
                  ret = new CharacterIdNameBuddyCapacity(rs.getInt("id"), rs.getString("name"), rs.getInt("buddyCapacity"));
                  capacity = ret.buddyCapacity();
               }
            }
         }

         PreparedStatement ps = con.prepareStatement(
               "SELECT b.buddyid, b.pending, b.group, c.name as buddyname FROM buddies as b, characters as c WHERE c.id = b.buddyid AND b.characterid = ?");
         ps.setInt(1, key.characterId());
         ResultSet rs = ps.executeQuery();
         while (rs.next()) {
            if (rs.getInt("pending") == 1) {
               pendingRequests.push(new BuddyRequestInfo(0, rs.getInt("buddyid"), rs.getString("buddyname"), 0, 0,
                     rs.getString("group")));
            } else {
               BuddyListEntry entry = new BuddyListEntry(rs.getString("buddyname"), rs.getString("group"), rs.getInt("buddyid"),
                     (byte) -1, true);
               buddies.put(entry.characterId(), entry);
            }
         }
         rs.close();
         ps.close();
         ps = con.prepareStatement("DELETE FROM buddies WHERE pending = 1 AND characterid = ?");
         ps.setInt(1, key.characterId());
         ps.executeUpdate();
         ps.close();
         con.close();
      } catch (SQLException ex) {
         log.error("Error loading buddy list", ex);
      }
      return new BuddyList(capacity, buddies, pendingRequests);
   }

   @Deprecated
   protected static CharacterIdNameBuddyCapacity getCharacterIdAndNameFromDatabase(String name) throws SQLException {
      Connection con = DatabaseConnection.getConnection();
      CharacterIdNameBuddyCapacity ret;
      try (PreparedStatement ps = con.prepareStatement("SELECT id, name, buddyCapacity FROM characters WHERE name LIKE ?")) {
         ps.setString(1, name);
         try (ResultSet rs = ps.executeQuery()) {
            ret = null;
            if (rs.next()) {
               ret = new CharacterIdNameBuddyCapacity(rs.getInt("id"), rs.getString("name"), rs.getInt("buddyCapacity"));
            }
         }
      }
      con.close();
      return ret;
   }
}
