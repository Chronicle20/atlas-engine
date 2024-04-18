package client.creator.veteran;

import java.util.Arrays;

import client.MapleClient;
import client.MapleJob;
import client.creator.CharacterFactory;
import client.creator.CharacterFactoryRecipe;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import server.ItemInformationProvider;

public class BowmanCreator extends CharacterFactory {
   private static final int[] equips = {1040067, 1041054, 1060056, 1061050, 1072081};
   private static final int[] weapons = {1452005, 1462000};
   private static final int[] startingHpMp = {797, 404};

   private static CharacterFactoryRecipe createRecipe(MapleJob job, int level, int map, int top, int bottom, int shoes,
                                                      int weapon) {
      CharacterFactoryRecipe recipe = new CharacterFactoryRecipe(job, level, map, top, bottom, shoes, weapon);
      ItemInformationProvider ii = ItemInformationProvider.getInstance();

      recipe.setDex(25);
      recipe.setRemainingAp(133);
      recipe.setRemainingSp(61);

      recipe.setMaxHp(startingHpMp[0]);
      recipe.setMaxMp(startingHpMp[1]);

      recipe.setMeso(100000);

      Arrays.stream(weapons).forEach(w -> giveEquipment(recipe, ii, w));

      giveItem(recipe, 2000002, 100, MapleInventoryType.USE);
      giveItem(recipe, 2000003, 100, MapleInventoryType.USE);
      giveItem(recipe, 3010000, 1, MapleInventoryType.SETUP);

      return recipe;
   }

   private static void giveEquipment(CharacterFactoryRecipe recipe, ItemInformationProvider ii, int equipid) {
      Item nEquip = ii.getEquipById(equipid);
      recipe.addStartingEquipment(nEquip);
   }

   private static void giveItem(CharacterFactoryRecipe recipe, int itemid, int quantity, MapleInventoryType itemType) {
      recipe.addStartingItem(itemid, quantity, itemType);
   }

   public static int createCharacter(MapleClient c, String name, int face, int hair, int skin, int gender, int improveSp) {
      return createNewCharacter(c, name, face, hair, skin, gender,
            createRecipe(MapleJob.BOWMAN, 30, 100000000, equips[gender], equips[2 + gender], equips[4], weapons[0]));
   }
}
