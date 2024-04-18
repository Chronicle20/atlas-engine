package connection.packets;

import java.util.Collection;

import client.inventory.Item;
import client.inventory.MapleInventoryType;
import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CTrunkDlg {
   public static Packet getStorage(int npcId, byte slots, Collection<Item> items, int meso) {
      final OutPacket p = OutPacket.create(SendOpcode.STORAGE);
      p.writeByte(0x16);
      p.writeInt(npcId);
      p.writeByte(slots);
      p.writeShort(0x7E);
      p.writeShort(0);
      p.writeInt(0);
      p.writeInt(meso);
      p.writeShort(0);
      p.writeByte((byte) items.size());
      for (Item item : items) {
         CCommon.addItemInfo(p, item, true);
      }
      p.writeShort(0);
      p.writeByte(0);
      return p;
   }

   /*
    * 0x0A = Inv full
    * 0x0B = You do not have enough mesos
    * 0x0C = One-Of-A-Kind error
    */
   public static Packet getStorageError(byte i) {
      final OutPacket p = OutPacket.create(SendOpcode.STORAGE);
      p.writeByte(i);
      return p;
   }

   public static Packet mesoStorage(byte slots, int meso) {
      final OutPacket p = OutPacket.create(SendOpcode.STORAGE);
      p.writeByte(0x13);
      p.writeByte(slots);
      p.writeShort(2);
      p.writeShort(0);
      p.writeInt(0);
      p.writeInt(meso);
      return p;
   }

   public static Packet storeStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
      final OutPacket p = OutPacket.create(SendOpcode.STORAGE);
      p.writeByte(0xD);
      p.writeByte(slots);
      p.writeShort(type.getBitfieldEncoding());
      p.writeShort(0);
      p.writeInt(0);
      p.writeByte(items.size());
      for (Item item : items) {
         CCommon.addItemInfo(p, item, true);
      }
      return p;
   }

   public static Packet takeOutStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
      final OutPacket p = OutPacket.create(SendOpcode.STORAGE);
      p.writeByte(0x9);
      p.writeByte(slots);
      p.writeShort(type.getBitfieldEncoding());
      p.writeShort(0);
      p.writeInt(0);
      p.writeByte(items.size());
      for (Item item : items) {
         CCommon.addItemInfo(p, item, true);
      }
      return p;
   }

   public static Packet arrangeStorage(byte slots, Collection<Item> items) {
      final OutPacket p = OutPacket.create(SendOpcode.STORAGE);
      p.writeByte(0xF);
      p.writeByte(slots);
      p.writeByte(124);
      p.skip(10);
      p.writeByte(items.size());
      for (Item item : items) {
         CCommon.addItemInfo(p, item, true);
      }
      p.writeByte(0);
      return p;
   }
}
