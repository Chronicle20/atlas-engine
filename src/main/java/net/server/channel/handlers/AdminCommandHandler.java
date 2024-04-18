package net.server.channel.handlers;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ban.BanProcessor;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import connection.packets.CField;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.ItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.quest.MapleQuest;
import tools.Randomizer;

public final class AdminCommandHandler extends AbstractMaplePacketHandler {
   private static final Logger log = LoggerFactory.getLogger(AdminCommandHandler.class);

   private static void issueWarn(MapleClient c, String victim, String message) {
      c.getChannelServer().getPlayerStorage().getCharacterByName(victim)
            .ifPresentOrElse(v -> sendWarningMessage(c, v, message), () -> sendWarningMessage(c));
   }

   private static void sendWarningMessage(MapleClient client, MapleCharacter target, String message) {
      target.sendPacket(CWvsContext.serverNotice(1, message));
      client.sendPacket(CField.getGMEffect(0x1E, (byte) 1));
   }

   private static void sendWarningMessage(MapleClient c) {
      c.sendPacket(CField.getGMEffect(0x1E, (byte) 0));
   }

   private static void performHide(MapleClient c, byte hide) {
      c.getPlayer().Hide(hide == 1);
   }

   private static void deprecatedBan(MapleClient c) {
      c.getPlayer().yellowMessage("Please use !ban <IGN> <Reason>");
   }

   private static void setExp(MapleClient c, int exp) {
      c.getPlayer().setExp(exp);
   }

   private static void killMonster(MapleClient c, int mobToKill, int amount) {
      List<MapleMapObject> monsterx = c.getPlayer().getMap()
            .getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, List.of(MapleMapObjectType.MONSTER));
      for (int x = 0; x < amount; x++) {
         MapleMonster monster = (MapleMonster) monsterx.get(x);
         if (monster.getId() == mobToKill) {
            c.getPlayer().getMap().killMonster(monster, c.getPlayer(), true);
         }
      }
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (!c.getPlayer().isGM()) {
         return;
      }
      byte mode = p.readByte();
      String victim;
      switch (mode) {
         case 0x00: // Level1~Level8 & Package1~Package2
            int[][] toSpawn = ItemInformationProvider.getInstance().getSummonMobs(p.readInt());
            for (int[] toSpawnChild : toSpawn) {
               if (Randomizer.nextInt(100) < toSpawnChild[1]) {
                  c.getPlayer().getMap()
                        .spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(toSpawnChild[0]), c.getPlayer().getPosition());
               }
            }
            c.sendPacket(CWvsContext.enableActions());
            break;
         case 0x01: { // /d (inv)
            byte typeIndex = p.readByte();
            Optional<MapleInventoryType> type = MapleInventoryType.getByType(typeIndex);
            if (type.isEmpty()) {
               return;
            }

            MapleInventory in = c.getPlayer().getInventory(type.get());
            for (short i = 1; i <= in.getSlotLimit(); i++) { //TODO What is the point of this loop?
               if (in.getItem(i) != null) {
                  MapleInventoryManipulator.removeFromSlot(c, type.get(), i, in.getItem(i).getQuantity(), false);
               }
               return;
            }
            break;
         }
         case 0x02: // Exp
            int exp = p.readInt();
            setExp(c, exp);
            break;
         case 0x03: // /ban <name>
            deprecatedBan(c);
            break;
         case 0x04: // /block <name> <duration (in days)> <HACK/BOT/AD/HARASS/CURSE/SCAM/MISCONDUCT/SELL/ICASH/TEMP/GM/IPROGRAM/MEGAPHONE>
            victim = p.readString();
            int type = p.readByte(); //reason
            int duration = p.readInt();
            String description = p.readString();
            String reason = c.getPlayer().getName() + " used /ban to ban";
            Optional<MapleCharacter> target = c.getChannelServer().getPlayerStorage().getCharacterByName(victim);
            if (target.isPresent()) {
               String readableTargetName = MapleCharacter.makeMapleReadable(target.get().getName());
               String ip = target.get().getClient().getRemoteAddress();
               reason += readableTargetName + " (IP: " + ip + ")";
               if (duration == -1) {
                  target.get().ban(description + " " + reason);
               } else {
                  target.get().block(type, duration, description);
                  target.get().sendPolice(duration, reason, 6000);
               }
               c.sendPacket(CField.getGMEffect(4, (byte) 0));
            } else if (BanProcessor.getInstance().ban(victim, reason, false)) {
               c.sendPacket(CField.getGMEffect(4, (byte) 0));
            } else {
               c.sendPacket(CField.getGMEffect(6, (byte) 1));
            }
            break;
         case 0x10: // /h, information added by vana -- <and tele mode f1> ... hide ofcourse
            byte hide = p.readByte();
            performHide(c, hide);
            break;
         case 0x11: // Entering a map
            switch (p.readByte()) {
               case 0:// /u
                  StringBuilder sb = new StringBuilder("USERS ON THIS MAP: ");
                  for (MapleCharacter mc : c.getPlayer().getMap().getCharacters()) {
                     sb.append(mc.getName());
                     sb.append(" ");
                  }
                  c.getPlayer().message(sb.toString());
                  break;
               case 12:// /uclip and entering a map
                  break;
            }
            break;
         case 0x12: // Send
            victim = p.readString();
            int mapId = p.readInt();
            c.getChannelServer().getPlayerStorage().getCharacterByName(victim).ifPresent(character -> character.changeMap(mapId));
            break;
         case 0x15: // Kill
            int mobToKill = p.readInt();
            int amount = p.readInt();
            killMonster(c, mobToKill, amount);
            break;
         case 0x16: // Questreset
            MapleQuest.getInstance(p.readShort()).reset(c.getPlayer());
            break;
         case 0x17: // Summon
            int mobId = p.readInt();
            int quantity = p.readInt();
            for (int i = 0; i < quantity; i++) {
               c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mobId), c.getPlayer().getPosition());
            }
            break;
         case 0x18: // Maple & Mobhp
            int mobHp = p.readInt();
            c.getPlayer().dropMessage("Monsters HP");
            List<MapleMapObject> monsters = c.getPlayer().getMap()
                  .getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, List.of(MapleMapObjectType.MONSTER));
            for (MapleMapObject mobs : monsters) {
               MapleMonster monster = (MapleMonster) mobs;
               if (monster.getId() == mobHp) {
                  c.getPlayer().dropMessage(monster.getName() + ": " + monster.getHp());
               }
            }
            break;
         case 0x1E: // Warn
            victim = p.readString();
            String message = p.readString();
            issueWarn(c, victim, message);
            break;
         case 0x24:// /Artifact Ranking
            break;
         case 0x77: //Testing purpose
            if (p.available() == 4) {
               log.debug("int {}", p.readInt());
            } else if (p.available() == 2) {
               log.debug("short {}", p.readShort());
            }
            break;
         default:
            log.debug("New GM packet encountered (MODE : {}: {}", mode, p);
            break;
      }
   }
}
