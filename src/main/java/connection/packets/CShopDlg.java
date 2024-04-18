package connection.packets;

import java.util.List;

import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import connection.headers.SendOpcode;
import constants.inventory.ItemConstants;
import net.packet.OutPacket;
import net.packet.Packet;
import server.ItemInformationProvider;
import server.MapleShopItem;
import tools.StringUtil;

public class CShopDlg {
   public static Packet getNPCShop(MapleClient c, int npcTemplateId, List<MapleShopItem> items) {
      final OutPacket p = OutPacket.create(SendOpcode.OPEN_NPC_SHOP);
      p.writeInt(npcTemplateId); // dwNpcTemplateID
      p.writeShort(items.size()); // nCount
      items.forEach(i -> writeShopItem(c, i, p));
      return p;
   }

   public static void writeShopItem(MapleClient c, MapleShopItem item, OutPacket p) {
      ItemInformationProvider ii = ItemInformationProvider.getInstance();
      p.writeInt(item.getItemId()); // nItemID
      p.writeInt(item.getPrice()); // nPrice

      p.writeInt(0); // nTokenPrice
      p.writeInt(0); // nItemPeriod
      p.writeInt(0); // nLevelLimited

      if (ItemConstants.isRechargeable(item.getItemId())) {
         p.writeShort(0);
         p.writeInt(0);
         p.writeShort(doubleToShortBits(ii.getUnitPrice(item.getItemId())));
         p.writeShort(ii.getSlotMax(c, item.getItemId()));
      } else {
         p.writeShort(1); // nQuantity
         p.writeShort(item.getBuyable()); // nMaxPerSlot
      }
   }

   /* 00 = /
    * 01 = You don't have enough in stock
    * 02 = You do not have enough mesos
    * 03 = Please check if your inventory is full or not
    * 05 = You don't have enough in stock
    * 06 = Due to an error, the trade did not happen
    * 07 = Due to an error, the trade did not happen
    * 08 = /
    * 0D = You need more items
    * 0E = CRASH; LENGTH NEEDS TO BE LONGER :O
    */
   public static Packet shopTransaction(byte code) {
      final OutPacket p = OutPacket.create(SendOpcode.CONFIRM_SHOP_TRANSACTION);
      p.writeByte(code);
      return p;
   }

   // someone thought it was a good idea to handle floating point representation through packets ROFL
   private static int doubleToShortBits(double d) {
      return (int) (Double.doubleToLongBits(d) >> 48);
   }

   public static void addCashItemInformation(OutPacket p, Item item, int accountId, String giftMessage) {
      boolean isGift = giftMessage != null;
      boolean isRing = false;
      Equip equip = null;
      if (item.getInventoryType().equals(MapleInventoryType.EQUIP)) {
         equip = (Equip) item;
         isRing = equip.getRingId() > -1;
      }
      p.writeLong(item.isPet() ? item.getPetId().orElseThrow() : isRing ? equip.getRingId() : item.getCashId());
      if (!isGift) {
         p.writeInt(accountId);
         p.writeInt(0);
      }
      p.writeInt(item.getItemId());
      if (!isGift) {
         p.writeInt(item.getSN());
         p.writeShort(item.getQuantity());
      }
      p.writeFixedString(StringUtil.getRightPaddedStr(item.getGiftFrom(), '\0', 13));
      if (isGift) {
         p.writeFixedString(StringUtil.getRightPaddedStr(giftMessage, '\0', 73));
         return;
      }
      CCommon.addExpirationTime(p, item.getExpiration());
      p.writeLong(0);
   }
}
