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
import net.packet.InPacket;
import net.server.Server;
import server.MapleStatEffect;
import server.life.MapleMonster;

public final class SpecialMoveHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
      p.readInt();
      chr.getAutobanManager().setTimestamp(4, Server.getInstance().getCurrentTimestamp(), 28);
      int skillid = p.readInt();

      int __skillLevel = p.readByte();
      Skill skill = SkillFactory.getSkill(skillid).orElseThrow();
      int skillLevel = chr.getSkillLevel(skill);
      if (skillid % 10000000 == 1010 || skillid % 10000000 == 1011) {
         if (chr.getDojoEnergy() < 10000) { // PE hacking or maybe just lagging
            return;
         }
         skillLevel = 1;
         chr.setDojoEnergy(0);
         c.sendPacket(CWvsContext.getEnergy("energy", chr.getDojoEnergy()));
         c.sendPacket(CWvsContext.serverNotice(5, "As you used the secret skill, your energy bar has been reset."));
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

            c.sendPacket(CUserLocal.skillCooldown(skillid, cooldownTime));
            chr.addCooldown(skillid, currentServerTime(), cooldownTime * 1000L);
         }
      }
      if (skillid == Hero.MONSTER_MAGNET || skillid == Paladin.MONSTER_MAGNET
            || skillid == DarkKnight.MONSTER_MAGNET) { // Monster Magnet
         int num = p.readInt();
         for (int i = 0; i < num; i++) {
            int mobOid = p.readInt();
            byte success = p.readByte();
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
         byte direction = p.readByte();   // thanks MedicOP for pointing some 3rd-party related issues with Magnet
         chr.getMap()
               .broadcastMessage(chr, CUser.showBuffeffect(chr.getId(), skillid, chr.getSkillLevel(skillid), 1, direction), false);
         c.sendPacket(CWvsContext.enableActions());
         return;
      } else if (skillid == Brawler.MP_RECOVERY) {// MP Recovery
         Skill s = SkillFactory.getSkill(skillid).orElseThrow();
         MapleStatEffect ef = s.getEffect(chr.getSkillLevel(s));

         int lose = chr.safeAddHP(-1 * (chr.getCurrentMaxHp() / ef.getX()));
         int gain = -lose * (ef.getY() / 100);
         chr.addMP(gain);
      } else if (skillid == SuperGM.HEAL_PLUS_DISPEL) {
         p.skip(11);
         chr.getMap().broadcastMessage(chr, CUser.showBuffeffect(chr.getId(), skillid, chr.getSkillLevel(skillid)), false);
      } else if (skillid % 10000000 == 1004) {
         p.readShort();
      }

      Point pos = null;
      if (p.available() == 5) {
         pos = new Point(p.readShort(), p.readShort());
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

            c.sendPacket(CWvsContext.enableActions());
         }
      } else {
         c.sendPacket(CWvsContext.enableActions());
      }
   }
}