package client.creator.veteran;

import java.util.Arrays;

import client.MapleClient;
import client.MapleJob;
import client.creator.CharacterFactory;
import client.creator.CharacterFactoryRecipe;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import server.ItemInformationProvider;

public class PirateCreator extends CharacterFactory {
   private static final int[] equips = {0, 0, 0, 0, 1072294};
   private static final int[] weapons = {1482004, 1492004};
   private static final int[] startingHpMp = {846, 503};

   private static CharacterFactoryRecipe createRecipe(MapleJob job, int level, int map, int top, int bottom, int shoes,
                                                      int weapon) {
      CharacterFactoryRecipe recipe = new CharacterFactoryRecipe(job, level, map, top, bottom, shoes, weapon);
      ItemInformationProvider ii = ItemInformationProvider.getInstance();

      recipe.setDex(20);
      recipe.setRemainingAp(138);
      recipe.setRemainingSp(61);

      recipe.setMaxHp(startingHpMp[0]);
      recipe.setMaxMp(startingHpMp[1]);

      recipe.setMeso(100000);

      giveEquipment(recipe, ii, 1052107);

      Arrays.stream(weapons).forEach(w -> giveEquipment(recipe, ii, w));

      giveItem(recipe, 2330000, 800, MapleInventoryType.USE);

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
            createRecipe(MapleJob.PIRATE, 30, 120000000, equips[gender], equips[2 + gender], equips[4], weapons[0]));
   }
}
