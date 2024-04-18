package net.server.channel.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.manipulator.MapleInventoryManipulator;
import connection.packets.CCashShop;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import server.CashShop;
import server.ItemInformationProvider;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.Pair;

public final class CouponCodeHandler extends AbstractMaplePacketHandler {

   private static List<Pair<Integer, Pair<Integer, Integer>>> getNXCodeItems(MapleCharacter chr, Connection con, int codeid) throws
         SQLException {
      Map<Integer, Integer> couponItems = new HashMap<>();
      Map<Integer, Integer> couponPoints = new HashMap<>(5);

      PreparedStatement ps = con.prepareStatement("SELECT * FROM nxcode_items WHERE codeid = ?");
      ps.setInt(1, codeid);

      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
         int type = rs.getInt("type"), quantity = rs.getInt("quantity");
         if (type < 5) {
            couponPoints.merge(type, quantity, Integer::sum);
         } else {
            int item = rs.getInt("item");
            couponItems.merge(item, quantity, Integer::sum);
         }
      }

      rs.close();
      ps.close();

      List<Pair<Integer, Pair<Integer, Integer>>> ret = new LinkedList<>();
      if (!couponItems.isEmpty()) {
         for (Entry<Integer, Integer> e : couponItems.entrySet()) {
            int item = e.getKey(), qty = e.getValue();

            if (ItemInformationProvider.getInstance().getName(item) == null) {
               item = 4000000;
               qty = 1;

               FilePrinter.printError(FilePrinter.UNHANDLED_EVENT,
                     "Error trying to redeem itemid " + item + " from codeid " + codeid + ".");
            }

            if (!chr.canHold(item, qty)) {
               return null;
            }

            ret.add(new Pair<>(5, new Pair<>(item, qty)));
         }
      }

      if (!couponPoints.isEmpty()) {
         for (Entry<Integer, Integer> e : couponPoints.entrySet()) {
            ret.add(new Pair<>(e.getKey(), new Pair<>(777, e.getValue())));
         }
      }

      return ret;
   }

   private static Pair<Integer, List<Pair<Integer, Pair<Integer, Integer>>>> getNXCodeResult(MapleCharacter chr, String code) {
      MapleClient c = chr.getClient();
      List<Pair<Integer, Pair<Integer, Integer>>> ret = new LinkedList<>();
      try {
         if (!c.attemptCsCoupon()) {
            return new Pair<>(-5, null);
         }

         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM nxcode WHERE code = ?");
         ps.setString(1, code);

         ResultSet rs = ps.executeQuery();
         if (!rs.next()) {
            return new Pair<>(-1, null);
         }

         if (rs.getString("retriever") != null) {
            return new Pair<>(-2, null);
         }

         if (rs.getLong("expiration") < Server.getInstance().getCurrentTime()) {
            return new Pair<>(-3, null);
         }

         int codeid = rs.getInt("id");
         rs.close();
         ps.close();

         ret = getNXCodeItems(chr, con, codeid);
         if (ret == null) {
            return new Pair<>(-4, null);
         }

         ps = con.prepareStatement("UPDATE nxcode SET retriever = ? WHERE code = ?");
         ps.setString(1, chr.getName());
         ps.setString(2, code);
         ps.executeUpdate();

         ps.close();
         con.close();
      } catch (SQLException ex) {
         ex.printStackTrace();
      }

      c.resetCsCoupon();
      return new Pair<>(0, ret);
   }

   private static int parseCouponResult(int res) {
      return switch (res) {
         case -1 -> 0xB0;
         case -2 -> 0xB3;
         case -3 -> 0xB2;
         case -4 -> 0xBB;
         default -> 0xB1;
      };
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      p.skip(2);
      String code = p.readString();

      if (c.tryacquireClient()) {
         try {
            Pair<Integer, List<Pair<Integer, Pair<Integer, Integer>>>> codeRes = getNXCodeResult(c.getPlayer(), code.toUpperCase());
            int type = codeRes.getLeft();
            if (type < 0) {
               c.sendPacket(CCashShop.showCashShopMessage((byte) parseCouponResult(type)));
            } else {
               List<Item> cashItems = new LinkedList<>();
               List<Pair<Integer, Integer>> items = new LinkedList<>();
               int nxCredit = 0;
               int maplePoints = 0;
               int nxPrepaid = 0;
               int mesos = 0;

               for (Pair<Integer, Pair<Integer, Integer>> ps : codeRes.getRight()) {
                  type = ps.getLeft();
                  int quantity = ps.getRight().getRight();

                  CashShop cs = c.getPlayer().getCashShop();
                  switch (type) {
                     case 0:
                        c.getPlayer().gainMeso(quantity, false); //mesos
                        mesos += quantity;
                        break;
                     case 4:
                        cs.gainCash(1, quantity);    //nxCredit
                        nxCredit += quantity;
                        break;
                     case 1:
                        cs.gainCash(2, quantity);    //maplePoint
                        maplePoints += quantity;
                        break;
                     case 2:
                        cs.gainCash(4, quantity);    //nxPrepaid
                        nxPrepaid += quantity;
                        break;
                     case 3:
                        cs.gainCash(1, quantity);
                        nxCredit += quantity;
                        cs.gainCash(4, (quantity / 5000));
                        nxPrepaid += quantity / 5000;
                        break;

                     default:
                        int item = ps.getRight().getLeft();

                        short qty;
                        if (quantity > Short.MAX_VALUE) {
                           qty = Short.MAX_VALUE;
                        } else if (quantity < Short.MIN_VALUE) {
                           qty = Short.MIN_VALUE;
                        } else {
                           qty = (short) quantity;
                        }

                        if (ItemInformationProvider.getInstance().isCash(item)) {
                           Item it = CashShop.generateCouponItem(item, qty);

                           cs.addToInventory(it);
                           cashItems.add(it);
                        } else {
                           MapleInventoryManipulator.addById(c, item, qty, "", -1);
                           items.add(new Pair<>((int) qty, item));
                        }
                        break;
                  }
               }
               if (cashItems.size() > 255) {
                  List<Item> oldList = cashItems;
                  cashItems = Arrays.asList(new Item[255]);
                  int index = 0;
                  for (Item item : oldList) {
                     cashItems.set(index, item);
                     index++;
                  }
               }
               if (nxCredit != 0 || nxPrepaid != 0) { //coupon packet can only show maple points (afaik)
                  c.sendPacket(CCashShop.showBoughtQuestItem(0));
               } else {
                  c.sendPacket(CCashShop.showCouponRedeemedItems(c.getAccID(), maplePoints, mesos, cashItems, items));
               }
               c.enableCSActions();
            }
         } finally {
            c.releaseClient();
         }
      }
   }
}
