package tools.exceptions;

import net.packet.InPacket;

/**
 * @author Ronan
 */
public class EmptyMovementException extends Exception {

   public EmptyMovementException(InPacket p) {
      super("Empty movement: " + p);
   }
}
