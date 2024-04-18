package net.server.channel.handlers;

import java.util.Map;
import java.util.Optional;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.ItemInformationProvider;
import server.QuestConsumableItem;
import server.quest.MapleQuest;

public class RaiseIncExpHandler extends AbstractMaplePacketHandler {
   @Override
   public final void handlePacket(InPacket p, MapleClient c) {
      byte typeIndex = p.readByte();
      short slot = p.readShort();
      int itemId = p.readInt();

      if (c.tryacquireClient()) {
         try {
            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            Optional<QuestConsumableItem> consumableItem = ii.getQuestConsumablesInfo(itemId);
            if (consumableItem.isEmpty()) {
               return;
            }

            int infoNumber = consumableItem.get().questId();
            Map<Integer, Integer> consumables = consumableItem.get().items();

            MapleCharacter chr = c.getPlayer();
            MapleQuest quest = MapleQuest.getInstanceFromInfoNumber(infoNumber);
            if (!chr.getQuest(quest).getStatus().equals(MapleQuestStatus.Status.STARTED)) {
               c.sendPacket(CWvsContext.enableActions());
               return;
            }

            int consId;
            Optional<MapleInventoryType> type = MapleInventoryType.getByType(typeIndex);
            if (type.isEmpty()) {
               c.sendPacket(CWvsContext.enableActions());
               return;
            }

            MapleInventory inv = chr.getInventory(type.get());
            inv.lockInventory();
            try {
               consId = inv.getItem(slot).getItemId();
               if (!consumables.containsKey(consId) || !chr.haveItem(consId)) {
                  return;
               }

               MapleInventoryManipulator.removeFromSlot(c, type.get(), slot, (short) 1, false, true);
            } finally {
               inv.unlockInventory();
            }

            int questId = quest.getId();
            int nextValue =
                  Math.min(consumables.get(consId) + c.getAbstractPlayerInteraction().getQuestProgressInt(questId, infoNumber),
                        consumableItem.get().experience() * consumableItem.get().grade());
            c.getAbstractPlayerInteraction().setQuestProgress(questId, infoNumber, nextValue);

            c.sendPacket(CWvsContext.enableActions());
         } finally {
            c.releaseClient();
         }
      }
   }
}
