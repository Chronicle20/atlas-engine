package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.guild.MapleAlliance;

public final class DenyAllianceRequestHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      p.readByte();
      String inviterName = p.readString();
      String guildName = p.readString();

      c.getWorldServer().getPlayerStorage().getCharacterByName(inviterName).flatMap(MapleCharacter::getAlliance)
            .map(MapleAlliance::getId).ifPresent(id -> MapleAlliance.answerInvitation(c.getPlayer().getId(), guildName, id, false));
   }
}