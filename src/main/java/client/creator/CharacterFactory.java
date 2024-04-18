package client.creator;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleSkinColor;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import config.YamlConfig;
import connection.packets.CLogin;
import connection.packets.CWvsContext;
import net.server.Server;
import server.ItemInformationProvider;
import tools.FilePrinter;

public abstract class CharacterFactory {

    protected synchronized static int createNewCharacter(MapleClient c, String name, int face, int hair, int skin, int gender, CharacterFactoryRecipe recipe) {
        if (YamlConfig.config.server.COLLECTIVE_CHARSLOT ? c.getAvailableCharacterSlots() <= 0 : c.getAvailableCharacterWorldSlots() <= 0) {
            return -3;
        }

        if (!MapleCharacter.canCreateChar(name)) {
            return -1;
        }

        MapleCharacter newchar = MapleCharacter.getDefault(c);
        newchar.setWorld(c.getWorld());
        newchar.setSkinColor(MapleSkinColor.getById(skin).orElseThrow());
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setHair(hair);
        newchar.setFace(face);

        newchar.setLevel(recipe.getLevel());
        newchar.setJob(recipe.getJob());
        newchar.setMapId(recipe.getMap());

        MapleInventory equipped = newchar.getInventory(MapleInventoryType.EQUIPPED);
        ItemInformationProvider ii = ItemInformationProvider.getInstance();

        int top = recipe.getTop(), bottom = recipe.getBottom(), shoes = recipe.getShoes(), weapon = recipe.getWeapon();

        if (top > 0) {
            Item eq_top = ii.getEquipById(top);
            eq_top.setPosition((byte) -5);
            equipped.addItemFromDB(eq_top);
        }

        if (bottom > 0) {
            Item eq_bottom = ii.getEquipById(bottom);
            eq_bottom.setPosition((byte) -6);
            equipped.addItemFromDB(eq_bottom);
        }

        if (shoes > 0) {
            Item eq_shoes = ii.getEquipById(shoes);
            eq_shoes.setPosition((byte) -7);
            equipped.addItemFromDB(eq_shoes);
        }

        if (weapon > 0) {
            Item eq_weapon = ii.getEquipById(weapon);
            eq_weapon.setPosition((byte) -11);
            equipped.addItemFromDB(eq_weapon.copy());
        }

        if (!newchar.insertNewChar(recipe)) {
            return -2;
        }
        c.sendPacket(CLogin.addNewCharEntry(newchar));

        Server.getInstance().createCharacterEntry(newchar);
        Server.getInstance().broadcastGMMessage(c.getWorld(), CWvsContext.sendYellowTip("[New Char]: " + c.getAccountName() + " has created a new character with IGN " + name));
        FilePrinter.print(FilePrinter.CREATED_CHAR + c.getAccountName() + ".txt", c.getAccountName() + " created character with IGN " + name);

        return 0;
    }
}
