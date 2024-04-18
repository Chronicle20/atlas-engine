package connection.packets;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import server.life.MapleNPC;
import server.life.MaplePlayerNPC;

public class CNpcPool {
   public static Packet getPlayerNPC(MaplePlayerNPC npc) {     // thanks to Arnah
      final OutPacket p = OutPacket.create(SendOpcode.IMITATED_NPC_DATA);
      p.writeByte(0x01);
      p.writeInt(npc.getScriptId());
      p.writeString(npc.getName());
      p.writeByte(npc.getGender());
      p.writeByte(npc.getSkin());
      p.writeInt(npc.getFace());
      p.writeByte(0);
      p.writeInt(npc.getHair());
      Map<Short, Integer> equip = npc.getEquips();
      Map<Short, Integer> myEquip = new LinkedHashMap<>();
      Map<Short, Integer> maskedEquip = new LinkedHashMap<>();
      for (short position : equip.keySet()) {
         short pos = (byte) (position * -1);
         if (pos < 100 && myEquip.get(pos) == null) {
            myEquip.put(pos, equip.get(position));
         } else if ((pos > 100 && pos != 111) || pos == -128) { // don't ask. o.o
            pos -= 100;
            if (myEquip.get(pos) != null) {
               maskedEquip.put(pos, myEquip.get(pos));
            }
            myEquip.put(pos, equip.get(position));
         } else if (myEquip.get(pos) != null) {
            maskedEquip.put(pos, equip.get(position));
         }
      }
      for (Map.Entry<Short, Integer> entry : myEquip.entrySet()) {
         p.writeByte(entry.getKey());
         p.writeInt(entry.getValue());
      }
      p.writeByte(0xFF);
      for (Map.Entry<Short, Integer> entry : maskedEquip.entrySet()) {
         p.writeByte(entry.getKey());
         p.writeInt(entry.getValue());
      }
      p.writeByte(0xFF);
      Integer cWeapon = equip.get((byte) -111);
      p.writeInt(Objects.requireNonNullElse(cWeapon, 0));
      for (int i = 0; i < 3; i++) {
         p.writeInt(0);
      }
      return p;
   }

   public static Packet removePlayerNPC(int oid) {
      final OutPacket p = OutPacket.create(SendOpcode.IMITATED_NPC_DATA);
      p.writeByte(0x00);
      p.writeInt(oid);

      return p;
   }

   public static Packet spawnNPC(MapleNPC life) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_NPC);
      p.writeInt(life.getObjectId());
      p.writeInt(life.getId());
      p.writeShort(life.getPosition().x);
      p.writeShort(life.getCy());
      p.writeByte(life.getF() == 1 ? 0 : 1);
      p.writeShort(life.getFh());
      p.writeShort(life.getRx0());
      p.writeShort(life.getRx1());
      p.writeByte(1);
      return p;
   }

   public static Packet removeNPC(int oid) {
      final OutPacket p = OutPacket.create(SendOpcode.REMOVE_NPC);
      p.writeInt(oid);

      return p;
   }

   public static Packet spawnNPCRequestController(MapleNPC life, boolean MiniMap) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_NPC_REQUEST_CONTROLLER);
      p.writeByte(1);
      p.writeInt(life.getObjectId());
      p.writeInt(life.getId());
      p.writeShort(life.getPosition().x);
      p.writeShort(life.getCy());
      if (life.getF() == 1) {
         p.writeByte(0);
      } else {
         p.writeByte(1);
      }
      p.writeShort(life.getFh());
      p.writeShort(life.getRx0());
      p.writeShort(life.getRx1());
      p.writeBool(MiniMap);
      return p;
   }

   public static Packet spawnPlayerNPC(MaplePlayerNPC npc) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_NPC_REQUEST_CONTROLLER);
      p.writeByte(1);
      p.writeInt(npc.getObjectId());
      p.writeInt(npc.getScriptId());
      p.writeShort(npc.getPosition().x);
      p.writeShort(npc.getCY());
      p.writeByte(npc.getDirection());
      p.writeShort(npc.getFH());
      p.writeShort(npc.getRX0());
      p.writeShort(npc.getRX1());
      p.writeByte(1);
      return p;
   }

   public static Packet removeNPCController(int objectid) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_NPC_REQUEST_CONTROLLER);
      p.writeByte(0);
      p.writeInt(objectid);

      return p;
   }
}
