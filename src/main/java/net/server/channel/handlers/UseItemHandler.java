package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import config.YamlConfig;
import connection.packets.CWvsContext;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.ItemInformationProvider;
import server.MapleStatEffect;

public final class UseItemHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();

      if (!chr.isAlive()) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }
      ItemInformationProvider ii = ItemInformationProvider.getInstance();
      p.readInt();
      short slot = p.readShort();
      int itemId = p.readInt();
      Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
      if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemId) {
         if (itemId == 2050004) {
            chr.dispelDebuffs();
            remove(c, slot);
            return;
         } else if (itemId == 2050001) {
            chr.dispelDebuff(MapleDisease.DARKNESS);
            remove(c, slot);
            return;
         } else if (itemId == 2050002) {
            chr.dispelDebuff(MapleDisease.WEAKEN);
            chr.dispelDebuff(MapleDisease.SLOW);
            remove(c, slot);
            return;
         } else if (itemId == 2050003) {
            chr.dispelDebuff(MapleDisease.SEAL);
            chr.dispelDebuff(MapleDisease.CURSE);
            remove(c, slot);
            return;
         } else if (ItemConstants.isTownScroll(itemId)) {
            int banMap = chr.getMapId();
            int banSp = chr.getMap().findClosestPlayerSpawnpoint(chr.getPosition()).getId();
            long banTime = currentServerTime();

            if (ii.getItemEffect(toUse.getItemId()).applyTo(chr)) {
               if (YamlConfig.config.server.USE_BANISHABLE_TOWN_SCROLL) {
                  chr.setBanishPlayerData(banMap, banSp, banTime);
               }

               remove(c, slot);
            }
            return;
         } else if (ItemConstants.isAntibanishScroll(itemId)) {
            if (ii.getItemEffect(toUse.getItemId()).applyTo(chr)) {
               remove(c, slot);
            } else {
               chr.dropMessage(5, "You cannot recover from a banish state at the moment.");
            }
            return;
         }

         remove(c, slot);

         if (toUse.getItemId() != 2022153) {
            ii.getItemEffect(toUse.getItemId()).applyTo(chr);
         } else {
            MapleStatEffect mse = ii.getItemEffect(toUse.getItemId());
            for (MapleCharacter player : chr.getMap().getCharacters()) {
               mse.applyTo(player);
            }
         }
      }
   }

   private void remove(MapleClient c, short slot) {
      MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
      c.sendPacket(CWvsContext.enableActions());
   }
}
