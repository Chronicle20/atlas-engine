package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class OpenFamilyPedigreeHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (!YamlConfig.config.server.USE_FAMILY_SYSTEM) {
         return;
      }
      c.getChannelServer().getPlayerStorage().getCharacterByName(p.readString()).filter(t -> t.getFamily().isPresent())
            .map(MapleCharacter::getFamilyEntry).map(CWvsContext::showPedigree).ifPresent(c::sendPacket);
   }
}

