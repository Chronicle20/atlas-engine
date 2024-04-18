package net.server.channel.handlers;

import java.util.Optional;

import client.MapleClient;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class ItemMoveHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int updateTime = p.readInt();
      if (c.getPlayer().getAutobanManager().getLastSpam(6) + 300 > currentServerTime()) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }

      byte nType = p.readByte();
      Optional<MapleInventoryType> type = MapleInventoryType.getByType(nType);
      if (type.isEmpty()) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }

      short nOldPos = p.readShort();     //is there any reason to use byte instead of short in src and action?
      short nNewPos = p.readShort();
      short nCount = p.readShort();

      if (nOldPos < 0 && nNewPos > 0) {
         MapleInventoryManipulator.unequip(c, nOldPos, nNewPos);
      } else if (nNewPos < 0) {
         MapleInventoryManipulator.equip(c, nOldPos, nNewPos);
      } else if (nNewPos == 0) {
         MapleInventoryManipulator.drop(c, type.get(), nOldPos, nCount);
      } else {
         MapleInventoryManipulator.move(c, type.get(), nOldPos, nNewPos);
      }

      if (c.getPlayer().getMap().getHPDec() > 0) {
         c.getPlayer().resetHpDecreaseTask();
      }
      c.getPlayer().getAutobanManager().spam(6);
   }
}