package net.server.channel.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.processor.action.BuybackProcessor;
import config.YamlConfig;
import connection.packets.CField;
import connection.packets.CStage;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import server.MTSItemInfo;
import server.maps.FieldLimit;
import server.maps.MapleMiniDungeonInfo;
import tools.DatabaseConnection;

public final class EnterMTSHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();

      if (!chr.isAlive() && YamlConfig.config.server.USE_BUYBACK_SYSTEM) {
         BuybackProcessor.processBuyback(c);
         c.sendPacket(CWvsContext.enableActions());
      } else {
         if (!YamlConfig.config.server.USE_MTS) {
            c.sendPacket(CWvsContext.enableActions());
            return;
         }

         if (chr.getEventInstance().isPresent()) {
            c.sendPacket(CWvsContext.serverNotice(5, "Entering Cash Shop or MTS are disabled when registered on an event."));
            c.sendPacket(CWvsContext.enableActions());
            return;
         }

         if (MapleMiniDungeonInfo.isDungeonMap(chr.getMapId())) {
            c.sendPacket(CWvsContext.serverNotice(5,
                  "Changing channels or entering Cash Shop or MTS are disabled when inside a Mini-Dungeon."));
            c.sendPacket(CWvsContext.enableActions());
            return;
         }

         if (FieldLimit.CANNOTMIGRATE.check(chr.getMap().getFieldLimit())) {
            chr.dropMessage(1, "You can't do it here in this map.");
            c.sendPacket(CWvsContext.enableActions());
            return;
         }

         if (!chr.isAlive()) {
            c.sendPacket(CWvsContext.enableActions());
            return;
         }
         if (chr.getLevel() < 10) {
            c.sendPacket(CField.blockedMessage2(5));
            c.sendPacket(CWvsContext.enableActions());
            return;
         }

         chr.closePlayerInteractions();
         chr.closePartySearchInteractions();

         chr.unregisterChairBuff();
         Server.getInstance().getPlayerBuffStorage().addBuffsToStorage(chr.getId(), chr.getAllBuffs());
         Server.getInstance().getPlayerBuffStorage().addDiseasesToStorage(chr.getId(), chr.getAllDiseases());
         chr.setAwayFromChannelWorld();
         chr.notifyMapTransferToPartner(-1);
         chr.removeIncomingInvites();
         chr.cancelAllBuffs(true);
         chr.cancelAllDebuffs();
         chr.cancelBuffExpireTask();
         chr.cancelDiseaseExpireTask();
         chr.cancelSkillCooldownTask();
         chr.cancelExpirationTask();

         chr.forfeitExpirableQuests();
         chr.cancelQuestExpirationTask();

         chr.saveCharToDB();

         c.getChannelServer().removePlayer(chr);
         chr.getMap().removePlayer(c.getPlayer());
         try {
            c.sendPacket(CStage.openCashShop(c, true));
         } catch (Exception ex) {
            ex.printStackTrace();
         }
         chr.getCashShop().open(true);// xD
         c.enableCSActions();
         c.sendPacket(CField.MTSWantedListingOver(0, 0));
         c.sendPacket(CField.showMTSCash(c.getPlayer()));
         List<MTSItemInfo> items = new ArrayList<>();
         int pages = 0;
         try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps =
                  con.prepareStatement("SELECT * FROM mts_items WHERE tab = 1 AND transfer = 0 ORDER BY id DESC LIMIT 16, 16");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
               if (rs.getInt("type") != 1) {
                  Item i = new Item(rs.getInt("itemid"), (short) 0, (short) rs.getInt("quantity"));
                  i.setOwner(rs.getString("owner"));
                  items.add(new MTSItemInfo(i, rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1), rs.getInt("id"),
                        rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
               } else {
                  Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
                  equip.setOwner(rs.getString("owner"));
                  equip.setQuantity((short) 1);
                  equip.setAcc((short) rs.getInt("acc"));
                  equip.setAvoid((short) rs.getInt("avoid"));
                  equip.setDex((short) rs.getInt("dex"));
                  equip.setHands((short) rs.getInt("hands"));
                  equip.setHp((short) rs.getInt("hp"));
                  equip.setInt((short) rs.getInt("int"));
                  equip.setJump((short) rs.getInt("jump"));
                  equip.setVicious((short) rs.getInt("vicious"));
                  equip.setFlag((short) rs.getInt("flag"));
                  equip.setLuk((short) rs.getInt("luk"));
                  equip.setMatk((short) rs.getInt("matk"));
                  equip.setMdef((short) rs.getInt("mdef"));
                  equip.setMp((short) rs.getInt("mp"));
                  equip.setSpeed((short) rs.getInt("speed"));
                  equip.setStr((short) rs.getInt("str"));
                  equip.setWatk((short) rs.getInt("watk"));
                  equip.setWdef((short) rs.getInt("wdef"));
                  equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                  equip.setLevel((byte) rs.getInt("level"));
                  equip.setItemLevel(rs.getByte("itemlevel"));
                  equip.setItemExp(rs.getInt("itemexp"));
                  equip.setRingId(rs.getInt("ringid"));
                  equip.setExpiration(rs.getLong("expiration"));
                  equip.setGiftFrom(rs.getString("giftFrom"));

                  items.add(new MTSItemInfo(equip, rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1), rs.getInt("id"),
                        rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
               }
            }
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT COUNT(*) FROM mts_items");
            rs = ps.executeQuery();
            if (rs.next()) {
               pages = (int) Math.ceil(rs.getInt(1) / 16);
            }
            rs.close();
            ps.close();
            con.close();
         } catch (SQLException e) {
            e.printStackTrace();
         }
         c.sendPacket(CField.sendMTS(items, 1, 0, 0, pages));
         c.sendPacket(CField.transferInventory(getTransfer(chr.getId())));
         c.sendPacket(CField.notYetSoldInv(getNotYetSold(chr.getId())));
      }
   }

   private List<MTSItemInfo> getNotYetSold(int cid) {
      List<MTSItemInfo> items = new ArrayList<>();
      try {
         Connection con = DatabaseConnection.getConnection();
         try (PreparedStatement ps = con.prepareStatement(
               "SELECT * FROM mts_items WHERE seller = ? AND transfer = 0 ORDER BY id DESC")) {
            ps.setInt(1, cid);
            try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                  if (rs.getInt("type") != 1) {
                     Item i = new Item(rs.getInt("itemid"), (short) 0, (short) rs.getInt("quantity"));
                     i.setOwner(rs.getString("owner"));
                     items.add(
                           new MTSItemInfo(i, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"),
                                 rs.getString("sell_ends")));
                  } else {
                     Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
                     equip.setOwner(rs.getString("owner"));
                     equip.setQuantity((short) 1);
                     equip.setAcc((short) rs.getInt("acc"));
                     equip.setAvoid((short) rs.getInt("avoid"));
                     equip.setDex((short) rs.getInt("dex"));
                     equip.setHands((short) rs.getInt("hands"));
                     equip.setHp((short) rs.getInt("hp"));
                     equip.setInt((short) rs.getInt("int"));
                     equip.setJump((short) rs.getInt("jump"));
                     equip.setVicious((short) rs.getInt("vicious"));
                     equip.setLuk((short) rs.getInt("luk"));
                     equip.setMatk((short) rs.getInt("matk"));
                     equip.setMdef((short) rs.getInt("mdef"));
                     equip.setMp((short) rs.getInt("mp"));
                     equip.setSpeed((short) rs.getInt("speed"));
                     equip.setStr((short) rs.getInt("str"));
                     equip.setWatk((short) rs.getInt("watk"));
                     equip.setWdef((short) rs.getInt("wdef"));
                     equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                     equip.setLevel((byte) rs.getInt("level"));
                     equip.setItemLevel(rs.getByte("itemlevel"));
                     equip.setItemExp(rs.getInt("itemexp"));
                     equip.setRingId(rs.getInt("ringid"));
                     equip.setFlag((short) rs.getInt("flag"));
                     equip.setExpiration(rs.getLong("expiration"));
                     equip.setGiftFrom(rs.getString("giftFrom"));
                     items.add(new MTSItemInfo(equip, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"),
                           rs.getString("sellername"), rs.getString("sell_ends")));
                  }
               }
            }
         }
         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
      return items;
   }

   private List<MTSItemInfo> getTransfer(int cid) {
      List<MTSItemInfo> items = new ArrayList<>();
      try {
         Connection con = DatabaseConnection.getConnection();
         try (PreparedStatement ps = con.prepareStatement(
               "SELECT * FROM mts_items WHERE transfer = 1 AND seller = ? ORDER BY id DESC")) {
            ps.setInt(1, cid);
            try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                  if (rs.getInt("type") != 1) {
                     Item i = new Item(rs.getInt("itemid"), (short) 0, (short) rs.getInt("quantity"));
                     i.setOwner(rs.getString("owner"));
                     items.add(
                           new MTSItemInfo(i, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"),
                                 rs.getString("sell_ends")));
                  } else {
                     Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
                     equip.setOwner(rs.getString("owner"));
                     equip.setQuantity((short) 1);
                     equip.setAcc((short) rs.getInt("acc"));
                     equip.setAvoid((short) rs.getInt("avoid"));
                     equip.setDex((short) rs.getInt("dex"));
                     equip.setHands((short) rs.getInt("hands"));
                     equip.setHp((short) rs.getInt("hp"));
                     equip.setInt((short) rs.getInt("int"));
                     equip.setJump((short) rs.getInt("jump"));
                     equip.setVicious((short) rs.getInt("vicious"));
                     equip.setLuk((short) rs.getInt("luk"));
                     equip.setMatk((short) rs.getInt("matk"));
                     equip.setMdef((short) rs.getInt("mdef"));
                     equip.setMp((short) rs.getInt("mp"));
                     equip.setSpeed((short) rs.getInt("speed"));
                     equip.setStr((short) rs.getInt("str"));
                     equip.setWatk((short) rs.getInt("watk"));
                     equip.setWdef((short) rs.getInt("wdef"));
                     equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                     equip.setLevel((byte) rs.getInt("level"));
                     equip.setItemLevel(rs.getByte("itemlevel"));
                     equip.setItemExp(rs.getInt("itemexp"));
                     equip.setRingId(rs.getInt("ringid"));
                     equip.setFlag((short) rs.getInt("flag"));
                     equip.setExpiration(rs.getLong("expiration"));
                     equip.setGiftFrom(rs.getString("giftFrom"));
                     items.add(new MTSItemInfo(equip, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"),
                           rs.getString("sellername"), rs.getString("sell_ends")));
                  }
               }
            }
         }
         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
      return items;
   }
}