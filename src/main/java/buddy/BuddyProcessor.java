package buddy;

import static buddy.BuddyList.BuddyOperation.ADDED;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import client.BuddyRequestInfo;
import client.MapleCharacter;
import connection.constants.BuddylistErrorMode;
import connection.packets.CWvsContext;
import net.packet.Packet;
import net.server.PlayerStorage;
import net.server.Server;
import net.server.world.World;
import tools.DatabaseConnection;

public class BuddyProcessor {
   private static BuddyProcessor instance = null;

   private BuddyProcessor() {
   }

   public static synchronized BuddyProcessor getInstance() {
      if (instance == null) {
         instance = new BuddyProcessor();
      }

      return instance;
   }

   private void buddyDeleted(int cidFrom, String name, MapleCharacter addChar) {
      BuddyCharacterKey key = new BuddyCharacterKey(addChar.getWorld(), addChar.getId());
      BuddyList buddyList = BuddyCache.getInstance().getBuddyList(key);

      if (buddyList.contains(cidFrom)) {
         Optional<BuddyListEntry> entry = buddyList.get(cidFrom);
         BuddyCache.getInstance().addBuddy(key, new BuddyListEntry(name, BuddyConstants.DEFAULT_GROUP, cidFrom, (byte) -1,
               entry.map(BuddyListEntry::visible).orElse(false)));
         addChar.sendPacket(CWvsContext.updateBuddyChannel(cidFrom, (byte) -1));
      }
   }

   private void buddyAdded(int cidFrom, String name, int channel, MapleCharacter addChar) {
      BuddyCharacterKey key = new BuddyCharacterKey(addChar.getWorld(), addChar.getId());
      BuddyList buddyList = BuddyCache.getInstance().getBuddyList(key);

      if (buddyList.contains(cidFrom)) {
         BuddyCache.getInstance().addBuddy(key, new BuddyListEntry(name, BuddyConstants.DEFAULT_GROUP, cidFrom, channel, true));
         addChar.sendPacket(CWvsContext.updateBuddyChannel(cidFrom, (byte) (channel - 1)));
      }
   }

   public void refreshBuddies(MapleCharacter character) {
      character.sendPacket(CWvsContext.updateBuddylist(getBuddyList(character.getWorld(), character.getId()).getBuddies()));
   }

   public BuddyList getBuddyList(int worldId, int characterId) {
      return BuddyCache.getInstance().getBuddyList(worldId, characterId);
   }

   public void addBuddy(MapleCharacter character, String toAddName, String group) {
      if (group.length() > BuddyConstants.MAX_GROUP_SIZE || toAddName.length() < 4 || toAddName.length() > 13) {
         return;
      }
      BuddyCharacterKey key = new BuddyCharacterKey(character.getWorld(), character.getId());
      BuddyList buddyList = BuddyCache.getInstance().getBuddyList(key);
      Optional<BuddyListEntry> buddy = buddyList.get(toAddName);
      if (buddy.isPresent()) {
         if (!buddy.get().visible() && group.equals(buddy.get().group())) {
            character.sendPacket(CWvsContext.buddylistMessage(BuddylistErrorMode.ALREADY_REGISTERED_AS_FRIEND));
            return;
         }

         buddyList = BuddyCache.getInstance().changeGroup(key, buddy.get().characterId(), group).orElseThrow();
         character.sendPacket(CWvsContext.updateBuddylist(buddyList.getBuddies()));
         return;
      }

      if (buddyList.isFull()) {
         character.sendPacket(CWvsContext.buddylistMessage(BuddylistErrorMode.YOUR_BUDDY_LIST_IS_FULL));
         return;
      }

      try {
         World world = character.getClient().getWorldServer();
         CharacterIdNameBuddyCapacity charWithId;
         int channel;
         Optional<MapleCharacter> otherChar =
               character.getClient().getChannelServer().getPlayerStorage().getCharacterByName(toAddName);
         if (otherChar.isPresent()) {
            channel = character.getClient().getChannel();
            charWithId = new CharacterIdNameBuddyCapacity(otherChar.get().getId(), otherChar.get().getName(),
                  getBuddyList(otherChar.get().getWorld(), otherChar.get().getId()).capacity());
         } else {
            channel = world.find(toAddName);
            charWithId = BuddyProvider.getCharacterIdAndNameFromDatabase(toAddName);
         }

         if (charWithId == null) {
            character.sendPacket(CWvsContext.buddylistMessage(BuddylistErrorMode.CHARACTER_NOT_FOUND));
            return;
         }

         BuddyList.BuddyAddResult buddyAddResult = null;
         if (channel != -1) {
            BuddyRequestInfo requestInfo = new BuddyRequestInfo(character.getClient().getChannel(), character.getId(),
                  character.getName(), character.getLevel(), character.getJob().getId(), group);
            buddyAddResult = requestBuddyAdd(character, toAddName, requestInfo);
         } else {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps =
                  con.prepareStatement("SELECT COUNT(*) as buddyCount FROM buddies WHERE characterid = ? AND pending = 0");
            ps.setInt(1, charWithId.id());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
               throw new RuntimeException("Result set expected");
            } else if (rs.getInt("buddyCount") >= charWithId.buddyCapacity()) {
               buddyAddResult = BuddyList.BuddyAddResult.BUDDYLIST_FULL;
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?");
            ps.setInt(1, charWithId.id());
            ps.setInt(2, character.getId());
            rs = ps.executeQuery();
            if (rs.next()) {
               buddyAddResult = BuddyList.BuddyAddResult.ALREADY_ON_LIST;
            }
            rs.close();
            ps.close();
            con.close();
         }

         if (buddyAddResult == BuddyList.BuddyAddResult.BUDDYLIST_FULL) {
            character.sendPacket(CWvsContext.buddylistMessage(BuddylistErrorMode.OTHER_BUDDY_LIST_IS_FULL));
            return;
         }

         int displayChannel;
         displayChannel = -1;
         int otherCid = charWithId.id();
         if (buddyAddResult == BuddyList.BuddyAddResult.ALREADY_ON_LIST && channel != -1) {
            displayChannel = channel;
            notifyRemoteChannel(world, character, channel, otherCid, ADDED);
         } else if (buddyAddResult != BuddyList.BuddyAddResult.ALREADY_ON_LIST && channel == -1) {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                  "INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, 1)")) {
               ps.setInt(1, charWithId.id());
               ps.setInt(2, character.getId());
               ps.executeUpdate();
            }
            con.close();
         }
         buddyList = BuddyCache.getInstance().addBuddy(key, new BuddyListEntry(charWithId.name(), group, otherCid, displayChannel,
               true)).orElseThrow();
         character.sendPacket(CWvsContext.updateBuddylist(buddyList.getBuddies()));
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   public void acceptBuddy(MapleCharacter character, int otherCharacterId) {
      BuddyCharacterKey key = new BuddyCharacterKey(character.getWorld(), character.getId());
      BuddyList buddyList = BuddyCache.getInstance().getBuddyList(key);
      if (!buddyList.isFull()) {
         try {
            World world = character.getClient().getWorldServer();
            int remoteChannel = world.find(otherCharacterId);
            String otherName = null;
            Optional<MapleCharacter> otherChar =
                  character.getClient().getChannelServer().getPlayerStorage().getCharacterById(otherCharacterId);
            if (otherChar.isEmpty()) {
               Connection con = DatabaseConnection.getConnection();
               try (PreparedStatement ps = con.prepareStatement("SELECT name FROM characters WHERE id = ?")) {
                  ps.setInt(1, otherCharacterId);
                  try (ResultSet rs = ps.executeQuery()) {
                     if (rs.next()) {
                        otherName = rs.getString("name");
                     }
                  }
               }
               con.close();
            } else {
               otherName = otherChar.get().getName();
            }
            if (otherName != null) {
               buddyList = BuddyCache.getInstance().addBuddy(key, new BuddyListEntry(otherName, BuddyConstants.DEFAULT_GROUP,
                     otherCharacterId, remoteChannel, true)).orElseThrow();
               character.sendPacket(CWvsContext.updateBuddylist(buddyList.getBuddies()));
               notifyRemoteChannel(world, character, remoteChannel, otherCharacterId, ADDED);
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
      nextPendingRequest(character);
   }

   private void notifyRemoteChannel(World world, MapleCharacter character, int remoteChannel, int otherCid,
                                    BuddyList.BuddyOperation operation) {
      if (remoteChannel != -1) {
         buddyChanged(world, otherCid, character.getId(), character.getName(), character.getClient().getChannel(), operation);
      }
   }

   public void nextPendingRequest(MapleCharacter character) {
      BuddyCharacterKey key = new BuddyCharacterKey(character.getWorld(), character.getId());
      BuddyCache.getInstance().pollPendingRequest(key)
            .ifPresent(a -> character.sendPacket(CWvsContext.requestBuddylistAdd(character.getId(), a)));
   }

   private BuddyList.BuddyAddResult requestBuddyAdd(MapleCharacter character, String addName, BuddyRequestInfo requestInfo) {
      Optional<MapleCharacter> addChar = character.getWorldServer().getPlayerStorage().getCharacterByName(addName);
      if (addChar.isEmpty()) {
         return BuddyList.BuddyAddResult.OK;
      }
      BuddyList buddylist = getBuddyList(addChar.get().getWorld(), addChar.get().getId());
      if (buddylist.isFull()) {
         return BuddyList.BuddyAddResult.BUDDYLIST_FULL;
      }

      if (!buddylist.contains(requestInfo.characterId())) {
         BuddyCharacterKey key = new BuddyCharacterKey(addChar.get().getWorld(), addChar.get().getId());
         BuddyCache.getInstance().requestBuddyAdd(key, requestInfo,
               () -> addChar.get().sendPacket(CWvsContext.requestBuddylistAdd(addChar.get().getId(), requestInfo)));
         return BuddyList.BuddyAddResult.OK;
      }

      if (buddylist.containsVisible(requestInfo.characterId())) {
         return BuddyList.BuddyAddResult.ALREADY_ON_LIST;
      }
      return BuddyList.BuddyAddResult.OK;
   }

   private void buddyChanged(World world, int cid, int cidFrom, String name, int channel, BuddyList.BuddyOperation operation) {
      world.getPlayerStorage().getCharacterById(cid).ifPresent(c -> buddyChanged(c, cidFrom, name, channel, operation));
   }

   private void buddyChanged(MapleCharacter character, int characterIdFrom, String name, int channelId,
                             BuddyList.BuddyOperation operation) {
      switch (operation) {
         case ADDED -> buddyAdded(characterIdFrom, name, channelId, character);
         case DELETED -> buddyDeleted(characterIdFrom, name, character);
      }
   }

   private void notifyRemoteChannel(MapleCharacter character, int remoteChannel, int otherCid, BuddyList.BuddyOperation operation) {
      if (remoteChannel != -1) {
         buddyChanged(character.getWorldServer(), otherCid, character.getId(), character.getName(),
               character.getClient().getChannel(),
               operation);
      }
   }

   public void deleteBuddy(MapleCharacter character, int otherCharacterId) {
      BuddyCharacterKey key = new BuddyCharacterKey(character.getWorld(), character.getId());
      if (BuddyCache.getInstance().getBuddyList(key).containsVisible(otherCharacterId)) {
         notifyRemoteChannel(character, character.getWorldServer().find(otherCharacterId), otherCharacterId,
               BuddyList.BuddyOperation.DELETED);
      }
      Optional<BuddyList> buddyList = BuddyCache.getInstance().remove(key, otherCharacterId);
      buddyList.ifPresent(list -> character.sendPacket(CWvsContext.updateBuddylist(list.getBuddies())));
      nextPendingRequest(character);
   }

   public void buddyChat(int worldId, int characterId, int[] recipientCharacterIds, Packet packet) {
      PlayerStorage playerStorage = Server.getInstance().getWorld(worldId).orElseThrow().getPlayerStorage();

      Arrays.stream(recipientCharacterIds).parallel()
            .mapToObj(playerStorage::getCharacterById)
            .flatMap(Optional::stream)
            .filter(c -> getBuddyList(worldId, c.getId()).containsVisible(characterId))
            .forEach(MapleCharacter.announcePacket(packet));
   }

   public void setBuddyCapacity(MapleCharacter character, int capacity) {
      BuddyCharacterKey key = new BuddyCharacterKey(character.getWorld(), character.getId());
      BuddyCache.getInstance().updateCapacity(key, capacity);
      character.sendPacket(CWvsContext.updateBuddyCapacity(capacity));
   }

   public void broadcast(int worldId, int characterId, Packet packet) {
      BuddyCharacterKey key = new BuddyCharacterKey(worldId, characterId);
      BuddyList buddyList = BuddyCache.getInstance().getBuddyList(key);

      PlayerStorage playerStorage = Server.getInstance().getWorld(worldId).orElseThrow().getPlayerStorage();

      buddyList.getBuddyIds().stream().parallel()
            .map(playerStorage::getCharacterById)
            .flatMap(Optional::stream)
            .filter(MapleCharacter::isLoggedinWorld)
            .forEach(MapleCharacter.announcePacket(packet));
   }

   public void notifyLogon(int worldId, int characterId, int channel) {
      BuddyCharacterKey key = new BuddyCharacterKey(worldId, characterId);
      BuddyList buddyList = BuddyCache.getInstance().getBuddyList(key);

      PlayerStorage playerStorage = Server.getInstance().getWorld(worldId).orElseThrow().getPlayerStorage();
      buddyList.getBuddyIds().stream().parallel()
            .map(playerStorage::getCharacterById)
            .flatMap(Optional::stream)
            .forEach(c -> {
               updateBuddyChannel(worldId, c.getId(), characterId, channel);
               c.sendPacket(CWvsContext.updateBuddyChannel(characterId, channel - 1));
            });
   }

   public void notifyLogoff(int worldId, int characterId) {
      BuddyCharacterKey key = new BuddyCharacterKey(worldId, characterId);
      BuddyList buddyList = BuddyCache.getInstance().getBuddyList(key);

      PlayerStorage playerStorage = Server.getInstance().getWorld(worldId).orElseThrow().getPlayerStorage();
      buddyList.getBuddyIds().stream().parallel()
            .map(playerStorage::getCharacterById)
            .flatMap(Optional::stream)
            .forEach(c -> {
               updateBuddyChannel(worldId, c.getId(), characterId, -1);
               c.sendPacket(CWvsContext.updateBuddyChannel(characterId, -1));
            });
   }

   private void updateBuddyChannel(int worldId, int characterId, int referenceId, int channel) {
      BuddyCharacterKey key = new BuddyCharacterKey(worldId, characterId);
      BuddyCache.getInstance().updateChannel(key, referenceId, channel);
   }

   public void updateBuddyChannels(MapleCharacter character) {
      BuddyCharacterKey key = new BuddyCharacterKey(character.getWorld(), character.getId());

      PlayerStorage playerStorage = Server.getInstance().getWorld(character.getWorld()).orElseThrow().getPlayerStorage();

      Map<Integer, Integer> updates = BuddyCache.getInstance().getBuddyList(key)
            .getBuddyIds().stream().parallel()
            .map(playerStorage::getCharacterById)
            .flatMap(Optional::stream)
            .collect(Collectors.toMap(MapleCharacter::getId, c -> c.getClient().getChannel()));
      BuddyCache.getInstance().updateChannels(key, updates);
      refreshBuddies(character);
   }

   public void storeBuddies(int worldId, int characterId, Connection con) {
      try (PreparedStatement ps = con.prepareStatement("DELETE FROM buddies WHERE characterid = ? AND pending = 0")) {
         ps.setInt(1, characterId);
         ps.executeUpdate();
      } catch (SQLException e) {
         throw new RuntimeException(e);
      }

      BuddyCharacterKey key = new BuddyCharacterKey(worldId, characterId);
      BuddyList buddyList = BuddyCache.getInstance().getBuddyList(key);

      try (PreparedStatement ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`, `group`) VALUES "
            + "(?, ?, 0, ?)")) {
         ps.setInt(1, characterId);
         for (BuddyListEntry entry : buddyList.getBuddies()) {
            if (entry.visible()) {
               ps.setInt(2, entry.characterId());
               ps.setString(3, entry.group());
               ps.addBatch();
            }
         }
         ps.executeBatch();
      } catch (SQLException e) {
         throw new RuntimeException(e);
      }
   }

   public void deleteCharacter(Connection con, MapleCharacter character) {
      try (PreparedStatement ps = con.prepareStatement("SELECT buddyid FROM buddies WHERE characterid = ?")) {
         ps.setInt(1, character.getId());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               int buddyid = rs.getInt("buddyid");
               Server.getInstance().getWorld(character.getWorld())
                     .map(World::getPlayerStorage)
                     .flatMap(w -> w.getCharacterById(buddyid))
                     .ifPresent(b -> deleteBuddy(character, character.getId()));
            }
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }
      try (PreparedStatement ps = con.prepareStatement("DELETE FROM buddies WHERE characterid = ?")) {
         ps.setInt(1, character.getId());
         ps.executeUpdate();
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }
}
