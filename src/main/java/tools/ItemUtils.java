package tools;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import character.BodyPart;
import character.EquipPrefix;
import client.MapleCharacter;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import server.ItemInformationProvider;

public interface ItemUtils {
   private static int getItemPrefix(int nItemID) {
      return nItemID / 10000;
   }

   static BodyPart getBodyPartFromItem(int itemID) {
      EquipPrefix prefix = EquipPrefix.getByVal(getItemPrefix(itemID));
      BodyPart bodyPart = BodyPart.BPBase;
      if (prefix != null) {
         switch (prefix) {
            case Hat -> bodyPart = BodyPart.Hat;
            case FaceAccessory -> bodyPart = BodyPart.FaceAccessory;
            case EyeAccessory -> bodyPart = BodyPart.EyeAccessory;
            case Earrings -> bodyPart = BodyPart.Earrings;
            case Top, Overall -> bodyPart = BodyPart.Top;
            case Bottom -> bodyPart = BodyPart.Bottom;
            case Shoes -> bodyPart = BodyPart.Shoes;
            case Gloves -> bodyPart = BodyPart.Gloves;
            case Shield, Katara, SecondaryWeapon -> bodyPart = BodyPart.Shield;
            case Cape -> bodyPart = BodyPart.Cape;
            case Ring -> bodyPart = BodyPart.Ring1;
            case Pendant -> bodyPart = BodyPart.Pendant;
            case Belt -> bodyPart = BodyPart.Belt;
            case Medal -> bodyPart = BodyPart.Medal;
            case Shoulder -> bodyPart = BodyPart.Shoulder;
            case PetWear -> bodyPart = BodyPart.PetEquip;
            case TamingMob -> bodyPart = BodyPart.TamingMob;
            case Saddle -> bodyPart = BodyPart.Saddle;
            case EvanHat -> bodyPart = BodyPart.EvanHat;
            case EvanPendant -> bodyPart = BodyPart.EvanPendant;
            case EvanWing -> bodyPart = BodyPart.EvanWing;
            case EvanShoes -> bodyPart = BodyPart.EvanShoes;
            case OneHandedAxe, OneHandedSword, OneHandedBluntWeapon, TwoHandedBluntWeapon, TwoHandedAxe, TwoHandedSword, PoleArm,
                 Spear,
                 Staff, Wand, Bow, Crossbow, Claw, Dagger, Gauntlet, Gun, Knuckle, Katana -> bodyPart = BodyPart.Weapon;
            case CashWeapon -> bodyPart = BodyPart.CashWeapon;
            default -> System.out.println("idk? " + prefix);
         }
      }
      return bodyPart;
   }

   static void fillEquipsMaps(MapleCharacter chr,
                              Map<BodyPart, Integer> charEquips,
                              Map<BodyPart, Integer> charMaskedEquips,
                              List<Integer> cWeapon) {
      MapleInventory equip = chr.getInventory(MapleInventoryType.EQUIPPED);
      Collection<Item> ii = ItemInformationProvider.getInstance().canWearEquipment(chr, equip.list());

      for (Item item : ii) {
         BodyPart bodyPart = getBodyPartFromItem(item.getItemId());
         if (bodyPart != BodyPart.BPBase) {

            if (bodyPart.getVal() == BodyPart.CashWeapon.getVal()) {
               cWeapon.add(item.getItemId());
            } else if (bodyPart.getVal() < BodyPart.BPEnd.getVal()) {
               if (!charEquips.containsKey(bodyPart)) {
                  charEquips.put(bodyPart, item.getItemId());
               } else if (ItemInformationProvider.getInstance().isCash(item.getItemId())) {
                  int nonCashItem = charEquips.remove(bodyPart);
                  charEquips.put(bodyPart, item.getItemId());
                  charMaskedEquips.put(bodyPart, nonCashItem);
               } else {
                  charMaskedEquips.put(bodyPart, item.getItemId());
               }
            }
         }
      }
   }
}
