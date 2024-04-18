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

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.maps.AnimatedMapleMapObject;
import server.movement.AbsoluteLifeMovement;
import server.movement.ChangeEquip;
import server.movement.JumpDownMovement;
import server.movement.LifeMovementFragment;
import server.movement.RelativeLifeMovement;
import server.movement.TeleportMovement;
import tools.exceptions.EmptyMovementException;

public abstract class AbstractMovementPacketHandler extends AbstractMaplePacketHandler {
   private static final Logger log = LoggerFactory.getLogger(AbstractMaplePacketHandler.class);

   protected List<LifeMovementFragment> parseMovement(InPacket p) throws EmptyMovementException {
      List<LifeMovementFragment> res = new ArrayList<>();
      byte numCommands = p.readByte();
      if (numCommands < 1) {
         throw new EmptyMovementException(p);
      }
      for (byte i = 0; i < numCommands; i++) {
         byte command = p.readByte();
         switch (command) {
            case 0: // normal move
            case 5:
            case 17: { // Float
               short xpos = p.readShort();
               short ypos = p.readShort();
               short xwobble = p.readShort();
               short ywobble = p.readShort();
               short fh = p.readShort();
               byte newstate = p.readByte();
               short duration = p.readShort();
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
               short xpos = p.readShort();
               short ypos = p.readShort();
               byte newstate = p.readByte();
               short duration = p.readShort();
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
               short xpos = p.readShort();
               short ypos = p.readShort();
               short xwobble = p.readShort();
               short ywobble = p.readShort();
               byte newstate = p.readByte();
               TeleportMovement tm = new TeleportMovement(command, new Point(xpos, ypos), newstate);
               tm.setPixelsPerSecond(new Point(xwobble, ywobble));
               res.add(tm);
               break;
            }
            case 14:
               p.skip(9); // jump down (?)
               break;
            case 10: // Change Equip
               res.add(new ChangeEquip(p.readByte()));
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
               short xpos = p.readShort();
               short ypos = p.readShort();
               short xwobble = p.readShort();
               short ywobble = p.readShort();
               short fh = p.readShort();
               short ofh = p.readShort();
               byte newstate = p.readByte();
               short duration = p.readShort();
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
               p.skip(3);
               break;
            }
            default:
               log.debug("parseMovement does not handle the command {}", command);
               throw new EmptyMovementException(p);
         }
      }

      if (res.isEmpty()) {
         throw new EmptyMovementException(p);
      }
      return res;
   }

   protected void updatePosition(InPacket p, AnimatedMapleMapObject target, int yOffset) throws EmptyMovementException {
      p.readShort();
      p.readShort();

      byte numCommands = p.readByte();
      if (numCommands < 1) {
         throw new EmptyMovementException(p);
      }
      for (byte i = 0; i < numCommands; i++) {
         byte command = p.readByte();

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
               tx = p.readShort();
               ty = p.readShort();
               vx = p.readShort();
               vy = p.readShort();
               fh = p.readShort();
               if (command == 15) {
                  fhFallStart = p.readShort();
               }
               mxOffset = p.readShort();
               myOffset = p.readShort();
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
               vx = p.readShort();
               vy = p.readShort();
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
               tx = p.readShort();
               ty = p.readShort();
               fh = p.readShort();
               break;
            }
            case 14:
               tx = p.readShort();
               ty = p.readShort();
               fh = p.readShort();
               break;
            case 10: // Change Equip
               //ignored server-side
               p.readByte();
               break;
            case 23: {
               tx = p.readShort();
               ty = p.readShort();
               vx = p.readShort();
               vy = p.readShort();
               break;
            }
            default:
               log.debug("updatePosition does not handle the command {}", command);
               throw new EmptyMovementException(p);
         }

         byte newstate = p.readByte();
         target.setStance(newstate);
         short tElapse = p.readShort();
      }
   }
}
