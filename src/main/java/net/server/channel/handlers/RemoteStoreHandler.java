package net.server.channel.handlers;

import java.util.Optional;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.maps.MapleHiredMerchant;

public class RemoteStoreHandler extends AbstractMaplePacketHandler {
   private static Optional<MapleHiredMerchant> getMerchant(MapleClient c) {
      if (c.getPlayer().hasMerchant()) {
         return c.getWorldServer().getHiredMerchant(c.getPlayer().getId());
      }
      return Optional.empty();
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
      Optional<MapleHiredMerchant> hm = getMerchant(c);
      if (hm.isEmpty() || !hm.get().isOwner(chr)) {
         chr.dropMessage(1, "You don't have a Merchant open.");
         c.sendPacket(CWvsContext.enableActions());
         return;
      }

      if (hm.get().getChannel() == chr.getClient().getChannel()) {
         hm.get().visitShop(chr);
      } else {
         c.sendPacket(CWvsContext.remoteChannelChange((byte) (hm.get().getChannel() - 1)));
      }
   }
}
