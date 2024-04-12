package door;

import java.util.ArrayList;
import java.util.Collections;
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

//   private final Map<DoorPartyKey, List<MapleDoor>> partyCache;
//   private final Map<DoorPartyKey, ReadWriteLock> partyLocks;
   private final Map<DoorCharacterKey, MapleDoor> characterCache;
   private final Map<DoorCharacterKey, ReadWriteLock> characterLocks;

   private DoorCache() {
//      partyCache = new ConcurrentHashMap<>();
//      partyLocks = new ConcurrentHashMap<>();
      characterCache = new ConcurrentHashMap<>();
      characterLocks = new ConcurrentHashMap<>();
   }

   public static synchronized DoorCache getInstance() {
      if (instance == null) {
         instance = new DoorCache();
      }

      return instance;
   }

//   private ReadWriteLock getPartyReadWriteLock(DoorPartyKey key) {
//      return partyLocks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
//   }
//
//   private Lock getPartyReadLock(DoorPartyKey key) {
//      return getPartyReadWriteLock(key).readLock();
//   }
//
//   private Lock getPartyWriteLock(DoorPartyKey key) {
//      return getPartyReadWriteLock(key).writeLock();
//   }

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

//   public void addDoor(MapleDoor door, int partyId) {
//      DoorPartyKey key = new DoorPartyKey(door.worldId(), partyId);
//      Lock lock = getPartyWriteLock(key);
//      if (lock.tryLock()) {
//         try {
//            if (partyCache.containsKey(key)) {
//               List<MapleDoor> doors = new ArrayList<>(partyCache.get(key));
//               doors.add(door);
//               partyCache.put(key, Collections.unmodifiableList(doors));
//               return;
//            }
//            partyCache.put(key, Collections.singletonList(door));
//         } finally {
//            lock.unlock();
//         }
//      }
//   }

//   public void removeDoorFromParty(MapleDoor door, int partyId) {
//      removeDoorFromParty(door.worldId(), partyId, door.ownerId());
//   }

//   public void removeDoorFromParty(int worldId, int partyId, int ownerId) {
//      DoorPartyKey key = new DoorPartyKey(worldId, partyId);
//      Lock lock = getPartyWriteLock(key);
//      if (lock.tryLock()) {
//         try {
//            if (partyCache.containsKey(key)) {
//               List<MapleDoor> newDoors = partyCache.get(key).stream()
//                     .filter(d -> d.ownerId() != ownerId)
//                     .toList();
//               partyCache.put(key, newDoors);
//            }
//         } finally {
//            lock.unlock();
//         }
//      }
//   }

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

//   public void updateDoor(MapleDoor door, int partyId) {
//      DoorPartyKey key = new DoorPartyKey(door.worldId(), partyId);
//      Lock lock = getPartyWriteLock(key);
//      if (lock.tryLock()) {
//         try {
//            if (partyCache.containsKey(key)) {
//               List<MapleDoor> doors = new ArrayList<>(partyCache.get(key).stream()
//                     .filter(d -> d.ownerId() != door.ownerId())
//                     .toList());
//               doors.add(door);
//               partyCache.put(key, Collections.unmodifiableList(doors));
//               return;
//            }
//            partyCache.put(key, Collections.singletonList(door));
//         } finally {
//            lock.unlock();
//         }
//      }
//   }
}
