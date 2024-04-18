package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CField;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public class UseMapleLifeHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter player = c.getPlayer();
      long timeNow = currentServerTime();

      if (timeNow - player.getLastUsedCashItem() < 3000) {
         player.dropMessage(5, "Please wait a moment before trying again.");
         c.sendPacket(CField.sendMapleLifeError(3));
         c.sendPacket(CWvsContext.enableActions());
         return;
      }
      player.setLastUsedCashItem(timeNow);

      String name = p.readString();
      if (MapleCharacter.canCreateChar(name)) {
         c.sendPacket(CField.sendMapleLifeCharacterInfo());
      } else {
         c.sendPacket(CField.sendMapleLifeNameError());
      }
      c.sendPacket(CWvsContext.enableActions());
   }
}
