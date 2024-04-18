package server.quest.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import client.MapleCharacter;
import client.MapleJob;
import client.Skill;
import client.SkillFactory;
import provider.MapleData;
import provider.MapleDataTool;
import server.quest.MapleQuest;
import server.quest.MapleQuestActionType;

public class SkillAction extends MapleQuestAction {
   Map<Integer, SkillData> skillData = new HashMap<>();

   public SkillAction(MapleQuest quest, MapleData data) {
      super(MapleQuestActionType.SKILL, quest);
      processData(data);
   }

   @Override
   public void run(MapleCharacter chr, Integer extSelection) {
      for (SkillData skill : skillData.values()) {
         Optional<Skill> skillObject = SkillFactory.getSkill(skill.id());
         if (skillObject.isEmpty()) {
            continue;
         }

         boolean shouldLearn = false;

         if (skill.jobsContains(chr.getJob()) || skillObject.get().isBeginnerSkill()) {
            shouldLearn = true;
         }

         byte skillLevel = (byte) Math.max(skill.level(), chr.getSkillLevel(skillObject.get()));
         int masterLevel = Math.max(skill.masterLevel(), chr.getMasterLevel(skillObject.get()));
         if (shouldLearn) {
            chr.changeSkillLevel(skillObject.get(), skillLevel, masterLevel, -1);
         }
      }
   }

   @Override
   public void processData(MapleData data) {
      for (MapleData sEntry : data) {
         byte skillLevel = 0;
         int skillid = MapleDataTool.getInt(sEntry.getChildByPath("id"));
         MapleData skillLevelData = sEntry.getChildByPath("skillLevel");
         if (skillLevelData != null) {
            skillLevel = (byte) MapleDataTool.getInt(skillLevelData);
         }
         int masterLevel = MapleDataTool.getInt(sEntry.getChildByPath("masterLevel"));
         List<Integer> jobs = new ArrayList<>();

         MapleData applicableJobs = sEntry.getChildByPath("job");
         if (applicableJobs != null) {
            for (MapleData applicableJob : applicableJobs.getChildren()) {
               jobs.add(MapleDataTool.getInt(applicableJob));
            }
         }

         skillData.put(skillid, new SkillData(skillid, skillLevel, masterLevel, jobs));
      }
   }

   private record SkillData(int id, int level, int masterLevel, List<Integer> jobs) {
      public boolean jobsContains(MapleJob job) {
         return jobs.contains(job.getId());
      }
   }
} 