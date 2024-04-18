package server.movement;

import java.awt.*;

import net.packet.InPacket;
import net.packet.OutPacket;

public class Elem {

   private Point startPosition;

   private byte bMoveAction;
   private byte bStat;
   private short x;
   private short y;
   private short vx;
   private short vy;
   private short fh;
   private short fhFallStart;
   private short xOffset;
   private short yOffset;
   private short tElapse;
   private byte type;

   public Elem(Point startPosition) {
      this.startPosition = startPosition;
   }

   public Elem(Point startPosition, byte bMoveAction, byte bStat, short x, short y, short vx, short vy, short fh, short fhFallStart,
               short xOffset, short yOffset, short tElapse, byte type) {
      this.startPosition = startPosition;
      this.bMoveAction = bMoveAction;
      this.bStat = bStat;
      this.x = x;
      this.y = y;
      this.vx = vx;
      this.vy = vy;
      this.fh = fh;
      this.fhFallStart = fhFallStart;
      this.xOffset = xOffset;
      this.yOffset = yOffset;
      this.tElapse = tElapse;
      this.type = type;
   }

   public Point getPosition(short yOffset) {
      return new Point(x, y + yOffset);
   }

   public byte getType() {
      return type;
   }

   public byte getBMoveAction() {
      return bMoveAction;
   }

   public void decode(InPacket p) {
      type = p.readByte();

      switch (type) {
         case 0: // normal move
         case 5:
         case 0xF:
         case 0x11:
         case 0x1F:
         case 0x20:
            x = p.readShort();
            y = p.readShort();
            vx = p.readShort();
            vy = p.readShort();
            fh = p.readShort();
            if (type == 15) {
               fhFallStart = p.readShort();
            }
            xOffset = p.readShort();
            yOffset = p.readShort();
            bMoveAction = p.readByte();
            tElapse = p.readShort();
            break;
         case 3:
         case 4:
         case 7:
         case 8:
         case 9:
         case 0xB:
            x = p.readShort();
            y = p.readShort();
            fh = p.readShort();
            vx = 0;
            vy = 0;
            bMoveAction = p.readByte();
            tElapse = p.readShort();
            break;
         case 0xE:
            x = (short) startPosition.x;
            y = (short) startPosition.y;
            vx = p.readShort();
            vy = p.readShort();
            fhFallStart = p.readShort();
            bMoveAction = p.readByte();
            tElapse = p.readShort();
            break;
         case 0x18:
            x = p.readShort();
            y = p.readShort();
            vx = p.readShort();
            vy = p.readShort();
            bMoveAction = p.readByte();
            tElapse = p.readShort();
            break;
         case 1:
         case 2:
         case 6:
         case 0xC:
         case 0xD:
         case 0x10:
         case 0x12:
         case 0x13:
         case 0x14:
         case 0x17:
         case 0x19:
         case 0x1B:
         case 0x1C:
         case 0x1D:
         case 0x1E:
            fh = 0;
            x = (short) startPosition.x;
            y = (short) startPosition.y;
            vx = p.readShort();
            vy = p.readShort();
            bMoveAction = p.readByte();
            tElapse = p.readShort();
            break;
         case 0xA:
            bStat = p.readByte();
            break;
         default:
            bMoveAction = p.readByte();
            tElapse = p.readShort();
            break;
      }
   }

   public void encode(OutPacket p) {
      p.writeByte(type);
      switch (type) {
         case 0: // normal move
         case 5:
         case 0xF:
         case 0x11:
         case 0x1F:
         case 0x20:
            p.writeShort(x);
            p.writeShort(y);
            p.writeShort(vx);
            p.writeShort(vy);
            p.writeShort(fh);
            if (type == 15) {
               p.writeShort(fhFallStart);
            }
            p.writeShort(xOffset);
            p.writeShort(yOffset);
            p.writeByte(bMoveAction);
            p.writeShort(tElapse);
            break;
         case 3:
         case 4:
         case 7:
         case 8:
         case 9:
         case 0xB:
            p.writeShort(x);
            p.writeShort(y);
            p.writeShort(fh);
            p.writeByte(bMoveAction);
            p.writeShort(tElapse);
            break;
         case 0xE:
            p.writeShort(vx);
            p.writeShort(vy);
            p.writeShort(fhFallStart);
            p.writeByte(bMoveAction);
            p.writeShort(tElapse);
            break;
         case 0x18:
            p.writeShort(x);
            p.writeShort(y);
            p.writeShort(vx);
            p.writeShort(vy);
            p.writeByte(bMoveAction);
            p.writeShort(tElapse);
            break;
         case 1:
         case 2:
         case 6:
         case 0xC:
         case 0xD:
         case 0x10:
         case 0x12:
         case 0x13:
         case 0x14:
         case 0x17:
         case 0x19:
         case 0x1B:
         case 0x1C:
         case 0x1D:
         case 0x1E:
            p.writeShort(vx);
            p.writeShort(vy);
            p.writeByte(bMoveAction);
            p.writeShort(tElapse);
            break;
         case 0xA:
            p.writeByte(bStat);
            break;
         default:
            p.writeByte(bMoveAction);
            p.writeShort(tElapse);
            break;
      }
   }
}
