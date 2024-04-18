package client.command.commands.gm0;

import client.MapleClient;
import client.command.Command;
import connection.packets.CWvsContext;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestScriptManager;

public class DisposeCommand extends Command {
   {
      setDescription("");
   }

   @Override
   public void execute(MapleClient c, String[] params) {
      NPCScriptManager.getInstance().dispose(c);
      QuestScriptManager.getInstance().dispose(c);
      c.sendPacket(CWvsContext.enableActions());
      c.removeClickedNPC();
      c.getPlayer().message("You've been disposed.");
   }
}
