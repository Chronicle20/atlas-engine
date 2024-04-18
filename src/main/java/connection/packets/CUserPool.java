package connection.packets;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleMount;
import client.TemporaryStatType;
import client.inventory.MapleInventoryType;
import client.newyear.NewYearCardRecord;
import connection.headers.SendOpcode;
import constants.inventory.ItemConstants;
import net.packet.OutPacket;
import net.packet.Packet;
import net.server.guild.MapleGuildSummary;
import tools.Pair;

public class CUserPool {
   /**
    * Gets a packet spawning a player as a mapobject to other clients.
    *
    * @param target        The client receiving this packet.
    * @param chr           The character to spawn to other clients.
    * @param enteringField Whether the character to spawn is not yet present in the map or already is.
    * @return The spawn player packet.
    */
   public static Packet spawnPlayerMapObject(MapleClient target, MapleCharacter chr, boolean enteringField) {
      final OutPacket p = OutPacket.create(SendOpcode.SPAWN_PLAYER);
      p.writeInt(chr.getId());
      p.writeByte(chr.getLevel()); //v83
      p.writeString(chr.getName());

      if (chr.getGuildId() < 1) {
         p.writeString("");
         p.writeShort(0);
         p.writeByte(0);
         p.writeShort(0);
         p.writeByte(0);
      } else {
         MapleGuildSummary gs = chr.getClient().getWorldServer().getGuildSummary(chr.getGuildId(), chr.getWorld());
         if (gs != null) {
            p.writeString(gs.getName());
            p.writeShort(gs.getLogoBG());
            p.writeByte(gs.getLogoBGColor());
            p.writeShort(gs.getLogo());
            p.writeByte(gs.getLogoColor());
         } else {
            p.writeString("");
            p.writeShort(0);
            p.writeByte(0);
            p.writeShort(0);
            p.writeByte(0);
         }
      }

      writeForeignBuffs(p, chr);

      p.writeShort(chr.getJob().getId());

      CCommon.addCharLook(p, chr, false);

      p.writeInt(0);
      p.writeInt(0);
      p.writeInt(0);
      p.writeInt(chr.getItemEffect());
      p.writeInt(ItemConstants.getInventoryType(chr.getChair()) == MapleInventoryType.SETUP ? chr.getChair() : 0);

      if (enteringField) {
         Point spawnPos = new Point(chr.getPosition());
         spawnPos.y -= 42;
         p.writePos(spawnPos);
         p.writeByte(6);
      } else {
         p.writePos(chr.getPosition());
         p.writeByte(chr.getStance());
      }

      p.writeShort(0);//chr.getFh()
      //        p.writeByte(0);
      //        MaplePet[] pet = chr.getPets();
      //        for (int i = 0; i < 3; i++) {
      //            if (pet[i] != null) {
      //                CCommon.addPetInfo(p, pet[i], false);
      //            }
      //        }
      p.writeByte(0); //end of pets

      p.writeInt(chr.getMount().map(MapleMount::getLevel).orElse(1));
      p.writeInt(chr.getMount().map(MapleMount::getExp).orElse(0));
      p.writeInt(chr.getMount().map(MapleMount::getTiredness).orElse(0));

      //        MaplePlayerShop mps = chr.getPlayerShop();
      //        if (mps != null && mps.isOwner(chr)) {
      //            if (mps.hasFreeSlot()) {
      //                CCommon.addAnnounceBox(p, mps, mps.getVisitors().length);
      //            } else {
      //                CCommon.addAnnounceBox(p, mps, 1);
      //            }
      //        } else {
      //            MapleMiniGame miniGame = chr.getMiniGame();
      //            if (miniGame != null && miniGame.isOwner(chr)) {
      //                if (miniGame.hasFreeSlot()) {
      //                    CCommon.addAnnounceBox(p, miniGame, 1, 0);
      //                } else {
      //                    CCommon.addAnnounceBox(p, miniGame, 2, miniGame.isMatchInProgress() ? 1 : 0);
      //                }
      //            } else {
      p.writeByte(0); // mini room
      //            }
      //        }

      //        if (chr.getChalkboard()
      //                .isPresent()) {
      //            p.writeByte(1);
      //            p.writeMapleAsciiString(chr.getChalkboard()
      //                    .get());
      //        } else {
      p.writeByte(0); // ad board
      //        }
      //        CCommon.addRingLook(p, chr, true);  // crush
      //        CCommon.addRingLook(p, chr, false); // friendship
      //        CCommon.addMarriageRingLook(target, p, chr);
      //        encodeNewYearCardInfo(p, chr);  // new year seems to crash sometimes...
      //        p.writeByte(0);
      //        p.writeByte(0);
      //        p.writeByte(chr.getTeam());//only needed in specific fields
      p.writeByte(0);
      p.writeByte(0);
      p.writeByte(0);
      p.writeByte(0); // getEffectMask?
      p.writeByte(0); // looks like a new years card
      p.writeByte(chr.getTeam());
      return p;
   }

   public static Packet removePlayerFromMap(int cid) {
      final OutPacket p = OutPacket.create(SendOpcode.REMOVE_PLAYER_FROM_MAP);
      p.writeInt(cid);
      return p;
   }

   private static void writeForeignBuffs(OutPacket p, MapleCharacter chr) {
      int[] buffmask = new int[4];
      int monsterRiding = 0;
      List<Pair<Integer, Integer>> buffList = new ArrayList<>();
      buffmask[TemporaryStatType.ENERGY_CHARGE.getSet()] |= TemporaryStatType.ENERGY_CHARGE.getMask();
      buffmask[TemporaryStatType.DASH_SPEED.getSet()] |= TemporaryStatType.DASH_SPEED.getMask();
      buffmask[TemporaryStatType.DASH_JUMP.getSet()] |= TemporaryStatType.DASH_JUMP.getMask();
      buffmask[TemporaryStatType.MONSTER_RIDING.getSet()] |= TemporaryStatType.MONSTER_RIDING.getMask();
      buffmask[TemporaryStatType.SPEED_INFUSION.getSet()] |= TemporaryStatType.SPEED_INFUSION.getMask();
      buffmask[TemporaryStatType.HOMING_BEACON.getSet()] |= TemporaryStatType.HOMING_BEACON.getMask();
      buffmask[TemporaryStatType.UNDEAD.getSet()] |= TemporaryStatType.UNDEAD.getMask();

      for (Pair<TemporaryStatType, Integer> statup : chr.getAllActiveStatups()) {
         if (statup.getLeft() == TemporaryStatType.SPEED) {
            buffmask[TemporaryStatType.SPEED.getSet()] |= TemporaryStatType.SPEED.getMask();
            buffList.add(new Pair<>(chr.getBuffedValue(TemporaryStatType.SPEED), 1));
         }
         if (statup.getLeft() == TemporaryStatType.COMBO) {
            buffmask[TemporaryStatType.COMBO.getSet()] |= TemporaryStatType.COMBO.getMask();
            buffList.add(new Pair<>(chr.getBuffedValue(TemporaryStatType.COMBO), 1));
         }
         if (statup.getLeft() == TemporaryStatType.WK_CHARGE) {
            buffmask[TemporaryStatType.WK_CHARGE.getSet()] |= TemporaryStatType.WK_CHARGE.getMask();
            buffList.add(new Pair<>(chr.getBuffedValue(TemporaryStatType.WK_CHARGE), 4));
         }
         if (statup.getLeft() == TemporaryStatType.SHADOWPARTNER) {
            buffmask[TemporaryStatType.SHADOWPARTNER.getSet()] |= TemporaryStatType.SHADOWPARTNER.getMask();
         }
         if (statup.getLeft() == TemporaryStatType.DARKSIGHT) {
            buffmask[TemporaryStatType.DARKSIGHT.getSet()] |= TemporaryStatType.DARKSIGHT.getMask();
         }
         if (statup.getLeft() == TemporaryStatType.SOULARROW) {
            buffmask[TemporaryStatType.SOULARROW.getSet()] |= TemporaryStatType.SOULARROW.getMask();
         }
         if (statup.getLeft() == TemporaryStatType.MORPH) {
            buffmask[TemporaryStatType.MORPH.getSet()] |= TemporaryStatType.MORPH.getMask();
            buffList.add(new Pair<>(chr.getBuffedValue(TemporaryStatType.MORPH), 2));
         }
         if (statup.getLeft() == TemporaryStatType.GHOST_MORPH) {
            buffmask[TemporaryStatType.GHOST_MORPH.getSet()] |= TemporaryStatType.GHOST_MORPH.getMask();
            buffList.add(new Pair<>(chr.getBuffedValue(TemporaryStatType.GHOST_MORPH), 2));
         }
         //            if(statup.getLeft() == TemporaryStatType.SEDUCE){
         //                buffmask[TemporaryStatType.SEDUCE.getSet()] |= TemporaryStatType.SEDUCE.getMask();
         //                buffList.add(new Pair<>(statup.getRight().getSourceLevel(), 2));
         //                buffList.add(new Pair<>(statup.getRight().getSourceID(), 2));
         //            }
         if (statup.getLeft() == TemporaryStatType.SHADOW_CLAW) {
            buffmask[TemporaryStatType.SHADOW_CLAW.getSet()] |= TemporaryStatType.SHADOW_CLAW.getMask();
            buffList.add(new Pair<>(chr.getBuffedValue(TemporaryStatType.SHADOW_CLAW), 4));
         }
         //            if(statup.getLeft() == TemporaryStatType.BAN_MAP){
         //                buffmask[TemporaryStatType.BAN_MAP.getSet()] |= TemporaryStatType.BAN_MAP.getMask();
         //                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
         //            }
         //            if(statup.getLeft() == TemporaryStatType.BARRIER){
         //                buffmask[TemporaryStatType.BARRIER.getSet()] |= TemporaryStatType.BARRIER.getMask();
         //                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
         //            }
         //            if(statup.getLeft() == TemporaryStatType.DOJANG_SHIELD){
         //                buffmask[TemporaryStatType.DOJANG_SHIELD.getSet()] |= TemporaryStatType.DOJANG_SHIELD.getMask();
         //                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
         //            }
         if (statup.getLeft() == TemporaryStatType.CONFUSE) {
            buffmask[TemporaryStatType.CONFUSE.getSet()] |= TemporaryStatType.CONFUSE.getMask();
            buffList.add(new Pair<>(1, 2)); // TODO get source level
            buffList.add(new Pair<>(chr.getBuffSource(TemporaryStatType.CONFUSE), 2));
         }
         if (statup.getLeft() == TemporaryStatType.RESPECT_PIMMUNE) {
            buffmask[TemporaryStatType.RESPECT_PIMMUNE.getSet()] |= TemporaryStatType.RESPECT_PIMMUNE.getMask();
            buffList.add(new Pair<>(chr.getBuffedValue(TemporaryStatType.RESPECT_PIMMUNE), 4));
         }
         if (statup.getLeft() == TemporaryStatType.RESPECT_MIMMUNE) {
            buffmask[TemporaryStatType.RESPECT_MIMMUNE.getSet()] |= TemporaryStatType.RESPECT_MIMMUNE.getMask();
            buffList.add(new Pair<>(chr.getBuffedValue(TemporaryStatType.RESPECT_MIMMUNE), 4));
         }
         if (statup.getLeft() == TemporaryStatType.DEFENSE_ATT) {
            buffmask[TemporaryStatType.DEFENSE_ATT.getSet()] |= TemporaryStatType.DEFENSE_ATT.getMask();
            buffList.add(new Pair<>(chr.getBuffedValue(TemporaryStatType.DEFENSE_ATT), 4));
         }
         if (statup.getLeft() == TemporaryStatType.DEFENSE_STATE) {
            buffmask[TemporaryStatType.DEFENSE_STATE.getSet()] |= TemporaryStatType.DEFENSE_STATE.getMask();
            buffList.add(new Pair<>(chr.getBuffedValue(TemporaryStatType.DEFENSE_STATE), 4));
         }
         if (statup.getLeft() == TemporaryStatType.BERSERK_FURY) {
            buffmask[TemporaryStatType.BERSERK_FURY.getSet()] |= TemporaryStatType.BERSERK_FURY.getMask();
         }
         if (statup.getLeft() == TemporaryStatType.DIVINE_BODY) {
            buffmask[TemporaryStatType.DIVINE_BODY.getSet()] |= TemporaryStatType.DIVINE_BODY.getMask();
         }
         if (statup.getLeft() == TemporaryStatType.WIND_WALK) {
            buffmask[TemporaryStatType.WIND_WALK.getSet()] |= TemporaryStatType.WIND_WALK.getMask();
         }
         //            if(statup.getLeft() == TemporaryStatType.REPEAT_EFFECT){
         //                buffmask[TemporaryStatType.REPEAT_EFFECT.getSet()] |= TemporaryStatType.REPEAT_EFFECT.getMask();
         //                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
         //            }
         //            if(statup.getLeft() == TemporaryStatType.STOP_PORTION){
         //                buffmask[TemporaryStatType.STOP_PORTION.getSet()] |= TemporaryStatType.STOP_PORTION.getMask();
         //                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
         //            }
         //            if(statup.getLeft() == TemporaryStatType.STOP_MOTION){
         //                buffmask[TemporaryStatType.STOP_MOTION.getSet()] |= TemporaryStatType.STOP_MOTION.getMask();
         //                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
         //            }
         //            if(statup.getLeft() == TemporaryStatType.FEAR){
         //                buffmask[TemporaryStatType.FEAR.getSet()] |= TemporaryStatType.FEAR.getMask();
         //                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
         //            }
         //            if(statup.getLeft() == TemporaryStatType.Flying){
         //                buffmask[TemporaryStatType.Flying.getSet()] |= TemporaryStatType.Flying.getMask();
         //            }
         //            if(statup.getLeft() == TemporaryStatType.Frozen){
         //                buffmask[TemporaryStatType.Frozen.getSet()] |= TemporaryStatType.Frozen.getMask();
         //                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
         //            }
         //            if(statup.getLeft() == TemporaryStatType.SuddenDeath){
         //                buffmask[TemporaryStatType.SuddenDeath.getSet()] |= TemporaryStatType.SuddenDeath.getMask();
         //                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
         //            }
         //            if(statup.getLeft() == TemporaryStatType.FinalCut){
         //                buffmask[TemporaryStatType.FinalCut.getSet()] |= TemporaryStatType.FinalCut.getMask();
         //                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
         //            }
         //            if(statup.getLeft() == TemporaryStatType.Cyclone){
         //                buffmask[TemporaryStatType.Cyclone.getSet()] |= TemporaryStatType.Cyclone.getMask();
         //                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
         //            }
         //            if(statup.getLeft() == TemporaryStatType.Sneak){
         //                buffmask[TemporaryStatType.Sneak.getSet()] |= TemporaryStatType.Sneak.getMask();
         //            }
         //            if(statup.getLeft() == TemporaryStatType.MorewildDamageUp){
         //                buffmask[TemporaryStatType.MorewildDamageUp.getSet()] |= TemporaryStatType.MorewildDamageUp.getMask();
         //            }
         if (statup.getLeft() == TemporaryStatType.STUN) {
            buffmask[TemporaryStatType.STUN.getSet()] |= TemporaryStatType.STUN.getMask();
            buffList.add(new Pair<>(1, 2)); // TODO get source level
            buffList.add(new Pair<>(chr.getBuffSource(TemporaryStatType.STUN), 2));
         }
         if (statup.getLeft() == TemporaryStatType.DARKNESS) {
            buffmask[TemporaryStatType.DARKNESS.getSet()] |= TemporaryStatType.DARKNESS.getMask();
            buffList.add(new Pair<>(1, 2)); // TODO get source level
            buffList.add(new Pair<>(chr.getBuffSource(TemporaryStatType.DARKNESS), 2));
         }
         if (statup.getLeft() == TemporaryStatType.SEAL) {
            buffmask[TemporaryStatType.SEAL.getSet()] |= TemporaryStatType.SEAL.getMask();
            buffList.add(new Pair<>(1, 2)); // TODO get source level
            buffList.add(new Pair<>(chr.getBuffSource(TemporaryStatType.SEAL), 2));
         }
         if (statup.getLeft() == TemporaryStatType.WEAKEN) {
            buffmask[TemporaryStatType.WEAKEN.getSet()] |= TemporaryStatType.WEAKEN.getMask();
            buffList.add(new Pair<>(1, 2)); // TODO get source level
            buffList.add(new Pair<>(chr.getBuffSource(TemporaryStatType.WEAKEN), 2));
         }
         //            if(statup.getLeft() == TemporaryStatType.CURSE){
         //                buffmask[TemporaryStatType.CURSE.getSet()] |= TemporaryStatType.CURSE.getMask();
         //                buffList.add(new Pair<>(statup.getRight().getSourceLevel(), 2));
         //                buffList.add(new Pair<>(statup.getRight().getSourceID(), 2));
         //            }
         if (statup.getLeft() == TemporaryStatType.POISON) {
            buffmask[TemporaryStatType.POISON.getSet()] |= TemporaryStatType.POISON.getMask();
            buffList.add(new Pair<>(chr.getBuffedValue(TemporaryStatType.POISON), 2));
            buffList.add(new Pair<>(1, 2)); // TODO get source level
            buffList.add(new Pair<>(chr.getBuffSource(TemporaryStatType.POISON), 2));
         }
         if (statup.getLeft() == TemporaryStatType.MONSTER_RIDING) {
            monsterRiding = chr.getBuffedValue(TemporaryStatType.MONSTER_RIDING);
         }
      }
      for (int i = 3; i >= 0; i--) {
         p.writeInt(buffmask[i]);
      }
      for (Pair<Integer, Integer> buff : buffList) {
         if (buff.right == 4) {
            p.writeInt(buff.left);
         } else if (buff.right == 2) {
            p.writeShort(buff.left);
         } else if (buff.right == 1) {
            p.writeByte(buff.left);
         }
      }

      p.writeByte(0);
      p.writeByte(0);

      CCommon.getTemporaryStats(chr).forEach(ts -> ts.EncodeForClient(p));
   }

   private static void encodeNewYearCardInfo(OutPacket p, MapleCharacter chr) {
      Set<NewYearCardRecord> newyears = chr.getReceivedNewYearRecords();
      if (!newyears.isEmpty()) {
         p.writeByte(1);

         p.writeInt(newyears.size());
         for (NewYearCardRecord nyc : newyears) {
            p.writeInt(nyc.getId());
         }
      } else {
         p.writeByte(0);
      }
   }
}
