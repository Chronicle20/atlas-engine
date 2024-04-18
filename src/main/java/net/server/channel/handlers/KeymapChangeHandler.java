package net.server.channel.handlers;

import client.MapleClient;
import client.Skill;
import client.SkillFactory;
import client.inventory.MapleInventoryType;
import client.keybind.MapleKeyBinding;
import constants.game.GameConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class KeymapChangeHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (p.available() >= 8) {
         int mode = p.readInt();
         if (mode == 0) {
            int numChanges = p.readInt();
            for (int i = 0; i < numChanges; i++) {
               int key = p.readInt();
               int type = p.readByte();
               int action = p.readInt();

               if (type == 1) {
                  boolean isBanndedSkill;
                  Skill skill = SkillFactory.getSkill(action).orElse(null);
                  if (skill != null) {
                     isBanndedSkill = GameConstants.bannedBindSkills(skill.id());
                     if (isBanndedSkill || (!c.getPlayer().isGM() && GameConstants.isGMSkills(skill.id())) || (
                           !GameConstants.isInJobTree(skill.id(), c.getPlayer().getJob().getId()) && !c.getPlayer().isGM())) {
                        continue;
                     }
                  }
               }

               c.getPlayer().changeKeybinding(key, new MapleKeyBinding(type, action));
            }
         } else if (mode == 1) { // Auto HP Potion
            int itemID = p.readInt();
            if (itemID != 0 && c.getPlayer().getInventory(MapleInventoryType.USE).findById(itemID).isEmpty()) {
               c.disconnect(false, false); // Don't let them send a packet with a use item they dont have.
               return;
            }
            c.getPlayer().changeKeybinding(91, new MapleKeyBinding(7, itemID));
         } else if (mode == 2) { // Auto MP Potion
            int itemID = p.readInt();
            if (itemID != 0 && c.getPlayer().getInventory(MapleInventoryType.USE).findById(itemID).isEmpty()) {
               c.disconnect(false, false); // Don't let them send a packet with a use item they dont have.
               return;
            }
            c.getPlayer().changeKeybinding(92, new MapleKeyBinding(7, itemID));
         }
      }
   }
}
