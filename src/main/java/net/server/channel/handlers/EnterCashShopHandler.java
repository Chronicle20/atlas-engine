package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CCashShop;
import connection.packets.CStage;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import server.maps.MapleMiniDungeonInfo;

public class EnterCashShopHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      try {
         int updateTime = p.readInt();
         MapleCharacter mc = c.getPlayer();

         if (mc.cannotEnterCashShop()) {
            c.sendPacket(CWvsContext.enableActions());
            return;
         }

         if (mc.getEventInstance().isPresent()) {
            c.sendPacket(CWvsContext.serverNotice(5, "Entering Cash Shop or MTS are disabled when registered on an event."));
            c.sendPacket(CWvsContext.enableActions());
            return;
         }

         if (MapleMiniDungeonInfo.isDungeonMap(mc.getMapId())) {
            c.sendPacket(CWvsContext.serverNotice(5,
                  "Changing channels or entering Cash Shop or MTS are disabled when inside a Mini-Dungeon."));
            c.sendPacket(CWvsContext.enableActions());
            return;
         }

         if (mc.getCashShop().isOpened()) {
            return;
         }

         mc.closePlayerInteractions();
         mc.closePartySearchInteractions();

         mc.unregisterChairBuff();
         Server.getInstance().getPlayerBuffStorage().addBuffsToStorage(mc.getId(), mc.getAllBuffs());
         Server.getInstance().getPlayerBuffStorage().addDiseasesToStorage(mc.getId(), mc.getAllDiseases());
         mc.setAwayFromChannelWorld();
         mc.notifyMapTransferToPartner(-1);
         mc.removeIncomingInvites();
         mc.cancelAllBuffs(true);
         mc.cancelAllDebuffs();
         mc.cancelBuffExpireTask();
         mc.cancelDiseaseExpireTask();
         mc.cancelSkillCooldownTask();
         mc.cancelExpirationTask();

         mc.forfeitExpirableQuests();
         mc.cancelQuestExpirationTask();

         c.sendPacket(CStage.openCashShop(c, false));
         c.sendPacket(CCashShop.showCashInventory(c));
         c.sendPacket(CCashShop.showGifts(mc.getCashShop().loadGifts()));
         c.sendPacket(CCashShop.showWishList(mc, false));
         c.sendPacket(CCashShop.showCash(mc));

         c.getChannelServer().removePlayer(mc);
         mc.getMap().removePlayer(mc);
         mc.getCashShop().open(true);
         mc.saveCharToDB();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
