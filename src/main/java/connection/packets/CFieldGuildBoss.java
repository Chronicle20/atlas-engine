package connection.packets;

import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CFieldGuildBoss {
   private static Packet GuildBoss_HealerMove(short nY) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_BOSS_HEALER_MOVE);
      p.writeShort(nY); //New Y Position
      return p;
   }

   private static Packet GuildBoss_PulleyStateChange(byte nState) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_BOSS_PULLEY_STATE_CHANGE);
      p.writeByte(nState);
      return p;
   }
}
