package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class OpenFamilyHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (!YamlConfig.config.server.USE_FAMILY_SYSTEM) {
         return;
      }
      MapleCharacter chr = c.getPlayer();
      c.sendPacket(CWvsContext.getFamilyInfo(chr.getFamilyEntry()));
   }
}

