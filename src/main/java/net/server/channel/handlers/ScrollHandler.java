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
package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.Skill;
import client.inventory.Equip;
import client.inventory.Equip.ScrollResult;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.ModifyInventory;
import client.inventory.manipulator.MapleInventoryManipulator;
import connection.packets.CUser;
import connection.packets.CWvsContext;
import constants.SkillConstants;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import server.ItemInformationProvider;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Matze
 * @author Frz
 */
public final class ScrollHandler extends AbstractMaplePacketHandler {

    private static void announceCannotScroll(MapleClient c, boolean legendarySpirit) {
        if (legendarySpirit) {
            c.announce(CUser.getScrollEffect(c.getPlayer()
                    .getId(), Equip.ScrollResult.FAIL, false, false));
        } else {
            c.announce(CWvsContext.getInventoryFull());
        }
    }

    private static boolean canScroll(int scrollid, int itemid) {
        int sid = scrollid / 100;

        switch (sid) {
            case 20492: //scroll for accessory (pendant, belt, ring)
                return canScroll(2041100, itemid) || canScroll(2041200, itemid) || canScroll(2041300, itemid);

            default:
                return (scrollid / 100) % 100 == (itemid / 10000) % 100;
        }
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.tryacquireClient()) {
            try {
                int updateTime = slea.readInt();
                short slot = slea.readShort();
                short dst = slea.readShort();
                short bWhiteScroll = slea.readShort();
                boolean bEnchantSkill = slea.readBool();
                boolean whiteScroll = bWhiteScroll == 2;
                boolean legendarySpirit = false;

                ItemInformationProvider ii = ItemInformationProvider.getInstance();
                MapleCharacter chr = c.getPlayer();
                Equip toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED)
                        .getItem(dst);
                if (bEnchantSkill && dst >= 0) {
                    for(Skill s : c.getPlayer().getSkills().keySet()){
                        if(SkillConstants.isLegendarySpirit(s.getId())){
                            legendarySpirit = true;
                            toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
                        }
                    }
                }
                byte oldLevel = toScroll.getLevel();
                byte oldSlots = toScroll.getUpgradeSlots();
                MapleInventory useInventory = chr.getInventory(MapleInventoryType.USE);
                Item scroll = useInventory.getItem(slot);
                Optional<Item> wscroll = Optional.empty();

                if (ItemConstants.isCleanSlate(scroll.getItemId())) {
                    Map<String, Integer> eqStats = ii.getEquipStats(toScroll.getItemId());  // clean slate issue found thanks to Masterrulax
                    if (eqStats == null || eqStats.get("tuc") == 0) {
                        announceCannotScroll(c, legendarySpirit);
                        return;
                    }
                } else if (!ItemConstants.isModifierScroll(scroll.getItemId()) && toScroll.getUpgradeSlots() < 1) {
                    announceCannotScroll(c, legendarySpirit);   // thanks onechord for noticing zero upgrade slots freezing Legendary Scroll UI
                    return;
                }

                List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
                if (scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())) {
                    announceCannotScroll(c, legendarySpirit);
                    return;
                }
                if (whiteScroll) {
                    wscroll = useInventory.findById(2340000);
                    if (wscroll.isEmpty()) {
                        whiteScroll = false;
                    }
                }

                if (!ItemConstants.isChaosScroll(scroll.getItemId()) && !ItemConstants.isCleanSlate(scroll.getItemId())) {
                    if (!canScroll(scroll.getItemId(), toScroll.getItemId())) {
                        announceCannotScroll(c, legendarySpirit);
                        return;
                    }
                }

                if (ItemConstants.isCleanSlate(scroll.getItemId()) && !ii.canUseCleanSlate(toScroll)) {
                    announceCannotScroll(c, legendarySpirit);
                    return;
                }

                Equip scrolled = (Equip) ii.scrollEquipWithId(toScroll, scroll.getItemId(), whiteScroll, 0, chr.isGM());
                ScrollResult scrollSuccess = Equip.ScrollResult.FAIL; // fail
                if (scrolled == null) {
                    scrollSuccess = Equip.ScrollResult.CURSE;
                } else if (scrolled.getLevel() > oldLevel || (ItemConstants.isCleanSlate(scroll.getItemId()) && scrolled.getUpgradeSlots() == oldSlots + 1) || ItemConstants.isFlagModifier(scroll.getItemId(), scrolled.getFlag())) {
                    scrollSuccess = Equip.ScrollResult.SUCCESS;
                }

                useInventory.lockInventory();
                try {
                    if (scroll.getQuantity() < 1) {
                        announceCannotScroll(c, legendarySpirit);
                        return;
                    }

                    if (whiteScroll && !ItemConstants.isCleanSlate(scroll.getItemId())) {
                        if (wscroll.get()
                                .getQuantity() < 1) {
                            announceCannotScroll(c, legendarySpirit);
                            return;
                        }

                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, wscroll.get()
                                .getPosition(), (short) 1, false, false);
                    }

                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, scroll.getPosition(), (short) 1, false);
                } finally {
                    useInventory.unlockInventory();
                }

                final List<ModifyInventory> mods = new ArrayList<>();
                if (scrollSuccess == Equip.ScrollResult.CURSE) {
                    if (!ItemConstants.isWeddingRing(toScroll.getItemId())) {
                        mods.add(new ModifyInventory(3, toScroll));
                        if (dst < 0) {
                            MapleInventory inv = chr.getInventory(MapleInventoryType.EQUIPPED);

                            inv.lockInventory();
                            try {
                                chr.unequippedItem(toScroll);
                                inv.removeItem(toScroll.getPosition());
                            } finally {
                                inv.unlockInventory();
                            }
                        } else {
                            MapleInventory inv = chr.getInventory(MapleInventoryType.EQUIP);

                            inv.lockInventory();
                            try {
                                inv.removeItem(toScroll.getPosition());
                            } finally {
                                inv.unlockInventory();
                            }
                        }
                    } else {
                        scrolled = toScroll;
                        scrollSuccess = Equip.ScrollResult.FAIL;

                        mods.add(new ModifyInventory(3, scrolled));
                        mods.add(new ModifyInventory(0, scrolled));
                    }
                } else {
                    mods.add(new ModifyInventory(3, scrolled));
                    mods.add(new ModifyInventory(0, scrolled));
                }
                c.announce(CWvsContext.modifyInventory(true, mods));
                chr.getMap()
                        .broadcastMessage(CUser.getScrollEffect(chr.getId(), scrollSuccess, legendarySpirit, whiteScroll));
                if (dst < 0 && (scrollSuccess == Equip.ScrollResult.SUCCESS || scrollSuccess == Equip.ScrollResult.CURSE)) {
                    chr.equipChanged();
                }
            } finally {
                c.releaseClient();
            }
        }
    }
}