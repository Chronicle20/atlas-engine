package net.server.channel.handlers;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import client.inventory.manipulator.MapleKarmaManipulator;
import config.YamlConfig;
import connection.packets.CWvsContext;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.MapleMarriage;
import tools.packets.Wedding;

public final class WeddingHandler extends AbstractMaplePacketHandler {
   private static final Logger log = LoggerFactory.getLogger(WeddingHandler.class);

   @Override
   public void handlePacket(InPacket p, MapleClient c) {

      if (c.tryacquireClient()) {
         try {
            MapleCharacter chr = c.getPlayer();
            final byte mode = p.readByte();

            if (mode == 6) { //additem
               short slot = p.readShort();
               int itemid = p.readInt();
               short quantity = p.readShort();

               MapleMarriage marriage = c.getPlayer().getMarriageInstance().orElse(null);
               if (marriage != null) {
                  try {
                     boolean groomWishlist = marriage.giftItemToSpouse(chr.getId());
                     String groomWishlistProp = "giftedItem" + (groomWishlist ? "G" : "B") + chr.getId();

                     int giftCount = marriage.getIntProperty(groomWishlistProp);
                     if (giftCount < YamlConfig.config.server.WEDDING_GIFT_LIMIT) {
                        int cid = marriage.getIntProperty(groomWishlist ? "groomId" : "brideId");
                        if (chr.getId() != cid) {   // cannot gift yourself
                           MapleCharacter spouse = marriage.getPlayerById(cid);
                           if (spouse != null) {
                              MapleInventoryType type = ItemConstants.getInventoryType(itemid);
                              MapleInventory chrInv = chr.getInventory(type);

                              Item newItem = null;
                              chrInv.lockInventory();
                              try {
                                 Item item = chrInv.getItem((byte) slot);
                                 if (item != null) {
                                    if (!item.isUntradeable()) {
                                       if (itemid == item.getItemId() && quantity <= item.getQuantity()) {
                                          newItem = item.copy();

                                          marriage.addGiftItem(groomWishlist, newItem);
                                          MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false, false);

                                          MapleKarmaManipulator.toggleKarmaFlagToUntradeable(newItem);
                                          marriage.setIntProperty(groomWishlistProp, giftCount + 1);

                                          c.sendPacket(
                                                Wedding.OnWeddingGiftResult((byte) 0xB, marriage.getWishlistItems(groomWishlist),
                                                      Collections.singletonList(newItem)));
                                       }
                                    } else {
                                       c.sendPacket(
                                             Wedding.OnWeddingGiftResult((byte) 0xE, marriage.getWishlistItems(groomWishlist),
                                                   null));
                                    }
                                 }
                              } finally {
                                 chrInv.unlockInventory();
                              }

                              if (newItem != null) {
                                 if (YamlConfig.config.server.USE_ENFORCE_MERCHANT_SAVE) {
                                    chr.saveCharToDB(false);
                                 }
                                 marriage.saveGiftItemsToDb(c, groomWishlist, cid);
                              }
                           } else {
                              c.sendPacket(Wedding.OnWeddingGiftResult((byte) 0xE, marriage.getWishlistItems(groomWishlist), null));
                           }
                        } else {
                           c.sendPacket(Wedding.OnWeddingGiftResult((byte) 0xE, marriage.getWishlistItems(groomWishlist), null));
                        }
                     } else {
                        c.sendPacket(Wedding.OnWeddingGiftResult((byte) 0xC, marriage.getWishlistItems(groomWishlist), null));
                     }
                  } catch (NumberFormatException nfe) {
                  }
               } else {
                  c.sendPacket(CWvsContext.enableActions());
               }
            } else if (mode == 7) { // take items
               p.readByte();    // invType
               int itemPos = p.readByte();

               MapleMarriage marriage = chr.getMarriageInstance().orElse(null);
               if (marriage != null) {
                  Boolean groomWishlist = marriage.isMarriageGroom(chr);
                  if (groomWishlist != null) {
                     Item item = marriage.getGiftItem(c, groomWishlist, itemPos);
                     if (item != null) {
                        if (MapleInventory.checkSpot(chr, item)) {
                           marriage.removeGiftItem(groomWishlist, item);
                           marriage.saveGiftItemsToDb(c, groomWishlist, chr.getId());

                           MapleInventoryManipulator.addFromDrop(c, item, true);

                           c.sendPacket(Wedding.OnWeddingGiftResult((byte) 0xF, marriage.getWishlistItems(groomWishlist),
                                 marriage.getGiftItems(c, groomWishlist)));
                        } else {
                           c.getPlayer().dropMessage(1, "Free a slot on your inventory before collecting this item.");
                           c.sendPacket(Wedding.OnWeddingGiftResult((byte) 0xE, marriage.getWishlistItems(groomWishlist),
                                 marriage.getGiftItems(c, groomWishlist)));
                        }
                     } else {
                        c.getPlayer().dropMessage(1, "You have already collected this item.");
                        c.sendPacket(Wedding.OnWeddingGiftResult((byte) 0xE, marriage.getWishlistItems(groomWishlist),
                              marriage.getGiftItems(c, groomWishlist)));
                     }
                  }
               } else {
                  List<Item> items = c.getAbstractPlayerInteraction().getUnclaimedMarriageGifts();
                  try {
                     Item item = items.get(itemPos);
                     if (MapleInventory.checkSpot(chr, item)) {
                        items.remove(itemPos);
                        MapleMarriage.saveGiftItemsToDb(c, items, chr.getId());

                        MapleInventoryManipulator.addFromDrop(c, item, true);
                        c.sendPacket(Wedding.OnWeddingGiftResult((byte) 0xF, Collections.singletonList(""), items));
                     } else {
                        c.getPlayer().dropMessage(1, "Free a slot on your inventory before collecting this item.");
                        c.sendPacket(Wedding.OnWeddingGiftResult((byte) 0xE, Collections.singletonList(""), items));
                     }
                  } catch (Exception e) {
                     c.getPlayer().dropMessage(1, "You have already collected this item.");
                     c.sendPacket(Wedding.OnWeddingGiftResult((byte) 0xE, Collections.singletonList(""), items));
                  }
               }
            } else if (mode == 8) { // out of Wedding Registry
               c.sendPacket(CWvsContext.enableActions());
            } else {
               log.debug("Unhandled mode {}.", mode);
            }
         } finally {
            c.releaseClient();
         }
      }
   }
}