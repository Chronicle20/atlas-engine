package client.inventory;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import client.MapleCharacter;
import client.inventory.manipulator.MapleCashidGenerator;
import connection.packets.CPet;
import connection.packets.CUser;
import constants.game.ExpTable;
import server.ItemInformationProvider;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import tools.DatabaseConnection;
import tools.Pair;

public class MaplePet extends Item {
   private String name;
   private int uniqueid;
   private int closeness = 0;
   private byte level = 1;
   private int fullness = 100;
   private int Fh;
   private Point pos;
   private int stance;
   private boolean summoned;
   private int petFlag = 0;

   private MaplePet(int id, short position, int uniqueid) {
      super(id, position, (short) 1);
      this.uniqueid = uniqueid;
      this.pos = new Point(0, 0);
   }

   public static Optional<MaplePet> loadFromDb(int itemid, short position, int petid) {
      try {
         MaplePet ret = new MaplePet(itemid, position, petid);
         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(
               "SELECT name, level, closeness, fullness, summoned, flag FROM pets WHERE petid = ?"); // Get pet details..
         ps.setInt(1, petid);
         ResultSet rs = ps.executeQuery();
         rs.next();
         ret.setName(rs.getString("name"));
         ret.setCloseness(Math.min(rs.getInt("closeness"), 30000));
         ret.setLevel((byte) Math.min(rs.getByte("level"), 30));
         ret.setFullness(Math.min(rs.getInt("fullness"), 100));
         ret.setSummoned(rs.getInt("summoned") == 1);
         ret.setPetFlag(rs.getInt("flag"));
         rs.close();
         ps.close();
         con.close();
         return Optional.of(ret);
      } catch (SQLException e) {
         e.printStackTrace();
         return Optional.empty();
      }
   }

   public static void deleteFromDb(MapleCharacter owner, int petid) {
      try {
         Connection con = DatabaseConnection.getConnection();

         PreparedStatement ps = con.prepareStatement(
               "DELETE FROM pets WHERE `petid` = ?");  // thanks Vcoc for detecting petignores remaining after deletion
         ps.setInt(1, petid);
         ps.executeUpdate();
         ps.close();

         con.close();

         owner.resetExcluded(petid);
         MapleCashidGenerator.freeCashId(petid);
      } catch (SQLException ex) {
         ex.printStackTrace();
      }
   }

   public static int createPet(int itemid) {
      try {
         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(
               "INSERT INTO pets (petid, name, level, closeness, fullness, summoned, flag) VALUES (?, ?, 1, 0, 100, 0, 0)");
         int ret = MapleCashidGenerator.generateCashId();
         ps.setInt(1, ret);
         ps.setString(2, ItemInformationProvider.getInstance().getName(itemid));
         ps.executeUpdate();
         ps.close();
         con.close();
         return ret;
      } catch (SQLException e) {
         e.printStackTrace();
         return -1;
      }
   }

   public static int createPet(int itemid, byte level, int closeness, int fullness) {
      try {
         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(
               "INSERT INTO pets (petid, name, level, closeness, fullness, summoned, flag) VALUES (?, ?, ?, ?, ?, 0, 0)");
         int ret = MapleCashidGenerator.generateCashId();
         ps.setInt(1, ret);
         ps.setString(2, ItemInformationProvider.getInstance().getName(itemid));
         ps.setByte(3, level);
         ps.setInt(4, closeness);
         ps.setInt(5, fullness);
         ps.executeUpdate();
         ps.close();
         con.close();
         return ret;
      } catch (SQLException e) {
         e.printStackTrace();
         return -1;
      }
   }

   public void saveToDb() {
      try {
         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(
               "UPDATE pets SET name = ?, level = ?, closeness = ?, fullness = ?, summoned = ?, flag = ? WHERE petid = ?");
         ps.setString(1, getName());
         ps.setInt(2, getLevel());
         ps.setInt(3, getCloseness());
         ps.setInt(4, getFullness());
         ps.setInt(5, isSummoned() ? 1 : 0);
         ps.setInt(6, getPetFlag());
         ps.setInt(7, getUniqueId());
         ps.executeUpdate();
         ps.close();
         con.close();
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public int getUniqueId() {
      return uniqueid;
   }

   public void setUniqueId(int id) {
      this.uniqueid = id;
   }

   public int getCloseness() {
      return closeness;
   }

   public void setCloseness(int closeness) {
      this.closeness = closeness;
   }

   public byte getLevel() {
      return level;
   }

   public void setLevel(byte level) {
      this.level = level;
   }

   public void gainClosenessFullness(MapleCharacter owner, int incCloseness, int incFullness, int type) {
      byte slot = owner.getPetIndex(this);
      boolean enjoyed;

      //will NOT increase pet's closeness if tried to feed pet with 100% fullness
      if (fullness < 100 || incFullness == 0) {   //incFullness == 0: command given
         int newFullness = fullness + incFullness;
         if (newFullness > 100) {
            newFullness = 100;
         }
         fullness = newFullness;

         if (incCloseness > 0 && closeness < 30000) {
            int newCloseness = closeness + incCloseness;
            if (newCloseness > 30000) {
               newCloseness = 30000;
            }

            closeness = newCloseness;
            while (newCloseness >= ExpTable.getClosenessNeededForLevel(level)) {
               level += 1;
               owner.sendPacket(CUser.showOwnPetLevelUp(slot));
               owner.getMap().broadcastMessage(CUser.showPetLevelUp(owner, slot));
            }
         }

         enjoyed = true;
      } else {
         int newCloseness = closeness - 1;
         if (newCloseness < 0) {
            newCloseness = 0;
         }

         closeness = newCloseness;
         if (level > 1 && newCloseness < ExpTable.getClosenessNeededForLevel(level - 1)) {
            level -= 1;
         }

         enjoyed = false;
      }

      owner.getMap().broadcastMessage(CPet.petFoodResponse(owner.getId(), slot, enjoyed, false));
      saveToDb();

      Item petz = owner.getInventory(MapleInventoryType.CASH).getItem(getPosition());
      if (petz != null) {
         owner.forceUpdateItem(petz);
      }
   }

   public int getFullness() {
      return fullness;
   }

   public void setFullness(int fullness) {
      this.fullness = fullness;
   }

   public int getFh() {
      return Fh;
   }

   public void setFh(int Fh) {
      this.Fh = Fh;
   }

   public Point getPos() {
      return pos;
   }

   public void setPos(Point pos) {
      this.pos = pos;
   }

   public int getStance() {
      return stance;
   }

   public void setStance(int stance) {
      this.stance = stance;
   }

   public boolean isSummoned() {
      return summoned;
   }

   public void setSummoned(boolean yes) {
      this.summoned = yes;
   }

   public int getPetFlag() {
      return this.petFlag;
   }

   private void setPetFlag(int flag) {
      this.petFlag = flag;
   }

   public void addPetFlag(MapleCharacter owner, PetFlag flag) {
      this.petFlag |= flag.getValue();
      saveToDb();

      Item petz = owner.getInventory(MapleInventoryType.CASH).getItem(getPosition());
      if (petz != null) {
         owner.forceUpdateItem(petz);
      }
   }

   public void removePetFlag(MapleCharacter owner, PetFlag flag) {
      this.petFlag &= 0xFFFFFFFF ^ flag.getValue();
      saveToDb();

      Item petz = owner.getInventory(MapleInventoryType.CASH).getItem(getPosition());
      if (petz != null) {
         owner.forceUpdateItem(petz);
      }
   }

   public Pair<Integer, Boolean> canConsume(int itemId) {
      return ItemInformationProvider.getInstance().canPetConsume(this.getItemId(), itemId);
   }

   public void updatePosition(List<LifeMovementFragment> movement) {
      for (LifeMovementFragment move : movement) {
         if (move instanceof LifeMovement) {
            if (move instanceof AbsoluteLifeMovement) {
               this.setPos(move.getPosition());
            }
            this.setStance(((LifeMovement) move).getNewstate());
         }
      }
   }

   public enum PetFlag {
      OWNER_SPEED(0x01);

      private final int i;

      PetFlag(int i) {
         this.i = i;
      }

      public int getValue() {
         return i;
      }
   }
}