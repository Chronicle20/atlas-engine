package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class FaceExpressionHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
      int emote = p.readInt();

      if (emote > 7) {
         int itemid = 5159992 + emote;   // thanks RajanGrewal (Darter) for reporting unchecked emote itemid
         if (!ItemConstants.isFaceExpression(itemid) || chr.getInventory(ItemConstants.getInventoryType(itemid)).findById(itemid)
               .isEmpty()) {
            return;
         }
      } else if (emote < 1) {
         return;
      }

      if (c.tryacquireClient()) {
         try {   // expecting players never intends to wear the emote 0 (default face, that changes back after 5sec timeout)
            if (chr.isLoggedinWorld()) {
               chr.changeFaceExpression(emote);
            }
         } finally {
            c.releaseClient();
         }
      }
   }
}
