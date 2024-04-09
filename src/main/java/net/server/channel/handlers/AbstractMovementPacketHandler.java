/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.server.channel.handlers;

import net.AbstractMaplePacketHandler;
import server.maps.AnimatedMapleMapObject;
import server.movement.AbsoluteLifeMovement;
import server.movement.ChangeEquip;
import server.movement.JumpDownMovement;
import server.movement.LifeMovementFragment;
import server.movement.RelativeLifeMovement;
import server.movement.TeleportMovement;
import tools.data.input.LittleEndianAccessor;
import tools.exceptions.EmptyMovementException;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMovementPacketHandler extends AbstractMaplePacketHandler {

    protected List<LifeMovementFragment> parseMovement(LittleEndianAccessor lea) throws EmptyMovementException {
        List<LifeMovementFragment> res = new ArrayList<>();
        byte numCommands = lea.readByte();
        if (numCommands < 1) {
            throw new EmptyMovementException(lea);
        }
        for (byte i = 0; i < numCommands; i++) {
            byte command = lea.readByte();
            switch (command) {
                case 0: // normal move
                case 5:
                case 17: { // Float
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short xwobble = lea.readShort();
                    short ywobble = lea.readShort();
                    short fh = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, new Point(xpos, ypos), duration, newstate);
                    alm.setFh(fh);
                    alm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(alm);
                    break;
                }
                case 1:
                case 2:
                case 6: // fj
                case 12:
                case 13: // Shot-jump-back thing
                case 16: // Float
                case 18:
                case 19: // Springs on maps
                case 20: // Aran Combat Step
                case 22: {
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    RelativeLifeMovement rlm = new RelativeLifeMovement(command, new Point(xpos, ypos), duration, newstate);
                    res.add(rlm);
                    break;
                }
                case 3:
                case 4: // tele... -.-
                case 7: // assaulter
                case 8: // assassinate
                case 9: // rush
                case 11: //chair
                {
//                case 14: {
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short xwobble = lea.readShort();
                    short ywobble = lea.readShort();
                    byte newstate = lea.readByte();
                    TeleportMovement tm = new TeleportMovement(command, new Point(xpos, ypos), newstate);
                    tm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(tm);
                    break;
                }
                case 14:
                    lea.skip(9); // jump down (?)
                    break;
                case 10: // Change Equip
                    res.add(new ChangeEquip(lea.readByte()));
                    break;
                /*case 11: { // Chair
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short fh = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    ChairMovement cm = new ChairMovement(command, new Point(xpos, ypos), duration, newstate);
                    cm.setFh(fh);
                    res.add(cm);
                    break;
                }*/
                case 15: {
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short xwobble = lea.readShort();
                    short ywobble = lea.readShort();
                    short fh = lea.readShort();
                    short ofh = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    JumpDownMovement jdm = new JumpDownMovement(command, new Point(xpos, ypos), duration, newstate);
                    jdm.setFh(fh);
                    jdm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    jdm.setOriginFh(ofh);
                    res.add(jdm);
                    break;
                }
                case 21: {//Causes aran to do weird stuff when attacking o.o
                    /*byte newstate = lea.readByte();
                     short unk = lea.readShort();
                     AranMovement am = new AranMovement(command, null, unk, newstate);
                     res.add(am);*/
                    lea.skip(3);
                    break;
                }
                default:
                    System.out.println("Unhandled Case:" + command);
                    throw new EmptyMovementException(lea);
            }
        }

        if (res.isEmpty()) {
            throw new EmptyMovementException(lea);
        }
        return res;
    }

    protected void updatePosition(LittleEndianAccessor lea, AnimatedMapleMapObject target, int yOffset) throws EmptyMovementException {
        lea.readShort();
        lea.readShort();

        byte numCommands = lea.readByte();
        if (numCommands < 1) {
            throw new EmptyMovementException(lea);
        }
        for (byte i = 0; i < numCommands; i++) {
            byte command = lea.readByte();

            short tx = -1;
            short ty = -1;
            short vx = -1;
            short fh = -1;
            short vy = -1;
            short fhFallStart = -1;
            short mxOffset = -1;
            short myOffset = -1;

            switch (command) {
                case 0: // normal move
                case 5:
                case 15:
                case 17: {
                    //Absolute movement - only this is important for the server, other movement can be passed to the client
                    tx = lea.readShort();
                    ty = lea.readShort();
                    vx = lea.readShort();
                    vy = lea.readShort();
                    fh = lea.readShort();
                    if (command == 15) {
                        fhFallStart = lea.readShort();
                    }
                    mxOffset = lea.readShort();
                    myOffset = lea.readShort();
                    target.setPosition(new Point(tx, ty + yOffset));
                    break;
                }
                case 1:
                case 2:
                case 6: // fj
                case 12:
                case 13: // Shot-jump-back thing
                case 16: // Float
                case 18:
                case 19: // Springs on maps
                case 20: // Aran Combat Step
                case 22:
                case 24: {
                    //Relative movement - server only cares about stance
                    vx = lea.readShort();
                    vy = lea.readShort();
                    break;
                }
                case 3:
                case 4: // tele... -.-
                case 7: // assaulter
                case 8: // assassinate
                case 9: // rush
                case 11: //chair
                {
//                case 14: {
                    //Teleport movement - same as above
                    tx = lea.readShort();
                    ty = lea.readShort();
                    fh = lea.readShort();
                    break;
                }
                case 14:
                    tx = lea.readShort();
                    ty = lea.readShort();
                    fh = lea.readShort();
                    break;
                case 10: // Change Equip
                    //ignored server-side
                    lea.readByte();
                    break;
                case 23: {
                    tx = lea.readShort();
                    ty = lea.readShort();
                    vx = lea.readShort();
                    vy = lea.readShort();
                    break;
                }
                default:
                    System.out.println("Unhandled Case:" + command);
                    throw new EmptyMovementException(lea);
            }

            byte newstate = lea.readByte();
            target.setStance(newstate);
            short tElapse = lea.readShort();
        }
    }
}