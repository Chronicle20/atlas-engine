package net.server.channel.handlers;

import client.MapleClient;
import client.SkillFactory;
import connection.packets.CUserRemote;
import constants.skills.Bishop;
import constants.skills.Bowmaster;
import constants.skills.Corsair;
import constants.skills.Evan;
import constants.skills.FPArchMage;
import constants.skills.ILArchMage;
import constants.skills.Marksman;
import constants.skills.WindArcher;
import net.AbstractMaplePacketHandler;
import net.MaplePacketHandler;
import net.packet.InPacket;

public final class CancelBuffHandler extends AbstractMaplePacketHandler implements MaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int sourceid = p.readInt();

      switch (sourceid) {
         case FPArchMage.BIG_BANG:
         case ILArchMage.BIG_BANG:
         case Bishop.BIG_BANG:
         case Bowmaster.HURRICANE:
         case Marksman.PIERCING_ARROW:
         case Corsair.RAPID_FIRE:
         case WindArcher.HURRICANE:
         case Evan.FIRE_BREATH:
         case Evan.ICE_BREATH:
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CUserRemote.skillCancel(c.getPlayer(), sourceid), false);
            break;

         default:
            c.getPlayer().cancelEffect(SkillFactory.getSkill(sourceid).orElseThrow().getEffect(1), false, -1);
            break;
      }
   }
}