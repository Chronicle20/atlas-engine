/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import client.TemporaryStatValue;
import client.TemporaryStatType;
import client.MapleCharacter;
import client.MapleDisease;
import client.MapleJob;
import client.MapleMount;
import client.Skill;
import client.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import config.YamlConfig;
import connection.constants.ShowItemGainInChatCode;
import connection.packets.CUser;
import connection.packets.CUserRemote;
import connection.packets.CWvsContext;
import constants.inventory.ItemConstants;
import constants.skills.Aran;
import constants.skills.Assassin;
import constants.skills.Bandit;
import constants.skills.Beginner;
import constants.skills.Bishop;
import constants.skills.BlazeWizard;
import constants.skills.Bowmaster;
import constants.skills.Brawler;
import constants.skills.Buccaneer;
import constants.skills.ChiefBandit;
import constants.skills.Cleric;
import constants.skills.Corsair;
import constants.skills.Crossbowman;
import constants.skills.Crusader;
import constants.skills.DarkKnight;
import constants.skills.DawnWarrior;
import constants.skills.DragonKnight;
import constants.skills.Evan;
import constants.skills.FPArchMage;
import constants.skills.FPMage;
import constants.skills.FPWizard;
import constants.skills.Fighter;
import constants.skills.GM;
import constants.skills.Gunslinger;
import constants.skills.Hermit;
import constants.skills.Hero;
import constants.skills.Hunter;
import constants.skills.ILArchMage;
import constants.skills.ILMage;
import constants.skills.ILWizard;
import constants.skills.Legend;
import constants.skills.Magician;
import constants.skills.Marauder;
import constants.skills.Marksman;
import constants.skills.NightLord;
import constants.skills.NightWalker;
import constants.skills.Noblesse;
import constants.skills.Outlaw;
import constants.skills.Page;
import constants.skills.Paladin;
import constants.skills.Pirate;
import constants.skills.Priest;
import constants.skills.Ranger;
import constants.skills.Rogue;
import constants.skills.Shadower;
import constants.skills.Sniper;
import constants.skills.Spearman;
import constants.skills.SuperGM;
import constants.skills.ThunderBreaker;
import constants.skills.WhiteKnight;
import constants.skills.WindArcher;
import door.DoorProcessor;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import provider.MapleData;
import provider.MapleDataTool;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.FieldLimit;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleMist;
import server.maps.MaplePortal;
import server.maps.MapleSummon;
import server.maps.SummonMovementType;
import server.partyquest.MapleCarnivalFactory;
import server.partyquest.MapleCarnivalFactory.MCSkill;
import tools.ArrayMap;
import tools.Pair;

/**
 * @author Matze
 * @author Frz
 * @author Ronan
 */
public class MapleStatEffect {

   private short watk, matk, wdef, mdef, acc, avoid, speed, jump;
   private short hp, mp;
   private double hpR, mpR;
   private short mhpRRate, mmpRRate, mobSkill, mobSkillLevel;
   private byte mhpR, mmpR;
   private short mpCon, hpCon;
   private int duration, target, barrier, mob;
   private boolean overTime, repeatEffect;
   private int sourceid;
   private int sourceLevel;
   private Integer skillLevel;
   private int moveTo;
   private int cp, nuffSkill;
   private List<MapleDisease> cureDebuffs;
   private boolean skill;
   private List<Pair<TemporaryStatType, TemporaryStatValue>> statups;
   private Map<MonsterStatus, Integer> monsterStatus;
   private int x, y, mobCount, moneyCon, cooldown, morphId = 0, ghost, fatigue, berserk, booster;
   private double prop;
   private int itemCon, itemConNo;
   private int damage, attackCount, fixdamage;
   private Point lt, rb;
   private short bulletCount, bulletConsume;
   private byte mapProtection;
   private CardItemupStats cardStats;

   public static MapleStatEffect loadSkillEffectFromData(MapleData source, int skillid, boolean overtime) {
      return loadFromData(source, skillid, true, overtime);
   }

   public static MapleStatEffect loadItemEffectFromData(MapleData source, int itemid) {
      return loadFromData(source, itemid, false, false);
   }

   private static byte mapProtection(int sourceid) {
      if (sourceid == 2022001 || sourceid == 2022186) {
         return 1;   //elnath cold
      } else if (sourceid == 2022040) {
         return 2;   //aqua road underwater
      } else {
         return 0;
      }
   }

   private static MapleStatEffect loadFromData(MapleData source, int sourceid, boolean skill, boolean overTime) {
      MapleStatEffect ret = new MapleStatEffect();
      ret.duration = MapleDataTool.getIntConvert("time", source, -1);
      ret.hp = (short) MapleDataTool.getInt("hp", source, 0);
      ret.hpR = MapleDataTool.getInt("hpR", source, 0) / 100.0;
      ret.mp = (short) MapleDataTool.getInt("mp", source, 0);
      ret.mpR = MapleDataTool.getInt("mpR", source, 0) / 100.0;
      ret.mpCon = (short) MapleDataTool.getInt("mpCon", source, 0);
      ret.hpCon = (short) MapleDataTool.getInt("hpCon", source, 0);
      int iprop = MapleDataTool.getInt("prop", source, 100);
      ret.prop = iprop / 100.0;

      ret.cp = MapleDataTool.getInt("cp", source, 0);
      List<MapleDisease> cure = new ArrayList<>(5);
      if (MapleDataTool.getInt("poison", source, 0) > 0) {
         cure.add(MapleDisease.POISON);
      }
      if (MapleDataTool.getInt("seal", source, 0) > 0) {
         cure.add(MapleDisease.SEAL);
      }
      if (MapleDataTool.getInt("darkness", source, 0) > 0) {
         cure.add(MapleDisease.DARKNESS);
      }
      if (MapleDataTool.getInt("weakness", source, 0) > 0) {
         cure.add(MapleDisease.WEAKEN);
         cure.add(MapleDisease.SLOW);
      }
      if (MapleDataTool.getInt("curse", source, 0) > 0) {
         cure.add(MapleDisease.CURSE);
      }
      ret.cureDebuffs = cure;
      ret.nuffSkill = MapleDataTool.getInt("nuffSkill", source, 0);

      ret.mobCount = MapleDataTool.getInt("mobCount", source, 1);
      ret.cooldown = MapleDataTool.getInt("cooltime", source, 0);
      ret.morphId = MapleDataTool.getInt("morph", source, 0);
      ret.ghost = MapleDataTool.getInt("ghost", source, 0);
      ret.fatigue = MapleDataTool.getInt("incFatigue", source, 0);
      ret.repeatEffect = MapleDataTool.getInt("repeatEffect", source, 0) > 0;

      MapleData mdd = source.getChildByPath("0");
      if (mdd != null && mdd.getChildren().size() > 0) {
         ret.mobSkill = (short) MapleDataTool.getInt("mobSkill", mdd, 0);
         ret.mobSkillLevel = (short) MapleDataTool.getInt("level", mdd, 0);
         ret.target = MapleDataTool.getInt("target", mdd, 0);
      } else {
         ret.mobSkill = 0;
         ret.mobSkillLevel = 0;
         ret.target = 0;
      }

      MapleData mdds = source.getChildByPath("mob");
      if (mdds != null) {
         if (mdds.getChildren() != null && mdds.getChildren().size() > 0) {
            ret.mob = MapleDataTool.getInt("mob", mdds, 0);
         }
      }
      ret.sourceid = sourceid;
      try {
      ret.skillLevel = Integer.parseInt(source.getName());
      } catch (NumberFormatException e) {
         ret.skillLevel = 0;
      }
      ret.skill = skill;
      if (!ret.skill && ret.duration > -1) {
         ret.overTime = true;
      } else {
         ret.duration *= 1000; // items have their times stored in ms, of course
         ret.overTime = overTime;
      }

      ArrayList<Pair<TemporaryStatType, TemporaryStatValue>> statups = new ArrayList<>();
      ret.watk = (short) MapleDataTool.getInt("pad", source, 0);
      ret.wdef = (short) MapleDataTool.getInt("pdd", source, 0);
      ret.matk = (short) MapleDataTool.getInt("mad", source, 0);
      ret.mdef = (short) MapleDataTool.getInt("mdd", source, 0);
      ret.acc = (short) MapleDataTool.getIntConvert("acc", source, 0);
      ret.avoid = (short) MapleDataTool.getInt("eva", source, 0);

      ret.speed = (short) MapleDataTool.getInt("speed", source, 0);
      ret.jump = (short) MapleDataTool.getInt("jump", source, 0);

      //      ret.barrier = MapleDataTool.getInt("barrier", source, 0);
      //      ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.AURA, ret.barrier);

      //      ret.mapProtection = mapProtection(sourceid);
      //      ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.MAP_PROTECTION, (int) ret.mapProtection);

      if (ret.overTime && ret.getSummonMovementType() == null) {
         if (!skill) {
            if (isPyramidBuff(sourceid)) {
               ret.berserk = MapleDataTool.getInt("berserk", source, 0);
               ret.booster = MapleDataTool.getInt("booster", source, 0);

               //               ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.BERSERK, ret.berserk);
               ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.BOOSTER, ret.booster);
            } else if (isDojoBuff(sourceid) || isHpMpRecovery(sourceid)) {
               ret.mhpR = (byte) MapleDataTool.getInt("mhpR", source, 0);
               ret.mhpRRate = (short) (MapleDataTool.getInt("mhpRRate", source, 0) * 100);
               ret.mmpR = (byte) MapleDataTool.getInt("mmpR", source, 0);
               ret.mmpRRate = (short) (MapleDataTool.getInt("mmpRRate", source, 0) * 100);

               //               ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.HPREC, (int) ret.mhpR);
               //               ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.MPREC, (int) ret.mmpR);
            } else if (isRateCoupon(sourceid)) {
               //               switch (MapleDataTool.getInt("expR", source, 0)) {
               //                  case 1:
               //                     ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.COUPON_EXP1, 1);
               //                     break;
               //
               //                  case 2:
               //                     ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.COUPON_EXP2, 1);
               //                     break;
               //
               //                  case 3:
               //                     ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.COUPON_EXP3, 1);
               //                     break;
               //
               //                  case 4:
               //                     ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.COUPON_EXP4, 1);
               //                     break;
               //               }
               //
               //               switch (MapleDataTool.getInt("drpR", source, 0)) {
               //                  case 1:
               //                     ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.COUPON_DRP1, 1);
               //                     break;
               //
               //                  case 2:
               //                     ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.COUPON_DRP2, 1);
               //                     break;
               //
               //                  case 3:
               //                     ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.COUPON_DRP3, 1);
               //                     break;
               //               }
            } else if (isMonsterCard(sourceid)) {
               int prob = 0, itemupCode = Integer.MAX_VALUE;
               List<Pair<Integer, Integer>> areas = null;
               boolean inParty = false;

               MapleData con = source.getChildByPath("con");
               if (con != null) {
                  areas = new ArrayList<>(3);

                  for (MapleData conData : con.getChildren()) {
                     int type = MapleDataTool.getInt("type", conData, -1);

                     if (type == 0) {
                        int startMap = MapleDataTool.getInt("sMap", conData, 0);
                        int endMap = MapleDataTool.getInt("eMap", conData, 0);

                        areas.add(new Pair<>(startMap, endMap));
                     } else if (type == 2) {
                        inParty = true;
                     }
                  }

                  if (areas.isEmpty()) {
                     areas = null;
                  }
               }

               if (MapleDataTool.getInt("mesoupbyitem", source, 0) != 0) {
                  ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.MESO_UP_BY_ITEM, 4);
                  prob = MapleDataTool.getInt("prob", source, 1);
               }

               int itemupType = MapleDataTool.getInt("itemupbyitem", source, 0);
               if (itemupType != 0) {
                  ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.ITEM_UP_BY_ITEM, 4);
                  prob = MapleDataTool.getInt("prob", source, 1);

                  switch (itemupType) {
                     case 2:
                        itemupCode = MapleDataTool.getInt("itemCode", source, 1);
                        break;

                     case 3:
                        itemupCode = MapleDataTool.getInt("itemRange", source, 1);    // 3 digits
                        break;
                  }
               }

               if (MapleDataTool.getInt("respectPimmune", source, 0) != 0) {
                  ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.RESPECT_PIMMUNE, 4);
               }

               if (MapleDataTool.getInt("respectMimmune", source, 0) != 0) {
                  ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.RESPECT_MIMMUNE, 4);
               }

               if (MapleDataTool.getString("defenseAtt", source, null) != null) {
                  ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.DEFENSE_ATT, 4);
               }

               if (MapleDataTool.getString("defenseState", source, null) != null) {
                  ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.DEFENSE_STATE, 4);
               }

               //               int thaw = MapleDataTool.getInt("thaw", source, 0);
               //               if (thaw != 0) {
               //                  ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.MAP_PROTECTION, thaw > 0 ? 1 : 2);
               //               }

               ret.cardStats = new CardItemupStats(itemupCode, prob, areas, inParty);
            } else if (isExpIncrease(sourceid)) {
               //               ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.EXP_INCREASE, MapleDataTool.getInt("expinc", source, 0));
            }
         } else {
            if (isMapChair(sourceid)) {
               //               ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.MAP_CHAIR, 1);
            } else if ((sourceid == Beginner.NIMBLE_FEET || sourceid == Noblesse.NIMBLE_FEET || sourceid == Evan.NIMBLE_FEET
                  || sourceid == Legend.AGILE_BODY) && YamlConfig.config.server.USE_ULTRA_NIMBLE_FEET == true) {
               ret.jump = (short) (ret.speed * 4);
               ret.speed *= 15;
            }
         }

         ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.WATK, (int) ret.watk);
         ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.WDEF, (int) ret.wdef);
         ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.MATK, (int) ret.matk);
         ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.MDEF, (int) ret.mdef);
         ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.ACC, (int) ret.acc);
         ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.AVOID, (int) ret.avoid);
         ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.SPEED, (int) ret.speed);
         ret.addBuffStatPairToListIfNotZero(statups, TemporaryStatType.JUMP, (int) ret.jump);
      }

      MapleData ltd = source.getChildByPath("lt");
      if (ltd != null) {
         ret.lt = (Point) ltd.getData();
         ret.rb = (Point) source.getChildByPath("rb").getData();

         if (YamlConfig.config.server.USE_MAXRANGE_ECHO_OF_HERO && (sourceid == Beginner.ECHO_OF_HERO
               || sourceid == Noblesse.ECHO_OF_HERO || sourceid == Legend.ECHO_OF_HERO || sourceid == Evan.ECHO_OF_HERO)) {
            ret.lt = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
            ret.rb = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
         }
      }

      int x = MapleDataTool.getInt("x", source, 0);

      if ((sourceid == Beginner.RECOVERY || sourceid == Noblesse.RECOVERY || sourceid == Evan.RECOVERY
            || sourceid == Legend.RECOVERY) && YamlConfig.config.server.USE_ULTRA_RECOVERY == true) {
         x *= 10;
      }
      ret.x = x;
      ret.y = MapleDataTool.getInt("y", source, 0);

      ret.damage = MapleDataTool.getIntConvert("damage", source, 100);
      ret.fixdamage = MapleDataTool.getIntConvert("fixdamage", source, -1);
      ret.attackCount = MapleDataTool.getIntConvert("attackCount", source, 1);
      ret.bulletCount = (short) MapleDataTool.getIntConvert("bulletCount", source, 1);
      ret.bulletConsume = (short) MapleDataTool.getIntConvert("bulletConsume", source, 0);
      ret.moneyCon = MapleDataTool.getIntConvert("moneyCon", source, 0);
      ret.itemCon = MapleDataTool.getInt("itemCon", source, 0);
      ret.itemConNo = MapleDataTool.getInt("itemConNo", source, 0);
      ret.moveTo = MapleDataTool.getInt("moveTo", source, -1);
      Map<MonsterStatus, Integer> monsterStatus = new ArrayMap<>();
      if (skill) {
         switch (sourceid) {
            // BEGINNER
            case Beginner.RECOVERY:
            case Noblesse.RECOVERY:
            case Legend.RECOVERY:
            case Evan.RECOVERY:
               statups.add(new Pair<>(TemporaryStatType.RECOVERY, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Beginner.ECHO_OF_HERO:
            case Noblesse.ECHO_OF_HERO:
            case Legend.ECHO_OF_HERO:
            case Evan.ECHO_OF_HERO:
               statups.add(new Pair<>(TemporaryStatType.ECHO_OF_HERO, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Beginner.MONSTER_RIDER:
            case Noblesse.MONSTER_RIDER:
            case Legend.MONSTER_RIDER:
            case Corsair.BATTLE_SHIP:
            case Beginner.SPACESHIP:
            case Noblesse.SPACESHIP:
            case Beginner.YETI_MOUNT1:
            case Beginner.YETI_MOUNT2:
            case Noblesse.YETI_MOUNT1:
            case Noblesse.YETI_MOUNT2:
            case Legend.YETI_MOUNT1:
            case Legend.YETI_MOUNT2:
            case Beginner.WITCH_BROOMSTICK:
            case Noblesse.WITCH_BROOMSTICK:
            case Legend.WITCH_BROOMSTICK:
            case Beginner.BALROG_MOUNT:
            case Noblesse.BALROG_MOUNT:
            case Legend.BALROG_MOUNT:
               statups.add(new Pair<>(TemporaryStatType.MONSTER_RIDING, new TemporaryStatValue(sourceid, ret.skillLevel, sourceid)));
               break;
            case Beginner.INVINCIBLE_BARRIER:
            case Noblesse.INVINCIBLE_BARRIER:
            case Legend.INVICIBLE_BARRIER:
            case Evan.INVINCIBLE_BARRIER:
               statups.add(new Pair<>(TemporaryStatType.DIVINE_BODY, new TemporaryStatValue(sourceid, ret.skillLevel, 1)));
               break;
            case Fighter.POWER_GUARD:
            case Page.POWER_GUARD:
               statups.add(new Pair<>(TemporaryStatType.POWERGUARD, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Spearman.HYPER_BODY:
            case GM.HYPER_BODY:
            case SuperGM.HYPER_BODY:
               statups.add(new Pair<>(TemporaryStatType.HYPERBODYHP, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               statups.add(new Pair<>(TemporaryStatType.HYPERBODYMP, new TemporaryStatValue(sourceid, ret.skillLevel, ret.y)));
               break;
            case Crusader.COMBO:
            case DawnWarrior.COMBO:
               statups.add(new Pair<>(TemporaryStatType.COMBO, new TemporaryStatValue(sourceid, ret.skillLevel, 1)));
               break;
            case WhiteKnight.BW_FIRE_CHARGE:
            case WhiteKnight.BW_ICE_CHARGE:
            case WhiteKnight.BW_LIT_CHARGE:
            case WhiteKnight.SWORD_FIRE_CHARGE:
            case WhiteKnight.SWORD_ICE_CHARGE:
            case WhiteKnight.SWORD_LIT_CHARGE:
            case Paladin.BW_HOLY_CHARGE:
            case Paladin.SWORD_HOLY_CHARGE:
            case DawnWarrior.SOUL_CHARGE:
            case ThunderBreaker.LIGHTNING_CHARGE:
               statups.add(new Pair<>(TemporaryStatType.WK_CHARGE, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case DragonKnight.DRAGON_BLOOD:
               statups.add(new Pair<>(TemporaryStatType.DRAGONBLOOD, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Hero.STANCE:
            case Paladin.STANCE:
            case DarkKnight.STANCE:
            case Aran.FREEZE_STANDING:
               statups.add(new Pair<>(TemporaryStatType.STANCE, new TemporaryStatValue(sourceid, ret.skillLevel, iprop)));
               break;
            case DawnWarrior.FINAL_ATTACK:
               statups.add(new Pair<>(TemporaryStatType.SOUL_MASTER_FINAL, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case WindArcher.FINAL_ATTACK:
               statups.add(new Pair<>(TemporaryStatType.WIND_BREAKER_FINAL, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            // MAGICIAN
            case Magician.MAGIC_GUARD:
            case BlazeWizard.MAGIC_GUARD:
            case Evan.MAGIC_GUARD:
               statups.add(new Pair<>(TemporaryStatType.MAGIC_GUARD, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Cleric.INVINCIBLE:
               statups.add(new Pair<>(TemporaryStatType.INVINCIBLE, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Priest.HOLY_SYMBOL:
            case SuperGM.HOLY_SYMBOL:
               statups.add(new Pair<>(TemporaryStatType.HOLY_SYMBOL, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case FPArchMage.INFINITY:
            case ILArchMage.INFINITY:
            case Bishop.INFINITY:
               statups.add(new Pair<>(TemporaryStatType.INFINITY, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case FPArchMage.MANA_REFLECTION:
            case ILArchMage.MANA_REFLECTION:
            case Bishop.MANA_REFLECTION:
               statups.add(new Pair<>(TemporaryStatType.MANA_REFLECTION, new TemporaryStatValue(sourceid, ret.skillLevel, 1)));
               break;
            case Bishop.HOLY_SHIELD:
               statups.add(new Pair<>(TemporaryStatType.HOLY_SHIELD, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case BlazeWizard.ELEMENTAL_RESET:
            case Evan.ELEMENTAL_RESET:
               statups.add(new Pair<>(TemporaryStatType.ELEMENTAL_RESET, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Evan.MAGIC_SHIELD:
               statups.add(new Pair<>(TemporaryStatType.MAGIC_SHIELD, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Evan.MAGIC_RESISTANCE:
               statups.add(new Pair<>(TemporaryStatType.MAGIC_RESIST, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Evan.SLOW:
               statups.add(new Pair<>(TemporaryStatType.EVAN_SLOW, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               // BOWMAN
            case Priest.MYSTIC_DOOR:
            case Hunter.SOUL_ARROW:
            case Crossbowman.SOUL_ARROW:
            case WindArcher.SOUL_ARROW:
               statups.add(new Pair<>(TemporaryStatType.SOULARROW, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Ranger.PUPPET:
            case Sniper.PUPPET:
            case WindArcher.PUPPET:
            case Outlaw.OCTOPUS:
            case Corsair.WRATH_OF_THE_OCTOPI:
               statups.add(new Pair<>(TemporaryStatType.PUPPET, new TemporaryStatValue(sourceid, ret.skillLevel, 1)));
               break;
            case Bowmaster.CONCENTRATE:
               statups.add(new Pair<>(TemporaryStatType.CONCENTRATE, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Bowmaster.HAMSTRING:
               statups.add(new Pair<>(TemporaryStatType.HAMSTRING, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               monsterStatus.put(MonsterStatus.SPEED, x);
               break;
            case Marksman.BLIND:
               statups.add(new Pair<>(TemporaryStatType.BLIND, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               monsterStatus.put(MonsterStatus.ACC, x);
               break;
            case Bowmaster.SHARP_EYES:
            case Marksman.SHARP_EYES:
               statups.add(new Pair<>(TemporaryStatType.SHARP_EYES, new TemporaryStatValue(sourceid, ret.skillLevel, (ret.x << 8 | ret.y))));
               break;
            case WindArcher.WIND_WALK:
               statups.add(new Pair<>(TemporaryStatType.WIND_WALK, new TemporaryStatValue(sourceid, ret.skillLevel, 1)));
            case Rogue.DARK_SIGHT:
            case NightWalker.DARK_SIGHT:
               statups.add(new Pair<>(TemporaryStatType.DARKSIGHT, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Hermit.MESO_UP:
               statups.add(new Pair<>(TemporaryStatType.MESOUP, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Hermit.SHADOW_PARTNER:
            case NightWalker.SHADOW_PARTNER:
               statups.add(new Pair<>(TemporaryStatType.SHADOWPARTNER, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case ChiefBandit.MESO_GUARD:
               statups.add(new Pair<>(TemporaryStatType.MESOGUARD, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case ChiefBandit.PICKPOCKET:
               statups.add(new Pair<>(TemporaryStatType.PICKPOCKET, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case NightLord.SHADOW_STARS:
               statups.add(new Pair<>(TemporaryStatType.SHADOW_CLAW, new TemporaryStatValue(sourceid, ret.skillLevel, 0)));
               break;
            // PIRATE
            case Pirate.DASH:
            case ThunderBreaker.DASH:
            case Beginner.SPACE_DASH:
            case Noblesse.SPACE_DASH:
               statups.add(new Pair<>(TemporaryStatType.DASH_SPEED, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               statups.add(new Pair<>(TemporaryStatType.DASH_JUMP, new TemporaryStatValue(sourceid, ret.skillLevel, ret.y)));
               break;
            case Corsair.SPEED_INFUSION:
            case Buccaneer.SPEED_INFUSION:
            case ThunderBreaker.SPEED_INFUSION:
               statups.add(new Pair<>(TemporaryStatType.SPEED_INFUSION, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Outlaw.HOMING_BEACON:
            case Corsair.BULLSEYE:
               statups.add(new Pair<>(TemporaryStatType.HOMING_BEACON, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case ThunderBreaker.SPARK:
               statups.add(new Pair<>(TemporaryStatType.SPARK, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            // MULTIPLE
            case Aran.POLEARM_BOOSTER:
            case Fighter.AXE_BOOSTER:
            case Fighter.SWORD_BOOSTER:
            case Page.BW_BOOSTER:
            case Page.SWORD_BOOSTER:
            case Spearman.POLEARM_BOOSTER:
            case Spearman.SPEAR_BOOSTER:
            case Hunter.BOW_BOOSTER:
            case Crossbowman.CROSSBOW_BOOSTER:
            case Assassin.CLAW_BOOSTER:
            case Bandit.DAGGER_BOOSTER:
            case FPMage.SPELL_BOOSTER:
            case ILMage.SPELL_BOOSTER:
            case Brawler.KNUCKLER_BOOSTER:
            case Gunslinger.GUN_BOOSTER:
            case DawnWarrior.SWORD_BOOSTER:
            case BlazeWizard.SPELL_BOOSTER:
            case WindArcher.BOW_BOOSTER:
            case NightWalker.CLAW_BOOSTER:
            case ThunderBreaker.KNUCKLER_BOOSTER:
            case Evan.MAGIC_BOOSTER:
            case Beginner.POWER_EXPLOSION:
            case Noblesse.POWER_EXPLOSION:
            case Legend.POWER_EXPLOSION:
               statups.add(new Pair<>(TemporaryStatType.BOOSTER, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Hero.MAPLE_WARRIOR:
            case Paladin.MAPLE_WARRIOR:
            case DarkKnight.MAPLE_WARRIOR:
            case FPArchMage.MAPLE_WARRIOR:
            case ILArchMage.MAPLE_WARRIOR:
            case Bishop.MAPLE_WARRIOR:
            case Bowmaster.MAPLE_WARRIOR:
            case Marksman.MAPLE_WARRIOR:
            case NightLord.MAPLE_WARRIOR:
            case Shadower.MAPLE_WARRIOR:
            case Corsair.MAPLE_WARRIOR:
            case Buccaneer.MAPLE_WARRIOR:
            case Aran.MAPLE_WARRIOR:
            case Evan.MAPLE_WARRIOR:
               statups.add(new Pair<>(TemporaryStatType.MAPLE_WARRIOR, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            // SUMMON
            case Ranger.SILVER_HAWK:
            case Sniper.GOLDEN_EAGLE:
               statups.add(new Pair<>(TemporaryStatType.SUMMON, new TemporaryStatValue(sourceid, ret.skillLevel, 1)));
               monsterStatus.put(MonsterStatus.STUN, 1);
               break;
            case FPArchMage.ELQUINES:
            case Marksman.FROST_PREY:
               statups.add(new Pair<>(TemporaryStatType.SUMMON, new TemporaryStatValue(sourceid, ret.skillLevel, 1)));
               monsterStatus.put(MonsterStatus.FREEZE, 1);
               break;
            case Priest.SUMMON_DRAGON:
            case Bowmaster.PHOENIX:
            case ILArchMage.IFRIT:
            case Bishop.BAHAMUT:
            case DarkKnight.BEHOLDER:
            case Outlaw.GAVIOTA:
            case DawnWarrior.SOUL:
            case BlazeWizard.FLAME:
            case WindArcher.STORM:
            case NightWalker.DARKNESS:
            case ThunderBreaker.LIGHTNING:
            case BlazeWizard.IFRIT:
               statups.add(new Pair<>(TemporaryStatType.SUMMON, new TemporaryStatValue(sourceid, ret.skillLevel, 1)));
               break;
            // ----------------------------- MONSTER STATUS ---------------------------------- //
            case Crusader.ARMOR_CRASH:
            case DragonKnight.POWER_CRASH:
            case WhiteKnight.MAGIC_CRASH:
               monsterStatus.put(MonsterStatus.SEAL_SKILL, 1);
               break;
            case Rogue.DISORDER:
               monsterStatus.put(MonsterStatus.WATK, ret.x);
               monsterStatus.put(MonsterStatus.WDEF, ret.y);
               break;
            case Corsair.HYPNOTIZE:
               monsterStatus.put(MonsterStatus.INERTMOB, 1);
               break;
            case NightLord.NINJA_AMBUSH:
            case Shadower.NINJA_AMBUSH:
               monsterStatus.put(MonsterStatus.NINJA_AMBUSH, ret.damage);
               break;
            case Page.THREATEN:
               monsterStatus.put(MonsterStatus.WATK, ret.x);
               monsterStatus.put(MonsterStatus.WDEF, ret.y);
               break;
            case DragonKnight.DRAGON_ROAR:
               ret.hpR = -x / 100.0;
               monsterStatus.put(MonsterStatus.STUN, 1);
               break;
            case Crusader.AXE_COMA:
            case Crusader.SWORD_COMA:
            case Crusader.SHOUT:
            case WhiteKnight.CHARGE_BLOW:
            case Hunter.ARROW_BOMB:
            case ChiefBandit.ASSAULTER:
            case Shadower.BOOMERANG_STEP:
            case Brawler.BACK_SPIN_BLOW:
            case Brawler.DOUBLE_UPPERCUT:
            case Buccaneer.DEMOLITION:
            case Buccaneer.SNATCH:
            case Buccaneer.BARRAGE:
            case Gunslinger.BLANK_SHOT:
            case DawnWarrior.COMA:
            case ThunderBreaker.BARRAGE:
            case Aran.ROLLING_SPIN:
            case Evan.FIRE_BREATH:
            case Evan.BLAZE:
               monsterStatus.put(MonsterStatus.STUN, 1);
               break;
            case NightLord.TAUNT:
            case Shadower.TAUNT:
               monsterStatus.put(MonsterStatus.SHOWDOWN, ret.x);
               monsterStatus.put(MonsterStatus.MDEF, ret.x);
               monsterStatus.put(MonsterStatus.WDEF, ret.x);
               break;
            case ILWizard.COLD_BEAM:
            case ILMage.ICE_STRIKE:
            case ILArchMage.BLIZZARD:
            case ILMage.ELEMENT_COMPOSITION:
            case Sniper.BLIZZARD:
            case Outlaw.ICE_SPLITTER:
            case FPArchMage.PARALYZE:
            case Aran.COMBO_TEMPEST:
            case Evan.ICE_BREATH:
               monsterStatus.put(MonsterStatus.FREEZE, 1);
               ret.duration *= 2; // freezing skills are a little strange
               break;
            case FPWizard.SLOW:
            case ILWizard.SLOW:
            case BlazeWizard.SLOW:
               monsterStatus.put(MonsterStatus.SPEED, ret.x);
               break;
            case FPWizard.POISON_BREATH:
            case FPMage.ELEMENT_COMPOSITION:
               monsterStatus.put(MonsterStatus.POISON, 1);
               break;
            case Priest.DOOM:
               monsterStatus.put(MonsterStatus.DOOM, 1);
               break;
            case ILMage.SEAL:
            case FPMage.SEAL:
            case BlazeWizard.SEAL:
               monsterStatus.put(MonsterStatus.SEAL, 1);
               break;
            case Hermit.SHADOW_WEB: // shadow web
            case NightWalker.SHADOW_WEB:
               monsterStatus.put(MonsterStatus.SHADOW_WEB, 1);
               break;
            case FPArchMage.FIRE_DEMON:
            case ILArchMage.ICE_DEMON:
               monsterStatus.put(MonsterStatus.POISON, 1);
               monsterStatus.put(MonsterStatus.FREEZE, 1);
               break;
            case Evan.PHANTOM_IMPRINT:
               monsterStatus.put(MonsterStatus.PHANTOM_IMPRINT, x);
               //ARAN
            case Aran.COMBO_ABILITY:
               statups.add(new Pair<>(TemporaryStatType.ARAN_COMBO, new TemporaryStatValue(sourceid, ret.skillLevel, 100)));
               break;
            case Aran.COMBO_BARRIER:
               statups.add(new Pair<>(TemporaryStatType.COMBO_BARRIER, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Aran.COMBO_DRAIN:
               statups.add(new Pair<>(TemporaryStatType.COMBO_DRAIN, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Aran.SMART_KNOCKBACK:
               statups.add(new Pair<>(TemporaryStatType.SMART_KNOCKBACK, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Aran.BODY_PRESSURE:
               statups.add(new Pair<>(TemporaryStatType.BODY_PRESSURE, new TemporaryStatValue(sourceid, ret.skillLevel, ret.x)));
               break;
            case Aran.SNOW_CHARGE:
               statups.add(new Pair<>(TemporaryStatType.WK_CHARGE, new TemporaryStatValue(sourceid, ret.skillLevel, ret.duration)));
               break;
            default:
               break;
         }
      }
      if (ret.isMorph()) {
         statups.add(new Pair<>(TemporaryStatType.MORPH, new TemporaryStatValue(sourceid, ret.skillLevel, ret.getMorph())));
      }
      if (ret.ghost > 0 && !skill) {
         statups.add(new Pair<>(TemporaryStatType.GHOST_MORPH, new TemporaryStatValue(sourceid, ret.skillLevel, ret.ghost)));
      }
      ret.monsterStatus = monsterStatus;
      statups.trimToSize();
      ret.statups = statups;
      return ret;
   }

   public static boolean isMapChair(int sourceid) {
      return sourceid == Beginner.MAP_CHAIR || sourceid == Noblesse.MAP_CHAIR || sourceid == Legend.MAP_CHAIR;
   }

   public static boolean isDojoBuff(int sourceid) {
      return sourceid >= 2022359 && sourceid <= 2022421;
   }

   public static boolean isHpMpRecovery(int sourceid) {
      return sourceid == 2022198 || sourceid == 2022337;
   }

   public static boolean isPyramidBuff(int sourceid) {
      return sourceid >= 2022585 && sourceid <= 2022617;
   }

   public static boolean isRateCoupon(int sourceid) {
      int itemType = sourceid / 1000;
      return itemType == 5211 || itemType == 5360;
   }

   public static boolean isExpIncrease(int sourceid) {
      return sourceid >= 2022450 && sourceid <= 2022452;
   }

   public static boolean isAriantShield(int sourceid) {
      return sourceid == 2022269;
   }

   public static boolean isMonsterCard(int sourceid) {
      int itemType = sourceid / 10000;
      return itemType == 238;
   }

   public static boolean isHerosWill(int skillid) {
      return switch (skillid) {
         case Hero.HEROS_WILL, Paladin.HEROS_WILL, DarkKnight.HEROS_WILL, FPArchMage.HEROS_WILL, ILArchMage.HEROS_WILL,
              Bishop.HEROS_WILL, Bowmaster.HEROS_WILL, Marksman.HEROS_WILL, NightLord.HEROS_WILL, Shadower.HEROS_WILL,
              Buccaneer.PIRATES_RAGE, Aran.HEROS_WILL -> true;
         default -> false;
      };
   }

   private void addBuffStatPairToListIfNotZero(List<Pair<TemporaryStatType, TemporaryStatValue>> list, TemporaryStatType buffstat,
                                               Integer val) {
      if (val != 0) {
         list.add(new Pair<>(buffstat, new TemporaryStatValue(getSourceId(), getSourceLevel(), val)));
      }
   }

   private boolean isEffectActive(int mapid, boolean partyHunting) {
      if (cardStats == null) {
         return true;
      }

      if (!cardStats.isInArea(mapid)) {
         return false;
      }

      if (cardStats.party && !partyHunting) {
         return false;
      }

      return true;
   }

   public boolean isActive(MapleCharacter applyto) {
      return isEffectActive(applyto.getMapId(), applyto.getPartyMembersOnSameMap().size() > 1);
   }

   public int getCardRate(int mapid, int itemid) {
      if (cardStats != null) {
         if (cardStats.itemCode == Integer.MAX_VALUE) {
            return cardStats.prob;
         } else if (cardStats.itemCode < 1000) {
            if (itemid / 10000 == cardStats.itemCode) {
               return cardStats.prob;
            }
         } else {
            if (itemid == cardStats.itemCode) {
               return cardStats.prob;
            }
         }
      }

      return 0;
   }

   /**
    * @param applyto
    * @param obj
    * @param attack  damage done by the skill
    */
   public void applyPassive(MapleCharacter applyto, MapleMapObject obj, int attack) {
      if (makeChanceResult()) {
         switch (sourceid) { // MP eater
            case FPWizard.MP_EATER:
            case ILWizard.MP_EATER:
            case Cleric.MP_EATER:
               if (obj == null || obj.getType() != MapleMapObjectType.MONSTER) {
                  return;
               }
               MapleMonster mob = (MapleMonster) obj; // x is absorb percentage
               if (!mob.isBoss()) {
                  int absorbMp = Math.min((int) (mob.getMaxMp() * (getX() / 100.0)), mob.getMp());
                  if (absorbMp > 0) {
                     mob.setMp(mob.getMp() - absorbMp);
                     applyto.addMP(absorbMp);
                     applyto.announce(CUser.showOwnBuffEffect(sourceid, ShowItemGainInChatCode.SKILL_USE));
                     applyto.getMap().broadcastMessage(applyto, CUser.showBuffeffect(applyto.getId(), sourceid, 1), false);
                  }
               }
               break;
         }
      }
   }

   public boolean applyEchoOfHero(MapleCharacter applyfrom) {
      Map<Integer, MapleCharacter> mapPlayers = applyfrom.getMap().getMapPlayers();
      mapPlayers.remove(applyfrom.getId());

      boolean hwResult = applyTo(applyfrom);
      for (MapleCharacter chr : mapPlayers.values()) {    // Echo of Hero not buffing players in the map detected thanks to Masterrulax
         applyTo(applyfrom, chr, false, null, false, 1);
      }

      return hwResult;
   }

   public boolean applyTo(MapleCharacter chr) {
      return applyTo(chr, chr, true, null, false, 1);
   }

   public boolean applyTo(MapleCharacter chr, boolean useMaxRange) {
      return applyTo(chr, chr, true, null, useMaxRange, 1);
   }

   public boolean applyTo(MapleCharacter chr, Point pos) {
      return applyTo(chr, chr, true, pos, false, 1);
   }

   // primary: the player caster of the buff
   private boolean applyTo(MapleCharacter applyfrom, MapleCharacter applyto, boolean primary, Point pos, boolean useMaxRange,
                           int affectedPlayers) {
      if (skill && (sourceid == GM.HIDE || sourceid == SuperGM.HIDE)) {
         applyto.toggleHide(false);
         return true;
      }

      if (primary && isHeal()) {
         affectedPlayers = applyBuff(applyfrom, useMaxRange);
      }

      int hpchange = calcHPChange(applyfrom, primary, affectedPlayers);
      int mpchange = calcMPChange(applyfrom, primary);
      if (primary) {
         if (itemConNo != 0) {
            if (!applyto.getAbstractPlayerInteraction().hasItem(itemCon, itemConNo)) {
               applyto.announce(CWvsContext.enableActions());
               return false;
            }
            MapleInventoryManipulator.removeById(applyto.getClient(), ItemConstants.getInventoryType(itemCon), itemCon, itemConNo,
                  false, true);
         }
      } else {
         if (isResurrection()) {
            hpchange = applyto.getCurrentMaxHp();
            applyto.broadcastStance(applyto.isFacingLeft() ? 5 : 4);
         }
      }

      if (isDispel() && makeChanceResult()) {
         applyto.dispelDebuffs();
      } else if (isCureAllAbnormalStatus()) {
         applyto.purgeDebuffs();
      } else if (isComboReset()) {
         applyto.setCombo((short) 0);
      }
        /*if (applyfrom.getMp() < getMpCon()) {
         AutobanFactory.MPCON.addPoint(applyfrom.getAutobanManager(), "mpCon hack for skill:" + sourceid + "; Player MP: " + applyto.getMp() + " MP Needed: " + getMpCon());
         } */

      if (!applyto.applyHpMpChange(hpCon, hpchange, mpchange)) {
         applyto.announce(CWvsContext.enableActions());
         return false;
      }

      if (moveTo != -1) {
         if (moveTo != applyto.getMapId()) {
            MapleMap target;
            MaplePortal pt;

            if (moveTo == 999999999) {
               if (sourceid != 2030100) {
                  target = applyto.getMap().getReturnMap();
                  pt = target.getRandomPlayerSpawnpoint();
               } else {
                  if (!applyto.canRecoverLastBanish()) {
                     return false;
                  }

                  Pair<Integer, Integer> lastBanishInfo = applyto.getLastBanishData();
                  target = applyto.getWarpMap(lastBanishInfo.getLeft());
                  pt = target.getPortal(lastBanishInfo.getRight());
               }
            } else {
               target = applyto.getClient().getWorldServer().getChannel(applyto.getClient().getChannel())
                     .map(Channel::getMapFactory)
                     .map(f -> f.getMap(moveTo))
                     .orElse(null);
               int targetid = target.getId() / 10000000;
               if (targetid != 60 && applyto.getMapId() / 10000000 != 61 && targetid != applyto.getMapId() / 10000000
                     && targetid != 21 && targetid != 20 && targetid != 12 && (applyto.getMapId() / 10000000 != 10
                     && applyto.getMapId() / 10000000 != 12)) {
                  return false;
               }

               pt = target.getRandomPlayerSpawnpoint();
            }

            applyto.changeMap(target, pt);
         } else {
            return false;
         }
      }
      if (isShadowClaw()) {
         short projectileConsume = this.getBulletConsume();  // noticed by shavit

         MapleInventory use = applyto.getInventory(MapleInventoryType.USE);
         use.lockInventory();
         try {
            Item projectile = null;
            for (int i = 1; i <= use.getSlotLimit(); i++) { // impose order...
               Item item = use.getItem((short) i);
               if (item != null) {
                  if (ItemConstants.isThrowingStar(item.getItemId()) && item.getQuantity() >= projectileConsume) {
                     projectile = item;
                     break;
                  }
               }
            }
            if (projectile == null) {
               return false;
            } else {
               MapleInventoryManipulator.removeFromSlot(applyto.getClient(), MapleInventoryType.USE, projectile.getPosition(),
                     projectileConsume, false, true);
            }
         } finally {
            use.unlockInventory();
         }
      }
      SummonMovementType summonMovementType = getSummonMovementType();
      if (overTime || isCygnusFA() || summonMovementType != null) {
         if (summonMovementType != null && pos != null) {
            if (summonMovementType.getValue() == SummonMovementType.STATIONARY.getValue()) {
               applyto.cancelBuffStats(TemporaryStatType.PUPPET);
            } else {
               applyto.cancelBuffStats(TemporaryStatType.SUMMON);
            }

            applyto.announce(CWvsContext.enableActions());
         }

         applyBuffEffect(applyfrom, applyto, primary);
      }

      if (primary) {
         if (overTime) {
            applyBuff(applyfrom, useMaxRange);
         }

         if (isMonsterBuff()) {
            applyMonsterBuff(applyfrom);
         }
      }

      if (this.getFatigue() != 0) {
         applyto.getMount().ifPresent(m -> m.setTiredness(m.getTiredness() + getFatigue()));
      }

      if (summonMovementType != null && pos != null) {
         final MapleSummon tosummon = new MapleSummon(applyfrom, sourceid, pos, summonMovementType);
         applyfrom.getMap().spawnSummon(tosummon);
         applyfrom.addSummon(sourceid, tosummon);
         tosummon.addHP(x);
         if (isBeholder()) {
            tosummon.addHP(1);
         }
      }
      if (isMagicDoor() && !FieldLimit.DOOR.check(applyto.getMap().getFieldLimit())) { // Magic Door
         int y = applyto.getFh();
         if (y == 0) {
            y = applyto.getMap().getGroundBelow(
                  applyto.getPosition()).y;    // thanks Lame for pointing out unusual cases of doors sending players on ground below
         }
         Point doorPosition = new Point(applyto.getPosition().x, y);
         DoorProcessor.getInstance().attemptDoorCreation(applyto, doorPosition, sourceid);
      } else if (isMist()) {
         Rectangle bounds =
               calculateBoundingBox(sourceid == NightWalker.POISON_BOMB ? pos : applyfrom.getPosition(), applyfrom.isFacingLeft());
         MapleMist mist = new MapleMist(bounds, applyfrom, this);
         applyfrom.getMap().spawnMist(mist, getDuration(), mist.isPoisonMist(), false, mist.isRecoveryMist());
      } else if (isTimeLeap()) {
         applyto.removeAllCooldownsExcept(Buccaneer.TIME_LEAP, true);
      } else if (cp != 0 && applyto.getMonsterCarnival() != null) {
         applyto.gainCP(cp);
      } else if (nuffSkill != 0 && applyto.getParty().isPresent() && applyto.getMap().isCPQMap()) {
         final MCSkill skill = MapleCarnivalFactory.getInstance().getSkill(nuffSkill);
         if (skill != null) {
            final Optional<MapleDisease> dis = skill.getDisease();
            Optional<MapleParty> opposition = applyfrom.getParty().map(MapleParty::getEnemy);
            if (skill.targetsAll) {
               opposition.map(MapleParty::getPartyMembers)
                     .orElse(Collections.emptyList()).stream()
                     .map(MaplePartyCharacter::getPlayer)
                     .flatMap(Optional::stream)
                     .filter(c -> c.getMap().isCPQMap())
                     .forEach(c -> {
                        if (dis.isEmpty()) {
                           c.dispel();
                        } else {
                           c.giveDebuff(dis.get(), MCSkill.getMobSkill(dis.get().getDisease(), skill.level));
                        }
                     });
            } else {
               int amount = opposition.map(MapleParty::getMembers).map(Collection::size).orElse(0);
               int randd = (int) Math.floor(Math.random() * amount);
               int randomOppositionId = opposition.map(p -> p.getMemberByPos(randd)).map(MaplePartyCharacter::getId).orElse(-1);
               Optional<MapleCharacter> chrApp = applyfrom.getMap().getCharacterById(randomOppositionId);
               if (chrApp.isPresent() && chrApp.get().getMap().isCPQMap()) {
                  if (dis.isEmpty()) {
                     chrApp.get().dispel();
                  } else {
                     chrApp.get().giveDebuff(dis.get(), MCSkill.getMobSkill(dis.get().getDisease(), skill.level));
                  }
               }
            }
         }
      } else if (cureDebuffs.size() > 0) {
         for (final MapleDisease debuff : cureDebuffs) {
            applyfrom.dispelDebuff(debuff);
         }
      } else if (mobSkill > 0 && mobSkillLevel > 0) {
         MobSkill ms = MobSkillFactory.getMobSkill(mobSkill, mobSkillLevel);

         Optional<MapleDisease> dis = MapleDisease.getBySkill(mobSkill);
         if (dis.isPresent()) {
            if (target > 0) {
               applyto.getMap().getAllPlayers().stream()
                     .filter(c -> c.getId() != applyto.getId())
                     .forEach(c -> c.giveDebuff(dis.get(), ms));
            } else {
               applyto.giveDebuff(dis.get(), ms);
            }
         }
      }
      return true;
   }

   private int applyBuff(MapleCharacter applyfrom, boolean useMaxRange) {
      int affectedc = 1;

      if (isPartyBuff() && (applyfrom.getParty().isPresent() || isGmBuff())) {
         Rectangle bounds = (!useMaxRange) ? calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft()) :
               new Rectangle(Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, Integer.MAX_VALUE, Integer.MAX_VALUE);
         List<MapleMapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds, List.of(MapleMapObjectType.PLAYER));
         List<MapleCharacter> affectedp = new ArrayList<>(affecteds.size());
         for (MapleMapObject affectedmo : affecteds) {
            MapleCharacter affected = (MapleCharacter) affectedmo;
            if (affected != applyfrom && (isGmBuff() || applyfrom.getParty().flatMap(p1 -> affected.getParty().map(p1::equals))
                  .orElse(false))) {
               if (!isResurrection()) {
                  if (affected.isAlive()) {
                     affectedp.add(affected);
                  }
               } else {
                  if (!affected.isAlive()) {
                     affectedp.add(affected);
                  }
               }
            }
         }

         affectedc += affectedp.size();   // used for heal
         for (MapleCharacter affected : affectedp) {
            applyTo(applyfrom, affected, false, null, useMaxRange, affectedc);
            affected.announce(CUser.showOwnBuffEffect(sourceid, ShowItemGainInChatCode.SKILL_AFFECTED));
            affected.getMap().broadcastMessage(affected, CUser.showBuffeffect(affected.getId(), sourceid, 2), false);
         }
      }

      return affectedc;
   }

   private void applyMonsterBuff(MapleCharacter applyFrom) {
      Rectangle bounds = calculateBoundingBox(applyFrom.getPosition(), applyFrom.isFacingLeft());
      List<MapleMapObject> affected = applyFrom.getMap().getMapObjectsInRect(bounds, List.of(MapleMapObjectType.MONSTER));
      int i = 0;
      for (MapleMapObject mo : affected) {
         MapleMonster monster = (MapleMonster) mo;
         Optional<Skill> skill = SkillFactory.getSkill(sourceid);
         if (skill.isEmpty()) {
            continue;
         }

         if (isDispel()) {
            monster.debuffMob(skill.get().id());
         } else if (isSeal() && monster.isBoss()) {  // thanks IxianMace for noticing seal working on bosses
            // do nothing
         } else {
            if (makeChanceResult()) {
               monster.applyStatus(applyFrom, new MonsterStatusEffect(getMonsterStati(), skill.get(), null, false), isPoison(),
                     getDuration());
               if (isCrash()) {
                  monster.debuffMob(skill.get().id());
               }
            }
         }
         i++;
         if (i >= mobCount) {
            break;
         }
      }
   }

   private Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft) {
      Point mylt;
      Point myrb;
      if (facingLeft) {
         mylt = new Point(lt.x + posFrom.x, lt.y + posFrom.y);
         myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
      } else {
         myrb = new Point(-lt.x + posFrom.x, rb.y
               + posFrom.y);  // thanks Conrad, April for noticing a disturbance in AoE skill behavior after a hitched refactor here
         mylt = new Point(-rb.x + posFrom.x, lt.y + posFrom.y);
      }
      return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
   }

   public int getBuffLocalDuration() {
      return !YamlConfig.config.server.USE_BUFF_EVERLASTING ? duration : Integer.MAX_VALUE;
   }

   public void silentApplyBuff(MapleCharacter chr, long localStartTime) {
      int localDuration = getBuffLocalDuration();
      localDuration = alchemistModifyVal(chr, localDuration, false);
      //CancelEffectAction cancelAction = new CancelEffectAction(chr, this, starttime);
      //ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((starttime + localDuration) - Server.getInstance().getCurrentTime()));

      chr.registerEffect(this, localStartTime, localStartTime + localDuration, true);
      SummonMovementType summonMovementType = getSummonMovementType();
      if (summonMovementType != null) {
         final MapleSummon tosummon = new MapleSummon(chr, sourceid, chr.getPosition(), summonMovementType);
         if (!tosummon.isStationary()) {
            chr.addSummon(sourceid, tosummon);
            tosummon.addHP(x);
         }
      }
      if (sourceid == Corsair.BATTLE_SHIP) {
         chr.announceBattleshipHp();
      }
   }

   public final void applyComboBuff(final MapleCharacter applyto, int combo) {
      final List<Pair<TemporaryStatType, TemporaryStatValue>> stat =
            Collections.singletonList(new Pair<>(TemporaryStatType.ARAN_COMBO, new TemporaryStatValue(sourceid, 0, combo)));
      applyto.announce(CWvsContext.giveBuff(applyto, sourceid, 99999, stat));

      final long starttime = Server.getInstance().getCurrentTime();
      //	final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
      //	final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((starttime + 99999) - Server.getInstance().getCurrentTime()));
      applyto.registerEffect(this, starttime, Long.MAX_VALUE, false);
   }

   public final void applyBeaconBuff(final MapleCharacter applyto, int objectid) {
      applyto.announce(CWvsContext.giveBuff(applyto, 1, sourceid, Collections.singletonList(new Pair<>(TemporaryStatType.HOMING_BEACON,
            new TemporaryStatValue(0, 0, objectid)))));

      final long starttime = Server.getInstance().getCurrentTime();
      applyto.registerEffect(this, starttime, Long.MAX_VALUE, false);
   }

   public void updateBuffEffect(MapleCharacter target, List<Pair<TemporaryStatType, TemporaryStatValue>> activeStats, long starttime) {
      int localDuration = getBuffLocalDuration();
      localDuration = alchemistModifyVal(target, localDuration, false);

      long leftDuration = (starttime + localDuration) - Server.getInstance().getCurrentTime();
      if (leftDuration > 0) {
         target.announce(CWvsContext.giveBuff(target, (skill ? sourceid : -sourceid), (int) leftDuration, activeStats));
      }
   }

   private void applyBuffEffect(MapleCharacter applyfrom, MapleCharacter applyto, boolean primary) {
      if (!isMonsterRiding() && !isCouponBuff() && !isMysticDoor() && !isHyperBody()
            && !isCombo()) {     // last mystic door already dispelled if it has been used before.
         applyto.cancelEffect(this, true, -1);
      }

      List<Pair<TemporaryStatType, TemporaryStatValue>> localstatups = statups;
      int localDuration = getBuffLocalDuration();
      int localsourceid = sourceid;
      int seconds = localDuration / 1000;
      MapleMount givemount = null;
      if (isMonsterRiding()) {
         int ridingMountId = 0;
         Item mount = applyfrom.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -18);
         if (mount != null) {
            ridingMountId = mount.getItemId();
         }

         if (sourceid == Corsair.BATTLE_SHIP) {
            ridingMountId = 1932000;
         } else if (sourceid == Beginner.SPACESHIP || sourceid == Noblesse.SPACESHIP) {
            ridingMountId = 1932000 + applyto.getSkillLevel(sourceid);
         } else if (sourceid == Beginner.YETI_MOUNT1 || sourceid == Noblesse.YETI_MOUNT1 || sourceid == Legend.YETI_MOUNT1) {
            ridingMountId = 1932003;
         } else if (sourceid == Beginner.YETI_MOUNT2 || sourceid == Noblesse.YETI_MOUNT2 || sourceid == Legend.YETI_MOUNT2) {
            ridingMountId = 1932004;
         } else if (sourceid == Beginner.WITCH_BROOMSTICK || sourceid == Noblesse.WITCH_BROOMSTICK
               || sourceid == Legend.WITCH_BROOMSTICK) {
            ridingMountId = 1932005;
         } else if (sourceid == Beginner.BALROG_MOUNT || sourceid == Noblesse.BALROG_MOUNT || sourceid == Legend.BALROG_MOUNT) {
            ridingMountId = 1932010;
         }

         // thanks inhyuk for noticing some skill mounts not acting properly for other players when changing maps
         givemount = applyto.mount(ridingMountId, sourceid);
         applyto.getClient().getWorldServer().registerMountHunger(applyto);

         localDuration = sourceid;
         localsourceid = ridingMountId;
         localstatups = Collections.singletonList(new Pair<>(TemporaryStatType.MONSTER_RIDING, TemporaryStatValue.empty()));
      } else if (isSkillMorph()) {
         for (int i = 0; i < localstatups.size(); i++) {
            if (localstatups.get(i).getLeft().equals(TemporaryStatType.MORPH)) {
               localstatups.set(i, new Pair<>(TemporaryStatType.MORPH, new TemporaryStatValue(sourceid, 0, getMorph(applyto))));
               break;
            }
         }
      }
      if (primary) {
         localDuration = alchemistModifyVal(applyfrom, localDuration, false);
         applyto.getMap().broadcastMessage(applyto, CUser.showBuffeffect(applyto.getId(), sourceid, 1, (byte) 3), false);
      }
      if (localstatups.size() > 0) {
         byte[] buff = null;
         byte[] mbuff = null;
         if (this.isActive(applyto)) {
            buff = CWvsContext.giveBuff(applyto, (skill ? sourceid : -sourceid), localDuration, localstatups);
         }
         if (isDash()) {
            buff = CWvsContext.giveBuff(applyto, sourceid, seconds, statups);
            mbuff = CUserRemote.giveForeignPirateBuff(applyto.getId(), sourceid, seconds, localstatups);
         } else if (isWkCharge()) {
            mbuff = CUserRemote.giveForeignWKChargeEffect(applyto.getId(), sourceid, localstatups);
         } else if (isInfusion()) {
            buff = CWvsContext.giveBuff(applyto, sourceid, seconds, localstatups);
            mbuff = CUserRemote.giveForeignPirateBuff(applyto.getId(), sourceid, seconds, localstatups);
         } else if (isDs()) {
            List<Pair<TemporaryStatType, TemporaryStatValue>> dsstat = Collections.singletonList(new Pair<>(TemporaryStatType.DARKSIGHT,
                  TemporaryStatValue.empty()));
            mbuff = CUserRemote.giveForeignBuff(applyto, dsstat);
         } else if (isWw()) {
            List<Pair<TemporaryStatType, TemporaryStatValue>> dsstat = Collections.singletonList(new Pair<>(TemporaryStatType.WIND_WALK,
                  TemporaryStatValue.empty()));
            mbuff = CUserRemote.giveForeignBuff(applyto, dsstat);
         } else if (isCombo()) {
            Integer comboCount = applyto.getBuffedValue(TemporaryStatType.COMBO);
            if (comboCount == null) {
               comboCount = 0;
            }

            List<Pair<TemporaryStatType, TemporaryStatValue>> cbstat = Collections.singletonList(new Pair<>(TemporaryStatType.COMBO,
                  new TemporaryStatValue(0, 0, comboCount)));
            buff = CWvsContext.giveBuff(applyto, (skill ? sourceid : -sourceid), localDuration, cbstat);
            mbuff = CUserRemote.giveForeignBuff(applyto, cbstat);
         } else if (isMonsterRiding()) {
            if (sourceid == Corsair.BATTLE_SHIP) {//hp
               if (applyto.getBattleshipHp() <= 0) {
                  applyto.resetBattleshipHp();
               }

               localstatups = statups;
            }
            buff = CWvsContext.giveBuff(applyto, localsourceid, localDuration, localstatups);
            mbuff = CUserRemote.showMonsterRiding(applyto.getId(), givemount);
            localDuration = duration;
         } else if (isShadowPartner()) {
            mbuff = CUserRemote.giveForeignBuff(applyto, Collections.singletonList(new Pair<>(TemporaryStatType.SHADOWPARTNER,
                  TemporaryStatValue.empty())));
         } else if (isSoulArrow()) {
            mbuff = CUserRemote.giveForeignBuff(applyto,
                  Collections.singletonList(new Pair<>(TemporaryStatType.SOULARROW, TemporaryStatValue.empty())));
         } else if (isEnrage()) {
            applyto.handleOrbconsume();
         } else if (isMorph()) {
            mbuff = CUserRemote.giveForeignBuff(applyto, Collections.singletonList(new Pair<>(TemporaryStatType.MORPH,
                  new TemporaryStatValue(sourceid, 0, getMorph(applyto)))));
         }
         //         else if (isAriantShield()) {
         //            mbuff = CUserRemote.giveForeignBuff(applyto, Collections.singletonList(new Pair<>(TemporaryStatType.AURA,
         //                  new TemporaryStatValue(sourceid, 0, 1))));
         //         }

         if (buff != null) {
            //Thanks flav for such a simple release! :)
            //Thanks Conrad, Atoot for noticing summons not using buff icon

            applyto.announce(buff);
         }

         long starttime = Server.getInstance().getCurrentTime();
         //CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
         //ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, localDuration);
         applyto.registerEffect(this, starttime, starttime + localDuration, false);
         if (mbuff != null) {
            applyto.getMap().broadcastMessage(applyto, mbuff, false);
         }
         if (sourceid == Corsair.BATTLE_SHIP) {
            applyto.announceBattleshipHp();
         }
      }
   }

   private int calcHPChange(MapleCharacter applyfrom, boolean primary, int affectedPlayers) {
      int hpchange = 0;
      if (hp != 0) {
         if (!skill) {
            if (primary) {
               hpchange += alchemistModifyVal(applyfrom, hp, true);
            } else {
               hpchange += hp;
            }
            if (applyfrom.hasDisease(MapleDisease.ZOMBIFY)) {
               hpchange /= 2;
            }
         } else { // assumption: this is heal
            float hpHeal = (applyfrom.getCurrentMaxHp() * (float) hp / (100.0f * affectedPlayers));
            hpchange += hpHeal;
            if (applyfrom.hasDisease(MapleDisease.ZOMBIFY)) {
               hpchange = -hpchange;
               hpCon = 0;
            }
         }
      }
      if (hpR != 0) {
         hpchange += (int) (applyfrom.getCurrentMaxHp() * hpR) / (applyfrom.hasDisease(MapleDisease.ZOMBIFY) ? 2 : 1);
      }
      if (primary) {
         if (hpCon != 0) {
            hpchange -= hpCon;
         }
      }
      if (isChakra()) {
         hpchange += makeHealHP(getY() / 100.0, applyfrom.getTotalLuk(), 2.3, 3.5);
      } else if (sourceid == SuperGM.HEAL_PLUS_DISPEL) {
         hpchange += applyfrom.getCurrentMaxHp();
      }

      return hpchange;
   }

   private int makeHealHP(double rate, double stat, double lowerfactor, double upperfactor) {
      return (int) ((Math.random() * ((int) (stat * upperfactor * rate) - (int) (stat * lowerfactor * rate) + 1)) + (int) (stat
            * lowerfactor * rate));
   }

   private int calcMPChange(MapleCharacter applyfrom, boolean primary) {
      int mpchange = 0;
      if (mp != 0) {
         if (primary) {
            mpchange += alchemistModifyVal(applyfrom, mp, true);
         } else {
            mpchange += mp;
         }
      }
      if (mpR != 0) {
         mpchange += (int) (applyfrom.getCurrentMaxMp() * mpR);
      }
      if (primary) {
         if (mpCon != 0) {
            double mod = 1.0;
            boolean isAFpMage = applyfrom.getJob().isA(MapleJob.FP_MAGE);
            boolean isCygnus = applyfrom.getJob().isA(MapleJob.BLAZEWIZARD2);
            boolean isEvan = applyfrom.getJob().isA(MapleJob.EVAN7);
            if (isAFpMage || isCygnus || isEvan || applyfrom.getJob().isA(MapleJob.IL_MAGE)) {
               Skill amp = isAFpMage ? SkillFactory.getSkill(FPMage.ELEMENT_AMPLIFICATION).orElseThrow() :
                     (isCygnus ? SkillFactory.getSkill(BlazeWizard.ELEMENT_AMPLIFICATION).orElseThrow() :
                           (isEvan ? SkillFactory.getSkill(Evan.MAGIC_AMPLIFICATION).orElseThrow() :
                                 SkillFactory.getSkill(ILMage.ELEMENT_AMPLIFICATION).orElseThrow()));
               int ampLevel = applyfrom.getSkillLevel(amp);
               if (ampLevel > 0) {
                  mod = amp.getEffect(ampLevel).getX() / 100.0;
               }
            }
            mpchange -= mpCon * mod;
            if (applyfrom.getBuffedValue(TemporaryStatType.INFINITY) != null) {
               mpchange = 0;
            } else if (applyfrom.getBuffedValue(TemporaryStatType.CONCENTRATE) != null) {
               mpchange -= (int) (mpchange * (applyfrom.getBuffedValue(TemporaryStatType.CONCENTRATE).doubleValue() / 100));
            }
         }
      }
      if (sourceid == SuperGM.HEAL_PLUS_DISPEL) {
         mpchange += applyfrom.getCurrentMaxMp();
      }

      return mpchange;
   }

   private int alchemistModifyVal(MapleCharacter chr, int val, boolean withX) {
      if (!skill && (chr.getJob().isA(MapleJob.HERMIT) || chr.getJob().isA(MapleJob.NIGHTWALKER3))) {
         return getAlchemistEffect(chr)
               .map(e -> (int) (val * ((withX ? e.getX() : e.getY()) / 100.0)))
               .orElse(val);
      }
      return val;
   }

   private Optional<MapleStatEffect> getAlchemistEffect(MapleCharacter chr) {
      int id = Hermit.ALCHEMIST;
      if (chr.isCygnus()) {
         id = NightWalker.ALCHEMIST;
      }
      Optional<Byte> level = SkillFactory.getSkill(id)
            .map(chr::getSkillLevel);
      if (level.isEmpty()) {
         return Optional.empty();
      }
      int alchemistLevel = level.get();
      if (alchemistLevel == 0) {
         return Optional.empty();
      }
      return SkillFactory.getSkill(id)
            .map(s -> s.getEffect(alchemistLevel));
   }

   private boolean isGmBuff() {
      return switch (sourceid) {
         case Beginner.ECHO_OF_HERO, Noblesse.ECHO_OF_HERO, Legend.ECHO_OF_HERO, Evan.ECHO_OF_HERO, SuperGM.HEAL_PLUS_DISPEL,
              SuperGM.HASTE, SuperGM.HOLY_SYMBOL, SuperGM.BLESS, SuperGM.RESURRECTION, SuperGM.HYPER_BODY -> true;
         default -> false;
      };
   }

   private boolean isMonsterBuff() {
      if (!skill) {
         return false;
      }
      return switch (sourceid) {
         case Page.THREATEN, FPWizard.SLOW, ILWizard.SLOW, FPMage.SEAL, ILMage.SEAL, Priest.DOOM, Hermit.SHADOW_WEB,
              NightLord.NINJA_AMBUSH, Shadower.NINJA_AMBUSH, BlazeWizard.SLOW, BlazeWizard.SEAL, NightWalker.SHADOW_WEB,
              Crusader.ARMOR_CRASH, DragonKnight.POWER_CRASH, WhiteKnight.MAGIC_CRASH, Priest.DISPEL, SuperGM.HEAL_PLUS_DISPEL ->
               true;
         default -> false;
      };
   }

   private boolean isPartyBuff() {
      if (lt == null || rb == null) {
         return false;
      }
      if ((sourceid >= 1211003 && sourceid <= 1211008) || sourceid == Paladin.SWORD_HOLY_CHARGE
            || sourceid == Paladin.BW_HOLY_CHARGE
            || sourceid == DawnWarrior.SOUL_CHARGE) {// wk charges have lt and rb set but are neither player nor monster buffs
         return false;
      }
      return true;
   }

   private boolean isHeal() {
      return sourceid == Cleric.HEAL || sourceid == SuperGM.HEAL_PLUS_DISPEL;
   }

   private boolean isResurrection() {
      return sourceid == Bishop.RESURRECTION || sourceid == GM.RESURRECTION || sourceid == SuperGM.RESURRECTION;
   }

   private boolean isTimeLeap() {
      return sourceid == Buccaneer.TIME_LEAP;
   }

   public boolean isDragonBlood() {
      return skill && sourceid == DragonKnight.DRAGON_BLOOD;
   }

   public boolean isBerserk() {
      return skill && sourceid == DarkKnight.BERSERK;
   }

   public boolean isRecovery() {
      return sourceid == Beginner.RECOVERY || sourceid == Noblesse.RECOVERY || sourceid == Legend.RECOVERY
            || sourceid == Evan.RECOVERY;
   }

   public boolean isMapChair() {
      return sourceid == Beginner.MAP_CHAIR || sourceid == Noblesse.MAP_CHAIR || sourceid == Legend.MAP_CHAIR;
   }

   public boolean isDojoBuff() {
      return sourceid >= 2022359 && sourceid <= 2022421;
   }

   private boolean isDs() {
      return skill && (sourceid == Rogue.DARK_SIGHT || sourceid == NightWalker.DARK_SIGHT);
   }

   private boolean isWw() {
      return skill && (sourceid == WindArcher.WIND_WALK);
   }

   private boolean isCombo() {
      return skill && (sourceid == Crusader.COMBO || sourceid == DawnWarrior.COMBO);
   }

   private boolean isEnrage() {
      return skill && sourceid == Hero.ENRAGE;
   }

   public boolean isBeholder() {
      return skill && sourceid == DarkKnight.BEHOLDER;
   }

   private boolean isShadowPartner() {
      return skill && (sourceid == Hermit.SHADOW_PARTNER || sourceid == NightWalker.SHADOW_PARTNER);
   }

   private boolean isChakra() {
      return skill && sourceid == ChiefBandit.CHAKRA;
   }

   private boolean isCouponBuff() {
      return isRateCoupon(sourceid);
   }

   private boolean isAriantShield() {
      int itemid = sourceid;
      return isAriantShield(itemid);
   }

   private boolean isMysticDoor() {
      return skill && sourceid == Priest.MYSTIC_DOOR;
   }

   public boolean isMonsterRiding() {
      return skill && (sourceid % 10000000 == 1004 || sourceid == Corsair.BATTLE_SHIP || sourceid == Beginner.SPACESHIP
            || sourceid == Noblesse.SPACESHIP
            || sourceid == Beginner.YETI_MOUNT1 || sourceid == Beginner.YETI_MOUNT2 || sourceid == Beginner.WITCH_BROOMSTICK
            || sourceid == Beginner.BALROG_MOUNT
            || sourceid == Noblesse.YETI_MOUNT1 || sourceid == Noblesse.YETI_MOUNT2 || sourceid == Noblesse.WITCH_BROOMSTICK
            || sourceid == Noblesse.BALROG_MOUNT
            || sourceid == Legend.YETI_MOUNT1 || sourceid == Legend.YETI_MOUNT2 || sourceid == Legend.WITCH_BROOMSTICK
            || sourceid == Legend.BALROG_MOUNT);
   }

   public boolean isMagicDoor() {
      return skill && sourceid == Priest.MYSTIC_DOOR;
   }

   public boolean isPoison() {
      return skill && (sourceid == FPMage.POISON_MIST || sourceid == FPWizard.POISON_BREATH
            || sourceid == FPMage.ELEMENT_COMPOSITION || sourceid == NightWalker.POISON_BOMB || sourceid == BlazeWizard.FLAME_GEAR);
   }

   public boolean isMorph() {
      return morphId > 0;
   }

   public boolean isMorphWithoutAttack() {
      return morphId > 0
            && morphId < 100; // Every morph item I have found has been under 100, pirate skill transforms start at 1000.
   }

   private boolean isMist() {
      return skill && (sourceid == FPMage.POISON_MIST || sourceid == Shadower.SMOKE_SCREEN || sourceid == BlazeWizard.FLAME_GEAR
            || sourceid == NightWalker.POISON_BOMB || sourceid == Evan.RECOVERY_AURA);
   }

   private boolean isSoulArrow() {
      return skill && (sourceid == Hunter.SOUL_ARROW || sourceid == Crossbowman.SOUL_ARROW || sourceid == WindArcher.SOUL_ARROW);
   }

   private boolean isShadowClaw() {
      return skill && sourceid == NightLord.SHADOW_STARS;
   }

   private boolean isCrash() {
      return skill && (sourceid == DragonKnight.POWER_CRASH || sourceid == Crusader.ARMOR_CRASH
            || sourceid == WhiteKnight.MAGIC_CRASH);
   }

   private boolean isSeal() {
      return skill && (sourceid == ILMage.SEAL || sourceid == FPMage.SEAL || sourceid == BlazeWizard.SEAL);
   }

   private boolean isDispel() {
      return skill && (sourceid == Priest.DISPEL || sourceid == SuperGM.HEAL_PLUS_DISPEL);
   }

   private boolean isCureAllAbnormalStatus() {
      if (skill) {
         return isHerosWill(sourceid);
      } else if (sourceid == 2022544) {
         return true;
      }

      return false;
   }

   private boolean isWkCharge() {
      if (!skill) {
         return false;
      }

      for (Pair<TemporaryStatType, TemporaryStatValue> p : statups) {
         if (p.getLeft().equals(TemporaryStatType.WK_CHARGE)) {
            return true;
         }
      }

      return false;
   }

   private boolean isDash() {
      return skill && (sourceid == Pirate.DASH || sourceid == ThunderBreaker.DASH || sourceid == Beginner.SPACE_DASH
            || sourceid == Noblesse.SPACE_DASH);
   }

   private boolean isSkillMorph() {
      return skill && (sourceid == Buccaneer.SUPER_TRANSFORMATION || sourceid == Marauder.TRANSFORMATION
            || sourceid == WindArcher.EAGLE_EYE || sourceid == ThunderBreaker.TRANSFORMATION);
   }

   private boolean isInfusion() {
      return skill && (sourceid == Buccaneer.SPEED_INFUSION || sourceid == Corsair.SPEED_INFUSION
            || sourceid == ThunderBreaker.SPEED_INFUSION);
   }

   private boolean isCygnusFA() {
      return skill && (sourceid == DawnWarrior.FINAL_ATTACK || sourceid == WindArcher.FINAL_ATTACK);
   }

   private boolean isHyperBody() {
      return skill && (sourceid == Spearman.HYPER_BODY || sourceid == GM.HYPER_BODY || sourceid == SuperGM.HYPER_BODY);
   }

   private boolean isComboReset() {
      return sourceid == Aran.COMBO_BARRIER || sourceid == Aran.COMBO_DRAIN;
   }

   private int getFatigue() {
      return fatigue;
   }

   private int getMorph() {
      return morphId;
   }

   private int getMorph(MapleCharacter chr) {
      if (morphId == 1000 || morphId == 1001 || morphId == 1003) { // morph skill
         return chr.getGender() == 0 ? morphId : morphId + 100;
      }
      return morphId;
   }

   private SummonMovementType getSummonMovementType() {
      if (!skill) {
         return null;
      }
      return switch (sourceid) {
         case Ranger.PUPPET, Sniper.PUPPET, WindArcher.PUPPET, Outlaw.OCTOPUS, Corsair.WRATH_OF_THE_OCTOPI ->
               SummonMovementType.STATIONARY;
         case Ranger.SILVER_HAWK, Sniper.GOLDEN_EAGLE, Priest.SUMMON_DRAGON, Marksman.FROST_PREY, Bowmaster.PHOENIX,
              Outlaw.GAVIOTA -> SummonMovementType.CIRCLE_FOLLOW;
         case DarkKnight.BEHOLDER, FPArchMage.ELQUINES, ILArchMage.IFRIT, Bishop.BAHAMUT, DawnWarrior.SOUL, BlazeWizard.FLAME,
              BlazeWizard.IFRIT, WindArcher.STORM, NightWalker.DARKNESS, ThunderBreaker.LIGHTNING -> SummonMovementType.FOLLOW;
         default -> null;
      };
   }

   public boolean isSkill() {
      return skill;
   }

   public int getSourceId() {
      return sourceid;
   }

   public int getSourceLevel() {
      return sourceLevel;
   }

   public int getBuffSourceId() {
      return skill ? sourceid : -sourceid;
   }

   public boolean makeChanceResult() {
      return prop == 1.0 || Math.random() < prop;
   }

   /*
    private static class CancelEffectAction implements Runnable {

    private MapleStatEffect effect;
    private WeakReference<MapleCharacter> target;
    private long startTime;

    public CancelEffectAction(MapleCharacter target, MapleStatEffect effect, long startTime) {
    this.effect = effect;
    this.target = new WeakReference<>(target);
    this.startTime = startTime;
    }

    @Override
    public void run() {
    MapleCharacter realTarget = target.get();
    if (realTarget != null) {
    realTarget.cancelEffect(effect, false, startTime);
    }
    }
    }
    */
   public short getHp() {
      return hp;
   }

   public short getMp() {
      return mp;
   }

   public double getHpRate() {
      return hpR;
   }

   public double getMpRate() {
      return mpR;
   }

   public byte getHpR() {
      return mhpR;
   }

   public byte getMpR() {
      return mmpR;
   }

   public short getHpRRate() {
      return mhpRRate;
   }

   public short getMpRRate() {
      return mmpRRate;
   }

   public short getHpCon() {
      return hpCon;
   }

   public short getMpCon() {
      return mpCon;
   }

   public short getMatk() {
      return matk;
   }

   public short getWatk() {
      return watk;
   }

   public int getDuration() {
      return duration;
   }

   public List<Pair<TemporaryStatType, TemporaryStatValue>> getStatups() {
      return statups;
   }

   public boolean sameSource(MapleStatEffect effect) {
      return this.sourceid == effect.sourceid && this.skill == effect.skill;
   }

   public int getX() {
      return x;
   }

   public int getY() {
      return y;
   }

   public int getDamage() {
      return damage;
   }

   public int getAttackCount() {
      return attackCount;
   }

   public int getMobCount() {
      return mobCount;
   }

   public int getFixDamage() {
      return fixdamage;
   }

   public short getBulletCount() {
      return bulletCount;
   }

   public short getBulletConsume() {
      return bulletConsume;
   }

   public int getMoneyCon() {
      return moneyCon;
   }

   public int getCooldown() {
      return cooldown;
   }

   public Map<MonsterStatus, Integer> getMonsterStati() {
      return monsterStatus;
   }

   private static class CardItemupStats {
      protected int itemCode, prob;
      protected boolean party;
      private List<Pair<Integer, Integer>> areas;

      private CardItemupStats(int code, int prob, List<Pair<Integer, Integer>> areas, boolean inParty) {
         this.itemCode = code;
         this.prob = prob;
         this.areas = areas;
         this.party = inParty;
      }

      private boolean isInArea(int mapid) {
         if (this.areas == null) {
            return true;
         }

         for (Pair<Integer, Integer> a : this.areas) {
            if (mapid >= a.left && mapid <= a.right) {
               return true;
            }
         }

         return false;
      }
   }
}
