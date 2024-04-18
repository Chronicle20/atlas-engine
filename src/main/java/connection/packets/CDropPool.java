package connection.packets;

import java.awt.*;

import client.MapleCharacter;
import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import server.maps.MapleMapItem;

public class CDropPool {
   public static Packet updateMapItemObject(MapleMapItem drop, boolean giveOwnership) {
      final OutPacket p = OutPacket.create(SendOpcode.DROP_ITEM_FROM_MAPOBJECT);
      p.writeByte(2);
      p.writeInt(drop.getObjectId());
      p.writeBool(drop.getMeso() > 0);
      p.writeInt(drop.getItemId());
      p.writeInt(giveOwnership ? 0 : -1);
      p.writeByte(drop.hasExpiredOwnershipTime() ? 2 : drop.getDropType());
      p.writePos(drop.getPosition());
      p.writeInt(giveOwnership ? 0 : -1);

      if (drop.getMeso() == 0) {
         CCommon.addExpirationTime(p, drop.getItem()
               .getExpiration());
      }
      p.writeByte(drop.isPlayerDrop() ? 0 : 1);
      p.writeByte(0);
      return p;
   }

   public static Packet dropItemFromMapObject(MapleCharacter player, MapleMapItem drop, Point dropfrom, Point dropto, byte mod) {
      int dropType = drop.getDropType();
      if (drop.hasClientsideOwnership(player) && dropType < 3) {
         dropType = 2;
      }

      final OutPacket p = OutPacket.create(SendOpcode.DROP_ITEM_FROM_MAPOBJECT);
      p.writeByte(mod);
      p.writeInt(drop.getObjectId());
      p.writeBool(drop.getMeso() > 0); // 1 mesos, 0 item, 2 and above all item meso bag,
      p.writeInt(drop.getItemId()); // drop object ID
      p.writeInt(drop.getClientsideOwnerId()); // owner charid/partyid :)
      p.writeByte(dropType); // 0 = timeout for non-owner, 1 = timeout for non-owner's party, 2 = FFA, 3 = explosive/FFA
      p.writePos(dropto);
      p.writeInt(drop.getDropper()
            .getObjectId()); // dropper oid, found thanks to Li Jixue

      if (mod != 2) {
         p.writePos(dropfrom);
         p.writeShort(0);//Fh?
      }
      if (drop.getMeso() == 0) {
         CCommon.addExpirationTime(p, drop.getItem()
               .getExpiration());
      }
      p.writeByte(drop.isPlayerDrop() ? 0 : 1); //pet EQP pickup
      p.writeByte(0);
      return p;
   }

   /**
    * animation: 0 - expire<br/> 1 - without animation<br/> 2 - pickup<br/> 4 -
    * explode<br/> cid is ignored for 0 and 1.<br /><br />Flagging pet as true
    * will make a pet pick up the item.
    *
    * @param oid
    * @param animation
    * @param cid
    * @param pet
    * @param slot
    * @return
    */
   public static Packet removeItemFromMap(int oid, int animation, int cid, boolean pet, int slot) {
      final OutPacket p = OutPacket.create(SendOpcode.REMOVE_ITEM_FROM_MAP);
      p.writeByte(animation); // expire
      p.writeInt(oid);
      if (animation >= 2) {
         p.writeInt(cid);
         if (pet) {
            p.writeByte(slot);
         }
      }
      return p;
   }

   /**
    * animation: 0 - expire<br/> 1 - without animation<br/> 2 - pickup<br/> 4 -
    * explode<br/> cid is ignored for 0 and 1
    *
    * @param oid
    * @param animation
    * @param cid
    * @return
    */
   public static Packet removeItemFromMap(int oid, int animation, int cid) {
      return removeItemFromMap(oid, animation, cid, false, 0);
   }

   public static Packet silentRemoveItemFromMap(int oid) {
      return removeItemFromMap(oid, 1, 0);
   }
}
