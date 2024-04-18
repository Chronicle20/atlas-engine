package net.server.handlers.login;

import java.util.List;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.Server;
import tools.Pair;

public final class ViewAllCharHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      try {
         // client breaks if the charlist request pops too soon
         if (!c.canRequestCharlist()) {
            c.sendPacket(CLogin.showAllCharacter(0, 0));
            return;
         }

         int accountId = c.getAccID();
         Pair<Pair<Integer, List<MapleCharacter>>, List<Pair<Integer, List<MapleCharacter>>>> loginBlob =
               Server.getInstance().loadAccountCharlist(accountId, c.getVisibleWorlds());

         List<Pair<Integer, List<MapleCharacter>>> worldChars = loginBlob.getRight();
         int chrTotal = loginBlob.getLeft().getLeft();
         List<MapleCharacter> lastwchars = loginBlob.getLeft().getRight();

         if (chrTotal > 9) {
            int padRight = chrTotal % 3;
            if (padRight > 0 && lastwchars != null) {
               MapleCharacter chr = lastwchars.getLast();

               for (int i = padRight; i < 3; i++) { // filling the remaining slots with the last character loaded
                  chrTotal++;
                  lastwchars.add(chr);
               }
            }
         }

         int charsSize = chrTotal;
         int unk = charsSize + (3 - charsSize % 3); //rowSize?
         c.sendPacket(CLogin.showAllCharacter(charsSize, unk));

         for (Pair<Integer, List<MapleCharacter>> wchars : worldChars) {
            c.sendPacket(CLogin.showAllCharacterInfo(wchars.getLeft(), wchars.getRight(),
                  YamlConfig.config.server.ENABLE_PIC && !c.canBypassPic()));
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
