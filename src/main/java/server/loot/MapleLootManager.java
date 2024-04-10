/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

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
package server.loot;

import client.MapleCharacter;
import drop.DropEntry;
import drop.DropProcessor;
import server.quest.MapleQuest;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Ronan
 */
public class MapleLootManager {

    private static boolean isRelevantDrop(DropEntry dropEntry, List<MapleCharacter> players, List<MapleLootInventory> playersInv) {
        int qStartAmount = 0, qCompleteAmount = 0;
        MapleQuest quest = MapleQuest.getInstance(dropEntry.questId());
        if (quest != null) {
            qStartAmount = quest.getStartItemAmountNeeded(dropEntry.itemId());
            qCompleteAmount = quest.getCompleteItemAmountNeeded(dropEntry.itemId());
        }

        //boolean restricted = MapleItemInformationProvider.getInstance().isPickupRestricted(dropEntry.itemId);
        for (int i = 0; i < players.size(); i++) {
            MapleLootInventory chrInv = playersInv.get(i);

            if (dropEntry.questId() > 0) {
                int qItemAmount, chrQuestStatus = players.get(i).getQuestStatus(dropEntry.questId());
                if (chrQuestStatus == 0) {
                    qItemAmount = qStartAmount;
                } else if (chrQuestStatus != 1) {
                    continue;
                } else {
                    qItemAmount = qCompleteAmount;
                }

                // thanks kvmba for noticing quest items with no required amount failing to be detected as such

                int qItemStatus = chrInv.hasItem(dropEntry.itemId(), qItemAmount);
                if (qItemStatus == 2) {
                    continue;
                } /*else if (restricted && qItemStatus == 1) {  // one-of-a-kind loots should be available everytime, thanks onechord for noticing
                    continue;
                }*/
            } /*else if (restricted && chrInv.hasItem(dropEntry.itemId, 1) > 0) {   // thanks Conrad, Legalize for noticing eligible loots not being available to drop for non-killer parties
                continue;
            }*/

            return true;
        }

        return false;
    }

    public static List<DropEntry> retrieveRelevantDrops(int monsterId, List<MapleCharacter> players) {
        List<DropEntry> loots = DropProcessor.getInstance().getDropsForMonster(monsterId);
        if (loots.isEmpty()) {
            return loots;
        }

        List<MapleLootInventory> playersInv = new LinkedList<>();
        for (MapleCharacter chr : players) {
            MapleLootInventory lootInv = new MapleLootInventory(chr);
            playersInv.add(lootInv);
        }

        List<DropEntry> effectiveLoot = new LinkedList<>();
        for (DropEntry mde : loots) {
            if (isRelevantDrop(mde, players, playersInv)) {
                effectiveLoot.add(mde);
            }
        }

        return effectiveLoot;
    }

}
