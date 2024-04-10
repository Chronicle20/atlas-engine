/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.life;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import drop.DropEntry;
import drop.DropProcessor;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.ItemInformationProvider;
import tools.Pair;

public class MonsterInformationProvider {
   private static final MonsterInformationProvider instance = new MonsterInformationProvider();
   private final Map<Integer, List<Integer>> dropsChancePool = new HashMap<>();    // thanks to ronan
   private final Map<Pair<Integer, Integer>, Integer> mobAttackAnimationTime = new HashMap<>();
   private final Map<MobSkill, Integer> mobSkillAnimationTime = new HashMap<>();
   private final Map<Integer, Pair<Integer, Integer>> mobAttackInfo = new HashMap<>();
   private final Map<Integer, Boolean> mobBossCache = new HashMap<>();
   private final Map<Integer, String> mobNameCache = new HashMap<>();

   protected MonsterInformationProvider() {
   }

   public static MonsterInformationProvider getInstance() {
      return instance;
   }

   public static List<Integer> getMobsIDsFromName(String search) {
      MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
      MapleData data = dataProvider.getData("Mob.img");
      return data.getChildren().stream()
            .filter(d -> nameMatches(d, search))
            .map(MapleData::getName)
            .map(Integer::parseInt)
            .collect(Collectors.toList());
   }

   private static boolean nameMatches(MapleData data, String search) {
      String name = MapleDataTool.getString(data.getChildByPath("name"), "NO-NAME");
      return name.toLowerCase().contains(search.toLowerCase());
   }

   public final List<Integer> retrieveDropPool(final int monsterId) {  // ignores Quest and Party Quest items
      if (dropsChancePool.containsKey(monsterId)) {
         return dropsChancePool.get(monsterId);
      }

      ItemInformationProvider ii = ItemInformationProvider.getInstance();

      List<DropEntry> dropList = DropProcessor.getInstance()
            .getDropsForMonster(monsterId);
      List<Integer> ret = new ArrayList<>();

      int accProp = 0;
      for (DropEntry mde : dropList) {
         if (!ii.isQuestItem(mde.itemId()) && !ii.isPartyQuestItem(mde.itemId())) {
            accProp += mde.chance();
         }

         ret.add(accProp);
      }

      if (accProp == 0) {
         ret.clear();    // don't accept mobs dropping no relevant items
      }
      dropsChancePool.put(monsterId, ret);
      return ret;
   }

   public final void setMobAttackAnimationTime(int monsterId, int attackPos, int animationTime) {
      mobAttackAnimationTime.put(new Pair<>(monsterId, attackPos), animationTime);
   }

   public final Integer getMobAttackAnimationTime(int monsterId, int attackPos) {
      Integer time = mobAttackAnimationTime.get(new Pair<>(monsterId, attackPos));
      return time == null ? 0 : time;
   }

   public final void setMobSkillAnimationTime(MobSkill skill, int animationTime) {
      mobSkillAnimationTime.put(skill, animationTime);
   }

   public final Integer getMobSkillAnimationTime(MobSkill skill) {
      Integer time = mobSkillAnimationTime.get(skill);
      return time == null ? 0 : time;
   }

   public final void setMobAttackInfo(int monsterId, int attackPos, int mpCon, int coolTime) {
      mobAttackInfo.put((monsterId << 3) + attackPos, new Pair<>(mpCon, coolTime));
   }

   public final Pair<Integer, Integer> getMobAttackInfo(int monsterId, int attackPos) {
      if (attackPos < 0 || attackPos > 7) {
         return null;
      }
      return mobAttackInfo.get((monsterId << 3) + attackPos);
   }

   public boolean isBoss(int id) {
      Boolean boss = mobBossCache.get(id);
      if (boss == null) {
         try {
            boss = MapleLifeFactory.getMonster(id)
                  .isBoss();
         } catch (NullPointerException npe) {
            boss = false;
         } catch (Exception e) {   //nonexistant mob
            boss = false;

            e.printStackTrace();
            System.err.println("Nonexistant mob id " + id);
         }

         mobBossCache.put(id, boss);
      }

      return boss;
   }

   public String getMobNameFromId(int id) {
      String mobName = mobNameCache.get(id);
      if (mobName == null) {
         MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
         MapleData mobData = dataProvider.getData("Mob.img");

         mobName = MapleDataTool.getString(mobData.getChildByPath(id + "/name"), "");
         mobNameCache.put(id, mobName);
      }

      return mobName;
   }

   public final void clearDrops() {
      dropsChancePool.clear();
   }
}
