package connection.packets;

import java.util.List;

import client.MapleCharacter;
import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CMapleTVMan {
   /**
    * Sends MapleTV
    *
    * @param chr      The character shown in TV
    * @param messages The message sent with the TV
    * @param type     The type of TV
    * @param partner  The partner shown with chr
    * @return the SEND_TV packet
    */
   public static Packet sendTV(MapleCharacter chr, List<String> messages, int type, MapleCharacter partner) {
      final OutPacket p = OutPacket.create(SendOpcode.SEND_TV);
      p.writeByte(partner != null ? 3 : 1);
      p.writeByte(type); //Heart = 2  Star = 1  Normal = 0
      CCommon.addCharLook(p, chr, false);
      p.writeString(chr.getName());
      if (partner != null) {
         p.writeString(partner.getName());
      } else {
         p.writeShort(0);
      }
      for (int i = 0; i < messages.size(); i++) {
         if (i == 4 && messages.get(4)
               .length() > 15) {
            p.writeString(messages.get(4)
                  .substring(0, 15));
         } else {
            p.writeString(messages.get(i));
         }
      }
      p.writeInt(1337); // time limit shit lol 'Your thing still start in blah blah seconds'
      if (partner != null) {
         CCommon.addCharLook(p, partner, false);
      }
      return p;
   }

   /**
    * Removes TV
    *
    * @return The Remove TV Packet
    */
   public static Packet removeTV() {
      final OutPacket p = OutPacket.create(SendOpcode.REMOVE_TV);
      return p;
   }

   public static Packet enableTV() {
      final OutPacket p = OutPacket.create(SendOpcode.ENABLE_TV);
      p.writeInt(0);
      p.writeByte(0);
      return p;
   }
}
