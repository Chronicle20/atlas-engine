package connection.packets;

import java.util.List;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import connection.constants.CashShopOperationClientMode;
import connection.constants.SendOpcode;
import constants.game.GameConstants;
import net.packet.OutPacket;
import net.packet.Packet;
import net.server.Server;
import net.server.world.World;
import server.CashShop;
import tools.Pair;

public class CCashShop {
   public static Packet showCash(MapleCharacter mc) {
      final OutPacket p = OutPacket.create(SendOpcode.QUERY_CASH_RESULT);
      p.writeInt(mc.getCashShop().getCash(1));
      p.writeInt(mc.getCashShop().getCash(2));
      return p;
   }

   public static Packet showWorldTransferSuccess(Item item, int accountId) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.TRANSFER_WORLD_DONE.getMode());
      addCashItemInformation(p, item, accountId);
      return p;
   }

   public static Packet showNameChangeSuccess(Item item, int accountId) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.NAME_CHANGE_BUY_DONE.getMode());
      addCashItemInformation(p, item, accountId);
      return p;
   }

   public static Packet showCouponRedeemedItems(int accountId, int maplePoints, int mesos, List<Item> cashItems,
                                                List<Pair<Integer, Integer>> items) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.USE_COUPON_DONE.getMode());
      p.writeByte((byte) cashItems.size());
      for (Item item : cashItems) {
         addCashItemInformation(p, item, accountId);
      }
      p.writeInt(maplePoints);
      p.writeInt(items.size());
      for (Pair<Integer, Integer> itemPair : items) {
         int quantity = itemPair.getLeft();
         p.writeShort((short) quantity); //quantity (0 = 1 for cash items)
         p.writeShort(0x1F); //0 = ?, >=0x20 = ?, <0x20 = ? (does nothing?)
         p.writeInt(itemPair.getRight());
      }
      p.writeInt(mesos);
      return p;
   }

   public static Packet showBoughtCashPackage(List<Item> cashPackage, int accountId) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.BUY_PACKAGE_DONE.getMode());
      p.writeByte(cashPackage.size());
      for (Item item : cashPackage) {
         addCashItemInformation(p, item, accountId);
      }
      p.writeShort(0);
      return p;
   }

   public static Packet showBoughtQuestItem(int itemId) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.BUY_NORMAL_DONE.getMode());
      p.writeInt(1);
      p.writeShort(1);
      p.writeByte(0x0B);
      p.writeByte(0);
      p.writeInt(itemId);
      return p;
   }

   public static Packet showWishList(MapleCharacter mc, boolean update) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);

      if (update) {
         p.writeByte(CashShopOperationClientMode.SET_WISH_DONE.getMode());
      } else {
         p.writeByte(CashShopOperationClientMode.LOAD_WISH_DONE.getMode());
      }
      mc.getCashShop()
            .getWishList()
            .forEach(p::writeInt);

      for (int i = mc.getCashShop()
            .getWishList()
            .size(); i < 10; i++) {
         p.writeInt(0);
      }

      return p;
   }

   public static Packet showBoughtCashItem(Item item, int accountId) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.BUY_DONE.getMode());
      addCashItemInformation(p, item, accountId);
      return p;
   }

   public static Packet showBoughtCashRing(Item ring, String recipient, int accountId) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.COUPLE_DONE.getMode());
      addCashItemInformation(p, ring, accountId);
      p.writeString(recipient);
      p.writeInt(ring.getItemId());
      p.writeShort(1); //quantity
      return p;
   }

   /*
    * 00 = Due to an unknown error, failed
    * A3 = Request timed out. Please try again.
    * A4 = Due to an unknown error, failed + warpout
    * A5 = You don't have enough cash.
    * A6 = long as shet msg
    * A7 = You have exceeded the allotted limit of price for gifts.
    * A8 = You cannot send a gift to your own account. Log in on the char and purchase
    * A9 = Please confirm whether the character's name is correct.
    * AA = Gender restriction!
    * AB = gift cannot be sent because recipient inv is full
    * AC = exceeded the number of cash items you can have
    * AD = check and see if the character name is wrong or there is gender restrictions
    * //Skipped a few
    * B0 = Wrong Coupon Code
    * B1 = Disconnect from CS because of 3 wrong coupon codes < lol
    * B2 = Expired Coupon
    * B3 = Coupon has been used already
    * B4 = Nexon internet cafes? lolfk
    * B8 = Due to gender restrictions, the coupon cannot be used.
    * BB = inv full
    * BC = long as shet "(not?) available to purchase by a use at the premium" msg
    * BD = invalid gift recipient
    * BE = invalid receiver name
    * BF = item unavailable to purchase at this hour
    * C0 = not enough items in stock, therefore not available
    * C1 = you have exceeded spending limit of NX
    * C2 = not enough mesos? Lol not even 1 mesos xD
    * C3 = cash shop unavailable during beta phase
    * C4 = check birthday code
    * C7 = only available to users buying cash item, whatever msg too long
    * C8 = already applied for this
    * CD = You have reached the daily purchase limit for the cash shop.
    * D0 = coupon account limit reached
    * D2 = coupon system currently unavailable
    * D3 = item can only be used 15 days after registration
    * D4 = not enough gift tokens
    * D6 = fresh people cannot gift items lul
    * D7 = bad people cannot gift items >:(
    * D8 = cannot gift due to limitations
    * D9 = cannot gift due to amount of gifted times
    * DA = cannot be gifted due to technical difficulties
    * DB = cannot transfer to char below level 20
    * DC = cannot transfer char to same world
    * DD = cannot transfer char to new server world
    * DE = cannot transfer char out of this world
    * DF = cannot transfer char due to no empty char slots
    * E0 = event or free test time ended
    * E6 = item cannot be purchased with MaplePoints
    * E7 = lol sorry for the inconvenience, eh?
    * E8 = cannot purchase by anyone under 7
    */
   public static Packet showCashShopMessage(byte message) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(0x61);
      p.writeByte(message);

      return p;
   }

   public static Packet showCashInventory(MapleClient c) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.LOAD_LOCKER_DONE.getMode());
      p.writeShort(c.getPlayer()
            .getCashShop()
            .getInventory()
            .size());

      for (Item item : c.getPlayer()
            .getCashShop()
            .getInventory()) {
         addCashItemInformation(p, item, c.getAccID());
      }

      p.writeShort(c.getPlayer()
            .getStorage()
            .getSlots());
      p.writeShort(c.getCharacterSlots());
      p.writeShort(0);
      p.writeShort(4);

      return p;
   }

   public static Packet showGifts(List<Pair<Item, String>> gifts) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.LOAD_GIFT_DONE.getMode());
      p.writeShort(gifts.size());

      for (Pair<Item, String> gift : gifts) {
         CShopDlg.addCashItemInformation(p, gift.getLeft(), 0, gift.getRight());
      }

      return p;
   }

   public static Packet showGiftSucceed(String to, CashShop.CashItem item) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.LOAD_GIFT_DONE.getMode());
      p.writeString(to);
      p.writeInt(item.getItemId());
      p.writeShort(item.getCount());
      p.writeInt(item.getPrice());

      return p;
   }

   public static Packet showBoughtInventorySlots(int type, short slots) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.INCREASE_SLOT_COUNT_DONE.getMode());
      p.writeByte(type);
      p.writeShort(slots);
      return p;
   }

   public static Packet showBoughtStorageSlots(short slots) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.INCREASE_TRUNK_COUNT_DONE.getMode());
      p.writeShort(slots);
      return p;
   }

   public static Packet showBoughtCharacterSlot(short slots) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.INCREASE_CHARACTER_SLOT_COUNT_DONE.getMode());
      p.writeShort(slots);
      return p;
   }

   public static Packet takeFromCashInventory(Item item) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.MOVE_L_TO_S_DONE.getMode());
      p.writeShort(item.getPosition());
      CCommon.addItemInfo(p, item, true);
      return p;
   }

   public static Packet deleteCashItem(Item item) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.DESTROY_DONE.getMode());
      p.writeLong(item.getCashId());
      return p;
   }

   public static Packet refundCashItem(Item item, int maplePoints) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.REBATE_DONE.getMode());
      p.writeLong(item.getCashId());
      p.writeInt(maplePoints);
      return p;
   }

   public static Packet putIntoCashInventory(Item item, int accountId) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_OPERATION);
      p.writeByte(CashShopOperationClientMode.MOVE_S_TO_L_DONE.getMode());
      addCashItemInformation(p, item, accountId);
      return p;
   }

   public static Packet sendNameTransferCheck(String availableName, boolean canUseName) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_CHECK_NAME_CHANGE);
      //Send provided name back to client to add to temporary cache of checked & accepted names
      p.writeString(availableName);
      p.writeBool(!canUseName);
      return p;
   }

   /*  0: no error, send rules
               1: name change already submitted
               2: name change within a month
               3: recently banned
               4: unknown error
           */
   public static Packet sendNameTransferRules(int error) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_CHECK_NAME_CHANGE_POSSIBLE_RESULT);
      p.writeInt(0);
      p.writeByte(error);
      p.writeInt(0);

      return p;
   }

   /*  1: cannot find char info,
               2: cannot transfer under 20,
               3: cannot send banned,
               4: cannot send married,
               5: cannot send guild leader,
               6: cannot send if account already requested transfer,
               7: cannot transfer within 30days,
               8: must quit family,
               9: unknown error
           */
   public static Packet sendWorldTransferRules(int error, MapleClient c) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_CHECK_TRANSFER_WORLD_POSSIBLE_RESULT);
      p.writeInt(0); //ignored
      p.writeByte(error);
      p.writeInt(0);
      p.writeBool(error == 0); //0 = ?, otherwise list servers
      if (error == 0) {
         List<World> worlds = Server.getInstance()
               .getWorlds();
         p.writeInt(worlds.size());
         for (World world : worlds) {
            p.writeString(GameConstants.WORLD_NAMES[world.getId()]);
         }
      }
      return p;
   }

   // Cash Shop Surprise packets found thanks to Arnah (Vertisy)
   public static Packet onCashItemGachaponOpenFailed() {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_CASH_ITEM_GACHAPON_RESULT);
      p.writeByte(0xE4);
      return p;
   }

   public static Packet onCashGachaponOpenSuccess(int accountid, long sn, int remainingBoxes, Item item, int itemid,
                                                  int nSelectedItemCount, boolean bJackpot) {
      final OutPacket p = OutPacket.create(SendOpcode.CASHSHOP_CASH_ITEM_GACHAPON_RESULT);
      p.writeByte(0xE5);   // subopcode thanks to Ubaware
      p.writeLong(sn);// sn of the box used
      p.writeInt(remainingBoxes);
      addCashItemInformation(p, item, accountid);
      p.writeInt(itemid);// the itemid of the liSN?
      p.writeByte(nSelectedItemCount);// the total count now? o.O
      p.writeBool(bJackpot);// "CashGachaponJackpot"
      return p;
   }

   public static Packet enableCSUse(MapleCharacter mc) {
      return showCash(mc);
   }

   public static void addCashItemInformation(OutPacket p, Item item, int accountId) {
      CShopDlg.addCashItemInformation(p, item, accountId, null);
   }
}
