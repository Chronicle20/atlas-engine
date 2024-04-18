package net.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleClient;
import io.netty.channel.socket.SocketChannel;
import net.PacketProcessor;
import net.server.coordinator.session.MapleSessionCoordinator;

public class LoginServerInitializer extends ServerChannelInitializer {
   private static final Logger log = LoggerFactory.getLogger(LoginServerInitializer.class);

   @Override
   public void initChannel(SocketChannel socketChannel) {
      final String clientIp = socketChannel.remoteAddress().getHostString();
      log.debug("Client connected to login server from {} ", clientIp);

      PacketProcessor packetProcessor = PacketProcessor.getLoginServerProcessor();
      final long clientSessionId = sessionId.getAndIncrement();
      final String remoteAddress = getRemoteAddress(socketChannel);
      final MapleClient client =
            MapleClient.createLoginClient(clientSessionId, remoteAddress, packetProcessor, LoginServer.WORLD_ID,
                  LoginServer.CHANNEL_ID);

      if (!MapleSessionCoordinator.getInstance().canStartLoginSession(client)) {
         socketChannel.close();
         return;
      }

      initPipeline(socketChannel, client);
   }
}
