package connection.packets;

import java.util.List;

import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import server.DueyPackage;

public class CParcelDlg {
   public static Packet removeItemFromDuey(boolean remove, int Package) {
      final OutPacket p = OutPacket.create(SendOpcode.PARCEL);
      p.writeByte(0x17);
      p.writeInt(Package);
      p.writeByte(remove ? 3 : 4);
      return p;
   }

   public static Packet sendDueyParcelReceived(String from, boolean quick) {    // thanks inhyuk
      final OutPacket p = OutPacket.create(SendOpcode.PARCEL);
      p.writeByte(0x19);
      p.writeString(from);
      p.writeBool(quick);
      return p;
   }

   public static Packet sendDueyParcelNotification(boolean quick) {
      final OutPacket p = OutPacket.create(SendOpcode.PARCEL);
      p.writeByte(0x1B);
      p.writeBool(quick);  // 0 : package received, 1 : quick delivery package
      return p;
   }

   public static Packet sendDuey(int operation, List<DueyPackage> packages) {
      final OutPacket p = OutPacket.create(SendOpcode.PARCEL);
      p.writeByte(operation);
      if (operation == 8) {
         p.writeByte(0);
         p.writeByte(packages.size());
         for (DueyPackage dp : packages) {
            p.writeInt(dp.getPackageId());
            p.writeFixedString(dp.getSender());
            for (int i = dp.getSender()
                  .length(); i < 13; i++) {
               p.writeByte(0);
            }

            p.writeInt(dp.getMesos());
            p.writeLong(CCommon.getTime(dp.sentTimeInMilliseconds()));

            String msg = dp.getMessage();
            if (msg != null) {
               p.writeInt(1);
               p.writeFixedString(msg);
               for (int i = msg.length(); i < 200; i++) {
                  p.writeByte(0);
               }
            } else {
               p.writeInt(0);
               p.skip(200);
            }

            p.writeByte(0);
            if (dp.getItem() != null) {
               p.writeByte(1);
               CCommon.addItemInfo(p, dp.getItem(), true);
            } else {
               p.writeByte(0);
            }
         }
         p.writeByte(0);
      }

      return p;
   }

   public static Packet sendDueyMSG(byte operation) {
      return sendDuey(operation, null);
   }
}
