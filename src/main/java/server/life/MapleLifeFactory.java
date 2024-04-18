package server.life;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import provider.wz.MapleDataType;
import tools.Pair;
import tools.StringUtil;

public class MapleLifeFactory {

   private static final Logger log = LoggerFactory.getLogger(MapleLifeFactory.class);

   private final static MapleDataProvider stringDataWZ =
         MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz"));
   private static MapleDataProvider data =
         MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Mob.wz"));
   private static MapleData mobStringData = stringDataWZ.getData("Mob.img");
   private static MapleData npcStringData = stringDataWZ.getData("Npc.img");
   private static Map<Integer, MapleMonsterStats> monsterStats = new HashMap<>();
   private static Set<Integer> hpbarBosses = getHpBarBosses();

   private static Set<Integer> getHpBarBosses() {
      Set<Integer> ret = new HashSet<>();

      MapleDataProvider uiDataWZ = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/UI.wz"));
      for (MapleData bossData : uiDataWZ.getData("UIWindow.img").getChildByPath("MobGage/Mob").getChildren()) {
         ret.add(Integer.valueOf(bossData.getName()));
      }

      return ret;
   }

   public static AbstractLoadedMapleLife getLife(int id, String type) {
      if (type.equalsIgnoreCase("n")) {
         return getNPC(id);
      } else if (type.equalsIgnoreCase("m")) {
         return getMonster(id);
      } else {
         log.debug("Unknown Life type: {}", type);
         return null;
      }
   }

   private static void setMonsterAttackInfo(int mid, List<MobAttackInfoHolder> attackInfos) {
      if (!attackInfos.isEmpty()) {
         MonsterInformationProvider mi = MonsterInformationProvider.getInstance();

         for (MobAttackInfoHolder attackInfo : attackInfos) {
            mi.setMobAttackInfo(mid, attackInfo.attackPos, attackInfo.mpCon, attackInfo.coolTime);
            mi.setMobAttackAnimationTime(mid, attackInfo.attackPos, attackInfo.animationTime);
         }
      }
   }

   private static Pair<MapleMonsterStats, List<MobAttackInfoHolder>> getMonsterStats(int mid) {
      MapleData monsterData = data.getData(StringUtil.getLeftPaddedStr(mid + ".img", '0', 11));
      if (monsterData == null) {
         return null;
      }
      MapleData monsterInfoData = monsterData.getChildByPath("info");

      List<MobAttackInfoHolder> attackInfos = new LinkedList<>();
      MapleMonsterStats stats = new MapleMonsterStats();

      int linkMid = MapleDataTool.getIntConvert("link", monsterInfoData, 0);
      if (linkMid != 0) {
         Pair<MapleMonsterStats, List<MobAttackInfoHolder>> linkStats = getMonsterStats(linkMid);
         if (linkStats == null) {
            return null;
         }

         // thanks resinate for noticing non-propagable infos such as revives getting retrieved
         attackInfos.addAll(linkStats.getRight());
      }

      stats.setHp(MapleDataTool.getIntConvert("maxHP", monsterInfoData));
      stats.setFriendly(MapleDataTool.getIntConvert("damagedByMob", monsterInfoData, stats.isFriendly() ? 1 : 0) == 1);
      stats.setPADamage(MapleDataTool.getIntConvert("PADamage", monsterInfoData));
      stats.setPDDamage(MapleDataTool.getIntConvert("PDDamage", monsterInfoData));
      stats.setMADamage(MapleDataTool.getIntConvert("MADamage", monsterInfoData));
      stats.setMDDamage(MapleDataTool.getIntConvert("MDDamage", monsterInfoData));
      stats.setMp(MapleDataTool.getIntConvert("maxMP", monsterInfoData, stats.getMp()));
      stats.setExp(MapleDataTool.getIntConvert("exp", monsterInfoData, stats.getExp()));
      stats.setLevel(MapleDataTool.getIntConvert("level", monsterInfoData));
      stats.setRemoveAfter(MapleDataTool.getIntConvert("removeAfter", monsterInfoData, stats.removeAfter()));
      stats.setBoss(MapleDataTool.getIntConvert("boss", monsterInfoData, stats.isBoss() ? 1 : 0) > 0);
      stats.setExplosiveReward(
            MapleDataTool.getIntConvert("explosiveReward", monsterInfoData, stats.isExplosiveReward() ? 1 : 0) > 0);
      stats.setFfaLoot(MapleDataTool.getIntConvert("publicReward", monsterInfoData, stats.isFfaLoot() ? 1 : 0) > 0);
      stats.setUndead(MapleDataTool.getIntConvert("undead", monsterInfoData, stats.isUndead() ? 1 : 0) > 0);
      stats.setName(MapleDataTool.getString(mid + "/name", mobStringData, "MISSINGNO"));
      stats.setBuffToGive(MapleDataTool.getIntConvert("buff", monsterInfoData, stats.getBuffToGive()));
      stats.setCP(MapleDataTool.getIntConvert("getCP", monsterInfoData, stats.getCP()));
      stats.setRemoveOnMiss(MapleDataTool.getIntConvert("removeOnMiss", monsterInfoData, stats.removeOnMiss() ? 1 : 0) > 0);

      MapleData special = monsterInfoData.getChildByPath("coolDamage");
      if (special != null) {
         int coolDmg = MapleDataTool.getIntConvert("coolDamage", monsterInfoData);
         int coolProb = MapleDataTool.getIntConvert("coolDamageProb", monsterInfoData, 0);
         stats.setCool(new Pair<>(coolDmg, coolProb));
      }
      special = monsterInfoData.getChildByPath("loseItem");
      if (special != null) {
         for (MapleData liData : special.getChildren()) {
            stats.addLoseItem(new loseItem(MapleDataTool.getInt(liData.getChildByPath("id")),
                  (byte) MapleDataTool.getInt(liData.getChildByPath("prop")),
                  (byte) MapleDataTool.getInt(liData.getChildByPath("x"))));
         }
      }
      special = monsterInfoData.getChildByPath("selfDestruction");
      if (special != null) {
         stats.setSelfDestruction(new selfDestruction((byte) MapleDataTool.getInt(special.getChildByPath("action")),
               MapleDataTool.getIntConvert("removeAfter", special, -1), MapleDataTool.getIntConvert("hp", special, -1)));
      }
      MapleData firstAttackData = monsterInfoData.getChildByPath("firstAttack");
      int firstAttack = 0;
      if (firstAttackData != null) {
         if (firstAttackData.getType() == MapleDataType.FLOAT) {
            firstAttack = Math.round(MapleDataTool.getFloat(firstAttackData));
         } else {
            firstAttack = MapleDataTool.getInt(firstAttackData);
         }
      }
      stats.setFirstAttack(firstAttack > 0);
      stats.setDropPeriod(MapleDataTool.getIntConvert("dropItemPeriod", monsterInfoData, stats.getDropPeriod() / 10000) * 10000);

      // thanks yuxaij, Riizade, Z1peR, Anesthetic for noticing some bosses crashing players due to missing requirements
      boolean hpbarBoss = stats.isBoss() && hpbarBosses.contains(mid);
      stats.setTagColor(hpbarBoss ? MapleDataTool.getIntConvert("hpTagColor", monsterInfoData, 0) : 0);
      stats.setTagBgColor(hpbarBoss ? MapleDataTool.getIntConvert("hpTagBgcolor", monsterInfoData, 0) : 0);

      for (MapleData idata : monsterData) {
         if (!idata.getName().equals("info")) {
            int delay = 0;
            for (MapleData pic : idata.getChildren()) {
               delay += MapleDataTool.getIntConvert("delay", pic, 0);
            }
            stats.setAnimationTime(idata.getName(), delay);
         }
      }
      MapleData reviveInfo = monsterInfoData.getChildByPath("revive");
      if (reviveInfo != null) {
         List<Integer> revives = new LinkedList<>();
         for (MapleData data_ : reviveInfo) {
            revives.add(MapleDataTool.getInt(data_));
         }
         stats.setRevives(revives);
      }
      decodeElementalString(stats, MapleDataTool.getString("elemAttr", monsterInfoData, ""));

      MonsterInformationProvider mi = MonsterInformationProvider.getInstance();
      MapleData monsterSkillInfoData = monsterInfoData.getChildByPath("skill");
      if (monsterSkillInfoData != null) {
         int i = 0;
         List<Pair<Integer, Integer>> skills = new ArrayList<>();
         while (monsterSkillInfoData.getChildByPath(Integer.toString(i)) != null) {
            int skillId = MapleDataTool.getInt(i + "/skill", monsterSkillInfoData, 0);
            int skillLv = MapleDataTool.getInt(i + "/level", monsterSkillInfoData, 0);
            skills.add(new Pair<>(skillId, skillLv));

            MapleData monsterSkillData = monsterData.getChildByPath("skill" + (i + 1));
            if (monsterSkillData != null) {
               int animationTime = 0;
               for (MapleData effectEntry : monsterSkillData.getChildren()) {
                  animationTime += MapleDataTool.getIntConvert("delay", effectEntry, 0);
               }

               MobSkill skill = MobSkillFactory.getMobSkill(skillId, skillLv);
               mi.setMobSkillAnimationTime(skill, animationTime);
            }

            i++;
         }
         stats.setSkills(skills);
      }

      int i = 0;
      MapleData monsterAttackData;
      while ((monsterAttackData = monsterData.getChildByPath("attack" + (i + 1))) != null) {
         int animationTime = 0;
         for (MapleData effectEntry : monsterAttackData.getChildren()) {
            animationTime += MapleDataTool.getIntConvert("delay", effectEntry, 0);
         }

         int mpCon = MapleDataTool.getIntConvert("info/conMP", monsterAttackData, 0);
         int coolTime = MapleDataTool.getIntConvert("info/attackAfter", monsterAttackData, 0);
         attackInfos.add(new MobAttackInfoHolder(i, mpCon, coolTime, animationTime));
         i++;
      }

      MapleData banishData = monsterInfoData.getChildByPath("ban");
      if (banishData != null) {
         stats.setBanishInfo(
               new BanishInfo(MapleDataTool.getString("banMsg", banishData), MapleDataTool.getInt("banMap/0/field", banishData, -1),
                     MapleDataTool.getString("banMap/0/portal", banishData, "sp")));
      }

      int noFlip = MapleDataTool.getInt("noFlip", monsterInfoData, 0);
      if (noFlip > 0) {
         Point origin = MapleDataTool.getPoint("stand/0/origin", monsterData, null);
         if (origin != null) {
            stats.setFixedStance(origin.getX() < 1 ? 5 : 4);    // fixed left/right
         }
      }

      return new Pair<>(stats, attackInfos);
   }

   public static MapleMonster getMonster(int mid) {
      try {
         MapleMonsterStats stats = monsterStats.get(mid);
         if (stats == null) {
            Pair<MapleMonsterStats, List<MobAttackInfoHolder>> mobStats = getMonsterStats(mid);
            stats = mobStats.getLeft();
            setMonsterAttackInfo(mid, mobStats.getRight());

            monsterStats.put(mid, stats);
         }
         return new MapleMonster(mid, stats);
      } catch (NullPointerException npe) {
         log.error("MOB {} failed to load. Could not retrieve monster. Issue: {}", mid, npe.getMessage());
         log.error("Exception: ", npe);
         return null;
      }
   }

   public static int getMonsterLevel(int mid) {
      try {
         MapleMonsterStats stats = monsterStats.get(mid);
         if (stats == null) {
            MapleData monsterData = data.getData(StringUtil.getLeftPaddedStr(mid + ".img", '0', 11));
            if (monsterData == null) {
               return -1;
            }
            MapleData monsterInfoData = monsterData.getChildByPath("info");
            return MapleDataTool.getIntConvert("level", monsterInfoData);
         } else {
            return stats.getLevel();
         }
      } catch (NullPointerException npe) {
         log.error("MOB {} failed to load. Could not retrieve monster level. Issue: {}", mid, npe.getMessage());
         log.error("Exception: ", npe);
      }

      return -1;
   }

   private static void decodeElementalString(MapleMonsterStats stats, String elemAttr) {
      for (int i = 0; i < elemAttr.length(); i += 2) {
         stats.setEffectiveness(Element.getFromChar(elemAttr.charAt(i)),
               ElementalEffectiveness.getByNumber(Integer.parseInt(String.valueOf(elemAttr.charAt(i + 1)))));
      }
   }

   public static MapleNPC getNPC(int nid) {
      return new MapleNPC(nid, new MapleNPCStats(MapleDataTool.getString(nid + "/name", npcStringData, "MISSINGNO")));
   }

   public static String getNPCDefaultTalk(int nid) {
      return MapleDataTool.getString(nid + "/d0", npcStringData, "(...)");
   }

   private static class MobAttackInfoHolder {
      protected int attackPos;
      protected int mpCon;
      protected int coolTime;
      protected int animationTime;

      protected MobAttackInfoHolder(int attackPos, int mpCon, int coolTime, int animationTime) {
         this.attackPos = attackPos;
         this.mpCon = mpCon;
         this.coolTime = coolTime;
         this.animationTime = animationTime;
      }
   }

   public static class BanishInfo {

      private int map;
      private String portal, msg;

      public BanishInfo(String msg, int map, String portal) {
         this.msg = msg;
         this.map = map;
         this.portal = portal;
      }

      public int getMap() {
         return map;
      }

      public String getPortal() {
         return portal;
      }

      public String getMsg() {
         return msg;
      }
   }

   public static class loseItem {

      private int id;
      private byte chance, x;

      private loseItem(int id, byte chance, byte x) {
         this.id = id;
         this.chance = chance;
         this.x = x;
      }

      public int getId() {
         return id;
      }

      public byte getChance() {
         return chance;
      }

      public byte getX() {
         return x;
      }
   }

   public static class selfDestruction {

      private byte action;
      private int removeAfter;
      private int hp;

      private selfDestruction(byte action, int removeAfter, int hp) {
         this.action = action;
         this.removeAfter = removeAfter;
         this.hp = hp;
      }

      public int getHp() {
         return hp;
      }

      public byte getAction() {
         return action;
      }

      public int removeAfter() {
         return removeAfter;
      }
   }
}
