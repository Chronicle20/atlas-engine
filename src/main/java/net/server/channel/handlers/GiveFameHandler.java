package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleCharacter.FameStatus;
import client.MapleClient;
import client.autoban.AutobanFactory;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import tools.FilePrinter;

public final class GiveFameHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter target = (MapleCharacter) c.getPlayer().getMap().getMapObject(p.readInt()).orElse(null);
      int mode = p.readByte();
      int famechange = 2 * mode - 1;
      MapleCharacter player = c.getPlayer();
      if (target == null || target.getId() == player.getId() || player.getLevel() < 15) {
         return;
      } else if (famechange != 1 && famechange != -1) {
         AutobanFactory.PACKET_EDIT.alert(c.getPlayer(), c.getPlayer().getName() + " tried to packet edit fame.");
         FilePrinter.printError(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt",
               c.getPlayer().getName() + " tried to fame hack with famechange " + famechange);
         c.disconnect(true, false);
         return;
      }

      FameStatus status = player.canGiveFame(target);
      if (status == FameStatus.OK) {
         if (target.gainFame(famechange, player, mode)) {
            if (!player.isGM()) {
               player.hasGivenFame(target);
            }
         } else {
            player.message("Could not process the request, since this character currently has the minimum/maximum level of fame.");
         }
      } else {
         c.sendPacket(CWvsContext.giveFameErrorResponse(status == FameStatus.NOT_TODAY ? 3 : 4));
      }
   }
}