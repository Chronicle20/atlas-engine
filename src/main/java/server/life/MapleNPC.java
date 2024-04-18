package server.life;

import client.MapleClient;
import connection.packets.CNpcPool;
import server.MapleShopFactory;
import server.maps.MapleMapObjectType;

public class MapleNPC extends AbstractLoadedMapleLife {
   private MapleNPCStats stats;

   public MapleNPC(int id, MapleNPCStats stats) {
      super(id);
      this.stats = stats;
   }

   public boolean hasShop() {
      return MapleShopFactory.getInstance().getShopForNPC(getId()) != null;
   }

   public void sendShop(MapleClient c) {
      MapleShopFactory.getInstance().getShopForNPC(getId()).sendShop(c);
   }

   @Override
   public void sendSpawnData(MapleClient client) {
      client.sendPacket(CNpcPool.spawnNPC(this));
      client.sendPacket(CNpcPool.spawnNPCRequestController(this, true));
   }

   @Override
   public void sendDestroyData(MapleClient client) {
      client.sendPacket(CNpcPool.removeNPCController(getObjectId()));
      client.sendPacket(CNpcPool.removeNPC(getObjectId()));
   }

   @Override
   public MapleMapObjectType getType() {
      return MapleMapObjectType.NPC;
   }

   public String getName() {
      return stats.getName();
   }
}
