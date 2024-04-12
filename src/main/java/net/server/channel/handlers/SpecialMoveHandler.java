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

import java.awt.*;

import client.MapleCharacter;
import client.MapleClient;
import client.Skill;
import client.SkillFactory;
import config.YamlConfig;
import connection.packets.CMob;
import connection.packets.CUser;
import connection.packets.CUserLocal;
import connection.packets.CWvsContext;
import constants.skills.Brawler;
import constants.skills.Corsair;
import constants.skills.DarkKnight;
import constants.skills.Hero;
import constants.skills.Paladin;
import constants.skills.Priest;
import constants.skills.SuperGM;
import door.DoorProcessor;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import server.MapleStatEffect;
import server.life.MapleMonster;
import tools.data.input.SeekableLittleEndianAccessor;

public final class SpecialMoveHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
      slea.readInt();
      chr.getAutobanManager().setTimestamp(4, Server.getInstance().getCurrentTimestamp(), 28);
      int skillid = slea.readInt();

      int __skillLevel = slea.readByte();
      Skill skill = SkillFactory.getSkill(skillid).orElseThrow();
      int skillLevel = chr.getSkillLevel(skill);
      if (skillid % 10000000 == 1010 || skillid % 10000000 == 1011) {
         if (chr.getDojoEnergy() < 10000) { // PE hacking or maybe just lagging
            return;
         }
         skillLevel = 1;
         chr.setDojoEnergy(0);
         c.announce(CWvsContext.getEnergy("energy", chr.getDojoEnergy()));
         c.announce(CWvsContext.serverNotice(5, "As you used the secret skill, your energy bar has been reset."));
      }
      if (skillLevel == 0 || skillLevel != __skillLevel) {
         return;
      }

      MapleStatEffect effect = skill.getEffect(skillLevel);
      if (effect.getCooldown() > 0) {
         if (chr.skillIsCooling(skillid)) {
            return;
         } else if (skillid != Corsair.BATTLE_SHIP) {
            int cooldownTime = effect.getCooldown();
            if (MapleStatEffect.isHerosWill(skillid) && YamlConfig.config.server.USE_FAST_REUSE_HERO_WILL) {
               cooldownTime /= 60;
            }

            c.announce(CUserLocal.skillCooldown(skillid, cooldownTime));
            chr.addCooldown(skillid, currentServerTime(), cooldownTime * 1000L);
         }
      }
      if (skillid == Hero.MONSTER_MAGNET || skillid == Paladin.MONSTER_MAGNET
            || skillid == DarkKnight.MONSTER_MAGNET) { // Monster Magnet
         int num = slea.readInt();
         for (int i = 0; i < num; i++) {
            int mobOid = slea.readInt();
            byte success = slea.readByte();
            chr.getMap().broadcastMessage(chr, CMob.catchMonster(mobOid, success), false);
            MapleMonster monster = chr.getMap().getMonsterByOid(mobOid).orElse(null);
            if (monster != null) {
               if (!monster.isBoss()) {
                  monster.aggroClearDamages();
                  monster.aggroMonsterDamage(chr, 1);
                  monster.aggroSwitchController(chr, true);
               }
            }
         }
         byte direction = slea.readByte();   // thanks MedicOP for pointing some 3rd-party related issues with Magnet
         chr.getMap()
               .broadcastMessage(chr, CUser.showBuffeffect(chr.getId(), skillid, chr.getSkillLevel(skillid), 1, direction), false);
         c.announce(CWvsContext.enableActions());
         return;
      } else if (skillid == Brawler.MP_RECOVERY) {// MP Recovery
         Skill s = SkillFactory.getSkill(skillid).orElseThrow();
         MapleStatEffect ef = s.getEffect(chr.getSkillLevel(s));

         int lose = chr.safeAddHP(-1 * (chr.getCurrentMaxHp() / ef.getX()));
         int gain = -lose * (ef.getY() / 100);
         chr.addMP(gain);
      } else if (skillid == SuperGM.HEAL_PLUS_DISPEL) {
         slea.skip(11);
         chr.getMap().broadcastMessage(chr, CUser.showBuffeffect(chr.getId(), skillid, chr.getSkillLevel(skillid)), false);
      } else if (skillid % 10000000 == 1004) {
         slea.readShort();
      }

      Point pos = null;
      if (slea.available() == 5) {
         pos = new Point(slea.readShort(), slea.readShort());
      }
      if (chr.isAlive()) {
         if (skill.id() != Priest.MYSTIC_DOOR) {
            if (skill.id() % 10000000 != 1005) {
               skill.getEffect(skillLevel).applyTo(chr, pos);
            } else {
               skill.getEffect(skillLevel).applyEchoOfHero(chr);
            }
         } else {
            if (c.tryacquireClient()) {
               try {
                  if (DoorProcessor.getInstance().canDoor(chr)) {
                     chr.cancelMagicDoor();
                     skill.getEffect(skillLevel).applyTo(chr, pos);
                  } else {
                     chr.message("Please wait 5 seconds before casting Mystic Door again.");
                  }
               } finally {
                  c.releaseClient();
               }
            }

            c.announce(CWvsContext.enableActions());
         }
      } else {
         c.announce(CWvsContext.enableActions());
      }
   }
}