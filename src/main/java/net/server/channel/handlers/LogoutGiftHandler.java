package net.server.channel.handlers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleClient;
import connection.packets.CWvsContext;
import gift.LogoutGift;
import gift.LogoutGiftProcessor;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.CashShop;

public class LogoutGiftHandler extends AbstractMaplePacketHandler {
   private static final Logger log = LoggerFactory.getLogger(LogoutGiftHandler.class);

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int index = p.readInt();
      log.debug("Character {} for account {} selected logout gift index {}.", c.getPlayer().getName(), c.getAccountName(), index);

      if (index < 0 || index >= 3) {
         log.warn("Character {} issued an out of bounds index.", c.getPlayer().getName());
         return;
      }

      List<LogoutGift> choices = LogoutGiftProcessor.getInstance().getGiftChoices(c.getWorld(), c.getPlayer().getId());
      LogoutGift choice = choices.get(index);
      CashShop.CashItem item = CashShop.CashItemFactory.getItem(choice.serialNumber());
      log.debug("Character {} selected logout gift {}.", c.getPlayer().getName(), item.getItemId());
      c.getPlayer().getAbstractPlayerInteraction().gainItem(item.getItemId());
      LogoutGiftProcessor.getInstance().makeChoice(c.getWorld(), c.getPlayer().getId());
      c.sendPacket(CWvsContext.confirmLogoutGift());
   }
}
