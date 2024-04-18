package connection.packets;

import java.awt.*;

import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import server.maps.MapleReactor;

public class CReactorPool {
   // is there a way to trigger reactors without performing the hit animation?
   public static Packet triggerReactor(MapleReactor reactor, int stance) {
      Point pos = reactor.getPosition();
      final OutPacket p = OutPacket.create(SendOpcode.REACTOR_HIT);
      p.writeInt(reactor.getObjectId());
      p.writeByte(reactor.getState());
      p.writePos(pos);
      p.writeByte(stance);
      p.writeShort(0);
      p.writeByte(5); // frame delay, set to 5 since there doesn't appear to be a fixed formula for it
      return p;
   }

   // is there a way to spawn reactors non-animated?
   public static Packet spawnReactor(MapleReactor reactor) {
      Point pos = reactor.getPosition();
      final OutPacket p = OutPacket.create(SendOpcode.REACTOR_SPAWN);
      p.writeInt(reactor.getObjectId());
      p.writeInt(reactor.getId());
      p.writeByte(reactor.getState());
      p.writePos(pos);
      p.writeByte(0);
      p.writeShort(0);
      return p;
   }

   public static Packet destroyReactor(MapleReactor reactor) {
      Point pos = reactor.getPosition();
      final OutPacket p = OutPacket.create(SendOpcode.REACTOR_DESTROY);
      p.writeInt(reactor.getObjectId());
      p.writeByte(reactor.getState());
      p.writePos(pos);
      return p;
   }
}
