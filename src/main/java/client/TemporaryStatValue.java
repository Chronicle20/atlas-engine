package client;

public record TemporaryStatValue(int sourceId, int sourceLevel, int value) {
   public static TemporaryStatValue empty() {
      return new TemporaryStatValue(0, 0, 0);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      final TemporaryStatValue other = (TemporaryStatValue) obj;
      if (this.sourceLevel != other.sourceLevel) {
         return false;
      }
      if (this.sourceId != other.sourceId) {
         return false;
      }
      return this.value == other.value;
   }

   @Override
   public int hashCode() {
      int hash = 7;
      hash = 89 * hash + this.sourceLevel;
      hash = 89 * hash + this.sourceId;
      hash = 89 * hash + this.value;
      return hash;
   }
}