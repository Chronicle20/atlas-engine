package net.server.channel.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import buddy.BuddyProcessor;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.MapleFamily;
import client.MapleFamilyEntry;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.keybind.MapleKeyBinding;
import config.YamlConfig;
import connection.packets.CFuncKeyMappedMan;
import connection.packets.CLogin;
import connection.packets.CNpcTemplate;
import connection.packets.CParcelDlg;
import connection.packets.CStage;
import connection.packets.CWvsContext;
import constants.game.GameConstants;
import constants.game.ScriptableNPCConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.PlayerBuffValueHolder;
import net.server.Server;
import net.server.channel.Channel;
import net.server.coordinator.world.MapleEventRecallCoordinator;
import net.server.guild.MapleAlliance;
import net.server.guild.MapleGuild;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import net.server.world.PartyOperation;
import net.server.world.World;
import scripting.event.EventInstanceManager;
import server.life.MobSkill;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.Pair;
import tools.packets.Wedding;

public final class PlayerLoggedinHandler extends AbstractMaplePacketHandler {
   private final static Logger log = LoggerFactory.getLogger(PlayerLoggedinHandler.class);

   private static Set<Integer> attemptingLoginAccounts = new HashSet<>();

   private static void showDueyNotification(MapleClient c, MapleCharacter player) {
      Connection con = null;
      PreparedStatement ps = null;
      PreparedStatement pss = null;
      ResultSet rs = null;
      try {
         con = DatabaseConnection.getConnection();
         ps = con.prepareStatement("SELECT Type FROM dueypackages WHERE ReceiverId = ? AND Checked = 1 ORDER BY Type DESC");
         ps.setInt(1, player.getId());
         rs = ps.executeQuery();
         if (rs.next()) {
            try {
               Connection con2 = DatabaseConnection.getConnection();
               pss = con2.prepareStatement("UPDATE dueypackages SET Checked = 0 WHERE ReceiverId = ?");
               pss.setInt(1, player.getId());
               pss.executeUpdate();
               pss.close();
               con2.close();

               c.sendPacket(CParcelDlg.sendDueyParcelNotification(rs.getInt("Type") == 1));
            } catch (SQLException e) {
               e.printStackTrace();
            }
         }
      } catch (SQLException e) {
         e.printStackTrace();
      } finally {
         try {
            if (rs != null) {
               rs.close();
            }
            if (pss != null) {
               pss.close();
            }
            if (ps != null) {
               ps.close();
            }
            if (con != null) {
               con.close();
            }
         } catch (SQLException ex) {
            ex.printStackTrace();
         }
      }
   }

   private static List<Pair<Long, PlayerBuffValueHolder>> getLocalStartTimes(List<PlayerBuffValueHolder> lpbvl) {
      List<Pair<Long, PlayerBuffValueHolder>> timedBuffs = new ArrayList<>();
      long curtime = currentServerTime();

      for (PlayerBuffValueHolder pb : lpbvl) {
         timedBuffs.add(new Pair<>(curtime - pb.usedTime, pb));
      }

      timedBuffs.sort(Comparator.comparing(Pair::getLeft));

      return timedBuffs;
   }

   private static void onLoginUpdatePartyStatus(MapleClient client, MapleParty party) {
      MaplePartyCharacter pchar = client.getPlayer().getMPC();

      //Use this in case of enabling party HPbar HUD when logging in, however "you created a party" will appear on chat.
      //c.sendPacket(MaplePacketCreator.partyCreated(pchar));

      pchar.setChannel(client.getChannel());
      pchar.setMapId(client.getPlayer().getMapId());
      pchar.setOnline(true);
      client.getWorldServer().updateParty(party.getId(), PartyOperation.LOG_ONOFF, pchar);
      client.getPlayer().updatePartyMemberHP();
   }

   private boolean tryAcquireAccount(int accId) {
      synchronized (attemptingLoginAccounts) {
         if (attemptingLoginAccounts.contains(accId)) {
            return false;
         }

         attemptingLoginAccounts.add(accId);
         return true;
      }
   }

   private void releaseAccount(int accId) {
      synchronized (attemptingLoginAccounts) {
         attemptingLoginAccounts.remove(accId);
      }
   }

   @Override
   public boolean validateState(MapleClient c) {
      return !c.isLoggedIn();
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      final int cid = p.readInt();
      log.debug("PlayerLoggedInHandler cid=[{}].", cid);
      final Server server = Server.getInstance();

      if (c.tryacquireClient()) { // thanks MedicOP for assisting on concurrency protection here
         try {
            Optional<World> wserv = server.getWorld(c.getWorld());
            if (wserv.isEmpty()) {
               c.disconnect(true, false);
               return;
            }

            Optional<Channel> cserv = wserv.get().getChannel(c.getChannel());
            if (cserv.isEmpty()) {
               c.setChannel(1);
               cserv = wserv.get().getChannel(c.getChannel());

               if (cserv.isEmpty()) {
                  c.disconnect(true, false);
                  return;
               }
            }

            Optional<MapleCharacter> player = wserv.get().getPlayerStorage().getCharacterById(cid);
            //                IoSession session = c.getSession();

            //                String remoteHwid;
            //                if (player.isEmpty()) {
            //                    remoteHwid = MapleSessionCoordinator.getInstance().pickLoginSessionHwid(session);
            //                    if (remoteHwid == null) {
            //                        c.disconnect(true, false);
            //                        return;
            //                    }
            //                } else {
            //                    remoteHwid = player.get().getClient().getHWID();
            //                }
            //
            //                int hwidLen = remoteHwid.length();
            //                session.setAttribute(MapleClient.CLIENT_HWID, remoteHwid);
            //                session.setAttribute(MapleClient.CLIENT_NIBBLEHWID, remoteHwid.substring(hwidLen - 8, hwidLen));
            //                c.setHWID(remoteHwid);

            if (!server.validateCharacteridInTransition(c, cid)) {
               c.disconnect(true, false);
               return;
            }

            boolean newcomer = false;
            if (player.isEmpty()) {
               player = MapleCharacter.loadCharFromDB(cid, c, true);
               newcomer = true;

               if (player.isEmpty()) {
                  c.disconnect(true, false);
                  return;
               }
            }
            c.setPlayer(player.get());
            c.setAccID(player.get().getAccountID());

            boolean allowLogin = true;

            int accId = c.getAccID();
            if (tryAcquireAccount(accId)) { // Sync this to prevent wrong login state for double loggedin handling
               try {
                  int state = c.getLoginState();
                  if (state != MapleClient.LOGIN_SERVER_TRANSITION || !allowLogin) {
                     c.setPlayer(null);
                     c.setAccID(0);

                     if (state == MapleClient.LOGIN_LOGGEDIN) {
                        c.disconnect(true, false);
                     } else {
                        c.sendPacket(CLogin.getAfterLoginError(7));
                     }

                     return;
                  }
                  c.updateLoginState(MapleClient.LOGIN_LOGGEDIN);
               } finally {
                  releaseAccount(accId);
               }
            } else {
               c.setPlayer(null);
               c.setAccID(0);
               c.sendPacket(CLogin.getAfterLoginError(10));
               return;
            }

            if (!newcomer) {
               c.setLanguage(player.get().getClient().getLanguage());
               c.setCharacterSlots((byte) player.get().getClient().getCharacterSlots());
               player.get().newClient(c);
            }

            cserv.get().addPlayer(player.get());
            wserv.get().addPlayer(player.get());
            player.get().setEnteredChannelWorld();

            List<PlayerBuffValueHolder> buffs = server.getPlayerBuffStorage().getBuffsFromStorage(cid);
            if (buffs != null) {
               List<Pair<Long, PlayerBuffValueHolder>> timedBuffs = getLocalStartTimes(buffs);
               player.get().silentGiveBuffs(timedBuffs);
            }

            Map<MapleDisease, Pair<Long, MobSkill>> diseases = server.getPlayerBuffStorage().getDiseasesFromStorage(cid);
            if (diseases != null) {
               player.get().silentApplyDiseases(diseases);
            }

            c.sendPacket(CStage.getCharInfo(player.get()));
            if (!player.get().isHidden()) {
               if (player.get().isGM() && YamlConfig.config.server.USE_AUTOHIDE_GM) {
                  player.get().toggleHide(true);
               }
            }
            player.get().sendKeymap();
            player.get().sendQuickmap();
            player.get().sendMacros();

            // pot bindings being passed through other characters on the account detected thanks to Croosade dev team
            MapleKeyBinding autohpPot = player.get().getKeymap().get(91);
            player.get().sendPacket(CFuncKeyMappedMan.sendAutoHpPot(autohpPot != null ? autohpPot.action() : 0));

            MapleKeyBinding autompPot = player.get().getKeymap().get(92);
            player.get().sendPacket(CFuncKeyMappedMan.sendAutoMpPot(autompPot != null ? autompPot.action() : 0));

            player.get().getMap().addPlayer(player.get());
            player.get().visitMap(player.get().getMap());

            BuddyProcessor.getInstance().notifyLogon(player.get().getWorld(), player.get().getId(), c.getChannel());
            BuddyProcessor.getInstance().updateBuddyChannels(player.get());

            c.sendPacket(CWvsContext.loadFamily(player.get()));
            if (player.get().getFamilyId() > 0) {
               MapleFamily f = wserv.get().getFamily(player.get().getFamilyId());
               if (f != null) {
                  MapleFamilyEntry familyEntry = f.getEntryByID(player.get().getId());
                  if (familyEntry != null) {
                     familyEntry.setCharacter(player.get());
                     player.get().setFamilyEntry(familyEntry);

                     c.sendPacket(CWvsContext.getFamilyInfo(familyEntry));
                     familyEntry.announceToSenior(CWvsContext.sendFamilyLoginNotice(player.get().getName(), true), true);
                  } else {
                     FilePrinter.printError(FilePrinter.FAMILY_ERROR,
                           "Player " + player.get().getName() + "'s family doesn't have an entry for them. (" + f.getID() + ")");
                  }
               } else {
                  FilePrinter.printError(FilePrinter.FAMILY_ERROR,
                        "Player " + player.get().getName() + " has an invalid family ID. (" + player.get().getFamilyId() + ")");
                  c.sendPacket(CWvsContext.getFamilyInfo(null));
               }
            } else {
               c.sendPacket(CWvsContext.getFamilyInfo(null));
            }
            if (player.get().getGuildId() > 0) {
               Optional<MapleGuild> playerGuild = server.getGuild(player.get().getGuildId(), player.get().getWorld(), player.get());
               if (playerGuild.isEmpty()) {
                  player.get().deleteGuild(player.get().getGuildId());
                  player.get().getMGC().ifPresent(mgc -> mgc.setGuildId(0));
                  player.get().getMGC().ifPresent(mgc -> mgc.setGuildRank(5));
               } else {
                  MapleCharacter finalPlayer = player.get();
                  playerGuild.flatMap(g -> g.getMGC(finalPlayer.getId())).ifPresent(mgc -> mgc.setCharacter(finalPlayer));
                  player.get().setMGC(playerGuild.flatMap(g -> g.getMGC(finalPlayer.getId())).orElseThrow());
                  server.setGuildMemberOnline(player.get(), true, c.getChannel());
                  c.sendPacket(CWvsContext.showGuildInfo(player.get()));
                  int allianceId = player.get().getGuild().map(MapleGuild::getAllianceId).orElse(0);
                  if (allianceId > 0) {
                     Optional<MapleAlliance> newAlliance = server.getAlliance(allianceId);
                     if (newAlliance.isEmpty()) {
                        newAlliance = MapleAlliance.loadAlliance(allianceId);
                        if (newAlliance.isPresent()) {
                           server.addAlliance(allianceId, newAlliance.get());
                        } else {
                           player.get().getGuild().ifPresent(g -> g.setAllianceId(0));
                        }
                     }
                     if (newAlliance.isPresent()) {
                        c.sendPacket(CWvsContext.updateAllianceInfo(newAlliance.get(), c.getWorld()));
                        c.sendPacket(CWvsContext.allianceNotice(newAlliance.get().getId(), newAlliance.get().getNotice()));

                        if (newcomer) {
                           server.allianceMessage(allianceId, CWvsContext.allianceMemberOnline(player.get(), true),
                                 player.get().getId(), -1);
                        }
                     }
                  }
               }
            }

            player.get().showNote();
            player.get().getParty().ifPresent(party -> onLoginUpdatePartyStatus(c, party));

            MapleInventory eqpInv = player.get().getInventory(MapleInventoryType.EQUIPPED);
            eqpInv.lockInventory();
            try {
               for (Item it : eqpInv.list()) {
                  player.get().equippedItem((Equip) it);
               }
            } finally {
               eqpInv.unlockInventory();
            }

            BuddyProcessor.getInstance().refreshBuddies(player.get());
            BuddyProcessor.getInstance().nextPendingRequest(player.get());

            //                c.sendPacket(CWvsContext.updateGender(player.get()));
            player.get().checkMessenger();
            c.sendPacket(CWvsContext.enableReport());
            player.get()
                  .changeSkillLevel(10000000 * player.get().getJobType() + 12, (byte) (player.get().getLinkedLevel() / 10), 20, -1);
            player.get().checkBerserk(player.get().isHidden());

            if (newcomer) {
               for (MaplePet pet : player.get().getPets()) {
                  if (pet != null) {
                     wserv.get().registerPetHunger(player.get(), player.get().getPetIndex(pet));
                  }
               }

               MapleCharacter finalPlayer = player.get();
               player.get().getMount().filter(m -> m.getItemId() != 0)
                     .ifPresent(m -> finalPlayer.sendPacket(CWvsContext.updateMount(finalPlayer.getId(), m, false)));
               player.get().reloadQuestExpirations();

               if (player.get().isGM()) {
                  Server.getInstance().broadcastGMMessage(c.getWorld(), CWvsContext.earnTitleMessage(
                        (player.get().gmLevel() < 6 ? "GM " : "Admin ") + player.get().getName() + " has logged in"));
               }

               if (diseases != null) {
                  for (Entry<MapleDisease, Pair<Long, MobSkill>> e : diseases.entrySet()) {
                     final List<Pair<MapleDisease, Integer>> debuff =
                           Collections.singletonList(new Pair<>(e.getKey(), e.getValue().getRight().getX()));
                     c.sendPacket(CWvsContext.giveDebuff(debuff, e.getValue().getRight()));
                  }
               }
            } else {
               if (player.get().isRidingBattleship()) {
                  player.get().announceBattleshipHp();
               }
            }

            player.get().buffExpireTask();
            player.get().diseaseExpireTask();
            player.get().skillCooldownTask();
            player.get().expirationTask();
            player.get().questExpirationTask();
            if (GameConstants.hasSPTable(player.get().getJob()) && player.get().getJob().getId() != 2001) {
               player.get().createDragon();
            }

            player.get().commitExcludedItems();
            showDueyNotification(c, player.get());

            if (player.get().getMap().getHPDec() > 0) {
               player.get().resetHpDecreaseTask();
            }

            player.get().resetPlayerRates();
            if (YamlConfig.config.server.USE_ADD_RATES_BY_LEVEL == true) {
               player.get().setPlayerRates();
            }
            player.get().setWorldRates();
            player.get().updateCouponRates();

            player.get().receivePartyMemberHP();

            if (player.get().getPartnerId() > 0) {
               int partnerId = player.get().getPartnerId();
               final MapleCharacter finalPlayer = player.get();
               wserv.map(World::getPlayerStorage).flatMap(s -> s.getCharacterById(partnerId))
                     .filter(partner -> !partner.isAwayFromWorld()).ifPresent(partner -> {
                        finalPlayer.sendPacket(Wedding.OnNotifyWeddingPartnerTransfer(partnerId, partner.getMapId()));
                        partner.sendPacket(Wedding.OnNotifyWeddingPartnerTransfer(finalPlayer.getId(), finalPlayer.getMapId()));
                     });
            }

            if (newcomer) {
               EventInstanceManager eim = MapleEventRecallCoordinator.getInstance().recallEventInstance(cid);
               if (eim != null) {
                  eim.registerPlayer(player.get());
               }
            }

            if (YamlConfig.config.server.USE_NPCS_SCRIPTABLE) {
               c.sendPacket(CNpcTemplate.setNPCScriptable(ScriptableNPCConstants.SCRIPTABLE_NPCS));
            }

            if (newcomer) {
               player.get().setLoginTime(System.currentTimeMillis());
            }
         } catch (Exception e) {
            e.printStackTrace();
         } finally {
            c.releaseClient();
         }
      } else {
         c.sendPacket(CLogin.getAfterLoginError(10));
      }
   }
}
