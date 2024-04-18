package net.netty;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleClient;
import config.YamlConfig;
import connection.packets.CClientSocket;
import constants.net.ServerConstants;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import net.encryption.ClientCyphers;
import net.encryption.InitializationVector;
import net.encryption.PacketCodec;
import net.packet.logging.InPacketLogger;
import net.packet.logging.OutPacketLogger;

public abstract class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
   static final AtomicLong sessionId = new AtomicLong(7777);
   private static final Logger log = LoggerFactory.getLogger(ServerChannelInitializer.class);
   private static final int IDLE_TIME_SECONDS = 30;
   private static final boolean LOG_PACKETS = YamlConfig.config.server.USE_DEBUG_SHOW_PACKET;
   private static final ChannelHandler sendPacketLogger = new OutPacketLogger();
   private static final ChannelHandler receivePacketLogger = new InPacketLogger();

   String getRemoteAddress(Channel channel) {
      String remoteAddress = "null";
      try {
         remoteAddress = ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
      } catch (NullPointerException npe) {
         log.warn("Unable to get remote address from netty Channel: {}", channel, npe);
      }

      return remoteAddress;
   }

   void initPipeline(SocketChannel socketChannel, MapleClient client) {
      final InitializationVector sendIv = InitializationVector.generateSend();
      final InitializationVector recvIv = InitializationVector.generateReceive();
      writeInitialUnencryptedHelloPacket(socketChannel, sendIv, recvIv);
      setUpHandlers(socketChannel.pipeline(), sendIv, recvIv, client);
   }

   private void writeInitialUnencryptedHelloPacket(SocketChannel socketChannel, InitializationVector sendIv,
                                                   InitializationVector recvIv) {
      socketChannel.writeAndFlush(
            Unpooled.wrappedBuffer(CClientSocket.getHello(ServerConstants.VERSION, sendIv, recvIv).getBytes()));
   }

   private void setUpHandlers(ChannelPipeline pipeline, InitializationVector sendIv, InitializationVector recvIv,
                              MapleClient client) {
      pipeline.addLast("IdleStateHandler", new IdleStateHandler(0, 0, IDLE_TIME_SECONDS));
      pipeline.addLast("PacketCodec", new PacketCodec(ClientCyphers.of(sendIv, recvIv)));
      pipeline.addLast("MapleClient", client);

      if (LOG_PACKETS) {
         pipeline.addBefore("MapleClient", "SendPacketLogger", sendPacketLogger);
         pipeline.addBefore("MapleClient", "ReceivePacketLogger", receivePacketLogger);
      }
   }
}
