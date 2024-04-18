package net.packet.logging;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleCharacter;
import client.MapleClient;
import connection.headers.RecvOpcode;
import net.jcip.annotations.NotThreadSafe;
import tools.HexTool;

@NotThreadSafe
public class MonitoredChrLogger {
   private static final Logger log = LoggerFactory.getLogger(MonitoredChrLogger.class);
   private static final Set<Integer> monitoredChrIds = new HashSet<>();

   /**
    * Toggle monitored status for a character id
    *
    * @return new status. true if the chrId is now monitored, otherwise false.
    */
   public static boolean toggleMonitored(int chrId) {
      if (monitoredChrIds.contains(chrId)) {
         monitoredChrIds.remove(chrId);
         return false;
      } else {
         monitoredChrIds.add(chrId);
         return true;
      }
   }

   public static Collection<Integer> getMonitoredChrIds() {
      return monitoredChrIds;
   }

   public static void logPacketIfMonitored(MapleClient c, short packetId, byte[] packetContent) {
      MapleCharacter chr = c.getPlayer();
      if (chr == null) {
         return;
      }
      if (!monitoredChrIds.contains(chr.getId())) {
         return;
      }
      RecvOpcode op = getOpcodeFromValue(packetId);
      if (isRecvBlocked(op)) {
         return;
      }

      String packet = packetContent.length > 0 ? HexTool.toHexString(packetContent) : "<empty>";
      log.info("{}-{} {}-{}", c.getAccountName(), chr.getName(), packetId, packet);
   }

   private static boolean isRecvBlocked(RecvOpcode op) {
      return switch (op) {
         case MOVE_PLAYER, GENERAL_CHAT, TAKE_DAMAGE, MOVE_PET, MOVE_LIFE, NPC_ACTION, FACE_EXPRESSION -> true;
         default -> false;
      };
   }

   private static RecvOpcode getOpcodeFromValue(int value) {
      return Arrays.stream(RecvOpcode.values())
            .filter(opcode -> value == opcode.getValue())
            .findAny()
            .orElse(null);
   }
}
