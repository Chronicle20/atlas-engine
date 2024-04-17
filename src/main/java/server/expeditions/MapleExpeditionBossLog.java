package server.expeditions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import config.YamlConfig;
import tools.DatabaseConnection;
import tools.Pair;

public class MapleExpeditionBossLog {

   public static void resetBossLogTable() {
        /*
        Boss logs resets 12am, weekly thursday 12AM - thanks Smitty Werbenjagermanjensen (superadlez) - https://www.reddit.com/r/Maplestory/comments/61tiup/about_reset_time/
        */

      Calendar thursday = Calendar.getInstance();
      thursday.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
      thursday.set(Calendar.HOUR, 0);
      thursday.set(Calendar.MINUTE, 0);
      thursday.set(Calendar.SECOND, 0);

      Calendar now = Calendar.getInstance();

      long weekLength = 7 * 24 * 60 * 60 * 1000;
      long halfDayLength = 12 * 60 * 60 * 1000;

      long deltaTime = now.getTime().getTime() - thursday.getTime().getTime();    // 2x time: get Date into millis
      deltaTime += halfDayLength;
      deltaTime %= weekLength;
      deltaTime -= halfDayLength;

      if (deltaTime < halfDayLength) {
         MapleExpeditionBossLog.resetBossLogTable(true, thursday);
      }

      now.set(Calendar.HOUR, 0);
      now.set(Calendar.MINUTE, 0);
      now.set(Calendar.SECOND, 0);

      MapleExpeditionBossLog.resetBossLogTable(false, now);
   }

   private static void resetBossLogTable(boolean week, Calendar c) {
      List<Pair<Timestamp, BossLogEntry>> resetTimestamps = BossLogEntry.getBossLogResetTimestamps(c, week);

      try {
         Connection con = DatabaseConnection.getConnection();

         for (Pair<Timestamp, BossLogEntry> p : resetTimestamps) {
            PreparedStatement ps =
                  con.prepareStatement("DELETE FROM " + getBossLogTable(week) + " WHERE attempttime <= ? AND bosstype LIKE ?");
            ps.setTimestamp(1, p.getLeft());
            ps.setString(2, p.getRight().name());
            ps.executeUpdate();
            ps.close();
         }

         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   private static String getBossLogTable(boolean week) {
      return week ? "bosslog_weekly" : "bosslog_daily";
   }

   private static int countPlayerEntries(int cid, BossLogEntry boss) {
      int ret_count;
      try {
         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps;
         ps = con.prepareStatement(
               "SELECT COUNT(*) FROM " + getBossLogTable(boss.week) + " WHERE characterid = ? AND bosstype LIKE ?");
         ps.setInt(1, cid);
         ps.setString(2, boss.name());
         ResultSet rs = ps.executeQuery();
         if (rs.next()) {
            ret_count = rs.getInt(1);
         } else {
            ret_count = -1;
         }
         rs.close();
         ps.close();
         con.close();
         return ret_count;
      } catch (SQLException e) {
         e.printStackTrace();
         return -1;
      }
   }

   private static void insertPlayerEntry(int cid, BossLogEntry boss) {
      try {
         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps =
               con.prepareStatement("INSERT INTO " + getBossLogTable(boss.week) + " (characterid, bosstype) VALUES (?,?)");
         ps.setInt(1, cid);
         ps.setString(2, boss.name());
         ps.executeUpdate();
         ps.close();
         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   public static boolean attemptBoss(int cid, int channel, MapleExpedition exped, boolean log) {
      if (!YamlConfig.config.server.USE_ENABLE_DAILY_EXPEDITIONS) {
         return true;
      }

      Optional<BossLogEntry> boss = BossLogEntry.getBossEntryByName(exped.getType().name());
      if (boss.isEmpty()) {
         return true;
      }

      if (channel < boss.get().minChannel || channel > boss.get().maxChannel) {
         return false;
      }

      if (countPlayerEntries(cid, boss.get()) >= boss.get().entries) {
         return false;
      }

      if (log) {
         insertPlayerEntry(cid, boss.get());
      }
      return true;
   }

   public enum BossLogEntry {
      ZAKUM(2, 1, false),
      HORNTAIL(2, 1, false),
      PINKBEAN(1, 1, false),
      SCARGA(1, 1, false),
      PAPULATUS(2, 1, false);

      private final int entries;
      private final int timeLength;
      private final int minChannel;
      private final int maxChannel;
      private final boolean week;

      BossLogEntry(int entries, int timeLength, boolean week) {
         this(entries, 0, Integer.MAX_VALUE, timeLength, week);
      }

      BossLogEntry(int entries, int minChannel, int maxChannel, int timeLength, boolean week) {
         this.entries = entries;
         this.minChannel = minChannel;
         this.maxChannel = maxChannel;
         this.timeLength = timeLength;
         this.week = week;
      }

      private static List<Pair<Timestamp, BossLogEntry>> getBossLogResetTimestamps(Calendar timeNow, boolean week) {
         List<Pair<Timestamp, BossLogEntry>> resetTimestamps = new LinkedList<>();

         Timestamp ts = new Timestamp(timeNow.getTime().getTime());  // reset all table entries actually, thanks Conrad
         for (BossLogEntry b : BossLogEntry.values()) {
            if (b.week == week) {
               resetTimestamps.add(new Pair<>(ts, b));
            }
         }

         return resetTimestamps;
      }

      private static Optional<BossLogEntry> getBossEntryByName(String name) {
         return Arrays.stream(BossLogEntry.values())
               .filter(ble -> name.contentEquals(ble.name()))
               .findFirst();
      }
   }
}
