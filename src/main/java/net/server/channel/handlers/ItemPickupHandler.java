package net.server.channel.handlers;

import java.awt.*;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.maps.MapleMapObject;
import tools.FilePrinter;

public final class ItemPickupHandler extends AbstractMaplePacketHandler {

   private static void performPickup(MapleClient c, MapleMapObject ob) {
      Point charPos = c.getPlayer().getPosition();
      Point obPos = ob.getPosition();
      if (Math.abs(charPos.getX() - obPos.getX()) > 800 || Math.abs(charPos.getY() - obPos.getY()) > 600) {
         FilePrinter.printError(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt",
               c.getPlayer().getName() + " tried to pick up an item too far away. Mapid: " + c.getPlayer().getMapId()
                     + " Player pos: " + charPos + " Object pos: " + obPos);
         return;
      }

      c.getPlayer().pickupItem(ob);
   }

   @Override
   public void handlePacket(final InPacket p, final MapleClient c) {
      p.readInt(); //Timestamp
      p.readByte();
      p.readPos(); //cpos
      int oid = p.readInt();
      c.getPlayer().getMap().getMapObject(oid).ifPresent(o -> performPickup(c, o));
   }
}
