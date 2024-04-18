package net.server.channel.handlers;

import java.util.Map;

import client.MapleCharacter;
import client.MapleClient;
import client.autoban.AutobanFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import connection.packets.CMob;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.life.MapleMonster;
import server.life.MonsterInformationProvider;
import server.maps.MapleMap;
import tools.FilePrinter;

public final class MobDamageMobHandler extends AbstractMaplePacketHandler {
   private static int calcMaxDamage(MapleMonster attacker, MapleMonster damaged, boolean magic) {
      int attackerAtk, damagedDef, attackerLevel = attacker.getLevel();
      double maxDamage;
      if (magic) {
         int atkRate = calcModifier(attacker, MonsterStatus.MAGIC_ATTACK_UP, MonsterStatus.MATK);
         attackerAtk = (attacker.getStats().getMADamage() * atkRate) / 100;

         int defRate = calcModifier(damaged, MonsterStatus.MAGIC_DEFENSE_UP, MonsterStatus.MDEF);
         damagedDef = (damaged.getStats().getMDDamage() * defRate) / 100;

         maxDamage = ((attackerAtk * (1.15 + (0.025 * attackerLevel))) - (0.75 * damagedDef)) * (
               Math.log(Math.abs(damagedDef - attackerAtk)) / Math.log(12));
      } else {
         int atkRate = calcModifier(attacker, MonsterStatus.WEAPON_ATTACK_UP, MonsterStatus.WATK);
         attackerAtk = (attacker.getStats().getPADamage() * atkRate) / 100;

         int defRate = calcModifier(damaged, MonsterStatus.WEAPON_DEFENSE_UP, MonsterStatus.WDEF);
         damagedDef = (damaged.getStats().getPDDamage() * defRate) / 100;

         maxDamage = ((attackerAtk * (1.15 + (0.025 * attackerLevel))) - (0.75 * damagedDef)) * (
               Math.log(Math.abs(damagedDef - attackerAtk)) / Math.log(17));
      }

      return (int) maxDamage;
   }

   private static int calcModifier(MapleMonster monster, MonsterStatus buff, MonsterStatus nerf) {
      int atkModifier;
      final Map<MonsterStatus, MonsterStatusEffect> monsterStati = monster.getStati();

      MonsterStatusEffect atkBuff = monsterStati.get(buff);
      if (atkBuff != null) {
         atkModifier = atkBuff.getStati().get(buff);
      } else {
         atkModifier = 100;
      }

      MonsterStatusEffect atkNerf = monsterStati.get(nerf);
      if (atkNerf != null) {
         atkModifier -= atkNerf.getStati().get(nerf);
      }

      return atkModifier;
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int from = p.readInt();
      p.readInt();
      int to = p.readInt();
      boolean magic = p.readByte() == 0;
      int dmg = p.readInt();
      MapleCharacter chr = c.getPlayer();

      MapleMap map = chr.getMap();
      MapleMonster attacker = map.getMonsterByOid(from).orElse(null);
      MapleMonster damaged = map.getMonsterByOid(to).orElse(null);

      if (attacker != null && damaged != null) {
         int maxDmg = calcMaxDamage(attacker, damaged, magic);     // thanks Darter (YungMoozi) for reporting unchecked dmg

         if (dmg > maxDmg) {
            AutobanFactory.DAMAGE_HACK.alert(c.getPlayer(),
                  "Possible packet editing hypnotize damage exploit.");   // thanks Rien dev team

            FilePrinter.printError(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt",
                  c.getPlayer().getName() + " had hypnotized " + MonsterInformationProvider.getInstance()
                        .getMobNameFromId(attacker.getId()) + " to attack " + MonsterInformationProvider.getInstance()
                        .getMobNameFromId(damaged.getId()) + " with damage " + dmg + " (max: " + maxDmg + ")");
            dmg = maxDmg;
         }

         map.damageMonster(chr, damaged, dmg);
         map.broadcastMessage(chr, CMob.damageMonster(to, dmg), false);
      }
   }
}
