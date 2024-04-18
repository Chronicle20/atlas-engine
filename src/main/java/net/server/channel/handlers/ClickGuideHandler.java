package net.server.channel.handlers;

import client.MapleClient;
import client.MapleJob;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import scripting.npc.NPCScriptManager;

public class ClickGuideHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (c.getPlayer().getJob().equals(MapleJob.NOBLESSE)) {
         NPCScriptManager.getInstance().start(c, 1101008, null);
      } else {
         NPCScriptManager.getInstance().start(c, 1202000, null);
      }
   }
}
