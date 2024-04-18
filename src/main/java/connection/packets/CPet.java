package connection.packets;

import java.util.List;

import client.MapleCharacter;
import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import server.movement.LifeMovementFragment;

public class CPet {
   public static Packet movePet(int cid, int pid, byte slot, List<LifeMovementFragment> moves) {
      final OutPacket p = OutPacket.create(SendOpcode.MOVE_PET);
      p.writeInt(cid);
      p.writeByte(slot);
      p.writeInt(pid);
      serializeMovementList(p, moves);
      return p;
   }

   public static Packet petChat(int cid, byte index, int act, String text) {
      final OutPacket p = OutPacket.create(SendOpcode.PET_CHAT);
      p.writeInt(cid);
      p.writeByte(index);
      p.writeByte(0);
      p.writeByte(act);
      p.writeString(text);
      p.writeByte(0);
      return p;
   }

   public static Packet changePetName(MapleCharacter chr, String newname, int slot) {
      final OutPacket p = OutPacket.create(SendOpcode.PET_NAMECHANGE);
      p.writeInt(chr.getId());
      p.writeByte(0);
      p.writeString(newname);
      p.writeByte(0);
      return p;
   }

   public static Packet loadExceptionList(final int cid, final int petId, final byte petIdx, final List<Integer> data) {
      final OutPacket p = OutPacket.create(SendOpcode.PET_EXCEPTION_LIST);
      p.writeInt(cid);
      p.writeByte(petIdx);
      p.writeLong(petId);
      p.writeByte(data.size());
      for (final Integer ids : data) {
         p.writeInt(ids);
      }
      return p;
   }

   public static Packet petFoodResponse(int cid, byte index, boolean success, boolean balloonType) {
      final OutPacket p = OutPacket.create(SendOpcode.PET_COMMAND);
      p.writeInt(cid);
      p.writeByte(index);
      p.writeByte(1);
      p.writeBool(success);
      p.writeBool(balloonType);
      return p;
   }

   public static Packet commandResponse(int cid, byte index, boolean talk, int animation, boolean balloonType) {
      final OutPacket p = OutPacket.create(SendOpcode.PET_COMMAND);
      p.writeInt(cid);
      p.writeByte(index);
      p.writeByte(0);
      p.writeByte(animation);
      p.writeBool(!talk);
      p.writeBool(balloonType);
      return p;
   }

   private static void serializeMovementList(OutPacket p, List<LifeMovementFragment> moves) {
      p.writeByte(moves.size());
      for (LifeMovementFragment move : moves) {
         move.serialize(p);
      }
   }
}
