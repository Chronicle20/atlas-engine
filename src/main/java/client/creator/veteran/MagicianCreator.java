package client.creator.veteran;

import java.util.Arrays;

import client.MapleClient;
import client.MapleJob;
import client.Skill;
import client.SkillFactory;
import client.creator.CharacterFactory;
import client.creator.CharacterFactoryRecipe;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.skills.Magician;
import server.ItemInformationProvider;

public class MagicianCreator extends CharacterFactory {
   private static final int[] equips = {0, 1041041, 0, 1061034, 1072075};
   private static final int[] weapons = {1372003, 1382017};
   private static final int[] startingHpMp = {405, 729};
   private static final int[] mpGain = {0, 40, 80, 118, 156, 194, 230, 266, 302, 336, 370};

   private static CharacterFactoryRecipe createRecipe(MapleJob job, int level, int map, int top, int bottom, int shoes, int weapon,
                                                      int gender, int improveSp) {
      CharacterFactoryRecipe recipe = new CharacterFactoryRecipe(job, level, map, top, bottom, shoes, weapon);
      ItemInformationProvider ii = ItemInformationProvider.getInstance();

      recipe.setInt(20);
      recipe.setRemainingAp(138);
      recipe.setRemainingSp(67);

      recipe.setMaxHp(startingHpMp[0]);
      recipe.setMaxMp(startingHpMp[1] + mpGain[improveSp]);

      recipe.setMeso(100000);

      if (gender == 0) {
         giveEquipment(recipe, ii, 1050003);
      }

      Arrays.stream(weapons).forEach(w -> giveEquipment(recipe, ii, w));

      giveItem(recipe, 2000001, 100, MapleInventoryType.USE);
      giveItem(recipe, 2000006, 100, MapleInventoryType.USE);
      giveItem(recipe, 3010000, 1, MapleInventoryType.SETUP);

      if (improveSp > 0) {
         improveSp += 5;
         recipe.setRemainingSp(recipe.getRemainingSp() - improveSp);

         int toUseSp = 5;
         Skill improveMpRec = SkillFactory.getSkill(Magician.IMPROVED_MP_RECOVERY).orElseThrow();
         recipe.addStartingSkillLevel(improveMpRec, toUseSp);
         improveSp -= toUseSp;

         if (improveSp > 0) {
            Skill improveMaxMp = SkillFactory.getSkill(Magician.IMPROVED_MAX_MP_INCREASE).orElseThrow();
            recipe.addStartingSkillLevel(improveMaxMp, improveSp);
         }
      }

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
            createRecipe(MapleJob.MAGICIAN, 30, 101000000, equips[gender], equips[2 + gender], equips[4], weapons[0], gender,
                  improveSp));
   }
}
