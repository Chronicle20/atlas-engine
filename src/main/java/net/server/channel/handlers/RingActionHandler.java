package net.server.channel.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleRing;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import client.processor.npc.DueyProcessor;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.channel.Channel;
import net.server.world.World;
import scripting.event.EventInstanceManager;
import server.ItemInformationProvider;
import tools.DatabaseConnection;
import tools.Pair;
import tools.packets.Wedding;

public final class RingActionHandler extends AbstractMaplePacketHandler {
   private static final Logger log = LoggerFactory.getLogger(RingActionHandler.class);

   private static int getBoxId(int useItemId) {
      return useItemId == 2240000 ? 4031357 : (useItemId == 2240001 ? 4031359 :
            (useItemId == 2240002 ? 4031361 : (useItemId == 2240003 ? 4031363 : (1112300 + (useItemId - 2240004)))));
   }

   public static void sendEngageProposal(final MapleClient c, final String name, final int itemid) {
      final int newBoxId = getBoxId(itemid);
      final MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(name).orElse(null);
      final MapleCharacter source = c.getPlayer();

      // TODO: get the correct packet bytes for these popups
      if (source.isMarried()) {
         source.dropMessage(1, "You're already married!");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (source.getPartnerId() > 0) {
         source.dropMessage(1, "You're already engaged!");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (source.getMarriageItemId() > 0) {
         source.dropMessage(1, "You're already engaging someone!");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (target == null) {
         source.dropMessage(1, "Unable to find " + name + " on this channel.");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (target == source) {
         source.dropMessage(1, "You can't engage yourself.");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (target.getLevel() < 50) {
         source.dropMessage(1, "You can only propose to someone level 50 or higher.");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (source.getLevel() < 50) {
         source.dropMessage(1, "You can only propose being level 50 or higher.");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (!target.getMap().equals(source.getMap())) {
         source.dropMessage(1, "Make sure your partner is on the same map!");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (!source.haveItem(itemid) || itemid < 2240000 || itemid > 2240015) {
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (target.isMarried()) {
         source.dropMessage(1, "The player is already married!");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (target.getPartnerId() > 0 || target.getMarriageItemId() > 0) {
         source.dropMessage(1, "The player is already engaged!");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (target.haveWeddingRing()) {
         source.dropMessage(1, "The player already holds a marriage ring...");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (source.haveWeddingRing()) {
         source.dropMessage(1, "You can't propose while holding a marriage ring!");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (target.getGender() == source.getGender()) {
         source.dropMessage(1, "You may only propose to a " + (source.getGender() == 1 ? "male" : "female") + "!");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (!MapleInventoryManipulator.checkSpace(c, newBoxId, 1, "")) {
         source.dropMessage(5, "You don't have a ETC slot available right now!");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      } else if (!MapleInventoryManipulator.checkSpace(target.getClient(), newBoxId + 1, 1, "")) {
         source.dropMessage(5, "The girl you proposed doesn't have a ETC slot available right now.");
         source.sendPacket(Wedding.OnMarriageResult((byte) 0));
         return;
      }

      source.setMarriageItemId(itemid);
      target.sendPacket(Wedding.OnMarriageRequest(source.getName(), source.getId()));
   }

   private static void eraseEngagementOffline(int characterId) {
      try {
         Connection con = DatabaseConnection.getConnection();
         eraseEngagementOffline(characterId, con);
         con.close();
      } catch (SQLException sqle) {
         sqle.printStackTrace();
      }
   }

   private static void eraseEngagementOffline(int characterId, Connection con) throws SQLException {
      PreparedStatement ps = con.prepareStatement("UPDATE characters SET marriageItemId=-1, partnerId=-1 WHERE id=?");
      ps.setInt(1, characterId);
      ps.executeUpdate();

      ps.close();
   }

   private static void breakEngagementOffline(int characterId) {
      try {
         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT marriageItemId FROM characters WHERE id=?");
         ps.setInt(1, characterId);
         ResultSet rs = ps.executeQuery();
         if (rs.next()) {
            int marriageItemId = rs.getInt("marriageItemId");

            if (marriageItemId > 0) {
               PreparedStatement ps2 =
                     con.prepareStatement("UPDATE inventoryitems SET expiration=0 WHERE itemid=? AND characterid=?");
               ps2.setInt(1, marriageItemId);
               ps2.setInt(2, characterId);

               ps2.executeUpdate();
               ps2.close();
            }
         }
         rs.close();
         ps.close();

         eraseEngagementOffline(characterId, con);

         con.close();
      } catch (SQLException ex) {
         log.error("Error updating offline breakup {}", ex.getMessage());
      }
   }

   private synchronized static void breakMarriage(MapleCharacter chr) {
      int partnerid = chr.getPartnerId();
      if (partnerid <= 0) {
         return;
      }

      chr.getClient().getWorldServer().deleteRelationship(chr.getId(), partnerid);
      chr.getMarriageRing().ifPresent(MapleRing::removeRing);

      MapleCharacter partner = chr.getClient().getWorldServer().getPlayerStorage().getCharacterById(partnerid).orElse(null);
      if (partner == null) {
         eraseEngagementOffline(partnerid);
      } else {
         partner.dropMessage(5, chr.getName() + " has decided to break up the marriage.");

         //partner.sendPacket(Wedding.OnMarriageResult((byte) 0)); ok, how to gracefully break engagement with someone without the need to cc?
         partner.sendPacket(Wedding.OnNotifyWeddingPartnerTransfer(0, 0));
         resetRingId(partner);
         partner.setPartnerId(-1);
         partner.setMarriageItemId(-1);
         partner.addMarriageRing(null);
      }

      String spouse = MapleCharacter.getNameById(partnerid).orElseThrow();
      chr.dropMessage(5, "You have successfully ended the marriage with " + spouse + ".");

      //chr.sendPacket(Wedding.OnMarriageResult((byte) 0));
      chr.sendPacket(Wedding.OnNotifyWeddingPartnerTransfer(0, 0));
      resetRingId(chr);
      chr.setPartnerId(-1);
      chr.setMarriageItemId(-1);
      chr.addMarriageRing(null);
   }

   private static void resetRingId(MapleCharacter player) {
      int ringitemid = player.getMarriageRing().map(MapleRing::getItemId).orElse(-1);
      player.getInventory(MapleInventoryType.EQUIP).findById(ringitemid)
            .or(() -> player.getInventory(MapleInventoryType.EQUIPPED).findById(ringitemid)).map(i -> (Equip) i)
            .ifPresent(e -> e.setRingId(-1));
   }

   private synchronized static void breakEngagement(MapleCharacter chr) {
      int partnerid = chr.getPartnerId();
      int marriageitemid = chr.getMarriageItemId();

      chr.getClient().getWorldServer().deleteRelationship(chr.getId(), partnerid);

      MapleCharacter partner = chr.getClient().getWorldServer().getPlayerStorage().getCharacterById(partnerid).orElse(null);
      if (partner == null) {
         breakEngagementOffline(partnerid);
      } else {
         partner.dropMessage(5, chr.getName() + " has decided to break up the engagement.");

         int partnerMarriageitemid = marriageitemid + ((chr.getGender() == 0) ? 1 : -1);
         if (partner.haveItem(partnerMarriageitemid)) {
            MapleInventoryManipulator.removeById(partner.getClient(), MapleInventoryType.ETC, partnerMarriageitemid, (short) 1,
                  false, false);
         }

         //partner.sendPacket(Wedding.OnMarriageResult((byte) 0)); ok, how to gracefully unengage someone without the need to cc?
         partner.sendPacket(Wedding.OnNotifyWeddingPartnerTransfer(0, 0));
         partner.setPartnerId(-1);
         partner.setMarriageItemId(-1);
      }

      if (chr.haveItem(marriageitemid)) {
         MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.ETC, marriageitemid, (short) 1, false, false);
      }
      String spouse = MapleCharacter.getNameById(partnerid).orElseThrow();
      chr.dropMessage(5, "You have successfully ended the engagement with " + spouse + ".");

      //chr.sendPacket(Wedding.OnMarriageResult((byte) 0));
      chr.sendPacket(Wedding.OnNotifyWeddingPartnerTransfer(0, 0));
      chr.setPartnerId(-1);
      chr.setMarriageItemId(-1);
   }

   public static void breakMarriageRing(MapleCharacter chr, final int wItemId) {
      final Optional<MapleInventoryType> type = MapleInventoryType.getByType((byte) (wItemId / 1000000));
      if (type.isEmpty()) {
         return;
      }

      final Optional<Item> wItem = chr.getInventory(type.get()).findById(wItemId);
      final boolean weddingToken = (wItem.isPresent() && type.get() == MapleInventoryType.ETC && wItemId / 10000 == 403);
      final boolean weddingRing = (wItem.isPresent() && wItemId / 10 == 111280);

      if (weddingRing) {
         if (chr.getPartnerId() > 0) {
            breakMarriage(chr);
         }

         chr.getMap().disappearingItemDrop(chr, chr, wItem.get(), chr.getPosition());
      } else if (weddingToken) {
         if (chr.getPartnerId() > 0) {
            breakEngagement(chr);
         }

         chr.getMap().disappearingItemDrop(chr, chr, wItem.get(), chr.getPosition());
      }
   }

   public static void giveMarriageRings(MapleCharacter player, MapleCharacter partner, int marriageRingId) {
      Pair<Integer, Integer> rings = MapleRing.createRing(marriageRingId, player, partner);
      ItemInformationProvider ii = ItemInformationProvider.getInstance();

      Item ringObj = ii.getEquipById(marriageRingId);
      Equip ringEqp = (Equip) ringObj;
      ringEqp.setRingId(rings.getLeft());
      player.addMarriageRing(MapleRing.loadFromDb(rings.getLeft()).orElseThrow());
      MapleInventoryManipulator.addFromDrop(player.getClient(), ringEqp, false, -1);
      player.broadcastMarriageMessage();

      ringObj = ii.getEquipById(marriageRingId);
      ringEqp = (Equip) ringObj;
      ringEqp.setRingId(rings.getRight());
      partner.addMarriageRing(MapleRing.loadFromDb(rings.getRight()).orElseThrow());
      MapleInventoryManipulator.addFromDrop(partner.getClient(), ringEqp, false, -1);
      partner.broadcastMarriageMessage();
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      byte mode = p.readByte();
      String name;
      byte slot;
      switch (mode) {
         case 0: // Send Proposal
            sendEngageProposal(c, p.readString(), p.readInt());
            break;

         case 1: // Cancel Proposal
            if (c.getPlayer().getMarriageItemId() / 1000000 != 4) {
               c.getPlayer().setMarriageItemId(-1);
            }
            break;

         case 2: // Accept/Deny Proposal
            final boolean accepted = p.readByte() > 0;
            name = p.readString();
            final int id = p.readInt();

            final MapleCharacter source = c.getWorldServer().getPlayerStorage().getCharacterByName(name).orElse(null);
            final MapleCharacter target = c.getPlayer();

            if (source == null) {
               target.sendPacket(CWvsContext.enableActions());
               return;
            }

            final int itemid = source.getMarriageItemId();
            if (target.getPartnerId() > 0 || source.getId() != id || itemid <= 0 || !source.haveItem(itemid)
                  || source.getPartnerId() > 0 || !source.isAlive() || !target.isAlive()) {
               target.sendPacket(CWvsContext.enableActions());
               return;
            }

            if (accepted) {
               final int newItemId = getBoxId(itemid);
               if (!MapleInventoryManipulator.checkSpace(c, newItemId, 1, "") || !MapleInventoryManipulator.checkSpace(
                     source.getClient(), newItemId, 1, "")) {
                  target.sendPacket(CWvsContext.enableActions());
                  return;
               }

               try {
                  MapleInventoryManipulator.removeById(source.getClient(), MapleInventoryType.USE, itemid, 1, false, false);

                  int marriageId = c.getWorldServer().createRelationship(source.getId(), target.getId());
                  source.setPartnerId(target.getId()); // engage them (new marriageitemid, partnerid for both)
                  target.setPartnerId(source.getId());

                  source.setMarriageItemId(newItemId);
                  target.setMarriageItemId(newItemId + 1);

                  MapleInventoryManipulator.addById(source.getClient(), newItemId, (short) 1);
                  MapleInventoryManipulator.addById(c, (newItemId + 1), (short) 1);

                  source.sendPacket(Wedding.OnMarriageResult(marriageId, source, false));
                  target.sendPacket(Wedding.OnMarriageResult(marriageId, source, false));

                  source.sendPacket(Wedding.OnNotifyWeddingPartnerTransfer(target.getId(), target.getMapId()));
                  target.sendPacket(Wedding.OnNotifyWeddingPartnerTransfer(source.getId(), source.getMapId()));
               } catch (Exception e) {
                  log.error("Error with engagement {}", e.getMessage());
               }
            } else {
               source.dropMessage(1, "She has politely declined your engagement request.");
               source.sendPacket(Wedding.OnMarriageResult((byte) 0));

               source.setMarriageItemId(-1);
            }
            break;

         case 3: // Break Engagement
            breakMarriageRing(c.getPlayer(), p.readInt());
            break;

         case 5: // Invite %s to Wedding
            name = p.readString();
            int marriageId = p.readInt();
            slot = p.readByte(); // this is an int

            int itemId;
            try {
               itemId = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem(slot).getItemId();
            } catch (NullPointerException npe) {
               c.sendPacket(CWvsContext.enableActions());
               return;
            }

            if ((itemId != 4031377 && itemId != 4031395) || !c.getPlayer().haveItem(itemId)) {
               c.sendPacket(CWvsContext.enableActions());
               return;
            }

            String groom = c.getPlayer().getName();
            String bride = MapleCharacter.getNameById(c.getPlayer().getPartnerId()).orElseThrow();
            int guest = MapleCharacter.getIdByName(name);
            if (bride.isEmpty() || guest <= 0) {
               c.getPlayer().dropMessage(5, "Unable to find " + name + "!");
               return;
            }

            World wserv = c.getWorldServer();
            Pair<Boolean, Boolean> registration = wserv.getMarriageQueuedLocation(marriageId);

            if (registration != null) {
               if (wserv.addMarriageGuest(marriageId, guest)) {
                  boolean cathedral = registration.getLeft();
                  int newItemId = cathedral ? 4031407 : 4031406;

                  Channel cserv = c.getChannelServer();
                  int resStatus = cserv.getWeddingReservationStatus(marriageId, cathedral);
                  if (resStatus > 0) {
                     long expiration = cserv.getWeddingTicketExpireTime(resStatus + 1);

                     MapleCharacter guestChr = c.getWorldServer().getPlayerStorage().getCharacterById(guest).orElse(null);
                     if (guestChr != null && MapleInventoryManipulator.checkSpace(guestChr.getClient(), newItemId, 1, "")
                           && MapleInventoryManipulator.addById(guestChr.getClient(), newItemId, (short) 1, expiration)) {
                        guestChr.dropMessage(6, "[Wedding] You've been invited to " + groom + " and " + bride + "'s Wedding!");
                     } else {
                        if (guestChr != null && guestChr.isLoggedinWorld()) {
                           guestChr.dropMessage(6, "[Wedding] You've been invited to " + groom + " and " + bride
                                 + "'s Wedding! Receive your invitation from Duey!");
                        } else {
                           c.getPlayer().sendNote(name, "You've been invited to " + groom + " and " + bride
                                 + "'s Wedding! Receive your invitation from Duey!", (byte) 0);
                        }

                        Item weddingTicket = new Item(newItemId, (short) 0, (short) 1);
                        weddingTicket.setExpiration(expiration);

                        DueyProcessor.dueyCreatePackage(weddingTicket, 0, groom, guest);
                     }
                  } else {
                     c.getPlayer().dropMessage(5, "Wedding is already under way. You cannot invite any more guests for the event.");
                  }
               } else {
                  c.getPlayer().dropMessage(5, "'" + name + "' is already invited for your marriage.");
               }
            } else {
               c.getPlayer().dropMessage(5, "Invitation was not sent to '" + name
                     + "'. Either the time for your marriage reservation already came or it was not found.");
            }

            c.getAbstractPlayerInteraction().gainItem(itemId, (short) -1);
            break;

         case 6: // Open Wedding Invitation
            slot = (byte) p.readInt();
            int invitationid = p.readInt();

            if (invitationid == 4031406 || invitationid == 4031407) {
               Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem(slot);
               if (item == null || item.getItemId() != invitationid) {
                  c.sendPacket(CWvsContext.enableActions());
                  return;
               }

               // collision case: most soon-to-come wedding will show up
               Pair<Integer, Integer> coupleId =
                     c.getWorldServer().getWeddingCoupleForGuest(c.getPlayer().getId(), invitationid == 4031407);
               if (coupleId != null) {
                  int groomId = coupleId.getLeft(), brideId = coupleId.getRight();
                  String spouse1 = MapleCharacter.getNameById(groomId).orElseThrow();
                  String spouse2 = MapleCharacter.getNameById(brideId).orElseThrow();
                  c.sendPacket(Wedding.sendWeddingInvitation(spouse1, spouse2));
               }
            }

            break;

         case 9:
            try {
               // By -- Dragoso (Drago)
               // Groom and Bride's Wishlist

               MapleCharacter player = c.getPlayer();

               EventInstanceManager eim = player.getEventInstance().orElse(null);
               if (eim != null) {
                  boolean isMarrying =
                        (player.getId() == eim.getIntProperty("groomId") || player.getId() == eim.getIntProperty("brideId"));

                  if (isMarrying) {
                     int amount = p.readShort();
                     if (amount > 10) {
                        amount = 10;
                     }

                     StringBuilder wishlistItems = new StringBuilder();
                     for (int i = 0; i < amount; i++) {
                        wishlistItems.append(p.readString()).append("\r\n");
                     }

                     String wlKey;
                     if (player.getId() == eim.getIntProperty("groomId")) {
                        wlKey = "groomWishlist";
                     } else {
                        wlKey = "brideWishlist";
                     }

                     if (eim.getProperty(wlKey).contentEquals("")) {
                        eim.setProperty(wlKey, wishlistItems.toString());
                     }
                  }
               }
            } catch (NumberFormatException nfe) {
            }

            break;

         default:
            log.debug("Unhandled RING_ACTION Mode: {}", p);
            break;
      }

      c.sendPacket(CWvsContext.enableActions());
   }
}
