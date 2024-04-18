package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CCashShop;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class TransferNameResultHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      String name = p.readString();
      c.sendPacket(CCashShop.sendNameTransferCheck(name, MapleCharacter.canCreateChar(name)));
   }
}