package net.server.channel.handlers;

import java.util.Optional;

import client.MapleCharacter;
import client.MapleClient;
import client.Skill;
import client.SkillFactory;
import client.TemporaryStatType;
import config.YamlConfig;
import connection.packets.CUserLocal;
import connection.packets.CUserRemote;
import connection.packets.CWvsContext;
import constants.game.GameConstants;
import constants.skills.Bishop;
import constants.skills.Evan;
import constants.skills.FPArchMage;
import constants.skills.ILArchMage;
import net.packet.InPacket;
import net.packet.Packet;
import server.MapleStatEffect;

public final class MagicDamageHandler extends AbstractDealDamageHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();

		/*long timeElapsed = currentServerTime() - chr.getAutobanManager().getLastSpam(8);
		if(timeElapsed < 300) {
			AutobanFactory.FAST_ATTACK.alert(chr, "Time: " + timeElapsed);
		}
		chr.getAutobanManager().spam(8);*/

      AttackInfo attack = parseDamage(p, chr, false, true);

      if (chr.getBuffEffect(TemporaryStatType.MORPH) != null) {
         if (chr.getBuffEffect(TemporaryStatType.MORPH).isMorphWithoutAttack()) {
            // How are they attacking when the client won't let them?
            chr.getClient().disconnect(false, false);
            return;
         }
      }

      if (GameConstants.isDojo(chr.getMap().getId()) && attack.numAttacked > 0) {
         chr.setDojoEnergy(chr.getDojoEnergy() + YamlConfig.config.server.DOJO_ENERGY_ATK);
         c.sendPacket(CWvsContext.getEnergy("energy", chr.getDojoEnergy()));
      }

      int charge = (attack.skill == Evan.FIRE_BREATH || attack.skill == Evan.ICE_BREATH || attack.skill == FPArchMage.BIG_BANG
            || attack.skill == ILArchMage.BIG_BANG || attack.skill == Bishop.BIG_BANG) ? attack.charge : -1;
      Packet packet = CUserRemote.magicAttack(chr, attack.skill, attack.skilllevel, attack.stance, attack.numAttackedAndDamage,
            attack.allDamage, charge, attack.speed, attack.direction, attack.display);

      chr.getMap().broadcastMessage(chr, packet, false, true);
      MapleStatEffect effect = attack.getAttackEffect(chr, null).orElseThrow();
      Skill skill = SkillFactory.getSkill(attack.skill).orElseThrow();
      MapleStatEffect effect_ = skill.getEffect(chr.getSkillLevel(skill));
      if (effect_.getCooldown() > 0) {
         if (chr.skillIsCooling(attack.skill)) {
            return;
         } else {
            c.sendPacket(CUserLocal.skillCooldown(attack.skill, effect_.getCooldown()));
            chr.addCooldown(attack.skill, currentServerTime(), effect_.getCooldown() * 1000L);
         }
      }
      applyAttack(attack, chr, effect.getAttackCount());
      Skill eaterSkill = SkillFactory.getSkill((chr.getJob().getId() - (chr.getJob().getId() % 10)) * 10000)
            .orElseThrow();// MP Eater, works with right job
      int eaterLevel = chr.getSkillLevel(eaterSkill);
      if (eaterLevel > 0) {
         attack.allDamage.keySet().stream().map(id -> chr.getMap().getMapObject(id)).flatMap(Optional::stream)
               .forEach(o -> eaterSkill.getEffect(eaterLevel).applyPassive(chr, o, 0));
      }
   }
}
