package buddy;

public record BuddyListEntry(String name, String group, int characterId, int channel, boolean visible) {
   public BuddyListEntry setChannel(int channel) {
      return new BuddyListEntry(name, group, characterId, channel, visible);
   }

   public boolean isOnline() {
      return channel >= 0;
   }

   public BuddyListEntry changeGroup(String group) {
      return new BuddyListEntry(name, group, characterId, channel, visible);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + characterId;
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      final BuddyListEntry other = (BuddyListEntry) obj;
      return characterId == other.characterId();
   }
}
