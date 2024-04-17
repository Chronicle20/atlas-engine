package buddy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import client.BuddyRequestInfo;
import tools.Pair;

public class BuddyCache {
   private static BuddyCache instance = null;

   private final Map<BuddyCharacterKey, BuddyList> characterCache;
   private final Map<BuddyCharacterKey, ReadWriteLock> characterLocks;

   private BuddyCache() {
      characterCache = new ConcurrentHashMap<>();
      characterLocks = new ConcurrentHashMap<>();
   }

   public static synchronized BuddyCache getInstance() {
      if (instance == null) {
         instance = new BuddyCache();
      }

      return instance;
   }

   private ReadWriteLock getCharacterReadWriteLock(BuddyCharacterKey key) {
      return characterLocks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
   }

   private Lock getCharacterReadLock(BuddyCharacterKey key) {
      return getCharacterReadWriteLock(key).readLock();
   }

   private Lock getCharacterWriteLock(BuddyCharacterKey key) {
      return getCharacterReadWriteLock(key).writeLock();
   }

   protected BuddyList getBuddyList(int worldId, int characterId) {
      return getBuddyList(new BuddyCharacterKey(worldId, characterId));
   }

   protected BuddyList getBuddyList(BuddyCharacterKey key) {
      Lock lock = getCharacterReadLock(key);
      if (lock.tryLock()) {
         try {
            if (characterCache.containsKey(key)) {
               return characterCache.get(key);
            }
         } finally {
            lock.unlock();
         }
         Lock writeLock = getCharacterWriteLock(key);
         if (writeLock.tryLock()) {
            try {
               BuddyList buddyList = BuddyProvider.loadBuddyList(key);
               characterCache.put(key, buddyList);
               return buddyList;
            } finally {
               writeLock.unlock();
            }
         } else {
            return new BuddyList(BuddyConstants.DEFAULT_CAPACITY);
         }
      } else {
         return new BuddyList(BuddyConstants.DEFAULT_CAPACITY);
      }
   }

   protected Optional<BuddyList> remove(BuddyCharacterKey key, int referenceId) {
      Lock lock = getCharacterWriteLock(key);
      if (lock.tryLock()) {
         try {
            if (characterCache.containsKey(key)) {
               BuddyList updated = characterCache.get(key).remove(referenceId);
               characterCache.put(key, updated);
               return Optional.of(updated);
            }
            return Optional.empty();
         } finally {
            lock.unlock();
         }
      }
      return Optional.empty();
   }

   protected void updateChannel(BuddyCharacterKey key, int referenceId, int channel) {
      Lock lock = getCharacterWriteLock(key);
      if (lock.tryLock()) {
         try {
            if (characterCache.containsKey(key)) {
               BuddyList updated = characterCache.get(key).updateChannel(referenceId, channel);
               characterCache.put(key, updated);
            }
         } finally {
            lock.unlock();
         }
      }
   }

   protected Optional<BuddyList> updateChannels(BuddyCharacterKey key, Map<Integer, Integer> updates) {
      Lock lock = getCharacterWriteLock(key);
      if (lock.tryLock()) {
         try {
            if (characterCache.containsKey(key)) {
               BuddyList finalResult = characterCache.get(key);
               for (Map.Entry<Integer, Integer> entry : updates.entrySet()) {
                  finalResult = finalResult.updateChannel(entry.getKey(), entry.getValue());
               }
               characterCache.put(key, finalResult);
               return Optional.of(finalResult);
            }
         } finally {
            lock.unlock();
         }
      }
      return Optional.empty();
   }

   protected Optional<BuddyList> updateCapacity(BuddyCharacterKey key, int capacity) {
      Lock lock = getCharacterWriteLock(key);
      if (lock.tryLock()) {
         try {
            if (characterCache.containsKey(key)) {
               BuddyList finalResult = characterCache.get(key).setCapacity(capacity);
               characterCache.put(key, finalResult);
               return Optional.of(finalResult);
            }
         } finally {
            lock.unlock();
         }
      }
      return Optional.empty();
   }

   protected Optional<BuddyList> addBuddy(BuddyCharacterKey key, BuddyListEntry entry) {
      Lock lock = getCharacterWriteLock(key);
      if (lock.tryLock()) {
         try {
            if (characterCache.containsKey(key)) {
               BuddyList finalResult = characterCache.get(key).put(entry);
               characterCache.put(key, finalResult);
               return Optional.of(finalResult);
            }
         } finally {
            lock.unlock();
         }
      }
      return Optional.empty();
   }

   protected Optional<BuddyList> requestBuddyAdd(BuddyCharacterKey key, BuddyRequestInfo info, Runnable r) {
      Lock lock = getCharacterWriteLock(key);
      if (lock.tryLock()) {
         try {
            if (characterCache.containsKey(key)) {
               BuddyListEntry entry = new BuddyListEntry(info.characterName(), BuddyConstants.DEFAULT_GROUP, info.characterId(),
                     info.channelId(), false);
               BuddyList finalResult = characterCache.get(key).put(entry);
               if (finalResult.hasPendingRequests()) {
                  finalResult = finalResult.queueRequest(info);
               } else {
                  r.run();
               }

               characterCache.put(key, finalResult);
               return Optional.of(finalResult);
            }
         } finally {
            lock.unlock();
         }
      }
      return Optional.empty();
   }

   protected Optional<BuddyRequestInfo> pollPendingRequest(BuddyCharacterKey key) {
      Lock lock = getCharacterWriteLock(key);
      if (lock.tryLock()) {
         try {
            if (characterCache.containsKey(key)) {
               Pair<Optional<BuddyRequestInfo>, BuddyList> finalResult = characterCache.get(key).pollPendingRequest();
               characterCache.put(key, finalResult.getRight());
               return finalResult.getLeft();
            }
         } finally {
            lock.unlock();
         }
      }
      return Optional.empty();
   }

   protected Optional<BuddyList> changeGroup(BuddyCharacterKey key, int referenceId, String group) {
      Lock lock = getCharacterWriteLock(key);
      if (lock.tryLock()) {
         try {
            if (characterCache.containsKey(key)) {
               BuddyList finalResult = characterCache.get(key);
               Optional<BuddyListEntry> buddyListEntry = finalResult.get(referenceId).map(e -> e.changeGroup(group));
               if (buddyListEntry.isPresent()) {
                  finalResult = finalResult.put(buddyListEntry.get());
               }
               characterCache.put(key, finalResult);
               return Optional.of(finalResult);
            }
         } finally {
            lock.unlock();
         }
      }
      return Optional.empty();
   }
}
