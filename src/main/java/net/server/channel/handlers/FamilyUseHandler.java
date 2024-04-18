package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleFamilyEntitlement;
import client.MapleFamilyEntry;
import config.YamlConfig;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.coordinator.world.MapleInviteCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator.InviteType;
import server.maps.FieldLimit;
import server.maps.MapleMap;

public final class FamilyUseHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (!YamlConfig.config.server.USE_FAMILY_SYSTEM) {
         return;
      }
      MapleFamilyEntitlement type = MapleFamilyEntitlement.values()[p.readInt()];
      int cost = type.getRepCost();
      MapleFamilyEntry entry = c.getPlayer().getFamilyEntry();
      if (entry.getReputation() < cost || entry.isEntitlementUsed(type)) {
         return; // shouldn't even be able to request it
      }
      c.sendPacket(CWvsContext.getFamilyInfo(entry));
      MapleCharacter victim;
      if (type == MapleFamilyEntitlement.FAMILY_REUINION || type == MapleFamilyEntitlement.SUMMON_FAMILY) {
         victim = c.getChannelServer().getPlayerStorage().getCharacterByName(p.readString()).orElse(null);
         if (victim != null && victim != c.getPlayer()) {
            if (victim.getFamily().orElse(null) == c.getPlayer().getFamily().orElse(null)) {
               MapleMap targetMap = victim.getMap();
               MapleMap ownMap = c.getPlayer().getMap();
               if (targetMap != null) {
                  if (type == MapleFamilyEntitlement.FAMILY_REUINION) {
                     if (!FieldLimit.CANNOTMIGRATE.check(ownMap.getFieldLimit()) && !FieldLimit.CANNOTVIPROCK.check(
                           targetMap.getFieldLimit()) && (targetMap.getForcedReturnId() == 999999999
                           || targetMap.getId() < 100000000) && targetMap.getEventInstance().isEmpty()) {

                        c.getPlayer().changeMap(victim.getMap(), victim.getMap().getPortal(0));
                        useEntitlement(entry, type);
                     } else {
                        c.sendPacket(CWvsContext.sendFamilyMessage(75,
                              0)); // wrong message, but close enough. (client should check this first anyway)
                     }
                  } else {
                     if (!FieldLimit.CANNOTMIGRATE.check(targetMap.getFieldLimit()) && !FieldLimit.CANNOTVIPROCK.check(
                           ownMap.getFieldLimit()) && (ownMap.getForcedReturnId() == 999999999 || ownMap.getId() < 100000000)
                           && ownMap.getEventInstance().isEmpty()) {

                        if (MapleInviteCoordinator.hasInvite(InviteType.FAMILY_SUMMON, victim.getId())) {
                           c.sendPacket(CWvsContext.sendFamilyMessage(74, 0));
                           return;
                        }
                        MapleInviteCoordinator.createInvite(InviteType.FAMILY_SUMMON, c.getPlayer(), victim, victim.getId(),
                              c.getPlayer().getMap());
                        victim.sendPacket(CWvsContext.sendFamilySummonRequest(c.getPlayer().getFamily().orElseThrow().getName(),
                              c.getPlayer().getName()));
                        useEntitlement(entry, type);
                     } else {
                        c.sendPacket(CWvsContext.sendFamilyMessage(75, 0));
                     }
                  }
               }
            } else {
               c.sendPacket(CWvsContext.sendFamilyMessage(67, 0));
            }
         }
      } else if (type == MapleFamilyEntitlement.FAMILY_BONDING) {
         //not implemented
      } else {
         boolean party = false;
         boolean isExp = false;
         float rate = 1.5f;
         int duration = 15;
         do {
            switch (type) {
               case PARTY_EXP_2_30MIN:
                  party = true;
                  isExp = true;
                  type = MapleFamilyEntitlement.SELF_EXP_2_30MIN;
                  continue;
               case PARTY_DROP_2_30MIN:
                  party = true;
                  type = MapleFamilyEntitlement.SELF_DROP_2_30MIN;
                  continue;
               case SELF_DROP_2_30MIN:
                  duration = 30;
               case SELF_DROP_2:
                  rate = 2.0f;
               case SELF_DROP_1_5:
                  break;
               case SELF_EXP_2_30MIN:
                  duration = 30;
               case SELF_EXP_2:
                  rate = 2.0f;
               case SELF_EXP_1_5:
                  isExp = true;
               default:
                  break;
            }
            break;
         } while (true);
         //not implemented
      }
   }

   private boolean useEntitlement(MapleFamilyEntry entry, MapleFamilyEntitlement entitlement) {
      if (entry.useEntitlement(entitlement)) {
         entry.gainReputation(-entitlement.getRepCost(), false);
         entry.getChr().sendPacket(CWvsContext.getFamilyInfo(entry));
         return true;
      }
      return false;
   }
}
