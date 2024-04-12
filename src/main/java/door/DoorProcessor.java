package door;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import client.MapleCharacter;
import client.TemporaryStatType;
import client.inventory.Item;
import client.inventory.manipulator.MapleInventoryManipulator;
import config.YamlConfig;
import net.server.Server;
import net.server.channel.Channel;
import net.server.services.task.channel.OverallService;
import net.server.services.type.ChannelServices;
import net.server.world.MapleParty;
import server.maps.MapleDoor;
import server.maps.MapleDoorObject;
import server.maps.MapleMap;
import server.maps.MaplePortal;
import tools.Pair;

public class DoorProcessor {
   private static DoorProcessor instance = null;

   private DoorProcessor() {
   }

   public static synchronized DoorProcessor getInstance() {
      if (instance == null) {
         instance = new DoorProcessor();
      }

      return instance;
   }

   public void updatePartyTownDoors(MapleParty party, MapleCharacter partyLeaver,
                                    List<MapleCharacter> partyMembers) {
      final List<MapleDoor> partyDoors;
      if (!partyMembers.isEmpty()) {
         partyDoors = getPartyDoors(party.getLeader().getWorld(), party.getMemberIds());

         partyMembers.stream()
               .flatMap(c -> partyDoors.stream().filter(d -> c.getId() == d.ownerId()).map( d -> new Pair<>(c, d)))
               .forEach(p -> updateDoorPortal(p.getLeft(), p.getRight()));

         partyDoors.stream()
               .map(d -> getDoorMapObject(d, d::townId, d::townDoorId))
               .flatMap(Optional::stream)
               .flatMap(mdo ->partyMembers.stream().map( c -> new Pair<>(c, mdo)))
               .forEach(p -> {
                  p.getRight().sendDestroyData(p.getLeft().getClient(), true);
                  p.getLeft().removeVisibleMapObject(p.getRight());
               });

         if (partyLeaver != null) {
            getCharacterDoors(partyLeaver).stream()
                  .map(d -> getDoorMapObject(d, d::townId, d::townDoorId))
                  .flatMap(Optional::stream)
                  .flatMap(mdo ->partyMembers.stream().map( c -> new Pair<>(c, mdo)))
                  .forEach(p -> {
                     p.getRight().sendDestroyData(p.getLeft().getClient(), true);
                     p.getLeft().removeVisibleMapObject(p.getRight());
                  });
         }

         party.getMembersSortedByHistory().stream()
               .flatMap(id -> partyDoors.stream().filter(d -> id == d.ownerId()))
               .map(d -> getDoorMapObject(d, d::townId, d::townDoorId))
               .flatMap(Optional::stream)
               .flatMap(mdo ->partyMembers.stream().map( c -> new Pair<>(c, mdo)))
               .forEach(p -> {
                  p.getRight().sendSpawnData(p.getLeft().getClient());
                  p.getLeft().addVisibleMapObject(p.getRight());
               });
      } else {
         partyDoors = Collections.emptyList();
      }

      if (partyLeaver != null) {
         if (!partyDoors.isEmpty()) {
            partyDoors.stream()
                  .map(d -> getDoorMapObject(d, d::townId, d::townDoorId))
                  .flatMap(Optional::stream)
                  .forEach(mdo -> {
                     mdo.sendDestroyData(partyLeaver.getClient(), true);
                     partyLeaver.removeVisibleMapObject(mdo);
                  });
         }

         List<MapleDoor> leaverDoors = getCharacterDoors(partyLeaver);
         leaverDoors.stream()
               .map(d -> getDoorMapObject(d, d::townId, d::townDoorId))
               .flatMap(Optional::stream)
               .forEach(mdo -> {
                  mdo.sendDestroyData(partyLeaver.getClient(), true);
                  partyLeaver.removeVisibleMapObject(mdo);
               });

         leaverDoors.forEach(d -> updateDoorPortal(partyLeaver, d));
         leaverDoors.stream()
               .map(d -> getDoorMapObject(d, d::townId, d::townDoorId))
               .flatMap(Optional::stream)
               .forEach(mdo -> {
                  mdo.sendSpawnData(partyLeaver.getClient());
                  partyLeaver.addVisibleMapObject(mdo);
               });
      }
   }

   public void attemptRemoveDoor(final MapleCharacter owner) {
      Optional<MapleDoor> destroyDoor = getCharacterOwnedDoor(owner);
      if (destroyDoor.isPresent() && destroyDoor.get().dispose()) {
         long effectTimeLeft = 3000 - destroyDoor.get().getElapsedDeployTime();   // portal deployment effect duration
         if (effectTimeLeft > 0) {
            MapleMap town = Server.getInstance()
                  .getWorld(destroyDoor.get().worldId())
                  .flatMap(w -> w.getChannel(destroyDoor.get().channelId()))
                  .map(Channel::getMapFactory)
                  .map(mf -> mf.getMap(destroyDoor.get().townId()))
                  .orElseThrow();

            OverallService service = (OverallService) town.getChannelServer().getServiceAccess(ChannelServices.OVERALL);
            service.registerOverallAction(town.getId(), () -> broadcastRemoveDoor(destroyDoor.get()), effectTimeLeft);
         } else {
            broadcastRemoveDoor(destroyDoor.get());
         }
      }
   }

   public void broadcastRemoveDoor(MapleDoor door) {
      MapleMap target = Server.getInstance()
            .getWorld(door.worldId())
            .flatMap(w -> w.getChannel(door.channelId()))
            .map(Channel::getMapFactory)
            .map(mf -> mf.getMap(door.targetId()))
            .orElseThrow();
      MapleMap town = Server.getInstance()
            .getWorld(door.worldId())
            .flatMap(w -> w.getChannel(door.channelId()))
            .map(Channel::getMapFactory)
            .map(mf -> mf.getMap(door.townId()))
            .orElseThrow();

      MapleDoorObject areaDoor = target.getDoorByOid(door.targetDoorId()).orElseThrow();
      MapleDoorObject townDoor = town.getDoorByOid(door.townDoorId()).orElseThrow();

      Collection<MapleCharacter> targetChars = target.getCharacters();
      Collection<MapleCharacter> townChars = town.getCharacters();

      target.removeMapObject(areaDoor);
      town.removeMapObject(townDoor);

      for (MapleCharacter chr : targetChars) {
         areaDoor.sendDestroyData(chr.getClient());
         chr.removeVisibleMapObject(areaDoor);
      }

      for (MapleCharacter chr : townChars) {
         townDoor.sendDestroyData(chr.getClient());
         chr.removeVisibleMapObject(townDoor);
      }

      DoorCache.getInstance().removeDoor(door);

      if (door.townPortalId() == 0x80) {
         for (MapleCharacter chr : townChars) {
            getMainTownDoor(chr).ifPresent(d -> {
               townDoor.sendSpawnData(chr.getClient());
               chr.addVisibleMapObject(townDoor);
            });
         }
      }
   }

   public void updateDoorPortal(MapleCharacter owner, MapleDoor door) {
      MapleMap town = Server.getInstance()
            .getWorld(door.worldId())
            .flatMap(w -> w.getChannel(door.channelId()))
            .map(Channel::getMapFactory)
            .map(mf -> mf.getMap(door.townId()))
            .orElseThrow();
      int slot = owner.fetchDoorSlot();
      MaplePortal portal = town.getDoorPortal(slot);
      if (portal != null) {
         MapleDoor newDoor = door.updateTownPortalId(portal.getId());
         DoorCache.getInstance().updateDoor(newDoor);

         MapleMap target = Server.getInstance()
               .getWorld(door.worldId())
               .flatMap(w -> w.getChannel(door.channelId()))
               .map(Channel::getMapFactory)
               .map(mf -> mf.getMap(door.targetId()))
               .orElseThrow();
         MapleDoorObject areaDoor = target.getDoorByOid(door.targetDoorId()).orElseThrow();
         areaDoor.update(portal.getId(), portal.getPosition());
      }
   }

   public void attemptDoorCreation(MapleCharacter owner, Point position, int skillId) {
      MapleMap target = owner.getMap();

      if (!target.canDeployDoor(position)) {
         MapleInventoryManipulator.addFromDrop(owner.getClient(), new Item(4006000, (short) 0, (short) 1), false);
         owner.dropMessage(5, "Mystic Door cannot be cast on a slope, try elsewhere.");
         owner.cancelBuffStats(TemporaryStatType.SOULARROW);
         return;
      }

      if (YamlConfig.config.server.USE_ENFORCE_MDOOR_POSITION) {
         MapleInventoryManipulator.addFromDrop(owner.getClient(), new Item(4006000, (short) 0, (short) 1), false);
         Pair<String, Integer> posStatus = target.getDoorPositionStatus(position);
         owner.dropMessage(5,
               "Mystic Door cannot be cast far from a spawn point. Nearest one is at " + posStatus.getRight()
                     + "pts " + posStatus.getLeft());
         owner.cancelBuffStats(TemporaryStatType.SOULARROW);
         return;
      }

      MapleMap town = target.getReturnMap();
      MaplePortal portal = town.getDoorPortal(owner.getDoorSlot());
      if (portal == null) {
         MapleInventoryManipulator.addFromDrop(owner.getClient(), new Item(4006000, (short) 0, (short) 1), false);
         owner.dropMessage(5, "There are no door portals available for the town at this moment. Try again later.");
         owner.cancelBuffStats(TemporaryStatType.SOULARROW);
         return;
      }

      MapleDoorObject areaDoor = new MapleDoorObject(owner.getId(), town, target, portal.getId(), position, portal.getPosition(), skillId);
      MapleDoorObject townDoor = new MapleDoorObject(owner.getId(), target, town, -1, portal.getPosition(), position, skillId);
      areaDoor.setPairOid(townDoor.getObjectId());
      townDoor.setPairOid(areaDoor.getObjectId());

      target.spawnDoor(areaDoor);
      town.spawnDoor(townDoor);

      MapleDoor door = new MapleDoor(owner.getWorld(), owner.getClient().getChannel(), owner.getId(), town.getId(),
            portal.getId(), townDoor.getObjectId(), target.getId(), areaDoor.getObjectId());

      DoorCache.getInstance().addDoor(door);
      owner.silentPartyUpdate();
   }

   public Optional<MapleDoorObject> getDoorMapObject(MapleDoor door, Supplier<Integer> mapSupplier, Supplier<Integer> idSupplier) {
      return Server.getInstance()
            .getWorld(door.worldId())
            .flatMap(w -> w.getChannel(door.channelId()))
            .map(Channel::getMapFactory)
            .map(mf -> mf.getMap(mapSupplier.get()))
            .flatMap(m -> m.getDoorByOid(idSupplier.get()));
   }

   public List<MapleDoor> getPartyDoors(int worldId, List<Integer> memberIds) {
      return DoorCache.getInstance().getDoorsForParty(worldId, memberIds);
   }

   public List<MapleDoor> getCharacterDoors(MapleCharacter character) {
      return character.getParty()
            .map(p -> getPartyDoors(character.getWorld(), p.getMemberIds()))
            .orElse(DoorCache.getInstance().getDoorForCharacter(character.getWorld(), character.getId()).stream().toList());
   }

   public Optional<MapleDoor> getCharacterOwnedDoor(MapleCharacter character) {
      return character.getParty()
            .map(p -> getPartyDoors(character.getWorld(), p.getMemberIds()).stream().filter(d -> d.ownerId() == character.getId()).findFirst())
            .orElse(DoorCache.getInstance().getDoorForCharacter(character.getWorld(), character.getId()));
   }

   public Optional<MapleDoor> getMainTownDoor(MapleCharacter character) {
      return getCharacterDoors(character).stream()
            .filter(d -> d.townPortalId() == 0x80)
            .findFirst();
   }

   public boolean canDoor(MapleCharacter character) {
      Optional<MapleDoor> door = getCharacterOwnedDoor(character);
      return door.map(mapleDoor -> mapleDoor.isActive() && mapleDoor.getElapsedDeployTime() > 5000).orElse(true);
   }
}
