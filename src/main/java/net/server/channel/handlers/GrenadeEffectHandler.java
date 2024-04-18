package net.server.channel.handlers;

import java.awt.*;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CUserRemote;
import constants.skills.Gunslinger;
import constants.skills.NightWalker;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import tools.FilePrinter;

public class GrenadeEffectHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
      Point position = new Point(p.readInt(), p.readInt());
      int keyDown = p.readInt();
      int skillId = p.readInt();

      switch (skillId) {
         case NightWalker.POISON_BOMB:
         case Gunslinger.GRENADE:
            int skillLevel = chr.getSkillLevel(skillId);
            if (skillLevel > 0) {
               chr.getMap().broadcastMessage(chr, CUserRemote.throwGrenade(chr.getId(), position, keyDown, skillId, skillLevel),
                     position);
            }
            break;
         default:
            FilePrinter.printError(FilePrinter.UNHANDLED_EVENT,
                  "The skill id: " + skillId + " is not coded in " + this.getClass().getName() + ".");
      }
   }
}