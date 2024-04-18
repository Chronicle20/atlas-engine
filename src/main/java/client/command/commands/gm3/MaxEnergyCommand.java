package client.command.commands.gm3;

import client.MapleClient;
import client.command.Command;
import connection.packets.CWvsContext;

public class MaxEnergyCommand extends Command {
   {
      setDescription("");
   }

   @Override
   public void execute(MapleClient c, String[] params) {
      c.getPlayer().setDojoEnergy(10000);
      c.sendPacket(CWvsContext.getEnergy("energy", 10000));
   }
}
