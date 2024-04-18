package connection.constants;

import java.util.Arrays;
import java.util.Optional;

public enum WorldState {
   NORMAL(0),
   EVENT(1),
   NEW(2),
   HOT(3);

   final byte state;

   WorldState(int state) {
      this.state = (byte) state;
   }

   public static Optional<WorldState> fromState(byte state) {
      return Arrays.stream(WorldState.values()).filter(x -> x.getState() == state).findFirst();
   }

   public byte getState() {
      return state;
   }
}
