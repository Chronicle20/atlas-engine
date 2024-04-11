package client;

public record SkillEntry(byte skillLevel, int masterLevel, long expiration) {
   @Override
   public String toString() {
      return skillLevel + ":" + masterLevel;
   }
}
