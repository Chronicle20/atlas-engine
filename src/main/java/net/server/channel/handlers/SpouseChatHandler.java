package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import connection.packets.CField;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import tools.LogHelper;

public final class SpouseChatHandler extends AbstractMaplePacketHandler {
   private static void spouseChat(MapleClient c, String msg, MapleCharacter spouse) {
      spouse.sendPacket(CField.OnCoupleMessage(c.getPlayer().getName(), msg, true));
      c.sendPacket(CField.OnCoupleMessage(c.getPlayer().getName(), msg, true));
      if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
         LogHelper.logChat(c, "Spouse", msg);
      }
   }

   private static void spouseChatError(MapleClient c) {
      c.getPlayer().dropMessage(5, "Your spouse is currently offline.");
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      p.readString();//recipient
      String msg = p.readString();

      int partnerId = c.getPlayer().getPartnerId();
      if (partnerId <= 0) {
         c.getPlayer().dropMessage(5, "You don't have a spouse.");
         return;
      }

      c.getWorldServer().getPlayerStorage().getCharacterById(partnerId)
            .ifPresentOrElse(s -> spouseChat(c, msg, s), () -> spouseChatError(c));
   }
}
