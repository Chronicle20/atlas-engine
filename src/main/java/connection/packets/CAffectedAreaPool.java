package connection.packets;

import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import server.maps.MapleMist;

public class CAffectedAreaPool {
   public static Packet spawnMist(int oid, int ownerCid, int skill, int level, MapleMist mist) {
      OutPacket p = OutPacket.create(SendOpcode.SPAWN_MIST);
      p.writeInt(oid);
      p.writeInt(mist.isMobMist() ? 0 : mist.isPoisonMist() ? 1 :
            mist.isRecoveryMist() ? 4 : 2); // mob mist = 0, player poison = 1, smokescreen = 2, unknown = 3, recovery = 4
      p.writeInt(ownerCid);
      p.writeInt(skill);
      p.writeByte(level);
      p.writeShort(mist.getSkillDelay()); // Skill delay
      p.writeInt(mist.getBox().x);
      p.writeInt(mist.getBox().y);
      p.writeInt(mist.getBox().x + mist.getBox().width);
      p.writeInt(mist.getBox().y + mist.getBox().height);
      p.writeInt(0);
      return p;
   }

   public static Packet removeMist(int oid) {
      OutPacket p = OutPacket.create(SendOpcode.REMOVE_MIST);
      p.writeInt(oid);
      return p;
   }
}
