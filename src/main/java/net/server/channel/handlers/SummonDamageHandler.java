package net.server.channel.handlers;

import java.util.ArrayList;
import java.util.List;

import client.MapleCharacter;
import client.MapleClient;
import client.Skill;
import client.SkillFactory;
import client.autoban.AutobanFactory;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import client.status.MonsterStatusEffect;
import connection.packets.CSummonedPool;
import constants.skills.Outlaw;
import net.packet.InPacket;
import server.ItemInformationProvider;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.life.MonsterInformationProvider;
import server.maps.MapleSummon;
import tools.FilePrinter;

public final class SummonDamageHandler extends AbstractDealDamageHandler {

   private static int calcMaxDamage(MapleStatEffect summonEffect, MapleCharacter player, boolean magic) {
      double maxDamage;

      if (magic) {
         int matk = Math.max(player.getTotalMagic(), 14);
         maxDamage = player.calculateMaxBaseMagicDamage(matk) * (0.05 * summonEffect.getMatk());
      } else {
         int watk = Math.max(player.getTotalWatk(), 14);
         Item weapon_item = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);

         int maxBaseDmg;  // thanks Conrad, Atoot for detecting some summons legitimately hitting over the calculated limit
         if (weapon_item != null) {
            maxBaseDmg =
                  player.calculateMaxBaseDamage(watk, ItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId()));
         } else {
            maxBaseDmg = player.calculateMaxBaseDamage(watk, MapleWeaponType.SWORD1H);
         }

         float summonDmgMod = (maxBaseDmg >= 438) ? 0.054f : 0.077f;
         maxDamage = maxBaseDmg * (summonDmgMod * summonEffect.getWatk());
      }

      return (int) maxDamage;
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int oid = p.readInt();
      MapleCharacter player = c.getPlayer();
      if (!player.isAlive()) {
         return;
      }
      MapleSummon summon = null;
      for (MapleSummon sum : player.getSummonsValues()) {
         if (sum.getObjectId() == oid) {
            summon = sum;
         }
      }
      if (summon == null) {
         return;
      }
      Skill summonSkill = SkillFactory.getSkill(summon.getSkill()).orElseThrow();
      MapleStatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());
      p.skip(4);
      List<SummonAttackEntry> allDamage = new ArrayList<>();
      byte direction = p.readByte();
      int numAttacked = p.readByte();
      p.skip(8); // I failed lol (mob x,y and summon x,y), Thanks Gerald
      for (int x = 0; x < numAttacked; x++) {
         int monsterOid = p.readInt(); // attacked oid
         p.skip(18);
         int damage = p.readInt();
         allDamage.add(new SummonAttackEntry(monsterOid, damage));
      }
      player.getMap()
            .broadcastMessage(player, CSummonedPool.summonAttack(player.getId(), summon.getObjectId(), direction, allDamage),
                  summon.getPosition());

      if (player.getMap().isOwnershipRestricted(player)) {
         return;
      }

      boolean magic = summonEffect.getWatk() == 0;
      int maxDmg = calcMaxDamage(summonEffect, player, magic);    // thanks Darter (YungMoozi) for reporting unchecked max dmg
      for (SummonAttackEntry attackEntry : allDamage) {
         int damage = attackEntry.getDamage();
         MapleMonster target = player.getMap().getMonsterByOid(attackEntry.getMonsterOid()).orElse(null);
         if (target != null) {
            if (damage > maxDmg) {
               AutobanFactory.DAMAGE_HACK.alert(c.getPlayer(), "Possible packet editing summon damage exploit.");

               FilePrinter.printError(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt",
                     c.getPlayer().getName() + " used a summon of skillid " + summon.getSkill() + " to attack "
                           + MonsterInformationProvider.getInstance().getMobNameFromId(target.getId()) + " with damage " + damage
                           + " (max: " + maxDmg + ")");
               damage = maxDmg;
            }

            if (damage > 0 && !summonEffect.getMonsterStati().isEmpty()) {
               if (summonEffect.makeChanceResult()) {
                  target.applyStatus(player, new MonsterStatusEffect(summonEffect.getMonsterStati(), summonSkill, null, false),
                        summonEffect.isPoison(), 4000);
               }
            }
            player.getMap().damageMonster(player, target, damage);
         }
      }

      if (summon.getSkill() == Outlaw.GAVIOTA) {  // thanks Periwinks for noticing Gaviota not cancelling after grenade toss
         player.cancelEffect(summonEffect, false, -1);
      }
   }

   public final class SummonAttackEntry {

      private int monsterOid;
      private int damage;

      public SummonAttackEntry(int monsterOid, int damage) {
         this.monsterOid = monsterOid;
         this.damage = damage;
      }

      public int getMonsterOid() {
         return monsterOid;
      }

      public int getDamage() {
         return damage;
      }
   }
}
