package net.server.channel.handlers;

import java.util.Collection;

import client.MapleClient;
import constants.skills.DarkKnight;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.maps.MapleSummon;

public final class BeholderHandler extends AbstractMaplePacketHandler {//Summon Skills noobs

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      Collection<MapleSummon> summons = c.getPlayer().getSummonsValues();
      int oid = p.readInt();
      MapleSummon summon = null;
      for (MapleSummon sum : summons) {
         if (sum.getObjectId() == oid) {
            summon = sum;
         }
      }
      if (summon != null) {
         int skillId = p.readInt();
         if (skillId == DarkKnight.AURA_OF_BEHOLDER) {
            p.readShort(); //Not sure.
         } else if (skillId == DarkKnight.HEX_OF_BEHOLDER) {
            p.readByte(); //Not sure.
         }            //show to others here
      } else {
         c.getPlayer().clearSummons();
      }
   }
}
