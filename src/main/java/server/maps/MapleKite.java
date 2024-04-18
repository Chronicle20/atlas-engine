package server.maps;

import java.awt.*;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CMessageBoxPool;
import net.packet.Packet;

public class MapleKite extends AbstractMapleMapObject {

   private final Point pos;
   private final MapleCharacter owner;
   private final String text;
   private final int ft;
   private final int itemid;

   public MapleKite(MapleCharacter owner, String text, int itemid) {
      this.owner = owner;
      this.pos = owner.getPosition();
      this.ft = owner.getFh();
      this.text = text;
      this.itemid = itemid;
   }

   @Override
   public MapleMapObjectType getType() {
      return MapleMapObjectType.KITE;
   }

   @Override
   public Point getPosition() {
      return pos.getLocation();
   }

   @Override
   public void setPosition(Point position) {
      throw new UnsupportedOperationException();
   }

   public MapleCharacter getOwner() {
      return owner;
   }

   @Override
   public void sendSpawnData(MapleClient client) {
      client.sendPacket(makeSpawnData());
   }

   @Override
   public void sendDestroyData(MapleClient client) {
      client.sendPacket(makeDestroyData());
   }

   public final Packet makeSpawnData() {
      return CMessageBoxPool.spawnKite(getObjectId(), itemid, owner.getName(), text, pos, ft);
   }

   public final Packet makeDestroyData() {
      return CMessageBoxPool.removeKite(getObjectId(), 0);
   }
}