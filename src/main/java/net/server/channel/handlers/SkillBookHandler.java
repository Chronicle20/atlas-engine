package net.server.channel.handlers;

import java.util.Map;

import client.MapleCharacter;
import client.MapleClient;
import client.Skill;
import client.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.ItemInformationProvider;

public final class SkillBookHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (!c.getPlayer().isAlive()) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }

      p.readInt();
      short slot = p.readShort();
      int itemId = p.readInt();

      boolean canuse;
      boolean success = false;
      int skill = 0;
      int maxlevel = 0;

      MapleCharacter player = c.getPlayer();
      if (c.tryacquireClient()) {
         try {
            MapleInventory inv = c.getPlayer().getInventory(MapleInventoryType.USE);
            Item toUse = inv.getItem(slot);
            if (toUse == null || toUse.getItemId() != itemId) {
               return;
            }
            Map<String, Integer> skilldata =
                  ItemInformationProvider.getInstance().getSkillStats(toUse.getItemId(), c.getPlayer().getJob().getId());
            if (skilldata == null) {
               return;
            }
            Skill skill2 = SkillFactory.getSkill(skilldata.get("skillid")).orElseThrow();
            if (skilldata.get("skillid") == 0) {
               canuse = false;
            } else if ((player.getSkillLevel(skill2) >= skilldata.get("reqSkillLevel") || skilldata.get("reqSkillLevel") == 0)
                  && player.getMasterLevel(skill2) < skilldata.get("masterLevel")) {
               inv.lockInventory();
               try {
                  Item used = inv.getItem(slot);
                  if (used != toUse
                        || toUse.getQuantity() < 1) {    // thanks ClouD for noticing skillbooks not being usable when stacked
                     return;
                  }

                  MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
               } finally {
                  inv.unlockInventory();
               }

               canuse = true;
               if (ItemInformationProvider.rollSuccessChance(skilldata.get("success"))) {
                  success = true;
                  player.changeSkillLevel(skill2, player.getSkillLevel(skill2),
                        Math.max(skilldata.get("masterLevel"), player.getMasterLevel(skill2)), -1);
               } else {
                  success = false;
                  //player.dropMessage("The skill book lights up, but the skill winds up as if nothing happened.");
               }
            } else {
               canuse = false;
            }
         } finally {
            c.releaseClient();
         }

         // thanks Vcoc for noting skill book result not showing for all in area
         player.getMap().broadcastMessage(CWvsContext.skillBookResult(player, skill, maxlevel, canuse, success));
      }
   }
}
