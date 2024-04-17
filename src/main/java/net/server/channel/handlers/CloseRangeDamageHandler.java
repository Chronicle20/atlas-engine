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
package net.server.channel.handlers;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import client.TemporaryStatValue;
import client.TemporaryStatType;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.Skill;
import client.SkillFactory;
import config.YamlConfig;
import connection.packets.CUserLocal;
import connection.packets.CUserRemote;
import connection.packets.CWvsContext;
import constants.game.GameConstants;
import constants.skills.Crusader;
import constants.skills.DawnWarrior;
import constants.skills.DragonKnight;
import constants.skills.Hero;
import constants.skills.NightWalker;
import constants.skills.Rogue;
import constants.skills.WindArcher;
import server.MapleStatEffect;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;

public final class CloseRangeDamageHandler extends AbstractDealDamageHandler {

   @Override
   public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
        
        /*long timeElapsed = currentServerTime() - chr.getAutobanManager().getLastSpam(8);
        if(timeElapsed < 300) {
                AutobanFactory.FAST_ATTACK.alert(chr, "Time: " + timeElapsed);
        }
        chr.getAutobanManager().spam(8);*/

      AttackInfo attack = parseDamage(slea, chr, false, false);
      if (chr.getBuffEffect(TemporaryStatType.MORPH) != null) {
         if (chr.getBuffEffect(TemporaryStatType.MORPH).isMorphWithoutAttack()) {
            // How are they attacking when the client won't let them?
            chr.getClient().disconnect(false, false);
            return;
         }
      }

      if (chr.getDojoEnergy() < 10000 && (attack.skill == 1009 || attack.skill == 10001009
            || attack.skill == 20001009)) // PE hacking or maybe just lagging
      {
         return;
      }
      if (GameConstants.isDojo(chr.getMap().getId()) && attack.numAttacked > 0) {
         chr.setDojoEnergy(chr.getDojoEnergy() + YamlConfig.config.server.DOJO_ENERGY_ATK);
         c.announce(CWvsContext.getEnergy("energy", chr.getDojoEnergy()));
      }

      chr.getMap().broadcastMessage(chr,
            CUserRemote.closeRangeAttack(chr, attack.skill, attack.skilllevel, attack.stance, attack.numAttackedAndDamage,
                  attack.allDamage, attack.speed, attack.direction, attack.display), false, true);
      int numFinisherOrbs = 0;
      Integer comboBuff = chr.getBuffedValue(TemporaryStatType.COMBO);
      if (GameConstants.isFinisherSkill(attack.skill)) {
         if (comboBuff != null) {
            numFinisherOrbs = comboBuff - 1;
         }
         chr.handleOrbconsume();
      } else if (attack.numAttacked > 0) {
         if (attack.skill != 1111008 && comboBuff != null) {
            int orbcount = chr.getBuffedValue(TemporaryStatType.COMBO);
            int oid = chr.isCygnus() ? DawnWarrior.COMBO : Crusader.COMBO;
            int advcomboid = chr.isCygnus() ? DawnWarrior.ADVANCED_COMBO : Hero.ADVANCED_COMBO;
            Skill combo = SkillFactory.getSkill(oid).orElseThrow();
            Skill advcombo = SkillFactory.getSkill(advcomboid).orElseThrow();
            MapleStatEffect ceffect;
            int advComboSkillLevel = chr.getSkillLevel(advcombo);
            if (advComboSkillLevel > 0) {
               ceffect = advcombo.getEffect(advComboSkillLevel);
            } else {
               int comboLv = chr.getSkillLevel(combo);
               if (comboLv <= 0 || chr.isGM()) {
                  comboLv = SkillFactory.getSkill(oid).orElseThrow().getMaxLevel();
               }

               if (comboLv > 0) {
                  ceffect = combo.getEffect(comboLv);
               } else {
                  ceffect = null;
               }
            }
            if (ceffect != null) {
               if (orbcount < ceffect.getX() + 1) {
                  int neworbcount = orbcount + 1;
                  if (advComboSkillLevel > 0 && ceffect.makeChanceResult()) {
                     if (neworbcount <= ceffect.getX()) {
                        neworbcount++;
                     }
                  }

                  int olv = chr.getSkillLevel(oid);
                  if (olv <= 0) {
                     olv = SkillFactory.getSkill(oid).orElseThrow().getMaxLevel();
                  }

                  int duration = combo.getEffect(olv).getDuration();
                  List<Pair<TemporaryStatType, TemporaryStatValue>> stat = Collections.singletonList(new Pair<>(TemporaryStatType.COMBO,
                        new TemporaryStatValue(0, 0, neworbcount)));
                  chr.setBuffedValue(TemporaryStatType.COMBO, neworbcount);
                  duration -= (int) (currentServerTime() - chr.getBuffedStarttime(TemporaryStatType.COMBO));
                  c.announce(CWvsContext.giveBuff(chr, oid, duration, stat));
                  chr.getMap().broadcastMessage(chr, CUserRemote.giveForeignBuff(chr, stat), false);
               }
            }
         } else if (chr.getSkillLevel(
               chr.isCygnus() ? SkillFactory.getSkill(15100004).orElseThrow() : SkillFactory.getSkill(5110001).orElseThrow()) > 0
               && (chr.getJob().isA(MapleJob.MARAUDER) || chr.getJob().isA(MapleJob.THUNDERBREAKER2))) {
            for (int i = 0; i < attack.numAttacked; i++) {
               chr.handleEnergyChargeGain();
            }
         }
      }
      if (attack.numAttacked > 0 && attack.skill == DragonKnight.SACRIFICE) {
         int totDamageToOneMonster = 0; // sacrifice attacks only 1 mob with 1 attack
         final Iterator<List<Integer>> dmgIt = attack.allDamage.values().iterator();
         if (dmgIt.hasNext()) {
            totDamageToOneMonster = dmgIt.next().getFirst();
         }

         chr.safeAddHP(-1 * totDamageToOneMonster * attack.getAttackEffect(chr, null).orElseThrow().getX() / 100);
      }
      if (attack.numAttacked > 0 && attack.skill == 1211002) {
         boolean advcharge_prob = false;
         int advcharge_level = chr.getSkillLevel(SkillFactory.getSkill(1220010).orElseThrow());
         if (advcharge_level > 0) {
            advcharge_prob = SkillFactory.getSkill(1220010).orElseThrow().getEffect(advcharge_level).makeChanceResult();
         }
         if (!advcharge_prob) {
            chr.cancelEffectFromBuffStat(TemporaryStatType.WK_CHARGE);
         }
      }
      int attackCount = 1;
      if (attack.skill != 0) {
         attackCount = attack.getAttackEffect(chr, null).orElseThrow().getAttackCount();
      }
      if (numFinisherOrbs == 0 && GameConstants.isFinisherSkill(attack.skill)) {
         return;
      }
      if (attack.skill % 10000000 == 1009) { // bamboo
         if (chr.getDojoEnergy() < 10000) { // PE hacking or maybe just lagging
            return;
         }

         chr.setDojoEnergy(0);
         c.announce(CWvsContext.getEnergy("energy", chr.getDojoEnergy()));
         c.announce(CWvsContext.serverNotice(5, "As you used the secret skill, your energy bar has been reset."));
      } else if (attack.skill > 0) {
         Skill skill = SkillFactory.getSkill(attack.skill).orElseThrow();
         MapleStatEffect effect_ = skill.getEffect(chr.getSkillLevel(skill));
         if (effect_.getCooldown() > 0) {
            if (chr.skillIsCooling(attack.skill)) {
               return;
            } else {
               c.announce(CUserLocal.skillCooldown(attack.skill, effect_.getCooldown()));
               chr.addCooldown(attack.skill, currentServerTime(), effect_.getCooldown() * 1000L);
            }
         }
      }
      if ((chr.getSkillLevel(SkillFactory.getSkill(NightWalker.VANISH).orElseThrow()) > 0
            || chr.getSkillLevel(SkillFactory.getSkill(Rogue.DARK_SIGHT).orElseThrow()) > 0)
            && chr.getBuffedValue(TemporaryStatType.DARKSIGHT) != null) {// && chr.getBuffSource(TemporaryStatType.DARKSIGHT) != 9101004
         chr.cancelEffectFromBuffStat(TemporaryStatType.DARKSIGHT);
         chr.cancelBuffStats(TemporaryStatType.DARKSIGHT);
      } else if (chr.getSkillLevel(SkillFactory.getSkill(WindArcher.WIND_WALK).orElseThrow()) > 0
            && chr.getBuffedValue(TemporaryStatType.WIND_WALK) != null) {
         chr.cancelEffectFromBuffStat(TemporaryStatType.WIND_WALK);
         chr.cancelBuffStats(TemporaryStatType.WIND_WALK);
      }

      applyAttack(attack, chr, attackCount);
   }
}