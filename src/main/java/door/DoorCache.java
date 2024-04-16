package door;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import server.maps.MapleDoor;

public class DoorCache {
   private static DoorCache instance = null;

   private final Map<DoorCharacterKey, MapleDoor> characterCache;
   private final Map<DoorCharacterKey, ReadWriteLock> characterLocks;

   private DoorCache() {
      characterCache = new ConcurrentHashMap<>();
      characterLocks = new ConcurrentHashMap<>();
   }

   public static synchronized DoorCache getInstance() {
      if (instance == null) {
         instance = new DoorCache();
      }

      return instance;
   }

   private ReadWriteLock getCharacterReadWriteLock(DoorCharacterKey key) {
      return characterLocks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
   }

   private Lock getCharacterReadLock(DoorCharacterKey key) {
      return getCharacterReadWriteLock(key).readLock();
   }

   private Lock getCharacterWriteLock(DoorCharacterKey key) {
      return getCharacterReadWriteLock(key).writeLock();
   }

   public void addDoor(MapleDoor door) {
      DoorCharacterKey key = new DoorCharacterKey(door.worldId(), door.ownerId());
      Lock lock = getCharacterWriteLock(key);
      if (lock.tryLock()) {
         try {
            characterCache.put(key, door);
         } finally {
            lock.unlock();
         }
      }
   }

   public void removeDoor(MapleDoor door) {
      removeDoor(door.worldId(), door.ownerId());
   }

   public void removeDoor(int worldId, int ownerId) {
      DoorCharacterKey key = new DoorCharacterKey(worldId, ownerId);
      Lock lock = getCharacterWriteLock(key);
      if (lock.tryLock()) {
         try {
            characterCache.remove(key);
         } finally {
            lock.unlock();
         }
      }
   }

   public List<MapleDoor> getDoorsForParty(int worldId, List<Integer> memberIds) {
      return memberIds.stream()
            .map(id -> getDoorForCharacter(worldId, id))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
   }

   public Optional<MapleDoor> getDoorForCharacter(int worldId, int characterId) {
      DoorCharacterKey key = new DoorCharacterKey(worldId, characterId);
      Lock lock = getCharacterReadLock(key);
      if (lock.tryLock()) {
         try {
            return Optional.ofNullable(characterCache.get(key));
         } finally {
            lock.unlock();
         }
      } else {
         return Optional.empty();
      }
   }

   public void updateDoor(MapleDoor newDoor) {
      addDoor(newDoor);
   }
}
