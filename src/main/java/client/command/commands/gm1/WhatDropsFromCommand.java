package client.command.commands.gm1;

import java.util.Comparator;

import client.MapleCharacter;
import client.MapleClient;
import client.command.Command;
import drop.DropEntry;
import drop.DropProcessor;
import server.ItemInformationProvider;
import server.life.MonsterInformationProvider;
import tools.NPCChatBuilder;

public class WhatDropsFromCommand extends Command {
   {
      setDescription("");
   }

   @Override
   public void execute(MapleClient c, String[] params) {
      MapleCharacter player = c.getPlayer();
      if (params.length < 1) {
         player.dropMessage(5, "Please do @whatdropsfrom <monster name>");
         return;
      }
      String monsterName = player.getLastCommandMessage();
      NPCChatBuilder output = new NPCChatBuilder();

      MonsterInformationProvider.getMobsIDsFromName(monsterName).stream()
            .limit(3)
            .forEach(mobId -> appendMonsterDropInformation(output, mobId, player));
      c.getAbstractPlayerInteraction()
            .npcTalk(9010000, output.toString());
   }

   private void appendMonsterDropInformation(NPCChatBuilder output, int mobId, MapleCharacter character) {
      output.boldText().showMonsterName(mobId).normalText()
            .addText(" drops the following items:").newLine().newLine();
      DropProcessor.getInstance()
            .getDropsForMonster(mobId)
            .stream()
            .filter(d -> d.chance() > 0)
            .filter(d -> ItemInformationProvider.itemExists(d.itemId()))
            .sorted(Comparator.comparingInt(DropEntry::itemId))
            .map(d -> itemDropString(character, mobId, d))
            .forEach(output::addText);
      output.newLine();
   }

   private String itemDropString(MapleCharacter character, int mobId, DropEntry dropEntry) {
      float chance = Math.max(1000000 / dropEntry.chance() / (!MonsterInformationProvider.getInstance()
            .isBoss(mobId) ? character.getDropRate() : character.getBossDropRate()), 1);
      return NPCChatBuilder.of()
            .addText("- ").showItemImage1(dropEntry.itemId())
            .addText(String.format(" (%d/%d)", 1, (int) chance))
            .newLine()
            .toString();
   }
}
