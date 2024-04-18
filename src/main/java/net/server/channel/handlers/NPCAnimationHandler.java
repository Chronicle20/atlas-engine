package net.server.channel.handlers;

import client.MapleClient;
import connection.constants.SendOpcode;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.packet.OutPacket;
import server.movement.MovePath;

public final class NPCAnimationHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket ip, MapleClient c) {
      if (c.getPlayer().isChangingMaps()) {
         return;
      }

      int length = ip.available();
      int npcId = ip.readInt();
      byte nAction = ip.readByte();
      byte nChatIdx = ip.readByte();

      final OutPacket op = OutPacket.create(SendOpcode.NPC_ACTION);
      op.writeInt(npcId);
      op.writeByte(nAction);
      op.writeByte(nChatIdx);
      if (length == 6) {
         // NPC Talk
         c.sendPacket(op);
         return;
      }

      // NPC Move
      final MovePath res = new MovePath();
      res.decode(ip);
      res.encode(op);
      c.sendPacket(op);
   }
}
