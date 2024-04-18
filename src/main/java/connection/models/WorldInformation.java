package connection.models;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import connection.constants.WorldState;
import net.packet.OutPacket;
import net.server.world.World;

public record WorldInformation(int id, String name, WorldState state, String eventDescription, short eventExpRate,
                               short eventDropRate, List<ChannelInformation> channelInformation, List<WorldBalloon> balloons) {
   public static WorldInformation fromWorld(World world) {
      List<ChannelInformation> channelInformation = world.getChannels().stream().map(ChannelInformation::fromChannel).collect(
            Collectors.toList());

      return new WorldInformation(world.getId(), world.getName(), world.getFlag(), world.getEventMessage(),
            (short) world.getExpRate(), (short) world.getDropRate(), channelInformation, Collections.emptyList());
   }

   public void encode(OutPacket p) {
      p.writeByte(id);
      p.writeString(name);
      p.writeByte(state.getState());
      p.writeString(eventDescription);
      p.writeShort(eventExpRate * 100);
      p.writeShort(eventDropRate * 100);
      p.writeByte(channelInformation.size());
      channelInformation.stream().map(c -> c.encode(id, name)).forEach(f -> f.accept(p));
      p.writeShort(balloons().size());
      balloons.forEach(b -> b.encode(p));
   }
}
