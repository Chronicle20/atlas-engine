package server.movement;

import java.awt.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.packet.InPacket;
import net.packet.OutPacket;

public class MovePath {
   private final List<Elem> lElem = new LinkedList<>();
   private Point startPosition;

   public static MovePath idle(Point position, byte stance) {
      MovePath movePath = new MovePath();
      movePath.startPosition = position;
      movePath.lElem.add(
            new Elem(position, stance, (byte) 0, (short) position.x, (short) position.y, (short) 0, (short) 0, (short) 0, (short) 0,
                  (short) 0, (short) 0, (short) 0, (byte) 0));
      return movePath;
   }

   public void decode(InPacket p) {
      startPosition = p.readPos();
      byte size = p.readByte();
      for (int i = 0; i < size; i++) {
         Elem elem = new Elem(startPosition);
         elem.decode(p);
         lElem.add(elem);
      }
   }

   public void encode(OutPacket p) {
      p.writePos(startPosition);
      p.writeByte(lElem.size());
      for (Elem elem : lElem) {
         elem.encode(p);
      }
   }

   public List<Elem> Movement() {
      return Collections.unmodifiableList(lElem);
   }
}
