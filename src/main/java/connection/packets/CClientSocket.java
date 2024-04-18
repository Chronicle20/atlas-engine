package connection.packets;

import java.net.InetAddress;

import connection.constants.SendOpcode;
import net.encryption.InitializationVector;
import net.packet.ByteBufOutPacket;
import net.packet.OutPacket;
import net.packet.Packet;

public class CClientSocket {
   /**
    * Gets a packet telling the client the IP of the new channel.
    *
    * @param inetAddr The InetAddress of the requested channel server.
    * @param port     The port the channel is on.
    * @return The server IP packet.
    */
   public static Packet getChannelChange(InetAddress inetAddr, int port) {
      final OutPacket p = OutPacket.create(SendOpcode.CHANGE_CHANNEL);
      p.writeByte(1);
      byte[] addr = inetAddr.getAddress();
      p.writeBytes(addr);
      p.writeShort(port);
      return p;
   }

   /**
    * Sends a ping packet.
    *
    * @return The packet.
    */
   public static Packet getPing() {
      return OutPacket.create(SendOpcode.PING);
   }

   /**
    * Sends a hello packet.
    *
    * @param mapleVersion The maple client version.
    * @param sendIv       the IV in use by the server for sending
    * @param recvIv       the IV in use by the server for receiving
    */
   public static Packet getHello(short mapleVersion, InitializationVector sendIv, InitializationVector recvIv) {
      OutPacket p = new ByteBufOutPacket();
      p.writeShort(0x0E);
      p.writeShort(mapleVersion);
      p.writeString("1");
      p.writeBytes(recvIv.getBytes());
      p.writeBytes(sendIv.getBytes());
      p.writeByte(3);
      return p;
   }
}
