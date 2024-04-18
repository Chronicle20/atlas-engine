package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.guild.MapleGuild;

public final class DenyGuildRequestHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      p.readByte();
      c.getWorldServer().getPlayerStorage().getCharacterByName(p.readString()).map(MapleCharacter::getGuildId)
            .ifPresent(id -> MapleGuild.answerInvitation(c.getPlayer().getId(), c.getPlayer().getName(), id, false));
   }
}
