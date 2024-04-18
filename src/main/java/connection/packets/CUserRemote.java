package connection.packets;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.MapleMount;
import client.TemporaryStatType;
import client.TemporaryStatValue;
import connection.constants.SendOpcode;
import constants.skills.Buccaneer;
import constants.skills.Corsair;
import constants.skills.ThunderBreaker;
import net.packet.OutPacket;
import net.packet.Packet;
import net.server.guild.MapleGuild;
import server.life.MobSkill;
import server.movement.MovePath;
import tools.Pair;

public class CUserRemote {
   public static Packet movePlayer(int cid, MovePath moves) {
      final OutPacket p = OutPacket.create(SendOpcode.MOVE_PLAYER);
      p.writeInt(cid);
      moves.encode(p);
      return p;
   }

   public static Packet closeRangeAttack(MapleCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage,
                                         Map<Integer, List<Integer>> damage, int speed, int direction, int display) {
      final OutPacket p = OutPacket.create(SendOpcode.CLOSE_RANGE_ATTACK);
      addAttackBody(p, chr, skill, skilllevel, stance, numAttackedAndDamage, 0, damage, speed, direction, display);
      return p;
   }

   public static Packet rangedAttack(MapleCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage,
                                     int projectile, Map<Integer, List<Integer>> damage, int speed, int direction, int display) {
      final OutPacket p = OutPacket.create(SendOpcode.RANGED_ATTACK);
      addAttackBody(p, chr, skill, skilllevel, stance, numAttackedAndDamage, projectile, damage, speed, direction, display);
      p.writeInt(0);
      return p;
   }

   public static Packet magicAttack(MapleCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage,
                                    Map<Integer, List<Integer>> damage, int charge, int speed, int direction, int display) {
      final OutPacket p = OutPacket.create(SendOpcode.MAGIC_ATTACK);
      addAttackBody(p, chr, skill, skilllevel, stance, numAttackedAndDamage, 0, damage, speed, direction, display);
      if (charge != -1) {
         p.writeInt(charge);
      }
      return p;
   }

   public static Packet skillEffect(MapleCharacter from, int skillId, int level, byte flags, int speed, byte direction) {
      final OutPacket p = OutPacket.create(SendOpcode.SKILL_EFFECT);
      p.writeInt(from.getId());
      p.writeInt(skillId);
      p.writeByte(level);
      p.writeByte(flags);
      p.writeByte(speed);
      p.writeByte(direction); //Mmmk
      return p;
   }

   public static Packet skillCancel(MapleCharacter from, int skillId) {
      final OutPacket p = OutPacket.create(SendOpcode.CANCEL_SKILL_EFFECT);
      p.writeInt(from.getId());
      p.writeInt(skillId);
      return p;
   }

   public static Packet damagePlayer(int skill, int monsteridfrom, int cid, int damage, int fake, int direction, boolean pgmr,
                                     int pgmr_1, boolean is_pg, int oid, int pos_x, int pos_y) {
      final OutPacket p = OutPacket.create(SendOpcode.DAMAGE_PLAYER);
      p.writeInt(cid);
      p.writeByte(skill);
      p.writeInt(damage);
      if (skill != -4) {
         p.writeInt(monsteridfrom);
         p.writeByte(direction);
         if (pgmr) {
            p.writeByte(pgmr_1);
            p.writeByte(is_pg ? 1 : 0);
            p.writeInt(oid);
            p.writeByte(6);
            p.writeShort(pos_x);
            p.writeShort(pos_y);
            p.writeByte(0);
         } else {
            p.writeShort(0);
         }
         p.writeInt(damage);
         if (fake > 0) {
            p.writeInt(fake);
         }
      } else {
         p.writeInt(damage);
      }

      return p;
   }

   public static Packet facialExpression(MapleCharacter from, int expression) {
      final OutPacket p = OutPacket.create(SendOpcode.FACIAL_EXPRESSION);
      p.writeInt(from.getId());
      p.writeInt(expression);
      p.writeInt(10000);
      return p;
   }

   public static Packet itemEffect(int characterid, int itemid) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_ITEM_EFFECT);
      p.writeInt(characterid);
      p.writeInt(itemid);
      return p;
   }

   public static Packet showChair(int characterid, int itemid) {
      final OutPacket p = OutPacket.create(SendOpcode.SHOW_CHAIR);
      p.writeInt(characterid);
      p.writeInt(itemid);
      return p;
   }

   public static Packet updateCharLook(MapleClient target, MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_CHAR_LOOK);
      p.writeInt(chr.getId());
      p.writeByte(1);
      CCommon.addCharLook(p, chr, false);
      CCommon.addRingLook(p, chr, true);
      CCommon.addRingLook(p, chr, false);
      CCommon.addMarriageRingLook(target, p, chr);
      p.writeInt(0);
      return p;
   }

   public static Packet showMonsterRiding(int cid, MapleMount mount) { //Gtfo with this, this is just giveForeignBuff
      final OutPacket p = OutPacket.create(SendOpcode.GIVE_FOREIGN_BUFF);
      p.writeInt(cid);
      CCommon.writeLongMaskFromList(p, Collections.singletonList(TemporaryStatType.MONSTER_RIDING));
      p.writeShort(0);
      p.writeInt(mount.getItemId());
      p.writeInt(mount.getSkillId());
      p.writeInt(0); //Server Tick value.
      p.writeShort(0);
      p.writeByte(0); //Times you have been buffed
      return p;
   }

   public static Packet giveForeignDebuff(int cid, List<Pair<MapleDisease, Integer>> statups, MobSkill skill) {
      // Poison damage visibility and missing diseases status visibility, extended through map transitions thanks to Ronan

      final OutPacket p = OutPacket.create(SendOpcode.GIVE_FOREIGN_BUFF);
      p.writeInt(cid);
      CCommon.writeLongMaskD(p, statups);
      for (Pair<MapleDisease, Integer> statup : statups) {
         if (statup.getLeft() == MapleDisease.POISON) {
            p.writeShort(statup.getRight().shortValue());
         }
         p.writeShort(skill.getSkillId());
         p.writeShort(skill.getSkillLevel());
      }
      p.writeShort(0); // same as give_buff
      p.writeShort(900);//Delay
      return p;
   }

   public static Packet giveForeignBuff(MapleCharacter character, List<Pair<TemporaryStatType, TemporaryStatValue>> statups) {
      final OutPacket p = OutPacket.create(SendOpcode.GIVE_FOREIGN_BUFF);
      p.writeInt(character.getId());
      CCommon.writeLongMask(p, statups);
      for (Pair<TemporaryStatType, TemporaryStatValue> statup : statups) {
         if (statup.getLeft().isDisease()) {
            if (statup.getLeft() == TemporaryStatType.POISON) {
               p.writeShort(statup.getRight().value());
            }
            p.writeShort(statup.getRight().sourceId());
            p.writeShort(statup.getRight().sourceLevel());
         } else {
            p.writeInt(statup.getRight().value());
         }
      }
      p.writeByte(0);
      p.writeByte(0);
      CCommon.getTemporaryStats(character).forEach(ts -> ts.EncodeForClient(p));
      p.writeShort(0);// tDelay
      return p;
   }

   public static Packet giveForeignSlowDebuff(int cid, List<Pair<MapleDisease, Integer>> statups, MobSkill skill) {
      final OutPacket p = OutPacket.create(SendOpcode.GIVE_FOREIGN_BUFF);
      p.writeInt(cid);
      writeLongMaskSlowD(p);
      for (Pair<MapleDisease, Integer> statup : statups) {
         if (statup.getLeft() == MapleDisease.POISON) {
            p.writeShort(statup.getRight().shortValue());
         }
         p.writeShort(skill.getSkillId());
         p.writeShort(skill.getSkillLevel());
      }
      p.writeShort(0); // same as give_buff
      p.writeShort(900);//Delay
      return p;
   }

   public static Packet giveForeignChairSkillEffect(int cid) {
      final OutPacket p = OutPacket.create(SendOpcode.GIVE_FOREIGN_BUFF);
      p.writeInt(cid);
      writeLongMaskChair(p);

      p.writeShort(0);
      p.writeShort(0);
      p.writeShort(100);
      p.writeShort(1);

      p.writeShort(0);
      p.writeShort(900);

      p.skip(7);

      return p;
   }

   // packet found thanks to Ronan
   public static Packet giveForeignWKChargeEffect(int cid, int buffid, List<Pair<TemporaryStatType, TemporaryStatValue>> statups) {
      final OutPacket p = OutPacket.create(SendOpcode.GIVE_FOREIGN_BUFF);
      p.writeInt(cid);
      CCommon.writeLongMask(p, statups);
      p.writeInt(buffid);
      p.writeShort(600);
      p.writeShort(1000);//Delay
      p.writeByte(1);

      return p;
   }

   public static Packet giveForeignPirateBuff(int cid, int buffid, int time,
                                              List<Pair<TemporaryStatType, TemporaryStatValue>> statups) {
      boolean infusion =
            buffid == Buccaneer.SPEED_INFUSION || buffid == ThunderBreaker.SPEED_INFUSION || buffid == Corsair.SPEED_INFUSION;
      final OutPacket p = OutPacket.create(SendOpcode.GIVE_FOREIGN_BUFF);
      p.writeInt(cid);
      CCommon.writeLongMask(p, statups);
      p.writeShort(0);
      for (Pair<TemporaryStatType, TemporaryStatValue> statup : statups) {
         p.writeInt(statup.getRight().value());
         p.writeInt(buffid);
         p.skip(infusion ? 10 : 5);
         p.writeShort(time);
      }
      p.writeShort(0);
      p.writeByte(2);
      return p;
   }

   public static Packet cancelForeignFirstDebuff(int cid, long mask) {
      final OutPacket p = OutPacket.create(SendOpcode.CANCEL_FOREIGN_BUFF);
      p.writeInt(cid);
      p.writeLong(mask);
      p.writeLong(0);
      return p;
   }

   public static Packet cancelForeignDebuff(int cid, long mask) {
      final OutPacket p = OutPacket.create(SendOpcode.CANCEL_FOREIGN_BUFF);
      p.writeInt(cid);
      p.writeLong(0);
      p.writeLong(mask);
      return p;
   }

   public static Packet cancelForeignBuff(int cid, List<TemporaryStatType> statups) {
      final OutPacket p = OutPacket.create(SendOpcode.CANCEL_FOREIGN_BUFF);
      p.writeInt(cid);
      CCommon.writeLongMaskFromList(p, statups);
      return p;
   }

   public static Packet cancelForeignSlowDebuff(int cid) {
      final OutPacket p = OutPacket.create(SendOpcode.CANCEL_FOREIGN_BUFF);
      p.writeInt(cid);
      writeLongMaskSlowD(p);
      return p;
   }

   public static Packet cancelForeignChairSkillEffect(int cid) {
      final OutPacket p = OutPacket.create(SendOpcode.CANCEL_FOREIGN_BUFF);
      p.writeInt(cid);
      writeLongMaskChair(p);

      return p;
   }

   public static Packet updatePartyMemberHP(int cid, int curhp, int maxhp) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_PARTYMEMBER_HP);
      p.writeInt(cid);
      p.writeInt(curhp);
      p.writeInt(maxhp);
      return p;
   }

   /**
    * Guild Name & Mark update packet, thanks to Arnah (Vertisy)
    *
    * @param guildName The Guild name, blank for nothing.
    */
   public static Packet guildNameChanged(int chrid, String guildName) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_NAME_CHANGED);
      p.writeInt(chrid);
      p.writeString(guildName);
      return p;
   }

   public static Packet guildMarkChanged(int chrid, MapleGuild guild) {
      final OutPacket p = OutPacket.create(SendOpcode.GUILD_MARK_CHANGED);
      p.writeInt(chrid);
      p.writeShort(guild.getLogoBG());
      p.writeByte(guild.getLogoBGColor());
      p.writeShort(guild.getLogo());
      p.writeByte(guild.getLogoColor());
      return p;
   }

   public static Packet throwGrenade(int cid, Point pt, int keyDown, int skillId,
                                     int skillLevel) { // packets found thanks to GabrielSin
      final OutPacket p = OutPacket.create(SendOpcode.THROW_GRENADE);
      p.writeInt(cid);
      p.writeInt(pt.x);
      p.writeInt(pt.y);
      p.writeInt(keyDown);
      p.writeInt(skillId);
      p.writeInt(skillLevel);
      return p;
   }

   public static Packet cancelChair(int id) {
      final OutPacket p = OutPacket.create(SendOpcode.CANCEL_CHAIR);
      if (id < 0) {
         p.writeByte(0);
      } else {
         p.writeByte(1);
         p.writeShort(id);
      }
      return p;
   }

   private static void addAttackBody(OutPacket p, MapleCharacter chr, int skill, int skilllevel, int stance,
                                     int numAttackedAndDamage, int projectile, Map<Integer, List<Integer>> damage, int speed,
                                     int direction, int display) {
      p.writeInt(chr.getId());
      p.writeByte(numAttackedAndDamage);
      p.writeByte(0x5B);//?
      p.writeByte(skilllevel);
      if (skilllevel > 0) {
         p.writeInt(skill);
      }
      p.writeByte(display);
      p.writeByte(direction);
      p.writeByte(stance);
      p.writeByte(speed);
      p.writeByte(0x0A);
      p.writeInt(projectile);
      for (Integer oned : damage.keySet()) {
         List<Integer> onedList = damage.get(oned);
         if (onedList != null) {
            p.writeInt(oned);
            p.writeByte(0x0);
            if (skill == 4211006) {
               p.writeByte(onedList.size());
            }
            for (Integer eachd : onedList) {
               p.writeInt(eachd);
            }
         }
      }
   }

   private static void writeLongMaskSlowD(OutPacket p) {
      p.writeInt(0);
      p.writeInt(2048);
      p.writeLong(0);
   }

   private static void writeLongMaskChair(OutPacket p) {
      p.writeInt(0);
      p.writeInt(262144);
      p.writeLong(0);
   }
}
