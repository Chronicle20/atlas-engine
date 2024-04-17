package server.expeditions;

import config.YamlConfig;

public enum MapleExpeditionType {

   BALROG_EASY(3, 30, 50, 255, 5),
   BALROG_NORMAL(6, 30, 50, 255, 5),
   SCARGA(6, 30, 100, 255, 5),
   SHOWA(3, 30, 100, 255, 5),
   ZAKUM(6, 30, 50, 255, 5),
   HORNTAIL(6, 30, 100, 255, 5),
   CHAOS_ZAKUM(6, 30, 120, 255, 5),
   CHAOS_HORNTAIL(6, 30, 120, 255, 5),
   ARIANT(2, 7, 20, 30, 5),
   ARIANT1(2, 7, 20, 30, 5),
   ARIANT2(2, 7, 20, 30, 5),
   PINKBEAN(6, 30, 120, 255, 5),
   CWKPQ(6, 30, 90, 255, 5);   // CWKPQ min-level 90, found thanks to Cato

   private final int minSize;
   private final int maxSize;
   private final int minLevel;
   private final int maxLevel;
   private final int registrationTime;

   MapleExpeditionType(int minSize, int maxSize, int minLevel, int maxLevel, int minutes) {
      this.minSize = minSize;
      this.maxSize = maxSize;
      this.minLevel = minLevel;
      this.maxLevel = maxLevel;
      this.registrationTime = minutes;
   }

   public int getMinSize() {
      return !YamlConfig.config.server.USE_ENABLE_SOLO_EXPEDITIONS ? minSize : 1;
   }

   public int getMaxSize() {
      return maxSize;
   }

   public int getMinLevel() {
      return minLevel;
   }

   public int getMaxLevel() {
      return maxLevel;
   }

   public int getRegistrationTime() {
      return registrationTime;
   }
}
