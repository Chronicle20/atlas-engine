package client.command.commands.gm3;

import client.MapleCharacter;
import client.MapleClient;
import client.command.Command;
import connection.packets.CField;
import connection.packets.CScriptMan;
import constants.game.GameConstants;
import constants.net.NPCTalkMessageType;

public class MusicCommand extends Command {
   {
      setDescription("");
   }

   private static String getSongList() {
      return GameConstants.GAME_SONGS.stream()
            .reduce(new StringBuilder("Song:\r\n"), (sb, s) -> sb.append("  ").append(s).append("\r\n"), StringBuilder::append)
            .toString();
   }

   @Override
   public void execute(MapleClient c, String[] params) {

      MapleCharacter player = c.getPlayer();
      if (params.length < 1) {
         String sendMsg = "";

         sendMsg += "Syntax: #r!music <song>#k\r\n\r\n";
         sendMsg += getSongList();

         c.sendPacket(CScriptMan.getNPCTalk(1052015, NPCTalkMessageType.ON_SAY, sendMsg, "00 00", (byte) 0));
         return;
      }

      String song = player.getLastCommandMessage();
      for (String s : GameConstants.GAME_SONGS) {
         if (s.equalsIgnoreCase(song)) {    // thanks Masterrulax for finding an issue here
            player.getMap().broadcastMessage(CField.musicChange(s));
            player.yellowMessage("Now playing song " + s + ".");
            return;
         }
      }

      String sendMsg = "";
      sendMsg += "Song not found, please enter a song below.\r\n\r\n";
      sendMsg += getSongList();

      c.sendPacket(CScriptMan.getNPCTalk(1052015, NPCTalkMessageType.ON_SAY, sendMsg, "00 00", (byte) 0));
   }
}
