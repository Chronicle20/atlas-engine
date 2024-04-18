/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.quest.actions;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import connection.packets.CWvsContext;
import constants.inventory.ItemConstants;
import provider.MapleData;
import provider.MapleDataTool;
import server.ItemInformationProvider;
import server.quest.MapleQuest;
import server.quest.MapleQuestActionType;
import tools.FilePrinter;
import tools.Pair;
import tools.Randomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Tyler (Twdtwd)
 * @author Ronan
 */
public class ItemAction extends MapleQuestAction {
    List<ItemData> items = new ArrayList<>();

    public ItemAction(MapleQuest quest, MapleData data) {
        super(MapleQuestActionType.ITEM, quest);
        processData(data);
    }


    @Override
    public void processData(MapleData data) {
        for (MapleData iEntry : data.getChildren()) {
            int id = MapleDataTool.getInt(iEntry.getChildByPath("id"));
            int count = MapleDataTool.getInt(iEntry.getChildByPath("count"), 1);
            int period = MapleDataTool.getInt(iEntry.getChildByPath("period"), 0);

            Integer prop = null;
            MapleData propData = iEntry.getChildByPath("prop");
            if (propData != null) {
                prop = MapleDataTool.getInt(propData);
            }

            int gender = 2;
            if (iEntry.getChildByPath("gender") != null) {
                gender = MapleDataTool.getInt(iEntry.getChildByPath("gender"));
            }

            int job = -1;
            if (iEntry.getChildByPath("job") != null) {
                job = MapleDataTool.getInt(iEntry.getChildByPath("job"));
            }

            items.add(new ItemData(Integer.parseInt(iEntry.getName()), id, count, prop, job, gender, period));
        }

        items.sort(Comparator.comparingInt(o -> o.map));
    }

    @Override
    public void run(MapleCharacter chr, Integer extSelection) {
        List<ItemData> takeItem = new LinkedList<>();
        List<ItemData> giveItem = new LinkedList<>();

        int props = 0, rndProps = 0, accProps = 0;
        for (ItemData item : items) {
            if (item.prop() != null && item.prop() != -1 && canGetItem(item, chr)) {
                props += item.prop();
            }
        }

        int extNum = 0;
        if (props > 0) {
            rndProps = Randomizer.nextInt(props);
        }
        for (ItemData iEntry : items) {
            if (!canGetItem(iEntry, chr)) {
                continue;
            }

            if (iEntry.prop() != null) {
                if (iEntry.prop() == -1) {
                    if (extSelection != extNum++) {
                        continue;
                    }
                } else {
                    accProps += iEntry.prop();

                    if (accProps <= rndProps) {
                        continue;
                    } else {
                        accProps = Integer.MIN_VALUE;
                    }
                }
            }

            if (iEntry.count() < 0) { // Remove Item
                takeItem.add(iEntry);
            } else {                    // Give Item
                giveItem.add(iEntry);
            }
        }

        // must take all needed items before giving others

        for (ItemData iEntry : takeItem) {
            int itemid = iEntry.id(), count = iEntry.count();

            MapleInventoryType type = ItemConstants.getInventoryType(itemid);
            int quantity = count * -1; // Invert
            if (type.equals(MapleInventoryType.EQUIP)) {
                if (chr.getInventory(type).countById(itemid) < quantity) {
                    // Not enough in the equip inventoty, so check Equipped...
                    if (chr.getInventory(MapleInventoryType.EQUIPPED).countById(itemid) > quantity) {
                        // Found it equipped, so change the type to equipped.
                        type = MapleInventoryType.EQUIPPED;
                    }
                }
            }

            MapleInventoryManipulator.removeById(chr.getClient(), type, itemid, quantity, true, false);
            chr.sendPacket(CWvsContext.getShowItemGain(itemid, (short) count, true));
        }

        for (ItemData iEntry : giveItem) {
            int itemid = iEntry.id(), count = iEntry.count(), period = iEntry.period();    // thanks Vcoc for noticing quest milestone item not getting removed from inventory after a while

            MapleInventoryManipulator.addById(chr.getClient(), itemid, (short) count, "", -1, period > 0 ? (System.currentTimeMillis() + (long) period * 60 * 1000) : -1);
            chr.sendPacket(CWvsContext.getShowItemGain(itemid, (short) count, true));
        }
    }

    @Override
    public boolean check(MapleCharacter chr, Integer extSelection) {
        List<Pair<Item, MapleInventoryType>> gainList = new LinkedList<>();
        List<Pair<Item, MapleInventoryType>> selectList = new LinkedList<>();
        List<Pair<Item, MapleInventoryType>> randomList = new LinkedList<>();

        List<Integer> allSlotUsed = new ArrayList<>(5);
        for (byte i = 0; i < 5; i++) allSlotUsed.add(0);

        for (ItemData item : items) {
            if (!canGetItem(item, chr)) {
                continue;
            }

            MapleInventoryType type = ItemConstants.getInventoryType(item.id());
            if (item.prop() != null) {
                Item toItem = new Item(item.id(), (short) 0, (short) item.count());

                if (item.prop() < 0) {
                    selectList.add(new Pair<>(toItem, type));
                } else {
                    randomList.add(new Pair<>(toItem, type));
                }

            } else {
                // Make sure they can hold the item.
                Item toItem = new Item(item.id(), (short) 0, (short) item.count());
                gainList.add(new Pair<>(toItem, type));

                if (item.count() < 0) {
                    // Make sure they actually have the item.
                    int quantity = item.count() * -1;

                    int freeSlotCount = chr.getInventory(type).freeSlotCountById(item.id(), quantity);
                    if (freeSlotCount == -1) {
                        if (type.equals(MapleInventoryType.EQUIP) && chr.getInventory(MapleInventoryType.EQUIPPED).countById(item.id()) > quantity) {
                            continue;
                        }

                        announceInventoryLimit(Collections.singletonList(item.id()), chr);
                        return false;
                    } else {
                        int idx = type.getType() - 1;   // more slots available from the given items!
                        allSlotUsed.set(idx, allSlotUsed.get(idx) - freeSlotCount);
                    }
                }
            }
        }

        if (!randomList.isEmpty()) {
            int result;
            MapleClient c = chr.getClient();

            List<Integer> rndUsed = new ArrayList<>(5);
            for (byte i = 0; i < 5; i++) rndUsed.add(allSlotUsed.get(i));

            for (Pair<Item, MapleInventoryType> it : randomList) {
                int idx = it.getRight().getType() - 1;

                result = MapleInventoryManipulator.checkSpaceProgressively(c, it.getLeft().getItemId(), it.getLeft().getQuantity(), "", rndUsed.get(idx), false);
                if (result % 2 == 0) {
                    announceInventoryLimit(Collections.singletonList(it.getLeft().getItemId()), chr);
                    return false;
                }

                allSlotUsed.set(idx, Math.max(allSlotUsed.get(idx), result >> 1));
            }
        }

        if (!selectList.isEmpty()) {
            Pair<Item, MapleInventoryType> selected = selectList.get(extSelection);
            gainList.add(selected);
        }

        if (!canHold(chr, gainList)) {
            List<Integer> gainItemids = new LinkedList<>();
            for (Pair<Item, MapleInventoryType> it : gainList) {
                gainItemids.add(it.getLeft().getItemId());
            }

            announceInventoryLimit(gainItemids, chr);
            return false;
        }
        return true;
    }

    private void announceInventoryLimit(List<Integer> itemids, MapleCharacter chr) {
        for (Integer id : itemids) {
            if (ItemInformationProvider.getInstance().isPickupRestricted(id) && chr.haveItemWithId(id, true)) {
                chr.dropMessage(1, "Please check if you already have a similar one-of-a-kind item in your inventory.");
                return;
            }
        }

        chr.dropMessage(1, "Please check if you have enough space in your inventory.");
    }

    private boolean canHold(MapleCharacter chr, List<Pair<Item, MapleInventoryType>> gainList) {
        List<Integer> toAddItemids = new LinkedList<>();
        List<Integer> toAddQuantity = new LinkedList<>();
        List<Integer> toRemoveItemids = new LinkedList<>();
        List<Integer> toRemoveQuantity = new LinkedList<>();

        for (Pair<Item, MapleInventoryType> item : gainList) {
            Item it = item.getLeft();

            if (it.getQuantity() > 0) {
                toAddItemids.add(it.getItemId());
                toAddQuantity.add((int) it.getQuantity());
            } else {
                toRemoveItemids.add(it.getItemId());
                toRemoveQuantity.add(-1 * ((int) it.getQuantity()));
            }
        }

        // thanks onechord for noticing quests unnecessarily giving out "full inventory" from quests that also takes items from players
        return chr.getAbstractPlayerInteraction().canHoldAllAfterRemoving(toAddItemids, toAddQuantity, toRemoveItemids, toRemoveQuantity);
    }

    private boolean canGetItem(ItemData item, MapleCharacter chr) {
        if (item.gender() != 2 && item.gender() != chr.getGender()) {
            return false;
        }

        if (item.job > 0) {
            final List<Integer> code = getJobBy5ByteEncoding(item.job());
            boolean jobFound = false;
            for (int codec : code) {
                if (codec / 100 == chr.getJob().getId() / 100) {
                    jobFound = true;
                    break;
                }
            }
            return jobFound;
        }

        return true;
    }

    public boolean restoreLostItem(MapleCharacter chr, int itemid) {
        if (!ItemInformationProvider.getInstance().isQuestItem(itemid)) {
            return false;
        }

        // thanks danielktran (MapleHeroesD)
        for (ItemData item : items) {
            if (item.id() == itemid) {
                int missingQty = item.count() - chr.countItem(itemid);
                if (missingQty > 0) {
                    if (!chr.canHold(itemid, missingQty)) {
                        chr.dropMessage(1, "Please check if you have enough space in your inventory.");
                        return false;
                    }

                    MapleInventoryManipulator.addById(chr.getClient(), item.id(), (short) missingQty);
                    FilePrinter.print(FilePrinter.QUEST_RESTORE_ITEM, chr + " obtained " + itemid + " qty. " + missingQty + " from quest " + questID);
                }
                return true;
            }
        }

        return false;
    }

    private record ItemData(int map, int id, int count, Integer prop, int job, int gender, int period) {
    }
} 
