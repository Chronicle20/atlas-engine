package connection.packets;

import client.MapleCharacter;
import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CUIMessenger {
   public static Packet messengerInvite(String from, int messengerid) {
      final OutPacket p = OutPacket.create(SendOpcode.MESSENGER);
      p.writeByte(0x03);
      p.writeString(from);
      p.writeByte(0);
      p.writeInt(messengerid);
      p.writeByte(0);
      return p;
   }

   public static Packet addMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
      final OutPacket p = OutPacket.create(SendOpcode.MESSENGER);
      p.writeByte(0x00);
      p.writeByte(position);
      CCommon.addCharLook(p, chr, true);
      p.writeString(from);
      p.writeByte(channel);
      p.writeByte(0x00);
      return p;
   }

   public static Packet removeMessengerPlayer(int position) {
      final OutPacket p = OutPacket.create(SendOpcode.MESSENGER);
      p.writeByte(0x02);
      p.writeByte(position);
      return p;
   }

   public static Packet updateMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
      final OutPacket p = OutPacket.create(SendOpcode.MESSENGER);
      p.writeByte(0x07);
      p.writeByte(position);
      CCommon.addCharLook(p, chr, true);
      p.writeString(from);
      p.writeByte(channel);
      p.writeByte(0x00);
      return p;
   }

   public static Packet joinMessenger(int position) {
      final OutPacket p = OutPacket.create(SendOpcode.MESSENGER);
      p.writeByte(0x01);
      p.writeByte(position);
      return p;
   }

   public static Packet messengerChat(String text) {
      final OutPacket p = OutPacket.create(SendOpcode.MESSENGER);
      p.writeByte(0x06);
      p.writeString(text);
      return p;
   }

   public static Packet messengerNote(String text, int mode, int mode2) {
      final OutPacket p = OutPacket.create(SendOpcode.MESSENGER);
      p.writeByte(mode);
      p.writeString(text);
      p.writeByte(mode2);
      return p;
   }
}
