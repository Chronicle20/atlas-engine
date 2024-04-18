package connection.models;

import java.util.function.Consumer;

import net.packet.OutPacket;
import net.server.channel.Channel;

public record ChannelInformation(int id, int capacity, boolean adult) {
   public Consumer<OutPacket> encode(int worldId, String worldName) {
      return p -> {
         p.writeString(String.format("%s - %d", worldName, id));
         p.writeInt(capacity);
         p.writeByte(worldId);
         p.writeByte(id - 1);
         p.writeBool(adult);
      };
   }

   public static ChannelInformation fromChannel(Channel channel) {
      return new ChannelInformation(channel.getId(), channel.getChannelCapacity(), channel.isAdultChannel());
   }
}