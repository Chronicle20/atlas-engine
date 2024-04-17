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
import client.MapleRing;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import config.YamlConfig;
import connection.constants.CashShopOperationServerMode;
import connection.packets.CCashShop;
import connection.packets.CWvsContext;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import server.CashShop;
import server.CashShop.CashItem;
import server.CashShop.CashItemFactory;
import server.ItemInformationProvider;
import tools.FilePrinter;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public final class CashOperationHandler extends AbstractMaplePacketHandler {

    public static boolean checkBirthday(MapleClient c, int idate) {
        int year = idate / 10000;
        int month = (idate - year * 10000) / 100;
        int day = idate - year * 10000 - month * 100;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(year, month - 1, day);
        return c.checkBirthDate(cal);
    }

    private static boolean canBuy(MapleCharacter chr, CashItem item, int cash) {
        if (item != null && item.isOnSale() && item.getPrice() <= cash) {
            FilePrinter.print(FilePrinter.CASHITEM_BOUGHT, chr + " bought " + ItemInformationProvider.getInstance()
                    .getName(item.getItemId()) + " (SN " + item.getSN() + ") for " + item.getPrice());
            return true;
        } else {
            return false;
        }
    }

    private int getPointType(boolean isMaplePoint) {
        return isMaplePoint ? 2 : 1;
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        CashShop cs = chr.getCashShop();

        if (!cs.isOpened()) {
            c.announce(CWvsContext.enableActions());
            return;
        }

        if (c.tryacquireClient()) {     // thanks Thora for finding out an exploit within cash operations
            try {
                byte modeByte = slea.readByte();
                var mode = CashShopOperationServerMode.from(modeByte);
                if (mode.isEmpty()) {
                    System.out.println("Unhandled action: " + modeByte + "\n" + slea);
                    return;
                }

                if (mode.get() == CashShopOperationServerMode.ON_BUY) {
                    int pointType = getPointType(slea.readBool());
                    int serialNumber = slea.readInt();
                    buyItem(c, cs, pointType, serialNumber);
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_BUY_PACKAGE) {
                    int pointType = getPointType(slea.readBool());
                    int serialNumber = slea.readInt();
                    buyPackage(c, cs, pointType, serialNumber);
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_BUY_NORMAL) {
                    int serialNumber = slea.readInt();
                    buyNormal(c, cs, serialNumber);
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_BUY_COUPLE) {
                    String birthday = slea.readMapleAsciiString();
                    int serialNumber = slea.readInt();
                    String recipientName = slea.readMapleAsciiString();
                    String text = slea.readMapleAsciiString();
                    buyCouple(c, cs, birthday, serialNumber, recipientName, text);
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_BUY_CHARACTER) {
                    int pointType = getPointType(slea.readBool());
                    int serialNumber = slea.readInt();
                    // TODO implement.
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_BUY_SLOT_INC) {
                    int pointType = getPointType(slea.readBool());
                    int slotIncreaseMode = slea.readByte();
                    if (slotIncreaseMode == 0) {
                        byte type = slea.readByte();
                        slotIncreaseByButton(c, cs, pointType, type);
                        return;
                    }

                    final int serialNumber = slea.readInt();
                    slotIncreaseByItem(c, cs, pointType, serialNumber);
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_INC_TRUNK_COUNT) {
                    int pointType = getPointType(slea.readBool());
                    int slotIncreaseMode = slea.readByte();

                    if (slotIncreaseMode == 0) {
                        increaseTrunkCountByButton(c, cs, pointType);
                        return;
                    }
                    final int serialNumber = slea.readInt();
                    increaseTrunkCountByItem(c, cs, pointType, serialNumber);
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_ENABLE_EQUIP_SLOT_EXT) {
                    int pointType = getPointType(slea.readBool());
                    int serialNumber = slea.readInt();
                    System.out.println("Unhandled action: " + modeByte + "\n");
                    // TODO implement.
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_REBATE_LOCKER_ITEM) {
                    String unknown1 = slea.readMapleAsciiString();
                    Long unknown2 = slea.readLong();
                    System.out.println("Unhandled action: " + modeByte + "\n");
                    // TODO implement.
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.REQUEST_CASH_PURCHASE_RECORD) {
                    int serialNumber = slea.readInt();
                    System.out.println("Unhandled action: " + modeByte + "\n");
                    // TODO implement.
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.SEND_GIFTS_PACKET) {
                    int unknown1 = slea.readInt();
                    System.out.println("Unhandled action: " + modeByte + "\n");
                    // TODO implement.
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_INC_CHARACTER_SLOT_COUNT) {
                    int pointType = getPointType(slea.readBool());
                    final int serialNumber = slea.readInt();
                    increaseCharacterSlotCount(c, cs, pointType, serialNumber);
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_GIFT_MATE_INFO_RESULT) {
                    byte unknown1 = slea.readByte();
                    String birthday = slea.readMapleAsciiString();
                    int serialNumber = slea.readInt();
                    String recipientName = slea.readMapleAsciiString();
                    String message = slea.readMapleAsciiString();
                    giftMateInfoResult(c, cs, unknown1, birthday, serialNumber, recipientName, message);
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_BUY_FRIENDSHIP) {
                    String birthday = slea.readMapleAsciiString();
                    int serialNumber = slea.readInt();
                    String recipientName = slea.readMapleAsciiString();
                    String message = slea.readMapleAsciiString();
                    onBuyFriendship(c, cs, birthday, serialNumber, recipientName, message);
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_SET_WISH) {
                    List<Integer> serialNumbers = IntStream.range(0, 10).mapToObj(i -> slea.readInt()).toList();
                    onSetWish(c, cs, serialNumbers);
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.APPLY_WISH_LIST_EVENT) {
                    System.out.println("Unhandled action: " + modeByte + "\n");
                    // TODO implement.
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_MOVE_CASH_ITEM_L_TO_S) {
                    long serialNumber = slea.readLong();
                    byte invType = slea.readByte();
                    short pos = slea.readShort();
                    moveCashItemLToS(c, cs, serialNumber, invType, pos);
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.ON_MOVE_CASH_ITEM_S_TO_L) {
                    long serialNumber = slea.readLong();
                    byte invType = slea.readByte();
                    moveCashItemSToL(c, cs, serialNumber, invType);
                    return;
                }
                if (mode.get() == CashShopOperationServerMode.BUY_TRANSFER_WORLD_ITEM_PACKET) {
                    int serialNumber = slea.readInt();
                    int newWorldSelection = slea.readInt();
                    buyTransferWorldItemPacket(c, cs, serialNumber, newWorldSelection);
                    return;
                }
            } finally {
                c.releaseClient();
            }
        } else {
            c.announce(CWvsContext.enableActions());
        }
    }

    private void buyTransferWorldItemPacket(MapleClient c, CashShop cs, int serialNumber, int newWorldSelection) {
        CashItem cItem = CashItemFactory.getItem(serialNumber);
        if (cItem == null || !canBuy(c.getPlayer(), cItem, cs.getCash(4))) {
            c.announce(CCashShop.showCashShopMessage((byte) 0));
            c.enableCSActions();
            return;
        }
        if (cItem.getSN() == 50600001 && YamlConfig.config.server.ALLOW_CASHSHOP_WORLD_TRANSFER) {
            int worldTransferError = c.getPlayer().checkWorldTransferEligibility();
            if (worldTransferError != 0 || newWorldSelection >= Server.getInstance()
                    .getWorldsSize() || Server.getInstance()
                    .getWorldsSize() <= 1) {
                c.announce(CCashShop.showCashShopMessage((byte) 0));
                return;
            } else if (newWorldSelection == c.getWorld()) {
                c.announce(CCashShop.showCashShopMessage((byte) 0xDC));
                return;
            } else if (c.getAvailableCharacterWorldSlots(newWorldSelection) < 1 || Server.getInstance()
                    .getAccountWorldCharacterCount(c.getAccID(), newWorldSelection) >= 3) {
                c.announce(CCashShop.showCashShopMessage((byte) 0xDF));
                return;
            } else if (c.getPlayer().registerWorldTransfer(newWorldSelection)) {
                Item item = cItem.toItem();
                c.announce(CCashShop.showWorldTransferSuccess(item, c.getAccID()));
                cs.gainCash(4, cItem, c.getPlayer().getWorld());
                cs.addToInventory(item);
            } else {
                c.announce(CCashShop.showCashShopMessage((byte) 0));
            }
        }
        c.enableCSActions();
    }

    private void moveCashItemSToL(MapleClient c, CashShop cs, long serialNumber, byte invType) {
        if (invType < 1 || invType > 5) {
            c.disconnect(false, false);
            return;
        }

        Optional<MapleInventoryType> type = MapleInventoryType.getByType(invType);
        if (type.isEmpty()) {
            c.enableCSActions();
            return;
        }
        MapleInventory mi = c.getPlayer().getInventory(type.get());
        Optional<Item> item = mi.findByCashId((int) serialNumber);
        if (item.isEmpty()) {
            c.enableCSActions();
            return;
        } else if (c.getPlayer()
                .getPetIndex(item.get()
                        .getPetId()
                        .orElse(-1)) > -1) {
            c.getPlayer().announce(CWvsContext.serverNotice(1, "You cannot put the pet you currently equip into the Cash Shop inventory."));
            c.enableCSActions();
            return;
        } else if (ItemConstants.isWeddingRing(item.get()
                .getItemId()) || ItemConstants.isWeddingToken(item.get()
                .getItemId())) {
            c.getPlayer().announce(CWvsContext.serverNotice(1, "You cannot put relationship items into the Cash Shop inventory."));
            c.enableCSActions();
            return;
        }
        cs.addToInventory(item.get());
        mi.removeSlot(item.get()
                .getPosition());
        c.announce(CCashShop.putIntoCashInventory(item.get(), c.getAccID()));
    }

    private void moveCashItemLToS(MapleClient c, CashShop cs, long serialNumber, byte invType, short pos) {
        Optional<Item> item = cs.findByCashId((int) serialNumber);
        if (item.isEmpty()) {
            c.enableCSActions();
            return;
        }
        if (c.getPlayer().getInventory(item.get()
                        .getInventoryType())
                .addItem(item.get()) != -1) {
            cs.removeFromInventory(item.get());
            c.announce(CCashShop.takeFromCashInventory(item.get()));

            if (item.get() instanceof Equip equip) {
               if (equip.getRingId() >= 0) {
                    MapleRing.loadFromDb(equip.getRingId())
                            .ifPresent(c.getPlayer()::addPlayerRing);
                }
            }
        }
    }

    private void onSetWish(MapleClient c, CashShop cs, List<Integer> serialNumbers) {
        cs.clearWishList();
        for (Integer serialNumber : serialNumbers) {
            CashItem cItem = CashItemFactory.getItem(serialNumber);
            if (cItem != null && cItem.isOnSale() && serialNumber != 0) {
                cs.addToWishList(serialNumber);
            }
        }
        c.announce(CCashShop.showWishList(c.getPlayer(), true));
    }

    private void onBuyFriendship(MapleClient c, CashShop cs, String birthday, int serialNumber, String recipientName, String message) {
        //TODO add birthday check
//        if (!checkBirthday(c, birthday)) {
//            c.announce(CCashShop.showCashShopMessage((byte) 0xC4));
//            return;
//        }


        CashItem itemRing = CashItemFactory.getItem(serialNumber);
        MapleCharacter partner = c.getChannelServer()
                .getPlayerStorage()
                .getCharacterByName(recipientName)
                .orElse(null);
        if (partner == null) {
            c.announce(CCashShop.showCashShopMessage((byte) 0xBE));
            return;
        }

        // Need to check to make sure its actually an equip and the right SN...
        if (itemRing.toItem() instanceof Equip eqp) {
           Pair<Integer, Integer> rings = MapleRing.createRing(itemRing.getItemId(), c.getPlayer(), partner);
            eqp.setRingId(rings.getLeft());
            cs.addToInventory(eqp);
            c.announce(CCashShop.showBoughtCashRing(eqp, partner.getName(), c.getAccID()));
            cs.gainCash(1, -itemRing.getPrice());
            cs.gift(partner.getId(), c.getPlayer().getName(), message, eqp.getSN(), rings.getRight());
            c.getPlayer().addFriendshipRing(MapleRing.loadFromDb(rings.getLeft())
                    .orElseThrow());
            c.getPlayer().sendNote(partner.getName(), message, (byte) 1);
            partner.showNote();
        }
        c.announce(CCashShop.showCash(c.getPlayer()));
    }

    private void giftMateInfoResult(MapleClient c, CashShop cs, byte unknown1, String birthday, int serialNumber, String recipientName, String message) {
        //TODO add birthday check
//        if (!checkBirthday(c, birthday)) {
//            c.announce(CCashShop.showCashShopMessage((byte) 0xC4));
//            return;
//        }

        Map<String, String> recipient = MapleCharacter.getCharacterFromDatabase(recipientName);
        if (recipient == null) {
            c.announce(CCashShop.showCashShopMessage((byte) 0xA9));
            return;
        }

        if (recipient.get("accountid")
                .equals(String.valueOf(c.getAccID()))) {
            c.announce(CCashShop.showCashShopMessage((byte) 0xA8));
            return;
        }

        CashItem cItem = CashItemFactory.getItem(serialNumber);
        if (!canBuy(c.getPlayer(), cItem, cs.getCash(4)) || message.length() < 1 || message.length() > 73) {
            c.enableCSActions();
            return;
        }

        cs.gainCash(4, cItem, c.getPlayer().getWorld());
        cs.gift(Integer.parseInt(recipient.get("id")), c.getPlayer().getName(), message, cItem.getSN());
        c.announce(CCashShop.showGiftSucceed(recipient.get("name"), cItem));
        c.announce(CCashShop.showCash(c.getPlayer()));
        c.getPlayer().sendNote(recipient.get("name"), c.getPlayer().getName() + " has sent you a gift! Go check out the Cash Shop.", (byte) 0); //fame or not
        c.getChannelServer()
                .getPlayerStorage()
                .getCharacterByName(recipient.get("name"))
                .ifPresent(MapleCharacter::showNote);
    }

    private static void increaseCharacterSlotCount(MapleClient c, CashShop cs, int pointType, int serialNumber) {
        CashItem cItem = CashItemFactory.getItem(serialNumber);
        if (!canBuy(c.getPlayer(), cItem, cs.getCash(pointType))) {
            c.enableCSActions();
            return;
        }
        if (!c.canGainCharacterSlot()) {
            c.getPlayer().dropMessage(1, "You have already used up all 12 extra character slots.");
            c.enableCSActions();
            return;
        }
        cs.gainCash(pointType, cItem, c.getPlayer().getWorld());
        if (c.gainCharacterSlot()) {
            c.announce(CCashShop.showBoughtCharacterSlot(c.getCharacterSlots()));
            c.announce(CCashShop.showCash(c.getPlayer()));
            return;
        }

        FilePrinter.printError(FilePrinter.CASHITEM_BOUGHT, "Could not add a character slot to " + MapleCharacter.makeMapleReadable(c.getPlayer().getName()) + "'s account.");
        c.enableCSActions();
    }

    private static void increaseTrunkCountByItem(MapleClient c, CashShop cs, int pointType, int serialNumber) {
        CashItem cItem = CashItemFactory.getItem(serialNumber);

        if (!canBuy(c.getPlayer(), cItem, cs.getCash(pointType))) {
            c.enableCSActions();
            return;
        }
        int qty = 8;
        if (!c.getPlayer().getStorage()
                .canGainSlots(qty)) {
            c.enableCSActions();
            return;
        }
        cs.gainCash(pointType, cItem, c.getPlayer().getWorld());
        if (c.getPlayer().getStorage()
                .gainSlots(qty)) {    // thanks ABaldParrot & Thora for detecting storage issues here
            FilePrinter.print(FilePrinter.STORAGE + c.getAccountName() + ".txt", c.getPlayer()
                    .getName() + " bought " + qty + " slots to their account storage.");
            c.getPlayer().setUsedStorage();

            c.announce(CCashShop.showBoughtStorageSlots(c.getPlayer().getStorage()
                    .getSlots()));
            c.announce(CCashShop.showCash(c.getPlayer()));
        } else {
            FilePrinter.printError(FilePrinter.CASHITEM_BOUGHT, "Could not add " + qty + " slots to " + MapleCharacter.makeMapleReadable(c.getPlayer().getName()) + "'s account.");
        }
        return;
    }

    private static void increaseTrunkCountByButton(MapleClient c, CashShop cs, int pointType) {
        if (cs.getCash(pointType) < 4000) {
            c.enableCSActions();
            return;
        }
        int qty = 4;
        if (!c.getPlayer().getStorage()
                .canGainSlots(qty)) {
            c.enableCSActions();
            return;
        }
        cs.gainCash(pointType, -4000);
        if (c.getPlayer().getStorage()
                .gainSlots(qty)) {
            FilePrinter.print(FilePrinter.STORAGE + c.getAccountName() + ".txt", c.getPlayer()
                    .getName() + " bought " + qty + " slots to their account storage.");
            c.getPlayer().setUsedStorage();

            c.announce(CCashShop.showBoughtStorageSlots(c.getPlayer().getStorage()
                    .getSlots()));
            c.announce(CCashShop.showCash(c.getPlayer()));
        } else {
            FilePrinter.printError(FilePrinter.CASHITEM_BOUGHT, "Could not add " + qty + " slots to " + MapleCharacter.makeMapleReadable(c.getPlayer().getName()) + "'s account.");
        }
    }

    private void slotIncreaseByItem(MapleClient c, CashShop cs, int pointType, int serialNumber) {
        CashItem cItem = CashItemFactory.getItem(serialNumber);
        int type = (cItem.getItemId() - 9110000) / 1000;
        if (!canBuy(c.getPlayer(), cItem, cs.getCash(pointType))) {
            c.enableCSActions();
            return;
        }
        int qty = 8;
        if (!c.getPlayer().canGainSlots(type, qty)) {
            c.enableCSActions();
            return;
        }
        cs.gainCash(pointType, cItem, c.getPlayer().getWorld());
        if (c.getPlayer().gainSlots(type, qty, false)) {
            c.announce(CCashShop.showBoughtInventorySlots(type, c.getPlayer().getSlots(type)));
            c.announce(CCashShop.showCash(c.getPlayer()));
        } else {
            FilePrinter.printError(FilePrinter.CASHITEM_BOUGHT, "Could not add " + qty + " slots of type " + type + " for player " + MapleCharacter.makeMapleReadable(c.getPlayer().getName()));
        }
    }

    private void slotIncreaseByButton(MapleClient c, CashShop cs, int pointType, byte type) {
        if (cs.getCash(pointType) < 4000) {
            c.enableCSActions();
            return;
        }
        int qty = 4;
        if (!c.getPlayer().canGainSlots(type, qty)) {
            c.enableCSActions();
            return;
        }
        cs.gainCash(pointType, -4000);
        if (c.getPlayer().gainSlots(type, qty, false)) {
            c.announce(CCashShop.showBoughtInventorySlots(type, c.getPlayer().getSlots(type)));
            c.announce(CCashShop.showCash(c.getPlayer()));
        } else {
            FilePrinter.printError(FilePrinter.CASHITEM_BOUGHT, "Could not add " + qty + " slots of type " + type + " for player " + MapleCharacter.makeMapleReadable(c.getPlayer().getName()));
        }
    }

    private static void buyItem(MapleClient c, CashShop cs, int pointType, int snCS) {
        CashItem cItem = CashItemFactory.getItem(snCS);
        if (!canBuy(c.getPlayer(), cItem, cs.getCash(pointType))) {
            FilePrinter.printError(FilePrinter.ITEM, "Denied to sell cash item with SN " + snCS);   // preventing NPE here thanks to MedicOP
            c.enableCSActions();
            return;
        }
        if (ItemConstants.isCashStore(cItem.getItemId()) && c.getPlayer()
                .getLevel() < 16) {
            c.enableCSActions();
            return;
        } else if (ItemConstants.isRateCoupon(cItem.getItemId()) && !YamlConfig.config.server.USE_SUPPLY_RATE_COUPONS) {
            c.getPlayer()
                    .dropMessage(1, "Rate coupons are currently unavailable to purchase.");
            c.enableCSActions();
            return;
        } else if (ItemConstants.isMapleLife(cItem.getItemId()) && c.getPlayer()
                .getLevel() < 30) {
            c.enableCSActions();
            return;
        }

        Item item = cItem.toItem();
        cs.gainCash(pointType, cItem, c.getPlayer()
                .getWorld());
        cs.addToInventory(item);
        c.announce(CCashShop.showBoughtCashItem(item, c.getAccID()));
        c.announce(CCashShop.showCash(c.getPlayer()));
    }

    private static void buyPackage(MapleClient c, CashShop cs, int pointType, int snCS) {
        CashItem cItem = CashItemFactory.getItem(snCS);
        if (!canBuy(c.getPlayer(), cItem, cs.getCash(pointType))) {
            FilePrinter.printError(FilePrinter.ITEM, "Denied to sell cash item with SN " + snCS);   // preventing NPE here thanks to MedicOP
            c.enableCSActions();
            return;
        }

        cs.gainCash(pointType, cItem, c.getPlayer().getWorld());

        List<Item> cashPackage = CashItemFactory.getPackage(cItem.getItemId());
        for (Item item : cashPackage) {
            cs.addToInventory(item);
        }
        c.announce(CCashShop.showBoughtCashPackage(cashPackage, c.getAccID()));
        c.announce(CCashShop.showCash(c.getPlayer()));
    }

    private static void buyNormal(MapleClient c, CashShop cs, int serialNumber) {
        if (serialNumber / 10000000 != 8) {
            c.announce(CCashShop.showCashShopMessage((byte) 0xC0));
            return;
        }

        CashItem item = CashItemFactory.getItem(serialNumber);
        if (item == null || !item.isOnSale()) {
            c.announce(CCashShop.showCashShopMessage((byte) 0xC0));
            return;
        }

        int itemId = item.getItemId();
        int itemPrice = item.getPrice();
        if (itemPrice <= 0) {
            c.announce(CCashShop.showCashShopMessage((byte) 0xC0));
            return;
        }

        if (c.getPlayer().getMeso() >= itemPrice) {
            if (c.getPlayer().canHold(itemId)) {
                c.getPlayer().gainMeso(-itemPrice, false);
                MapleInventoryManipulator.addById(c, itemId, (short) 1, "", -1);
                c.announce(CCashShop.showBoughtQuestItem(itemId));
            }
        }
        c.announce(CCashShop.showCash(c.getPlayer()));
    }

    private void buyCouple(MapleClient c, CashShop cs, String birthday, int serialNumber, String recipientName, String text) {
        //TODO not sure if this is birthday, need to validate it if possible.
        // c.announce(CCashShop.showCashShopMessage((byte) 0xC4));

        CashItem itemRing = CashItemFactory.getItem(serialNumber);
        MapleCharacter partner = c.getChannelServer()
                .getPlayerStorage()
                .getCharacterByName(recipientName)
                .orElse(null);
        if (partner == null) {
            c.getPlayer().announce(CWvsContext.serverNotice(1, "The partner you specified cannot be found.\r\nPlease make sure your partner is online and in the same channel."));
            return;
        }

        if (itemRing.toItem() instanceof Equip eqp) {
           Pair<Integer, Integer> rings = MapleRing.createRing(itemRing.getItemId(), c.getPlayer(), partner);
            eqp.setRingId(rings.getLeft());
            cs.addToInventory(eqp);
            c.announce(CCashShop.showBoughtCashItem(eqp, c.getAccID()));
            cs.gainCash(1, itemRing, c.getPlayer().getWorld());
            cs.gift(partner.getId(), c.getPlayer().getName(), text, eqp.getSN(), rings.getRight());
            c.getPlayer().addCrushRing(MapleRing.loadFromDb(rings.getLeft())
                    .orElseThrow());
            c.getPlayer().sendNote(partner.getName(), text, (byte) 1);
            partner.showNote();
        }

        c.announce(CCashShop.showCash(c.getPlayer()));
    }
}
