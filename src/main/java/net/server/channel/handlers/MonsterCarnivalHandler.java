package net.server.channel.handlers;

import java.awt.*;
import java.util.List;
import java.util.Optional;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import connection.packets.CFieldMonsterCarnival;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.partyquest.MapleCarnivalFactory;
import server.partyquest.MapleCarnivalFactory.MCSkill;
import server.partyquest.MonsterCarnival;
import tools.Pair;

public final class MonsterCarnivalHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (c.tryacquireClient()) {
         try {
            try {
               int tab = p.readByte();
               int num = p.readByte();
               int neededCP = 0;
               if (tab == 0) {
                  final List<Pair<Integer, Integer>> mobs = c.getPlayer().getMap().getMobsToSpawn();
                  if (num >= mobs.size() || c.getPlayer().getCP() < mobs.get(num).right) {
                     c.sendPacket(CFieldMonsterCarnival.CPQMessage((byte) 1));
                     c.sendPacket(CWvsContext.enableActions());
                     return;
                  }

                  final MapleMonster mob = MapleLifeFactory.getMonster(mobs.get(num).left);
                  MonsterCarnival mcpq = c.getPlayer().getMonsterCarnival();
                  if (mcpq != null) {
                     if (!mcpq.canSummonR() && c.getPlayer().getTeam() == 0 || !mcpq.canSummonB() && c.getPlayer().getTeam() == 1) {
                        c.sendPacket(CFieldMonsterCarnival.CPQMessage((byte) 2));
                        c.sendPacket(CWvsContext.enableActions());
                        return;
                     }

                     if (c.getPlayer().getTeam() == 0) {
                        mcpq.summonR();
                     } else {
                        mcpq.summonB();
                     }

                     Point spawnPos = c.getPlayer().getMap().getRandomSP(c.getPlayer().getTeam());
                     mob.setPosition(spawnPos);

                     c.getPlayer().getMap().addMonsterSpawn(mob, 1, c.getPlayer().getTeam());
                     c.getPlayer().getMap().addAllMonsterSpawn(mob, 1, c.getPlayer().getTeam());
                     c.sendPacket(CWvsContext.enableActions());
                  }

                  neededCP = mobs.get(num).right;
               } else if (tab == 1) { //debuffs
                  final List<Integer> skillid = c.getPlayer().getMap().getSkillIds();
                  if (num >= skillid.size()) {
                     c.getPlayer().dropMessage(5, "An unexpected error has occurred.");
                     c.sendPacket(CWvsContext.enableActions());
                     return;
                  }
                  final MCSkill skill = MapleCarnivalFactory.getInstance().getSkill(skillid.get(num)); //ugh wtf
                  if (skill == null || c.getPlayer().getCP() < skill.cpLoss) {
                     c.sendPacket(CFieldMonsterCarnival.CPQMessage((byte) 1));
                     c.sendPacket(CWvsContext.enableActions());
                     return;
                  }
                  Optional<MapleDisease> dis = skill.getDisease();
                  MapleParty enemies = c.getPlayer().getParty().orElseThrow().getEnemy();
                  if (skill.targetsAll) {
                     int hitChance = 0;
                     //TODO this is a NPE possibility need to review what the original logic was trying to do.
                     if (dis.get().getDisease() == 121 || dis.get().getDisease() == 122 || dis.get().getDisease() == 125
                           || dis.get().getDisease() == 126) {
                        hitChance = (int) (Math.random() * 100);
                     }
                     if (hitChance <= 80) {
                        enemies.getPartyMembers().stream().map(MaplePartyCharacter::getPlayer).flatMap(Optional::stream)
                              .forEach(mc -> {
                                 if (dis.isEmpty()) {
                                    mc.dispel();
                                 } else {
                                    mc.giveDebuff(dis.get(), skill.getSkill());
                                 }
                              });
                     }
                  } else {
                     int amount = enemies.getMembers().size() - 1;
                     int randd = (int) Math.floor(Math.random() * amount);
                     Optional<MapleCharacter> chrApp =
                           c.getPlayer().getMap().getCharacterById(enemies.getMemberByPos(randd).getId());
                     if (chrApp.isPresent() && chrApp.get().getMap().isCPQMap()) {
                        if (dis.isEmpty()) {
                           chrApp.get().dispel();
                        } else {
                           chrApp.get().giveDebuff(dis.get(), skill.getSkill());
                        }
                     }
                  }
                  neededCP = skill.cpLoss;
                  c.sendPacket(CWvsContext.enableActions());
               } else if (tab == 2) { //protectors
                  final MCSkill skill = MapleCarnivalFactory.getInstance().getGuardian(num);
                  if (skill == null || c.getPlayer().getCP() < skill.cpLoss) {
                     c.sendPacket(CFieldMonsterCarnival.CPQMessage((byte) 1));
                     c.sendPacket(CWvsContext.enableActions());
                     return;
                  }

                  MonsterCarnival mcpq = c.getPlayer().getMonsterCarnival();
                  if (mcpq != null) {
                     if (!mcpq.canGuardianR() && c.getPlayer().getTeam() == 0
                           || !mcpq.canGuardianB() && c.getPlayer().getTeam() == 1) {
                        c.sendPacket(CFieldMonsterCarnival.CPQMessage((byte) 2));
                        c.sendPacket(CWvsContext.enableActions());
                        return;
                     }

                     int success = c.getPlayer().getMap().spawnGuardian(c.getPlayer().getTeam(), num);
                     if (success != 1) {
                        switch (success) {
                           case -1:
                              c.sendPacket(CFieldMonsterCarnival.CPQMessage((byte) 3));
                              break;

                           case 0:
                              c.sendPacket(CFieldMonsterCarnival.CPQMessage((byte) 4));
                              break;

                           default:
                              c.sendPacket(CFieldMonsterCarnival.CPQMessage((byte) 3));
                        }
                        c.sendPacket(CWvsContext.enableActions());
                        return;
                     } else {
                        neededCP = skill.cpLoss;
                     }
                  }
               }
               c.getPlayer().gainCP(-neededCP);
               c.getPlayer().getMap().broadcastMessage(CFieldMonsterCarnival.playerSummoned(c.getPlayer().getName(), tab, num));
            } catch (Exception e) {
               e.printStackTrace();
            }
         } finally {
            c.releaseClient();
         }
      }
   }
}
