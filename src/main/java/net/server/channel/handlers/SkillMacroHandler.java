package net.server.channel.handlers;

import client.MapleClient;
import client.SkillMacro;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class SkillMacroHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int num = p.readByte();
      for (int i = 0; i < num; i++) {
         String name = p.readString();
         int shout = p.readByte();
         int skill1 = p.readInt();
         int skill2 = p.readInt();
         int skill3 = p.readInt();
         SkillMacro macro = new SkillMacro(skill1, skill2, skill3, name, shout, i);
         c.getPlayer().updateMacros(i, macro);
      }
   }
}
