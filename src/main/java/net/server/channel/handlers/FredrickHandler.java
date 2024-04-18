package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.processor.npc.FredrickProcessor;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public class FredrickHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
      byte operation = p.readByte();

      switch (operation) {
         case 0x19: //Will never come...
            //c.sendPacket(MaplePacketCreator.getFredrick((byte) 0x24));
            break;
         case 0x1A:
            FredrickProcessor.fredrickRetrieveItems(c);
            break;
         case 0x1C: //Exit
            break;
         default:
      }
   }
}
