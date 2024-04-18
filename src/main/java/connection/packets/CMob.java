package connection.packets;

import java.awt.*;
import java.util.List;
import java.util.Map;

import client.Skill;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import server.life.MapleMonster;
import server.movement.MovePath;

public class CMob {
   public static Packet moveMonster(int monsterId, boolean bNotForceLandingWhenDiscard, boolean bNotChangeAction,
                                    boolean bNextAttackPossible, byte bLeft, int skillData,
                                    List<Point> multiTargetForBall, List<Integer> randTimeForAreaAttack,
                                    MovePath moves) {
      final OutPacket p = OutPacket.create(SendOpcode.MOVE_MONSTER);
      p.writeInt(monsterId);
      p.writeBool(bNotForceLandingWhenDiscard);
      p.writeBool(bNotChangeAction);
      p.writeBool(bNextAttackPossible);
      p.writeByte(bLeft);
      p.writeInt(skillData);
      p.writeInt(multiTargetForBall.size());
      for (Point pt : multiTargetForBall) {
         p.writeInt(pt.x);
         p.writeInt(pt.y);
      }

      p.writeInt(randTimeForAreaAttack.size());
      for (Integer time : randTimeForAreaAttack) {
         p.writeInt(time);
      }

      moves.encode(p);
      return p;
   }

   /**
    * Gets a response to a move monster packet.
    *
    * @param objectid   The ObjectID of the monster being moved.
    * @param moveid     The movement ID.
    * @param currentMp  The current MP of the monster.
    * @param useSkills  Can the monster use skills?
    * @param skillId    The skill ID for the monster to use.
    * @param skillLevel The level of the skill to use.
    * @return The move response packet.
    */

   public static Packet moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills, int skillId,
                                            int skillLevel) {
      final OutPacket p = OutPacket.create(SendOpcode.MOVE_MONSTER_RESPONSE);
      p.writeInt(objectid);
      p.writeShort(moveid);
      p.writeBool(useSkills);
      p.writeShort(currentMp);
      p.writeByte(skillId);
      p.writeByte(skillLevel);
      return p;
   }

   public static Packet applyMonsterStatus(final int oid, final MonsterStatusEffect mse, final List<Integer> reflection) {
      Map<MonsterStatus, Integer> stati = mse.getStati();
      final OutPacket p = OutPacket.create(SendOpcode.APPLY_MONSTER_STATUS);
      p.writeInt(oid);
      p.writeLong(0);
      writeIntMask(p, stati);
      for (Map.Entry<MonsterStatus, Integer> stat : stati.entrySet()) {
         p.writeShort(stat.getValue());
         if (mse.isMonsterSkill()) {
            p.writeShort(mse.getMobSkill()
                  .getSkillId());
            p.writeShort(mse.getMobSkill()
                  .getSkillLevel());
         } else {
            p.writeInt(mse.getSkill()
                  .map(Skill::id)
                  .orElse(0));
         }
         p.writeShort(-1); // might actually be the buffTime but it's not displayed anywhere
      }
      int size = stati.size(); // size
      if (reflection != null) {
         for (Integer ref : reflection) {
            p.writeInt(ref);
         }
         if (!reflection.isEmpty()) {
            size /= 2; // This gives 2 buffs per reflection but it's really one buff
         }
      }
      p.writeByte(size); // size
      p.writeInt(0);
      return p;
   }

   public static Packet cancelMonsterStatus(int oid, Map<MonsterStatus, Integer> stats) {
      final OutPacket p = OutPacket.create(SendOpcode.CANCEL_MONSTER_STATUS);
      p.writeInt(oid);
      p.writeLong(0);
      writeIntMask(p, stats);
      p.writeInt(0);
      return p;
   }

   public static Packet damageMonster(int oid, int damage, int curhp, int maxhp) {
      final OutPacket p = OutPacket.create(SendOpcode.DAMAGE_MONSTER);
      p.writeInt(oid);
      p.writeByte(0);
      p.writeInt(damage);
      p.writeInt(curhp);
      p.writeInt(maxhp);
      return p;
   }

   public static Packet MobDamageMobFriendly(MapleMonster mob, int damage, int remainingHp) {
      final OutPacket p = OutPacket.create(SendOpcode.DAMAGE_MONSTER);
      p.writeInt(mob.getObjectId());
      p.writeByte(1); // direction ?
      p.writeInt(damage);
      p.writeInt(remainingHp);
      p.writeInt(mob.getMaxHp());
      return p;
   }

   /**
    * @param oid
    * @param remhppercentage
    * @return
    */
   public static Packet showMonsterHP(int oid, int remhppercentage) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_MONSTER_HP);
      p.writeInt(oid);
      p.writeByte(remhppercentage);
      return p;
   }

   public static Packet catchMonster(int mobOid, byte success) {   // updated packet structure found thanks to Rien dev team
      final OutPacket p = OutPacket.create(SendOpcode.CATCH_MONSTER);
      p.writeInt(mobOid);
      p.writeByte(success);
      return p;
   }

   public static Packet catchMonster(int mobOid, int itemid, byte success) {
      final OutPacket p = OutPacket.create(SendOpcode.CATCH_MONSTER_WITH_ITEM);
      p.writeInt(mobOid);
      p.writeInt(itemid);
      p.writeByte(success);
      return p;
   }

   /**
    * Gets a response to a move monster packet.
    *
    * @param objectid  The ObjectID of the monster being moved.
    * @param moveid    The movement ID.
    * @param currentMp The current MP of the monster.
    * @param useSkills Can the monster use skills?
    * @return The move response packet.
    */
   public static Packet moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills) {
      return moveMonsterResponse(objectid, moveid, currentMp, useSkills, 0, 0);
   }

   private static void writeIntMask(OutPacket p, Map<MonsterStatus, Integer> stats) {
      int firstmask = 0;
      int secondmask = 0;
      for (MonsterStatus stat : stats.keySet()) {
         if (stat.isFirst()) {
            firstmask |= stat.getValue();
         } else {
            secondmask |= stat.getValue();
         }
      }
      p.writeInt(firstmask);
      p.writeInt(secondmask);
   }

   public static Packet damageMonster(int oid, int damage) {
      return damageMonster(oid, damage, 0, 0);
   }

   public static Packet healMonster(int oid, int heal, int curhp, int maxhp) {
      return damageMonster(oid, -heal, curhp, maxhp);
   }
}
