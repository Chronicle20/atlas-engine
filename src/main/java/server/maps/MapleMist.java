package server.maps;

import java.awt.*;

import client.MapleCharacter;
import client.MapleClient;
import client.Skill;
import client.SkillFactory;
import connection.packets.CAffectedAreaPool;
import constants.skills.BlazeWizard;
import constants.skills.Evan;
import constants.skills.FPMage;
import constants.skills.NightWalker;
import constants.skills.Shadower;
import net.packet.Packet;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.life.MobSkill;

public class MapleMist extends AbstractMapleMapObject {
   private Rectangle mistPosition;
   private MapleCharacter owner = null;
   private MapleMonster mob = null;
   private MapleStatEffect source;
   private MobSkill skill;
   private boolean isMobMist, isPoisonMist, isRecoveryMist;
   private int skillDelay;

   public MapleMist(Rectangle mistPosition, MapleMonster mob, MobSkill skill) {
      this.mistPosition = mistPosition;
      this.mob = mob;
      this.skill = skill;
      isMobMist = true;
      isPoisonMist = true;
      isRecoveryMist = false;
      skillDelay = 0;
   }

   public MapleMist(Rectangle mistPosition, MapleCharacter owner, MapleStatEffect source) {
      this.mistPosition = mistPosition;
      this.owner = owner;
      this.source = source;
      this.skillDelay = 8;
      this.isMobMist = false;
      this.isRecoveryMist = false;
      this.isPoisonMist = false;
      switch (source.getSourceId()) {
         case Evan.RECOVERY_AURA:
            isRecoveryMist = true;
            break;

         case Shadower.SMOKE_SCREEN: // Smoke Screen
            isPoisonMist = false;
            break;

         case FPMage.POISON_MIST: // FP mist
         case BlazeWizard.FLAME_GEAR: // Flame Gear
         case NightWalker.POISON_BOMB: // Poison Bomb
            isPoisonMist = true;
            break;
      }
   }

   @Override
   public MapleMapObjectType getType() {
      return MapleMapObjectType.MIST;
   }

   @Override
   public Point getPosition() {
      return mistPosition.getLocation();
   }

   @Override
   public void setPosition(Point position) {
      throw new UnsupportedOperationException();
   }

   public Skill getSourceSkill() {
      return SkillFactory.getSkill(source.getSourceId()).orElseThrow();
   }

   public boolean isMobMist() {
      return isMobMist;
   }

   public boolean isPoisonMist() {
      return isPoisonMist;
   }

   public boolean isRecoveryMist() {
      return isRecoveryMist;
   }

   public int getSkillDelay() {
      return skillDelay;
   }

   public MapleMonster getMobOwner() {
      return mob;
   }

   public MapleCharacter getOwner() {
      return owner;
   }

   public Rectangle getBox() {
      return mistPosition;
   }

   public Packet makeDestroyData() {
      return CAffectedAreaPool.removeMist(getObjectId());
   }

   public Packet makeSpawnData() {
      if (owner != null) {
         return CAffectedAreaPool.spawnMist(getObjectId(), owner.getId(), getSourceSkill().id(),
               owner.getSkillLevel(SkillFactory.getSkill(source.getSourceId()).orElseThrow()), this);
      }
      return CAffectedAreaPool.spawnMist(getObjectId(), mob.getId(), skill.getSkillId(), skill.getSkillLevel(), this);
   }

   public Packet makeFakeSpawnData(int level) {
      if (owner != null) {
         return CAffectedAreaPool.spawnMist(getObjectId(), owner.getId(), getSourceSkill().id(), level, this);
      }
      return CAffectedAreaPool.spawnMist(getObjectId(), mob.getId(), skill.getSkillId(), skill.getSkillLevel(), this);
   }

   @Override
   public void sendSpawnData(MapleClient client) {
      client.sendPacket(makeSpawnData());
   }

   @Override
   public void sendDestroyData(MapleClient client) {
      client.sendPacket(makeDestroyData());
   }

   public boolean makeChanceResult() {
      return source.makeChanceResult();
   }
}
