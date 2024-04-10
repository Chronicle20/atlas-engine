package client.command.commands.gm1;

import java.util.Iterator;

import client.MapleCharacter;
import client.MapleClient;
import client.command.Command;
import server.ItemInformationProvider;
import tools.NPCChatBuilder;
import tools.Pair;

public class WhoDropsCommand extends Command {
   {
      setDescription("");
   }

   @Override
   public void execute(MapleClient c, String[] params) {
      MapleCharacter player = c.getPlayer();
      if (params.length < 1) {
         player.dropMessage(5, "Please do @whodrops <item name>");
         return;
      }

      if (c.tryacquireClient()) {
         try {
            String searchString = player.getLastCommandMessage();
            NPCChatBuilder output = new NPCChatBuilder();
            Iterator<Pair<Integer, String>> listIterator =
                  ItemInformationProvider.getInstance().getItemDataByName(searchString).iterator();
            if (listIterator.hasNext()) {
               int count = 1;
               while (listIterator.hasNext() && count <= 3) {
                  Pair<Integer, String> data = listIterator.next();
                  output.boldText().showItemName1(data.getLeft()).normalText().addText(" is dropped by:").newLine();
                  ItemInformationProvider.getInstance().getWhoDrops(data.getLeft())
                        .forEach(n -> output.addText(n).addText(", "));
                  output.newLine().newLine();
                  count++;
               }
            } else {
               player.dropMessage(5, "The item you searched for doesn't exist.");
               return;
            }

            c.getAbstractPlayerInteraction().npcTalk(9010000, output.toString());
         } finally {
            c.releaseClient();
         }
      } else {
         player.dropMessage(5, "Please wait a while for your request to be processed.");
      }
   }
}
