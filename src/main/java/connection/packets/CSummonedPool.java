package connection.packets;

import java.awt.*;
import java.util.List;

import connection.headers.SendOpcode;
import net.packet.InPacket;
import net.packet.OutPacket;
import net.packet.Packet;
import net.server.channel.handlers.SummonDamageHandler;
import server.maps.MapleSummon;

public class CSummonedPool {
   /**
    * Gets a packet to spawn a special map object.
    *
    * @param summon
    * @param animated Animated spawn?
    * @return The spawn packet for the map object.
    */
   public static Packet spawnSummon(MapleSummon summon, boolean animated) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_SPECIAL_MAPOBJECT);
      p.writeInt(summon.getOwner().getId());
      p.writeInt(summon.getObjectId());
      p.writeInt(summon.getSkill());
      p.writeByte(0x0A); //v83
      p.writeByte(summon.getSkillLevel());
      p.writePos(summon.getPosition());
      p.writeByte(summon.getStance());    //bMoveAction & foothold, found thanks to Rien dev team
      p.writeShort(0);
      p.writeByte(summon.getMovementType()
            .getValue()); // 0 = don't move, 1 = follow (4th mage summons?), 2/4 = only tele follow, 3 = bird follow
      p.writeByte(summon.isPuppet() ? 0 : 1); // 0 and the summon can't attack - but puppets don't attack with 1 either ^.-
      p.writeByte(animated ? 0 : 1);
      return p;
   }

   /**
    * Gets a packet to remove a special map object.
    *
    * @param summon
    * @param animated Animated removal?
    * @return The packet removing the object.
    */
   public static Packet removeSummon(MapleSummon summon, boolean animated) {
      final OutPacket p = OutPacket.create(SendOpcode.REMOVE_SPECIAL_MAPOBJECT);
      p.writeInt(summon.getOwner().getId());
      p.writeInt(summon.getObjectId());
      p.writeByte(animated ? 4 : 1); // ?
      return p;
   }

   public static Packet moveSummon(int cid, int oid, Point startPos, InPacket ip, long movementDataLength) {
      final OutPacket p = OutPacket.create(SendOpcode.MOVE_SUMMON);
      p.writeInt(cid);
      p.writeInt(oid);
      p.writePos(startPos);
      CCommon.rebroadcastMovementList(p, ip, movementDataLength);
      return p;
   }

   public static Packet summonAttack(int cid, int summonOid, byte direction,
                                     List<SummonDamageHandler.SummonAttackEntry> allDamage) {
      final OutPacket p = OutPacket.create(SendOpcode.SUMMON_ATTACK);
      p.writeInt(cid);
      p.writeInt(summonOid);
      p.writeByte(0);     // char level
      p.writeByte(direction);
      p.writeByte(allDamage.size());
      for (SummonDamageHandler.SummonAttackEntry attackEntry : allDamage) {
         p.writeInt(attackEntry.monsterOid()); // oid
         p.writeByte(6); // who knows
         p.writeInt(attackEntry.damage()); // damage
      }

      return p;
   }

   public static Packet damageSummon(int cid, int oid, int damage, int monsterIdFrom) {
      final OutPacket p = OutPacket.create(SendOpcode.DAMAGE_SUMMON);
      p.writeInt(cid);
      p.writeInt(oid);
      p.writeByte(12);
      p.writeInt(damage);         // damage display doesn't seem to work...
      p.writeInt(monsterIdFrom);
      p.writeByte(0);
      return p;
   }

   public static Packet summonSkill(int cid, int summonSkillId, int newStance) {
      final OutPacket p = OutPacket.create(SendOpcode.SUMMON_SKILL);
      p.writeInt(cid);
      p.writeInt(summonSkillId);
      p.writeByte(newStance);
      return p;
   }
}
