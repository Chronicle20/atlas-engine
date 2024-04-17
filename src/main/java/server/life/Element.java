package server.life;

public enum Element {
   NEUTRAL(0), PHYSICAL(1), FIRE(2, true), ICE(3, true), LIGHTING(4), POISON(5), HOLY(6, true), DARKNESS(7);

   private final int value;
   private final boolean special;

   Element(int v) {
      this.value = v;
      this.special = false;
   }

   Element(int v, boolean special) {
      this.value = v;
      this.special = special;
   }

   public static Element getFromChar(char c) {
      return switch (Character.toUpperCase(c)) {
         case 'F' -> FIRE;
         case 'I' -> ICE;
         case 'L' -> LIGHTING;
         case 'S' -> POISON;
         case 'H' -> HOLY;
         case 'D' -> DARKNESS;
         case 'P' -> NEUTRAL;
         default -> throw new IllegalArgumentException("unknown elemnt char " + c);
      };
   }

   public boolean isSpecial() {
      return special;
   }

   public int getValue() {
      return value;
   }
}
