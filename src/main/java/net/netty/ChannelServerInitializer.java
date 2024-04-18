package net.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleClient;
import io.netty.channel.socket.SocketChannel;
import net.PacketProcessor;
import net.server.Server;
import net.server.coordinator.session.MapleSessionCoordinator;

public class ChannelServerInitializer extends ServerChannelInitializer {
    private static final Logger log = LoggerFactory.getLogger(ChannelServerInitializer.class);

    private final int world;
    private final int channel;

    public ChannelServerInitializer(int world, int channel) {
        this.world = world;
        this.channel = channel;
    }

    @Override
    public void initChannel(SocketChannel socketChannel) {
        final String clientIp = socketChannel.remoteAddress().getHostString();
        log.debug("Client connecting to world {}, channel {} from {}", world, channel, clientIp);

        PacketProcessor packetProcessor = PacketProcessor.getChannelServerProcessor(world, channel);
        final long clientSessionId = sessionId.getAndIncrement();
        final String remoteAddress = getRemoteAddress(socketChannel);
        final MapleClient client = MapleClient.createChannelClient(clientSessionId, remoteAddress, packetProcessor, world, channel);

        if (Server.getInstance().getChannel(world, channel).isEmpty()) {
            MapleSessionCoordinator.getInstance().closeSession(client, true);
            socketChannel.close();
            return;
        }

        initPipeline(socketChannel, client);
    }
}
