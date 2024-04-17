package client.inventory;

public enum MapleWeaponType {
   NOT_A_WEAPON(0),
   GENERAL1H_SWING(4.4),
   GENERAL1H_STAB(3.2),
   GENERAL2H_SWING(4.8),
   GENERAL2H_STAB(3.4),
   BOW(3.4),
   CLAW(3.6),
   CROSSBOW(3.6),
   DAGGER_THIEVES(3.6),
   DAGGER_OTHER(4),
   GUN(3.6),
   KNUCKLE(4.8),
   POLE_ARM_SWING(5.0),
   POLE_ARM_STAB(3.0),
   SPEAR_STAB(5.0),
   SPEAR_SWING(3.0),
   STAFF(3.6),
   SWORD1H(4.0),
   SWORD2H(4.6),
   WAND(3.6);
   private final double damageMultiplier;

   MapleWeaponType(double maxDamageMultiplier) {
      this.damageMultiplier = maxDamageMultiplier;
   }

   public double getMaxDamageMultiplier() {
      return damageMultiplier;
   }
}
