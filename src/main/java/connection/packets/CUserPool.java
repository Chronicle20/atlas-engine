package connection.packets;

import client.BuffStat;
import client.GuidedBullet;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleMount;
import client.SpeedInfusion;
import client.TemporaryStatBase;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.newyear.NewYearCardRecord;
import connection.constants.SendOpcode;
import constants.inventory.ItemConstants;
import net.server.guild.MapleGuildSummary;
import server.maps.MapleMiniGame;
import server.maps.MaplePlayerShop;
import tools.Pair;
import tools.Randomizer;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CUserPool {
    /**
     * Gets a packet spawning a player as a mapobject to other clients.
     *
     * @param target        The client receiving this packet.
     * @param chr           The character to spawn to other clients.
     * @param enteringField Whether the character to spawn is not yet present in the map or already is.
     * @return The spawn player packet.
     */
    public static byte[] spawnPlayerMapObject(MapleClient target, MapleCharacter chr, boolean enteringField) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_PLAYER.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getLevel()); //v83
        mplew.writeMapleAsciiString(chr.getName());

        if (chr.getGuildId() < 1) {
            mplew.writeMapleAsciiString("");
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeShort(0);
            mplew.write(0);
        } else {
            MapleGuildSummary gs = chr.getClient()
                    .getWorldServer()
                    .getGuildSummary(chr.getGuildId(), chr.getWorld());
            if (gs != null) {
                mplew.writeMapleAsciiString(gs.getName());
                mplew.writeShort(gs.getLogoBG());
                mplew.write(gs.getLogoBGColor());
                mplew.writeShort(gs.getLogo());
                mplew.write(gs.getLogoColor());
            } else {
                mplew.writeMapleAsciiString("");
                mplew.writeShort(0);
                mplew.write(0);
                mplew.writeShort(0);
                mplew.write(0);
            }
        }

        writeForeignBuffs(mplew, chr);

        mplew.writeShort(chr.getJob()
                .getId());

        CCommon.addCharLook(mplew, chr, false);

        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(chr.getItemEffect());
        mplew.writeInt(ItemConstants.getInventoryType(chr.getChair()) == MapleInventoryType.SETUP ? chr.getChair() : 0);

        if (enteringField) {
            Point spawnPos = new Point(chr.getPosition());
            spawnPos.y -= 42;
            mplew.writePos(spawnPos);
            mplew.write(6);
        } else {
            mplew.writePos(chr.getPosition());
            mplew.write(chr.getStance());
        }

        mplew.writeShort(0);//chr.getFh()
//        mplew.write(0);
//        MaplePet[] pet = chr.getPets();
//        for (int i = 0; i < 3; i++) {
//            if (pet[i] != null) {
//                CCommon.addPetInfo(mplew, pet[i], false);
//            }
//        }
        mplew.write(0); //end of pets

        mplew.writeInt(chr.getMount()
                .map(MapleMount::getLevel)
                .orElse(1));
        mplew.writeInt(chr.getMount()
                .map(MapleMount::getExp)
                .orElse(0));
        mplew.writeInt(chr.getMount()
                .map(MapleMount::getTiredness)
                .orElse(0));

//        MaplePlayerShop mps = chr.getPlayerShop();
//        if (mps != null && mps.isOwner(chr)) {
//            if (mps.hasFreeSlot()) {
//                CCommon.addAnnounceBox(mplew, mps, mps.getVisitors().length);
//            } else {
//                CCommon.addAnnounceBox(mplew, mps, 1);
//            }
//        } else {
//            MapleMiniGame miniGame = chr.getMiniGame();
//            if (miniGame != null && miniGame.isOwner(chr)) {
//                if (miniGame.hasFreeSlot()) {
//                    CCommon.addAnnounceBox(mplew, miniGame, 1, 0);
//                } else {
//                    CCommon.addAnnounceBox(mplew, miniGame, 2, miniGame.isMatchInProgress() ? 1 : 0);
//                }
//            } else {
                mplew.write(0); // mini room
//            }
//        }

//        if (chr.getChalkboard()
//                .isPresent()) {
//            mplew.write(1);
//            mplew.writeMapleAsciiString(chr.getChalkboard()
//                    .get());
//        } else {
            mplew.write(0); // ad board
//        }
//        CCommon.addRingLook(mplew, chr, true);  // crush
//        CCommon.addRingLook(mplew, chr, false); // friendship
//        CCommon.addMarriageRingLook(target, mplew, chr);
//        encodeNewYearCardInfo(mplew, chr);  // new year seems to crash sometimes...
//        mplew.write(0);
//        mplew.write(0);
//        mplew.write(chr.getTeam());//only needed in specific fields
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0); // getEffectMask?
        mplew.write(0); // looks like a new years card
        mplew.write(chr.getTeam());
        return mplew.getPacket();
    }

    public static byte[] removePlayerFromMap(int cid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
        mplew.writeInt(cid);
        return mplew.getPacket();
    }

    private static void writeForeignBuffs(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        int[] buffmask = new int[4];
        int monsterRiding = 0;
        List<Pair<Integer, Integer>> buffList = new ArrayList<>();
        buffmask[BuffStat.ENERGY_CHARGE.getSet()] |= BuffStat.ENERGY_CHARGE.getMask();
        buffmask[BuffStat.DASH_SPEED.getSet()] |= BuffStat.DASH_SPEED.getMask();
        buffmask[BuffStat.DASH_JUMP.getSet()] |= BuffStat.DASH_JUMP.getMask();
        buffmask[BuffStat.MONSTER_RIDING.getSet()] |= BuffStat.MONSTER_RIDING.getMask();
        buffmask[BuffStat.SPEED_INFUSION.getSet()] |= BuffStat.SPEED_INFUSION.getMask();
        buffmask[BuffStat.HOMING_BEACON.getSet()] |= BuffStat.HOMING_BEACON.getMask();
        buffmask[BuffStat.UNDEAD.getSet()] |= BuffStat.UNDEAD.getMask();

        for(Pair<MapleBuffStat, Integer> statup : chr.getAllActiveStatups()){
            if(statup.getLeft() == MapleBuffStat.SPEED){
                buffmask[BuffStat.SPEED.getSet()] |= BuffStat.SPEED.getMask();
                buffList.add(new Pair<>(chr.getBuffedValue(MapleBuffStat.SPEED), 1));
            }
            if(statup.getLeft() == MapleBuffStat.COMBO){
                buffmask[BuffStat.COMBO.getSet()] |= BuffStat.COMBO.getMask();
                buffList.add(new Pair<>(chr.getBuffedValue(MapleBuffStat.COMBO), 1));
            }
            if(statup.getLeft() == MapleBuffStat.WK_CHARGE){
                buffmask[BuffStat.WK_CHARGE.getSet()] |= BuffStat.WK_CHARGE.getMask();
                buffList.add(new Pair<>(chr.getBuffedValue(MapleBuffStat.WK_CHARGE), 4));
            }
            if(statup.getLeft() == MapleBuffStat.SHADOWPARTNER){
                buffmask[BuffStat.SHADOWPARTNER.getSet()] |= BuffStat.SHADOWPARTNER.getMask();
            }
            if(statup.getLeft() == MapleBuffStat.DARKSIGHT){
                buffmask[BuffStat.DARKSIGHT.getSet()] |= BuffStat.DARKSIGHT.getMask();
            }
            if(statup.getLeft() == MapleBuffStat.SOULARROW){
                buffmask[BuffStat.SOULARROW.getSet()] |= BuffStat.SOULARROW.getMask();
            }
            if(statup.getLeft() == MapleBuffStat.MORPH){
                buffmask[BuffStat.MORPH.getSet()] |= BuffStat.MORPH.getMask();
                buffList.add(new Pair<>(chr.getBuffedValue(MapleBuffStat.MORPH), 2));
            }
            if(statup.getLeft() == MapleBuffStat.GHOST_MORPH){
                buffmask[BuffStat.GHOST_MORPH.getSet()] |= BuffStat.GHOST_MORPH.getMask();
                buffList.add(new Pair<>(chr.getBuffedValue(MapleBuffStat.GHOST_MORPH), 2));
            }
//            if(statup.getLeft() == MapleBuffStat.SEDUCE){
//                buffmask[BuffStat.SEDUCE.getSet()] |= BuffStat.SEDUCE.getMask();
//                buffList.add(new Pair<>(statup.getRight().getSourceLevel(), 2));
//                buffList.add(new Pair<>(statup.getRight().getSourceID(), 2));
//            }
            if(statup.getLeft() == MapleBuffStat.SHADOW_CLAW){
                buffmask[BuffStat.SHADOW_CLAW.getSet()] |= BuffStat.SHADOW_CLAW.getMask();
                buffList.add(new Pair<>(chr.getBuffedValue(MapleBuffStat.SHADOW_CLAW), 4));
            }
//            if(statup.getLeft() == MapleBuffStat.BAN_MAP){
//                buffmask[BuffStat.BAN_MAP.getSet()] |= BuffStat.BAN_MAP.getMask();
//                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
//            }
//            if(statup.getLeft() == MapleBuffStat.BARRIER){
//                buffmask[BuffStat.BARRIER.getSet()] |= BuffStat.BARRIER.getMask();
//                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
//            }
//            if(statup.getLeft() == MapleBuffStat.DOJANG_SHIELD){
//                buffmask[BuffStat.DOJANG_SHIELD.getSet()] |= BuffStat.DOJANG_SHIELD.getMask();
//                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
//            }
            if(statup.getLeft() == MapleBuffStat.CONFUSE){
                buffmask[BuffStat.CONFUSE.getSet()] |= BuffStat.CONFUSE.getMask();
                buffList.add(new Pair<>(1, 2)); // TODO get source level
                buffList.add(new Pair<>(chr.getBuffSource(MapleBuffStat.CONFUSE), 2));
            }
            if(statup.getLeft() == MapleBuffStat.RESPECT_PIMMUNE){
                buffmask[BuffStat.RESPECT_PIMMUNE.getSet()] |= BuffStat.RESPECT_PIMMUNE.getMask();
                buffList.add(new Pair<>(chr.getBuffedValue(MapleBuffStat.RESPECT_PIMMUNE), 4));
            }
            if(statup.getLeft() == MapleBuffStat.RESPECT_MIMMUNE){
                buffmask[BuffStat.RESPECT_MIMMUNE.getSet()] |= BuffStat.RESPECT_MIMMUNE.getMask();
                buffList.add(new Pair<>(chr.getBuffedValue(MapleBuffStat.RESPECT_MIMMUNE), 4));
            }
            if(statup.getLeft() == MapleBuffStat.DEFENSE_ATT){
                buffmask[BuffStat.DEFENSE_ATT.getSet()] |= BuffStat.DEFENSE_ATT.getMask();
                buffList.add(new Pair<>(chr.getBuffedValue(MapleBuffStat.DEFENSE_ATT), 4));
            }
            if(statup.getLeft() == MapleBuffStat.DEFENSE_STATE){
                buffmask[BuffStat.DEFENSE_STATE.getSet()] |= BuffStat.DEFENSE_STATE.getMask();
                buffList.add(new Pair<>(chr.getBuffedValue(MapleBuffStat.DEFENSE_STATE), 4));
            }
            if(statup.getLeft() == MapleBuffStat.BERSERK_FURY){
                buffmask[BuffStat.BERSERK_FURY.getSet()] |= BuffStat.BERSERK_FURY.getMask();
            }
            if(statup.getLeft() == MapleBuffStat.DIVINE_BODY){
                buffmask[BuffStat.DIVINE_BODY.getSet()] |= BuffStat.DIVINE_BODY.getMask();
            }
            if(statup.getLeft() == MapleBuffStat.WIND_WALK){
                buffmask[BuffStat.WIND_WALK.getSet()] |= BuffStat.WIND_WALK.getMask();
            }
//            if(statup.getLeft() == MapleBuffStat.REPEAT_EFFECT){
//                buffmask[BuffStat.REPEAT_EFFECT.getSet()] |= BuffStat.REPEAT_EFFECT.getMask();
//                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
//            }
//            if(statup.getLeft() == MapleBuffStat.STOP_PORTION){
//                buffmask[BuffStat.STOP_PORTION.getSet()] |= BuffStat.STOP_PORTION.getMask();
//                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
//            }
//            if(statup.getLeft() == MapleBuffStat.STOP_MOTION){
//                buffmask[BuffStat.STOP_MOTION.getSet()] |= BuffStat.STOP_MOTION.getMask();
//                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
//            }
//            if(statup.getLeft() == MapleBuffStat.FEAR){
//                buffmask[BuffStat.FEAR.getSet()] |= BuffStat.FEAR.getMask();
//                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
//            }
//            if(statup.getLeft() == MapleBuffStat.Flying){
//                buffmask[BuffStat.Flying.getSet()] |= BuffStat.Flying.getMask();
//            }
//            if(statup.getLeft() == MapleBuffStat.Frozen){
//                buffmask[BuffStat.Frozen.getSet()] |= BuffStat.Frozen.getMask();
//                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
//            }
//            if(statup.getLeft() == MapleBuffStat.SuddenDeath){
//                buffmask[BuffStat.SuddenDeath.getSet()] |= BuffStat.SuddenDeath.getMask();
//                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
//            }
//            if(statup.getLeft() == MapleBuffStat.FinalCut){
//                buffmask[BuffStat.FinalCut.getSet()] |= BuffStat.FinalCut.getMask();
//                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
//            }
//            if(statup.getLeft() == MapleBuffStat.Cyclone){
//                buffmask[BuffStat.Cyclone.getSet()] |= BuffStat.Cyclone.getMask();
//                buffList.add(new Pair<>(statup.getRight().getValue(), 4));
//            }
//            if(statup.getLeft() == MapleBuffStat.Sneak){
//                buffmask[BuffStat.Sneak.getSet()] |= BuffStat.Sneak.getMask();
//            }
//            if(statup.getLeft() == MapleBuffStat.MorewildDamageUp){
//                buffmask[BuffStat.MorewildDamageUp.getSet()] |= BuffStat.MorewildDamageUp.getMask();
//            }
            if(statup.getLeft() == MapleBuffStat.STUN){
                buffmask[BuffStat.STUN.getSet()] |= BuffStat.STUN.getMask();
                buffList.add(new Pair<>(1, 2)); // TODO get source level
                buffList.add(new Pair<>(chr.getBuffSource(MapleBuffStat.STUN), 2));
            }
            if(statup.getLeft() == MapleBuffStat.DARKNESS){
                buffmask[BuffStat.DARKNESS.getSet()] |= BuffStat.DARKNESS.getMask();
                buffList.add(new Pair<>(1, 2)); // TODO get source level
                buffList.add(new Pair<>(chr.getBuffSource(MapleBuffStat.DARKNESS), 2));
            }
            if(statup.getLeft() == MapleBuffStat.SEAL){
                buffmask[BuffStat.SEAL.getSet()] |= BuffStat.SEAL.getMask();
                buffList.add(new Pair<>(1, 2)); // TODO get source level
                buffList.add(new Pair<>(chr.getBuffSource(MapleBuffStat.SEAL), 2));
            }
            if(statup.getLeft() == MapleBuffStat.WEAKEN){
                buffmask[BuffStat.WEAKEN.getSet()] |= BuffStat.WEAKEN.getMask();
                buffList.add(new Pair<>(1, 2)); // TODO get source level
                buffList.add(new Pair<>(chr.getBuffSource(MapleBuffStat.WEAKEN), 2));
            }
//            if(statup.getLeft() == MapleBuffStat.CURSE){
//                buffmask[BuffStat.CURSE.getSet()] |= BuffStat.CURSE.getMask();
//                buffList.add(new Pair<>(statup.getRight().getSourceLevel(), 2));
//                buffList.add(new Pair<>(statup.getRight().getSourceID(), 2));
//            }
            if(statup.getLeft() == MapleBuffStat.POISON){
                buffmask[BuffStat.POISON.getSet()] |= BuffStat.POISON.getMask();
                buffList.add(new Pair<>(chr.getBuffedValue(MapleBuffStat.POISON), 2));
                buffList.add(new Pair<>(1, 2)); // TODO get source level
                buffList.add(new Pair<>(chr.getBuffSource(MapleBuffStat.POISON), 2));
            }
            if(statup.getLeft() == MapleBuffStat.MONSTER_RIDING){
                monsterRiding = chr.getBuffedValue(MapleBuffStat.MONSTER_RIDING);
            }
        }
        for(int i = 3; i >= 0; i--){
            mplew.writeInt(buffmask[i]);
        }
        for(Pair<Integer, Integer> buff : buffList){
            if(buff.right == 4){
                mplew.writeInt(buff.left);
            }else if(buff.right == 2){
                mplew.writeShort(buff.left);
            }else if(buff.right == 1){
                mplew.write(buff.left);
            }
        }

        mplew.write(0);
        mplew.write(0);

        getTemporaryStats(chr).forEach(ts -> ts.EncodeForClient(mplew));
    }

    private static List<TemporaryStatBase> getTemporaryStats(MapleCharacter character) {
        List<TemporaryStatBase> list = new ArrayList<>();
        list.add(new TemporaryStatBase(true)); // Energy Charged

        list.add(new TemporaryStatBase(true)); // Dash Speed

        list.add(new TemporaryStatBase(true)); // Dash Jump

        TemporaryStatBase mount;
        Integer bv = character.getBuffedValue(MapleBuffStat.MONSTER_RIDING);
        if (bv == null) {
            mount = new TemporaryStatBase(false);
        } else {
            int itemId = character.getMount()
                    .map(MapleMount::getItemId)
                    .orElse(0);
            int skillId = character.getMount()
                    .map(MapleMount::getSkillId)
                    .orElse(0);
            mount = new TemporaryStatBase(itemId, skillId, false);
        }
        list.add(mount);

        list.add(new SpeedInfusion()); // Speed Infusion

        list.add(new GuidedBullet()); // Guided Bullet

        list.add(new TemporaryStatBase(true)); // Undead

        return list;
    }

    private static void encodeNewYearCardInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        Set<NewYearCardRecord> newyears = chr.getReceivedNewYearRecords();
        if (!newyears.isEmpty()) {
            mplew.write(1);

            mplew.writeInt(newyears.size());
            for (NewYearCardRecord nyc : newyears) {
                mplew.writeInt(nyc.getId());
            }
        } else {
            mplew.write(0);
        }
    }
}
