package server.movement;

import tools.data.input.LittleEndianAccessor;
import tools.data.output.LittleEndianWriter;

import java.awt.*;

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

    public Elem(Point startPosition, byte bMoveAction, byte bStat, short x, short y, short vx, short vy, short fh, short fhFallStart, short xOffset, short yOffset, short tElapse, byte type) {
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

    public void decode(LittleEndianAccessor lea) {
        type = lea.readByte();

        switch (type) {
            case 0: // normal move
            case 5:
            case 0xF:
            case 0x11:
            case 0x1F:
            case 0x20:
                x = lea.readShort();
                y = lea.readShort();
                vx = lea.readShort();
                vy = lea.readShort();
                fh = lea.readShort();
                if (type == 15) {
                    fhFallStart = lea.readShort();
                }
                xOffset = lea.readShort();
                yOffset = lea.readShort();
                bMoveAction = lea.readByte();
                tElapse = lea.readShort();
                break;
            case 3:
            case 4:
            case 7:
            case 8:
            case 9:
            case 0xB:
                x = lea.readShort();
                y = lea.readShort();
                fh = lea.readShort();
                vx = 0;
                vy = 0;
                bMoveAction = lea.readByte();
                tElapse = lea.readShort();
                break;
            case 0xE:
                x = (short) startPosition.x;
                y = (short) startPosition.y;
                vx = lea.readShort();
                vy = lea.readShort();
                fhFallStart = lea.readShort();
                bMoveAction = lea.readByte();
                tElapse = lea.readShort();
                break;
            case 0x18:
                x = lea.readShort();
                y = lea.readShort();
                vx = lea.readShort();
                vy = lea.readShort();
                bMoveAction = lea.readByte();
                tElapse = lea.readShort();
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
                vx = lea.readShort();
                vy = lea.readShort();
                bMoveAction = lea.readByte();
                tElapse = lea.readShort();
                break;
            case 0xA:
                bStat = lea.readByte();
                break;
            default:
                bMoveAction = lea.readByte();
                tElapse = lea.readShort();
                break;
        }
    }

    public void encode(LittleEndianWriter lew) {
        lew.write(type);
        switch (type) {
            case 0: // normal move
            case 5:
            case 0xF:
            case 0x11:
            case 0x1F:
            case 0x20:
                lew.writeShort(x);
                lew.writeShort(y);
                lew.writeShort(vx);
                lew.writeShort(vy);
                lew.writeShort(fh);
                if (type == 15) {
                    lew.writeShort(fhFallStart);
                }
                lew.writeShort(xOffset);
                lew.writeShort(yOffset);
                lew.write(bMoveAction);
                lew.writeShort(tElapse);
                break;
            case 3:
            case 4:
            case 7:
            case 8:
            case 9:
            case 0xB:
                lew.writeShort(x);
                lew.writeShort(y);
                lew.writeShort(fh);
                lew.write(bMoveAction);
                lew.writeShort(tElapse);
                break;
            case 0xE:
                lew.writeShort(vx);
                lew.writeShort(vy);
                lew.writeShort(fhFallStart);
                lew.write(bMoveAction);
                lew.writeShort(tElapse);
                break;
            case 0x18:
                lew.writeShort(x);
                lew.writeShort(y);
                lew.writeShort(vx);
                lew.writeShort(vy);
                lew.write(bMoveAction);
                lew.writeShort(tElapse);
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
                lew.writeShort(vx);
                lew.writeShort(vy);
                lew.write(bMoveAction);
                lew.writeShort(tElapse);
                break;
            case 0xA:
                lew.write(bStat);
                break;
            default:
                lew.write(bMoveAction);
                lew.writeShort(tElapse);
                break;
        }
    }
}
