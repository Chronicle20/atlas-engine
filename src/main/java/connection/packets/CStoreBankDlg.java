package connection.packets;

import java.sql.SQLException;
import java.util.List;

import client.MapleCharacter;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.MapleInventoryType;
import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import tools.Pair;

public class CStoreBankDlg {
   public static Packet fredrickMessage(byte operation) {
      final OutPacket p = OutPacket.create(SendOpcode.FREDRICK_MESSAGE);
      p.writeByte(operation);
      return p;
   }

   public static Packet getFredrick(byte op) {
      final OutPacket p = OutPacket.create(SendOpcode.FREDRICK);
      p.writeByte(op);

      if (op == 0x24) {
         p.skip(8);
      } else {
         p.writeByte(0);
      }

      return p;
   }

   public static Packet getFredrick(MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.FREDRICK);
      p.writeByte(0x23);
      p.writeInt(9030000); // Fredrick
      p.writeInt(32272); //id
      p.skip(5);
      p.writeInt(chr.getMerchantNetMeso());
      p.writeByte(0);
      try {
         List<Pair<Item, MapleInventoryType>> items = ItemFactory.MERCHANT.loadItems(chr.getId(), false);
         p.writeByte(items.size());

         for (Pair<Item, MapleInventoryType> item : items) {
            CCommon.addItemInfo(p, item.getLeft(), true);
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }
      p.skip(3);
      return p;
   }
}
