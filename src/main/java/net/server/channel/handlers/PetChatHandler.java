package net.server.channel.handlers;

import client.MapleClient;
import client.autoban.AutobanFactory;
import config.YamlConfig;
import connection.packets.CPet;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import tools.FilePrinter;
import tools.LogHelper;

public final class PetChatHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int petId = p.readInt();
      p.readInt();
      p.readByte();
      int act = p.readByte();
      byte pet = c.getPlayer().getPetIndex(petId);
      if ((pet < 0 || pet > 3) || (act < 0 || act > 9)) {
         return;
      }
      String text = p.readString();
      if (text.length() > Byte.MAX_VALUE) {
         AutobanFactory.PACKET_EDIT.alert(c.getPlayer(), c.getPlayer().getName() + " tried to packet edit with pets.");
         FilePrinter.printError(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt",
               c.getPlayer().getName() + " tried to send text with length of " + text.length());
         c.disconnect(true, false);
         return;
      }
      c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CPet.petChat(c.getPlayer().getId(), pet, act, text), true);
      if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
         LogHelper.logChat(c, "Pet", text);
      }
   }
}
