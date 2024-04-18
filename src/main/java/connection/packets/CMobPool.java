package connection.packets;

import java.util.Collection;
import java.util.Map;

import client.Skill;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import server.life.MapleMonster;
import server.life.MobSkill;

public class CMobPool {
   /**
    * Internal function to handler monster spawning and controlling.
    *
    * @param life              The mob to perform operations with.
    * @param requestController Requesting control of mob?
    * @param newSpawn          New spawn (fade in?)
    * @param aggro             Aggressive mob?
    * @param effect            The spawn effect to use.
    * @return The spawn/control packet.
    */
   public static Packet spawnMonsterInternal(MapleMonster life, boolean requestController, boolean newSpawn, boolean aggro,
                                             int effect, boolean makeInvis) {
      final OutPacket p;
      if (makeInvis) {
         p = OutPacket.create(SendOpcode.SPAWN_MONSTER_CONTROL);
         p.writeByte(0);
         p.writeInt(life.getObjectId());
         return p;
      }
      if (requestController) {
         p = OutPacket.create(SendOpcode.SPAWN_MONSTER_CONTROL);
         p.writeByte(aggro ? 2 : 1);
      } else {
         p = OutPacket.create(SendOpcode.SPAWN_MONSTER);
      }
      p.writeInt(life.getObjectId());
      p.writeByte(life.getController()
            .isEmpty() ? 5 : 1);
      p.writeInt(life.getId());

      if (requestController) {
         encodeTemporary(p, life.getStati());    // thanks shot for noticing encode temporary buffs missing
      } else {
         p.skip(16);
      }

      p.writePos(life.getPosition());
      p.writeByte(life.getStance());
      p.writeShort(0); //Origin FH //life.getStartFh()
      p.writeShort(life.getFh());

      /**
       * -4: Fake -3: Appear after linked mob is dead -2: Fade in 1: Smoke 3:
       * King Slime spawn 4: Summoning rock thing, used for 3rd job? 6:
       * Magical shit 7: Smoke shit 8: 'The Boss' 9/10: Grim phantom shit?
       * 11/12: Nothing? 13: Frankenstein 14: Angry ^ 15: Orb animation thing,
       * ?? 16: ?? 19: Mushroom castle boss thing
       */

      if (life.getParentMobOid() != 0) {
         MapleMonster parentMob = life.getMap()
               .getMonsterByOid(life.getParentMobOid())
               .orElse(null);
         if (parentMob != null && parentMob.isAlive()) {
            p.writeByte(effect != 0 ? effect : -3);
            p.writeInt(life.getParentMobOid());
         } else {
            encodeParentlessMobSpawnEffect(p, newSpawn, effect);
         }
      } else {
         encodeParentlessMobSpawnEffect(p, newSpawn, effect);
      }

      p.writeByte(life.getTeam());
      p.writeInt(0); // getItemEffect
      return p;
   }

   /**
    * Makes a monster previously spawned as non-targettable, targettable.
    *
    * @param life The mob to make targettable.
    * @return The packet to make the mob targettable.
    */
   public static Packet makeMonsterReal(MapleMonster life) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_MONSTER);
      p.writeInt(life.getObjectId());
      p.writeByte(5);
      p.writeInt(life.getId());
      encodeTemporary(p, life.getStati());
      p.writePos(life.getPosition());
      p.writeByte(life.getStance());
      p.writeShort(0);//life.getStartFh()
      p.writeShort(life.getFh());
      p.writeShort(-1);
      p.writeInt(0);
      return p;
   }

   /**
    * Gets a packet telling the client that a monster was killed.
    *
    * @param oid       The objectID of the killed monster.
    * @param animation 0 = dissapear, 1 = fade out, 2+ = special
    * @return The kill monster packet.
    */
   public static Packet killMonster(int oid, int animation) {
      final OutPacket p = OutPacket.create(SendOpcode.KILL_MONSTER);
      p.writeInt(oid);
      p.writeByte(animation);
      p.writeByte(animation);
      return p;
   }

   /**
    * Removes a monster invisibility.
    *
    * @param life
    * @return
    */
   public static Packet removeMonsterInvisibility(MapleMonster life) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_MONSTER_CONTROL);
      p.writeByte(1);
      p.writeInt(life.getObjectId());
      return p;
   }

   /**
    * Handles monsters not being targettable, such as Zakum's first body.
    *
    * @param life   The mob to spawn as non-targettable.
    * @param effect The effect to show when spawning.
    * @return The packet to spawn the mob as non-targettable.
    */
   public static Packet spawnFakeMonster(MapleMonster life, int effect) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_MONSTER_CONTROL);
      p.writeByte(1);
      p.writeInt(life.getObjectId());
      p.writeByte(5);
      p.writeInt(life.getId());
      encodeTemporary(p, life.getStati());
      p.writePos(life.getPosition());
      p.writeByte(life.getStance());
      p.writeShort(0);//life.getStartFh()
      p.writeShort(life.getFh());
      if (effect > 0) {
         p.writeByte(effect);
         p.writeByte(0);
         p.writeShort(0);
      }
      p.writeShort(-2);
      p.writeByte(life.getTeam());
      p.writeInt(0);
      return p;
   }

   /**
    * Gets a stop control monster packet.
    *
    * @param oid The ObjectID of the monster to stop controlling.
    * @return The stop control monster packet.
    */
   public static Packet stopControllingMonster(int oid) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_MONSTER_CONTROL);
      p.writeByte(0);
      p.writeInt(oid);
      return p;
   }

   /**
    * Gets a spawn monster packet.
    *
    * @param life     The monster to spawn.
    * @param newSpawn Is it a new spawn?
    * @return The spawn monster packet.
    */
   public static Packet spawnMonster(MapleMonster life, boolean newSpawn) {
      return spawnMonsterInternal(life, false, newSpawn, false, 0, false);
   }

   /**
    * Gets a spawn monster packet.
    *
    * @param life     The monster to spawn.
    * @param newSpawn Is it a new spawn?
    * @param effect   The spawn effect.
    * @return The spawn monster packet.
    */
   public static Packet spawnMonster(MapleMonster life, boolean newSpawn, int effect) {
      return spawnMonsterInternal(life, false, newSpawn, false, effect, false);
   }

   /**
    * Gets a control monster packet.
    *
    * @param life     The monster to give control to.
    * @param newSpawn Is it a new spawn?
    * @param aggro    Aggressive monster?
    * @return The monster control packet.
    */
   public static Packet controlMonster(MapleMonster life, boolean newSpawn, boolean aggro) {
      return spawnMonsterInternal(life, true, newSpawn, aggro, 0, false);
   }

   /**
    * Makes a monster invisible for Ariant PQ.
    *
    * @param life
    * @return
    */
   public static Packet makeMonsterInvisible(MapleMonster life) {
      return spawnMonsterInternal(life, true, false, false, 0, true);
   }

   private static void encodeParentlessMobSpawnEffect(OutPacket p, boolean newSpawn, int effect) {
      if (effect > 0) {
         p.writeByte(effect);
         p.writeByte(0);
         p.writeShort(0);
         if (effect == 15) {
            p.writeByte(0);
         }
      }
      p.writeByte(newSpawn ? -2 : -1);
   }

   public static Packet killMonster(int oid, boolean animation) {
      return killMonster(oid, animation ? 1 : 0);
   }

   private static void encodeTemporary(OutPacket p, Map<MonsterStatus, MonsterStatusEffect> stati) {
      int pCounter = -1, mCounter = -1;

      writeLongEncodeTemporaryMask(p, stati.keySet());    // packet structure mapped thanks to Eric

      for (Map.Entry<MonsterStatus, MonsterStatusEffect> s : stati.entrySet()) {
         MonsterStatusEffect mse = s.getValue();
         p.writeShort(mse.getStati()
               .get(s.getKey()));

         MobSkill mobSkill = mse.getMobSkill();
         if (mobSkill != null) {
            p.writeShort(mobSkill.getSkillId());
            p.writeShort(mobSkill.getSkillLevel());

            switch (s.getKey()) {
               case WEAPON_REFLECT:
                  pCounter = mobSkill.getX();
                  break;

               case MAGIC_REFLECT:
                  mCounter = mobSkill.getY();
                  break;
            }
         } else {
            p.writeInt(mse.getSkill()
                  .map(Skill::id)
                  .orElse(0));
         }

         p.writeShort(-1);    // duration
      }

      // reflect packet structure found thanks to Arnah (Vertisy)
      if (pCounter != -1) {
         p.writeInt(pCounter);// wPCounter_
      }
      if (mCounter != -1) {
         p.writeInt(mCounter);// wMCounter_
      }
      if (pCounter != -1 || mCounter != -1) {
         p.writeInt(100);// nCounterProb_
      }
   }

   private static void writeLongEncodeTemporaryMask(OutPacket p, Collection<MonsterStatus> stati) {
      int[] masks = new int[4];

      for (MonsterStatus statup : stati) {
         int pos = statup.isFirst() ? 0 : 2;
         for (int i = 0; i < 2; i++) {
            masks[pos + i] |= statup.getValue() >> 32 * i;
         }
      }

      for (int mask : masks) {
         p.writeInt(mask);
      }
   }
}
