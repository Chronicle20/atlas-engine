package net.server.channel.handlers;

import client.MapleClient;
import client.autoban.AutobanFactory;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import scripting.npc.NPCScriptManager;

public final class RemoteGachaponHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int ticket = p.readInt();
      int gacha = p.readInt();
      if (ticket != 5451000) {
         AutobanFactory.GENERAL.alert(c.getPlayer(), " Tried to use RemoteGachaponHandler with item id: " + ticket);
         c.disconnect(false, false);
         return;
      } else if (gacha < 0 || gacha > 11) {
         AutobanFactory.GENERAL.alert(c.getPlayer(), " Tried to use RemoteGachaponHandler with mode: " + gacha);
         c.disconnect(false, false);
         return;
      } else if (c.getPlayer().getInventory(ItemConstants.getInventoryType(ticket)).countById(ticket) < 1) {
         AutobanFactory.GENERAL.alert(c.getPlayer(), " Tried to use RemoteGachaponHandler without a ticket.");
         c.disconnect(false, false);
         return;
      }
      int npcId = 9100100;
      if (gacha != 8 && gacha != 9) {
         npcId += gacha;
      } else {
         npcId = gacha == 8 ? 9100109 : 9100117;
      }
      NPCScriptManager.getInstance().start(c, npcId, "gachaponRemote", null);
   }
}
