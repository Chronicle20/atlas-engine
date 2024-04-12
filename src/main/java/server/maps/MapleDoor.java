package server.maps;

public record MapleDoor(int worldId, int channelId, int ownerId, int townId, int townPortalId, int townDoorId, int targetId,
                        int targetDoorId, long deployTime, boolean active) {
   public MapleDoor(int worldId, int channelId, int ownerId, int townId, int townPortalId, int townDoorId, int targetId,
                    int targetDoorId) {
      this(worldId, channelId, ownerId, townId, townPortalId, townDoorId, targetId, targetDoorId, System.currentTimeMillis(),
            true);
   }

   public long getElapsedDeployTime() {
      return System.currentTimeMillis() - deployTime;
   }

   public boolean dispose() {
      return active;
   }

   public boolean isActive() {
      return active;
   }

   public MapleDoor updateTownPortalId(int townPortalId) {
      return new MapleDoor(worldId, channelId, ownerId, townId, townPortalId, townDoorId, targetId, targetDoorId, deployTime,
            active);
   }
}
