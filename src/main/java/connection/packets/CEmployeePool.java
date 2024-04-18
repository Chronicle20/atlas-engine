package connection.packets;

import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import server.maps.MapleHiredMerchant;

public class CEmployeePool {
   public static Packet spawnHiredMerchantBox(MapleHiredMerchant hm) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_HIRED_MERCHANT);
      p.writeInt(hm.getOwnerId());
      p.writeInt(hm.getItemId());
      p.writeShort((short) hm.getPosition()
            .getX());
      p.writeShort((short) hm.getPosition()
            .getY());
      p.writeShort(0);
      p.writeString(hm.getOwner());
      p.writeByte(0x05);
      p.writeInt(hm.getObjectId());
      p.writeString(hm.getDescription());
      p.writeByte(hm.getItemId() % 100);
      p.writeBytes(new byte[]{1, 4});
      return p;
   }

   public static Packet removeHiredMerchantBox(int id) {
      final OutPacket p = OutPacket.create(SendOpcode.DESTROY_HIRED_MERCHANT);
      p.writeInt(id);
      return p;
   }

   public static Packet updateHiredMerchantBox(MapleHiredMerchant hm) {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_HIRED_MERCHANT);
      p.writeInt(hm.getOwnerId());

      updateHiredMerchantBoxInfo(p, hm);
      return p;
   }

   private static void updateHiredMerchantBoxInfo(OutPacket p, MapleHiredMerchant hm) {
      p.writeByte(5);
      p.writeInt(hm.getObjectId());
      p.writeString(hm.getDescription());
      p.writeByte(hm.getItemId() % 100);
      p.writeBytes(hm.getShopRoomInfo());
   }
}
