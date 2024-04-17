package buddy;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import client.BuddyRequestInfo;
import tools.Pair;

public record BuddyList(int capacity, Map<Integer, BuddyListEntry> buddies, Deque<BuddyRequestInfo> pendingRequests) {

   public BuddyList(int capacity) {
      this(capacity, new LinkedHashMap<>(), new LinkedList<>());
   }

   public boolean contains(int characterId) {
      return buddies.containsKey(characterId);
   }

   public boolean containsVisible(int characterId) {
      BuddyListEntry ble = buddies.get(characterId);
      if (ble == null) {
         return false;
      }
      return ble.visible();
   }

   public BuddyList setCapacity(int capacity) {
      return new BuddyList(capacity, buddies, pendingRequests);
   }

   public Optional<BuddyListEntry> get(int characterId) {
      return Optional.ofNullable(buddies.get(characterId));
   }

   public Optional<BuddyListEntry> get(String characterName) {
      return getBuddies().stream()
            .filter(b -> b.name().equalsIgnoreCase(characterName))
            .findFirst();
   }

   public BuddyList put(BuddyListEntry entry) {
      Map<Integer, BuddyListEntry> newBuddies = new HashMap<>(this.buddies);
      newBuddies.put(entry.characterId(), entry);
      return new BuddyList(capacity, newBuddies, pendingRequests);
   }

   public BuddyList remove(int characterId) {
      Map<Integer, BuddyListEntry> newBuddies = new HashMap<>(this.buddies);
      newBuddies.remove(characterId);
      return new BuddyList(capacity, newBuddies, pendingRequests);
   }

   public Collection<BuddyListEntry> getBuddies() {
      return Collections.unmodifiableCollection(buddies.values());
   }

   public boolean isFull() {
      return buddies.size() >= capacity;
   }

   public List<Integer> getBuddyIds() {
      return buddies.values().stream()
            .map(BuddyListEntry::characterId)
            .toList();
   }

   public Pair<Optional<BuddyRequestInfo>, BuddyList> pollPendingRequest() {
      BuddyRequestInfo requestInfo = pendingRequests.pollLast();
      return new Pair<>(Optional.ofNullable(requestInfo), new BuddyList(capacity, buddies, pendingRequests));
   }

   public BuddyList updateChannel(int referenceId, int channel) {
      Map<Integer, BuddyListEntry> newBuddies = new HashMap<>(this.buddies);
      newBuddies.put(referenceId, buddies.get(referenceId).setChannel(channel));
      return new BuddyList(capacity, newBuddies, pendingRequests);
   }

   public boolean hasPendingRequests() {
      return !pendingRequests.isEmpty();
   }

   public BuddyList queueRequest(BuddyRequestInfo info) {
      Deque<BuddyRequestInfo> pendingRequests = this.pendingRequests;
      pendingRequests.push(info);
      return new BuddyList(capacity, buddies, pendingRequests);
   }

   public enum BuddyOperation {
      ADDED, DELETED
   }

   public enum BuddyAddResult {
      BUDDYLIST_FULL, ALREADY_ON_LIST, OK
   }
}
