package server.gachapon;

import java.util.Arrays;
import java.util.Optional;

import server.ItemInformationProvider;
import tools.Randomizer;

public class MapleGachapon {

   private static final MapleGachapon instance = new MapleGachapon();

   public static MapleGachapon getInstance() {
      return instance;
   }

   public Optional<MapleGachaponItem> process(int npcId) {
      return Gachapon.getByNpcId(npcId).map(g -> new MapleGachaponItem(g.getTier(), g.getItem(g.getTier())));
   }

   public enum Gachapon {

      GLOBAL(-1, -1, -1, -1, new Global()),
      HENESYS(9100100, 90, 8, 2, new Henesys()),
      ELLINIA(9100101, 90, 8, 2, new Ellinia()),
      PERION(9100102, 90, 8, 2, new Perion()),
      KERNING_CITY(9100103, 90, 8, 2, new KerningCity()),
      SLEEPYWOOD(9100104, 90, 8, 2, new Sleepywood()),
      MUSHROOM_SHRINE(9100105, 90, 8, 2, new MushroomShrine()),
      SHOWA_SPA_MALE(9100106, 90, 8, 2, new ShowaSpaMale()),
      SHOWA_SPA_FEMALE(9100107, 90, 8, 2, new ShowaSpaFemale()),
      LUDIBRIUM(9100108, 90, 8, 2, new Ludibrium()),
      NEW_LEAF_CITY(9100109, 90, 8, 2, new NewLeafCity()),
      EL_NATH(9100110, 90, 8, 2, new ElNath()),
      NAUTILUS_HARBOR(9100117, 90, 8, 2, new NautilusHarbor());

      private static final Gachapon[] values = Gachapon.values();

      private final GachaponItems gachapon;
      private final int npcId;
      private final int common;
      private final int uncommon;
      private final int rare;

      Gachapon(int npcid, int c, int u, int r, GachaponItems g) {
         this.npcId = npcid;
         this.gachapon = g;
         this.common = c;
         this.uncommon = u;
         this.rare = r;
      }

      public static Optional<Gachapon> getByNpcId(int npcId) {
         return Arrays.stream(values).filter(g -> g.npcId == npcId).findFirst();
      }

      public static String[] getLootInfo() {
         ItemInformationProvider ii = ItemInformationProvider.getInstance();

         String[] strList = new String[values.length + 1];

         String menuStr = "";
         int j = 0;
         for (Gachapon gacha : values) {
            menuStr += "#L" + j + "#" + gacha.name() + "#l\r\n";
            j++;

            String str = "";
            for (int i = 0; i < 3; i++) {
               int[] gachaItems = gacha.getItems(i);

               if (gachaItems.length > 0) {
                  str += ("  #rTier " + i + "#k:\r\n");
                  for (int itemid : gachaItems) {
                     String itemName = ii.getName(itemid);
                     if (itemName == null) {
                        itemName = "MISSING NAME #" + itemid;
                     }

                     str += ("    " + itemName + "\r\n");
                  }

                  str += "\r\n";
               }
            }
            str += "\r\n";

            strList[j] = str;
         }
         strList[0] = menuStr;

         return strList;
      }

      private int getTier() {
         int chance = Randomizer.nextInt(common + uncommon + rare) + 1;
         if (chance > common + uncommon) {
            return 2; //Rare
         } else if (chance > common) {
            return 1; //Uncommon
         } else {
            return 0; //Common
         }
      }

      public int[] getItems(int tier) {
         return gachapon.getItems(tier);
      }

      public int getItem(int tier) {
         int[] gacha = getItems(tier);
         int[] global = GLOBAL.getItems(tier);
         int chance = Randomizer.nextInt(gacha.length + global.length);
         return chance < gacha.length ? gacha[chance] : global[chance - gacha.length];
      }
   }

   public record MapleGachaponItem(int id, int tier) {
   }
}
