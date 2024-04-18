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
package client.processor.action;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import config.YamlConfig;
import connection.packets.CUser;
import connection.packets.CUserLocal;
import connection.packets.CWvsContext;
import constants.game.GameConstants;
import constants.inventory.ItemConstants;
import net.packet.InPacket;
import server.MakerItemFactory;
import server.MakerItemFactory.MakerItemCreateEntry;
import server.ItemInformationProvider;
import tools.FilePrinter;
import tools.Pair;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Ronan
 */
public class MakerProcessor {

    private static ItemInformationProvider ii = ItemInformationProvider.getInstance();

    public static void makerAction(InPacket p, MapleClient c) {
        if (c.tryacquireClient()) {
            try {
                int type = p.readInt();
                int toCreate = p.readInt();
                int toDisassemble = -1, pos = -1;
                boolean makerSucceeded = true;

                MakerItemCreateEntry recipe;
                Map<Integer, Short> reagentids = new LinkedHashMap<>();
                int stimulantid = -1;

                if (type == 3) {    // building monster crystal
                    int fromLeftover = toCreate;
                    toCreate = ii.getMakerCrystalFromLeftover(toCreate);
                    if (toCreate == -1) {
                        c.sendPacket(CWvsContext.serverNotice(1, ii.getName(fromLeftover) + " is unavailable for Monster Crystal conversion."));
                        c.sendPacket(CUserLocal.makerEnableActions());
                        return;
                    }

                    recipe = MakerItemFactory.generateLeftoverCrystalEntry(fromLeftover, toCreate);
                } else if (type == 4) {  // disassembling
                    p.readInt(); // 1... probably inventory type
                    pos = p.readInt();

                    Item it = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) pos);
                    if (it != null && it.getItemId() == toCreate) {
                        toDisassemble = toCreate;

                        Pair<Integer, List<Pair<Integer, Integer>>> pr = generateDisassemblyInfo(toDisassemble);
                        if (pr != null) {
                            recipe = MakerItemFactory.generateDisassemblyCrystalEntry(toDisassemble, pr.getLeft(), pr.getRight());
                        } else {
                            c.sendPacket(CWvsContext.serverNotice(1, ii.getName(toCreate) + " is unavailable for Monster Crystal disassembly."));
                            c.sendPacket(CUserLocal.makerEnableActions());
                            return;
                        }
                    } else {
                        c.sendPacket(CWvsContext.serverNotice(1, "An unknown error occurred when trying to apply that item for disassembly."));
                        c.sendPacket(CUserLocal.makerEnableActions());
                        return;
                    }
                } else {
                    if (ItemConstants.isEquipment(toCreate)) {   // only equips uses stimulant and reagents
                        if (p.readByte() != 0) {  // stimulant
                            stimulantid = ii.getMakerStimulant(toCreate);
                            if (!c.getAbstractPlayerInteraction().haveItem(stimulantid)) {
                                stimulantid = -1;
                            }
                        }

                        int reagents = Math.min(p.readInt(), getMakerReagentSlots(toCreate));
                        for (int i = 0; i < reagents; i++) {  // crystals
                            int reagentid = p.readInt();
                            if (ItemConstants.isMakerReagent(reagentid)) {
                                Short rs = reagentids.get(reagentid);
                                if (rs == null) {
                                    reagentids.put(reagentid, (short) 1);
                                } else {
                                    reagentids.put(reagentid, (short) (rs + 1));
                                }
                            }
                        }

                        List<Pair<Integer, Short>> toUpdate = new LinkedList<>();
                        for (Map.Entry<Integer, Short> r : reagentids.entrySet()) {
                            int qty = c.getAbstractPlayerInteraction().getItemQuantity(r.getKey());

                            if (qty < r.getValue()) {
                                toUpdate.add(new Pair<>(r.getKey(), (short) qty));
                            }
                        }

                        // remove those not present on player inventory
                        if (!toUpdate.isEmpty()) {
                            for (Pair<Integer, Short> rp : toUpdate) {
                                if (rp.getRight() > 0) {
                                    reagentids.put(rp.getLeft(), rp.getRight());
                                } else {
                                    reagentids.remove(rp.getLeft());
                                }
                            }
                        }

                        if (!reagentids.isEmpty()) {
                            if (!removeOddMakerReagents(toCreate, reagentids)) {
                                c.sendPacket(CWvsContext.serverNotice(1, "You can only use WATK and MATK Strengthening Gems on weapon items."));
                                c.sendPacket(CUserLocal.makerEnableActions());
                                return;
                            }
                        }
                    }

                    recipe = MakerItemFactory.getItemCreateEntry(toCreate, stimulantid, reagentids);
                }

                short createStatus = getCreateStatus(c, recipe);

                switch (createStatus) {
                    case -1:// non-available for Maker itemid has been tried to forge
                        FilePrinter.printError(FilePrinter.EXPLOITS, "Player " + c.getPlayer().getName() + " tried to craft itemid " + toCreate + " using the Maker skill.");
                        c.sendPacket(CWvsContext.serverNotice(1, "The requested item could not be crafted on this operation."));
                        c.sendPacket(CUserLocal.makerEnableActions());
                        break;

                    case 1: // no items
                        c.sendPacket(CWvsContext.serverNotice(1, "You don't have all required items in your inventory to make " + ii.getName(toCreate) + "."));
                        c.sendPacket(CUserLocal.makerEnableActions());
                        break;

                    case 2: // no meso
                        c.sendPacket(CWvsContext.serverNotice(1, "You don't have enough mesos (" + GameConstants.numberWithCommas(recipe.getCost()) + ") to complete this operation."));
                        c.sendPacket(CUserLocal.makerEnableActions());
                        break;

                    case 3: // no req level
                        c.sendPacket(CWvsContext.serverNotice(1, "You don't have enough level to complete this operation."));
                        c.sendPacket(CUserLocal.makerEnableActions());
                        break;

                    case 4: // no req skill level
                        c.sendPacket(CWvsContext.serverNotice(1, "You don't have enough Maker level to complete this operation."));
                        c.sendPacket(CUserLocal.makerEnableActions());
                        break;

                    case 5: // inventory full
                        c.sendPacket(CWvsContext.serverNotice(1, "Your inventory is full."));
                        c.sendPacket(CUserLocal.makerEnableActions());
                        break;

                    default:
                        if (toDisassemble != -1) {
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.EQUIP, (short) pos, (short) 1, false);
                        } else {
                            recipe.getReqItems().forEach(pr -> c.getAbstractPlayerInteraction().gainItem(pr.getLeft(),
                                  (short) -pr.getRight(), false));
                        }

                        int cost = recipe.getCost();
                        if (stimulantid == -1 && reagentids.isEmpty()) {
                            if (cost > 0) {
                                c.getPlayer().gainMeso(-cost, false);
                            }

                            for (Pair<Integer, Integer> pr : recipe.getGainItems()) {
                                c.getPlayer().setCS(true);
                                c.getAbstractPlayerInteraction().gainItem(pr.getLeft(), pr.getRight().shortValue(), false);
                                c.getPlayer().setCS(false);
                            }
                        } else {
                            toCreate = recipe.getGainItems().getFirst().getLeft();

                            if (stimulantid != -1) {
                                c.getAbstractPlayerInteraction().gainItem(stimulantid, (short) -1, false);
                            }
                            if (!reagentids.isEmpty()) {
                                reagentids.forEach((key, value) -> c.getAbstractPlayerInteraction().gainItem(key, (short) (-1 * value), false));
                            }

                            if (cost > 0) {
                                c.getPlayer().gainMeso(-cost, false);
                            }
                            makerSucceeded = addBoostedMakerItem(c, toCreate, stimulantid, reagentids);
                        }

                        // thanks inhyuk for noticing missing MAKER_RESULT packets
                        if (type == 3) {
                            c.sendPacket(CUserLocal.makerResultCrystal(recipe.getGainItems().getFirst().getLeft(), recipe.getReqItems().getFirst().getLeft()));
                        } else if (type == 4) {
                            c.sendPacket(CUserLocal.makerResultDesynth(recipe.getReqItems().getFirst().getLeft(), recipe.getCost(), recipe.getGainItems()));
                        } else {
                            c.sendPacket(CUserLocal.makerResult(makerSucceeded, recipe.getGainItems().getFirst().getLeft(), recipe.getGainItems().getFirst().getRight(), recipe.getCost(), recipe.getReqItems(), stimulantid, new LinkedList<>(reagentids.keySet())));
                        }

                        c.sendPacket(CUser.showMakerEffect(makerSucceeded));
                        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CUser.showForeignMakerEffect(c.getPlayer().getId(), makerSucceeded), false);

                        if (toCreate == 4260003 && type == 3 && c.getPlayer().getQuestStatus(6033) == 1) {
                            c.getAbstractPlayerInteraction().setQuestProgress(6033, 1);
                        }
                }
            } finally {
                c.releaseClient();
            }
        }
    }

    // checks and prevents hackers from PE'ing Maker operations with invalid operations
    private static boolean removeOddMakerReagents(int toCreate, Map<Integer, Short> reagentids) {
        Map<Integer, Integer> reagentType = new LinkedHashMap<>();
        List<Integer> toRemove = new LinkedList<>();

        boolean isWeapon = ItemConstants.isWeapon(toCreate) || YamlConfig.config.server.USE_MAKER_PERMISSIVE_ATKUP;  // thanks Vcoc for finding a case where a weapon wouldn't be counted as such due to a bounding on isWeapon

        for (Map.Entry<Integer, Short> r : reagentids.entrySet()) {
            int curRid = r.getKey();
            int type = r.getKey() / 100;

            if (type < 42502 && !isWeapon) {     // only weapons should gain w.att/m.att from these.
                return false;   //toRemove.add(curRid);
            } else {
                Integer tableRid = reagentType.get(type);

                if (tableRid != null) {
                    if (tableRid < curRid) {
                        toRemove.add(tableRid);
                        reagentType.put(type, curRid);
                    } else {
                        toRemove.add(curRid);
                    }
                } else {
                    reagentType.put(type, curRid);
                }
            }
        }

        // removing less effective gems of repeated type
        toRemove.forEach(reagentids::remove);

        // the Maker skill will use only one of each gem
        reagentids.keySet().forEach(i -> reagentids.put(i, (short) 1));
        return true;
    }

    private static int getMakerReagentSlots(int itemId) {
        try {
            int eqpLevel = ii.getEquipLevelReq(itemId);

            if (eqpLevel < 78) {
                return 1;
            } else if (eqpLevel >= 78 && eqpLevel < 108) {
                return 2;
            } else {
                return 3;
            }
        } catch (NullPointerException npe) {
            return 0;
        }
    }

    private static Pair<Integer, List<Pair<Integer, Integer>>> generateDisassemblyInfo(int itemId) {
        int recvFee = ii.getMakerDisassembledFee(itemId);
        if (recvFee > -1) {
            List<Pair<Integer, Integer>> gains = ii.getMakerDisassembledItems(itemId);
            if (!gains.isEmpty()) {
                return new Pair<>(recvFee, gains);
            }
        }

        return null;
    }

    public static int getMakerSkillLevel(MapleCharacter chr) {
        return chr.getSkillLevel((chr.getJob().getId() / 1000) * 10000000 + 1007);
    }

    private static short getCreateStatus(MapleClient c, MakerItemCreateEntry recipe) {
        if (recipe.isInvalid()) {
            return -1;
        }

        if (!hasItems(c, recipe)) {
            return 1;
        }

        if (c.getPlayer().getMeso() < recipe.getCost()) {
            return 2;
        }

        if (c.getPlayer().getLevel() < recipe.getReqLevel()) {
            return 3;
        }

        if (getMakerSkillLevel(c.getPlayer()) < recipe.getReqSkillLevel()) {
            return 4;
        }

        List<Integer> addItemids = new LinkedList<>();
        List<Integer> addQuantity = new LinkedList<>();
        List<Integer> rmvItemids = new LinkedList<>();
        List<Integer> rmvQuantity = new LinkedList<>();

        for (Pair<Integer, Integer> p : recipe.getReqItems()) {
            rmvItemids.add(p.getLeft());
            rmvQuantity.add(p.getRight());
        }

        for (Pair<Integer, Integer> p : recipe.getGainItems()) {
            addItemids.add(p.getLeft());
            addQuantity.add(p.getRight());
        }

        if (!c.getAbstractPlayerInteraction().canHoldAllAfterRemoving(addItemids, addQuantity, rmvItemids, rmvQuantity)) {
            return 5;
        }

        return 0;
    }

    private static boolean hasItems(MapleClient c, MakerItemCreateEntry recipe) {
        return recipe.getReqItems().stream()
                .noneMatch(p -> c.getPlayer().getInventory(ItemConstants.getInventoryType(p.getLeft())).countById(p.getLeft()) < p.getRight());
    }

    private static boolean addBoostedMakerItem(MapleClient c, int itemid, int stimulantid, Map<Integer, Short> reagentids) {
        if (stimulantid != -1 && !ItemInformationProvider.rollSuccessChance(90.0)) {
            return false;
        }

        Item item = ii.getEquipById(itemid);
        if (item == null) {
            return false;
        }

        Equip eqp = (Equip) item;
        if (ItemConstants.isAccessory(item.getItemId()) && eqp.getUpgradeSlots() <= 0) {
            eqp.setUpgradeSlots(3);
        }

        if (YamlConfig.config.server.USE_ENHANCED_CRAFTING == true) {
            if (!(c.getPlayer().isGM() && YamlConfig.config.server.USE_PERFECT_GM_SCROLL)) {
                eqp.setUpgradeSlots((byte) (eqp.getUpgradeSlots() + 1));
            }
            item = ItemInformationProvider.getInstance().scrollEquipWithId(eqp, 2049100, true, 2049100, c.getPlayer().isGM());
        }

        if (!reagentids.isEmpty()) {
            Map<String, Integer> stats = new LinkedHashMap<>();
            List<Short> randOption = new LinkedList<>();
            List<Short> randStat = new LinkedList<>();

            for (Map.Entry<Integer, Short> r : reagentids.entrySet()) {
                Pair<String, Integer> reagentBuff = ii.getMakerReagentStatUpgrade(r.getKey());

                if (reagentBuff != null) {
                    String s = reagentBuff.getLeft();

                    if (s.substring(0, 4).contains("rand")) {
                        if (s.substring(4).equals("Stat")) {
                            randStat.add((short) (reagentBuff.getRight() * r.getValue()));
                        } else {
                            randOption.add((short) (reagentBuff.getRight() * r.getValue()));
                        }
                    } else {
                        String stat = s.substring(3);

                        if (!stat.equals("ReqLevel")) {    // improve req level... really?
                           stat = switch (stat) {
                              case "MaxHP" -> "MHP";
                              case "MaxMP" -> "MMP";
                              default -> stat;
                           };

                            stats.merge(stat, reagentBuff.getRight() * r.getValue(), Integer::sum);
                        }
                    }
                }
            }

            ItemInformationProvider.improveEquipStats(eqp, stats);
            for (Short sh : randStat) {
                ii.scrollOptionEquipWithChaos(eqp, sh, false);
            }

            for (Short sh : randOption) {
                ii.scrollOptionEquipWithChaos(eqp, sh, true);
            }
        }

        if (stimulantid != -1) {
            eqp = ii.randomizeUpgradeStats(eqp);
        }

        MapleInventoryManipulator.addFromDrop(c, item, false, -1);
        return true;
    }
}
