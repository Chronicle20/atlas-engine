package connection.packets;

import java.net.InetAddress;
import java.util.List;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import connection.constants.CharacterNameResponseCode;
import connection.constants.LoginStatusCode;
import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import net.server.Server;
import net.server.channel.Channel;
import tools.Pair;
import tools.Randomizer;

public class CLogin {
   /**
    * Gets a login failed packet.
    * <p>     *
    *
    * @param code The reason logging in failed.
    * @return The login failed packet.
    */
   public static Packet getLoginFailed(LoginStatusCode code) {
      final OutPacket p = OutPacket.create(SendOpcode.LOGIN_STATUS);
      p.writeByte(code.getCode());
      // Next value could be 32 or 64, which overrides any LoginStatusCode to display.
      // This ID has been confirmed to be fraudulent. Or it has been suspended due to an ID relate dto it. If similar behavior is repeated and determined to be malicious, appropriate measures will be taken. The account is prohibited from access.
      p.writeByte(0);
      return p;
   }

   public static Packet getPermBan(byte reason) {
      final OutPacket p = OutPacket.create(SendOpcode.LOGIN_STATUS);
      p.writeByte(LoginStatusCode.BANNED.getCode()); // Account is banned
      p.writeByte(0);
      p.writeByte(reason);
      p.writeLong(CCommon.getTime(-1));

      return p;
   }

   public static Packet getTempBan(long timestampTill, byte reason) {
      final OutPacket p = OutPacket.create(SendOpcode.LOGIN_STATUS);
      p.writeByte(LoginStatusCode.BANNED.getCode());
      p.writeByte(0);
      p.writeByte(reason);
      p.writeLong(CCommon.getTime(
            timestampTill)); // Tempban date is handled as a 64-bit long, number of 100NS intervals since 1/1/1601. Lulz.
      return p;
   }

   /**
    * Gets a successful authentication packet.
    *
    * @param c
    * @return the successful authentication packet
    */
   public static Packet getAuthSuccess(MapleClient c) {
      Server.getInstance()
            .loadAccountCharacters(c);    // locks the login session until data is recovered from the cache or the DB.
      Server.getInstance()
            .loadAccountStorages(c);

      final OutPacket p = OutPacket.create(SendOpcode.LOGIN_STATUS);
      p.writeByte(LoginStatusCode.OK.getCode());
      p.writeByte(0);
      p.writeInt(c.getAccID());
      p.writeByte(c.getGender());

      boolean canFly = Server.getInstance()
            .canFly(c.getAccID());
      p.writeBool((YamlConfig.config.server.USE_ENFORCE_ADMIN_ACCOUNT || canFly)
            && c.getGMLevel() > 1);    // thanks Steve(kaito1410) for pointing the GM account boolean here
      p.writeByte(((YamlConfig.config.server.USE_ENFORCE_ADMIN_ACCOUNT || canFly) && c.getGMLevel() > 1) ? 0x80 :
            0);  // Admin Byte. 0x80,0x40,0x20.. Rubbish.
      p.writeString(c.getAccountName());
      p.writeString(c.getAccountName());
      p.writeByte(0);
      p.writeByte(0);
      p.writeByte(0);
      p.writeByte(0);
      p.writeByte(0); // could be something? sends a 0x19 packet which appears like a second authentication packet.
      p.writeByte(0);
      p.writeLong(0);
      p.writeString(c.getAccountName());
      return p;
   }

   public static Packet sendGuestTOS() {
      final OutPacket p = OutPacket.create(SendOpcode.GUEST_ID_LOGIN);
      p.writeShort(0x100);
      p.writeInt(Randomizer.nextInt(999999));
      p.writeLong(0);
      p.writeLong(CCommon.getTime(-2));
      p.writeLong(CCommon.getTime(System.currentTimeMillis()));
      p.writeInt(0);
      p.writeString("http://maplesolaxia.com");
      return p;
   }

   /**
    * Gets a packet detailing a server status message.
    * <p>
    * Possible values for <code>status</code>:<br> 0 - Normal<br> 1 - Highly
    * populated<br> 2 - Full
    *
    * @param status The server status.
    * @return The server status packet.
    */
   public static Packet getServerStatus(int status) {
      final OutPacket p = OutPacket.create(SendOpcode.SERVERSTATUS);
      p.writeShort(status);
      return p;
   }

   /**
    * Gets a packet detailing a PIN operation.
    * <p>
    * Possible values for <code>mode</code>:<br> 0 - PIN was accepted<br> 1 -
    * Register a new PIN<br> 2 - Invalid pin / Reenter<br> 3 - Connection
    * failed due to system error<br> 4 - Enter the pin
    *
    * @param mode The mode.
    * @return
    */
   public static Packet pinOperation(byte mode) {
      final OutPacket p = OutPacket.create(SendOpcode.CHECK_PINCODE);
      p.writeByte(mode);
      return p;
   }

   public static Packet pinRegistered() {
      final OutPacket p = OutPacket.create(SendOpcode.UPDATE_PINCODE);
      p.writeByte(0);
      return p;
   }

   public static Packet showAllCharacter(int chars, int unk) {
      final OutPacket p = OutPacket.create(SendOpcode.VIEW_ALL_CHAR);
      p.writeByte(chars > 0 ? 1 : 5); // 2: already connected to server, 3 : unk error (view-all-characters), 5 : cannot find any
      p.writeInt(chars);
      p.writeInt(unk);
      return p;
   }

   public static Packet showAllCharacterInfo(int worldid, List<MapleCharacter> chars, boolean usePic) {
      final OutPacket p = OutPacket.create(SendOpcode.VIEW_ALL_CHAR);
      p.writeByte(0);
      p.writeByte(worldid);
      p.writeByte(chars.size());
      for (MapleCharacter chr : chars) {
         addCharEntry(p, chr, true);
      }
      p.writeByte(usePic ? 1 : 2);
      return p;
   }

   /**
    * Gets a login failed packet.
    * <p>
    * Possible values for <code>reason</code>:<br> 2: ID deleted or blocked<br>
    * 3: ID deleted or blocked<br> 4: Incorrect password<br> 5: Not a
    * registered id<br> 6: Trouble logging into the game?<br> 7: Already logged
    * in<br> 8: Trouble logging into the game?<br> 9: Trouble logging into the
    * game?<br> 10: Cannot process so many connections<br> 11: Only users older
    * than 20 can use this channel<br> 12: Trouble logging into the game?<br>
    * 13: Unable to log on as master at this ip<br> 14: Wrong gateway or
    * personal info and weird korean button<br> 15: Processing request with
    * that korean button!<br> 16: Please verify your account through
    * email...<br> 17: Wrong gateway or personal info<br> 21: Please verify
    * your account through email...<br> 23: Crashes<br> 25: Maple Europe notice
    * =[ FUCK YOU NEXON<br> 27: Some weird full client notice, probably for
    * trial versions<br>
    *
    * @param reason The reason logging in failed.
    * @return The login failed packet.
    */
   public static Packet getAfterLoginError(int reason) {//same as above o.o
      final OutPacket p = OutPacket.create(SendOpcode.SELECT_CHARACTER_BY_VAC);
      p.writeShort(reason);//using other types than stated above = CRASH
      return p;
   }

   /**
    * Gets a packet detailing a server and its channels.
    *
    * @param serverId
    * @param serverName  The name of the server.
    * @param flag
    * @param eventmsg
    * @param channelLoad Load of the channel - 1200 seems to be max.
    * @return The server info packet.
    */
   public static Packet getServerList(int serverId, String serverName, int flag, String eventmsg, List<Channel> channelLoad) {
      final OutPacket p = OutPacket.create(SendOpcode.SERVERLIST);
      p.writeByte(serverId);
      p.writeString(serverName);
      p.writeByte(flag);
      p.writeString(eventmsg);
      p.writeShort(100); // rate modifier, don't ask O.O!
      p.writeShort(100); // rate modifier, don't ask O.O!
      p.writeByte(channelLoad.size());
      for (Channel ch : channelLoad) {
         p.writeString(serverName + "-" + ch.getId());
         p.writeInt(ch.getChannelCapacity());
         p.writeByte(1);// nWorldID
         p.writeByte(ch.getId() - 1);// nChannelID
         p.writeBool(false);// bAdultChannel
      }
      p.writeShort(0);
      return p;
   }

   /**
    * Gets a packet saying that the server list is over.
    *
    * @return The end of server list packet.
    */
   public static Packet getEndOfServerList() {
      final OutPacket p = OutPacket.create(SendOpcode.SERVERLIST);
      p.writeByte(0xFF);
      return p;
   }

   /**
    * Gets a packet with a list of characters.
    *
    * @param c        The MapleClient to load characters of.
    * @param serverId The ID of the server requested.
    * @param status   The charlist request result.
    * @return The character list packet.
    * <p>
    * Possible values for <code>status</code>:
    * <br> 2: ID deleted or blocked<br>
    * <br> 3: ID deleted or blocked<br>
    * <br> 4: Incorrect password<br>
    * <br> 5: Not an registered ID<br>
    * <br> 6: Trouble logging in?<br>
    * <br> 10: Server handling too many connections<br>
    * <br> 11: Only 20 years or older<br>
    * <br> 13: Unable to log as master at IP<br>
    * <br> 14: Wrong gateway or personal info<br>
    * <br> 15: Still processing request<br>
    * <br> 16: Verify account via email<br>
    * <br> 17: Wrong gateway or personal info<br>
    * <br> 21: Verify account via email<br>
    */
   public static Packet getCharList(MapleClient c, int serverId, int status) {
      final OutPacket p = OutPacket.create(SendOpcode.CHARLIST);
      p.writeByte(status);
      p.writeString("");

      List<MapleCharacter> chars = c.loadCharacters(serverId);
      p.writeByte(chars.size());
      for (MapleCharacter chr : chars) {
         addCharEntry(p, chr, false);
      }

      //TODO this looks wrong based on the decodes.
      p.writeShort(2);
      p.writeLong(c.getCharacterSlots()); // character slots
      return p;
   }

   /**
    * Gets a packet telling the client the IP of the channel server.
    *
    * @param inetAddr The InetAddress of the requested channel server.
    * @param port     The port the channel is on.
    * @param clientId The ID of the client.
    * @return The server IP packet.
    */
   public static Packet getServerIP(InetAddress inetAddr, int port, int clientId) {
      final OutPacket p = OutPacket.create(SendOpcode.SERVER_IP);
      p.writeShort(LoginStatusCode.OK.getCode());
      byte[] addr = inetAddr.getAddress();
      p.writeBytes(addr);
      p.writeShort(port);
      p.writeInt(clientId);
      p.writeByte(0);
      p.writeInt(0);
      return p;
   }

   public static Packet charNameResponse(String charname, boolean nameUsed) {
      final OutPacket p = OutPacket.create(SendOpcode.CHAR_NAME_RESPONSE);
      p.writeString(charname);
      p.writeByte(nameUsed ? CharacterNameResponseCode.ALREADY_REGISTERED.getCode() : CharacterNameResponseCode.OK.getCode());
      return p;
   }

   public static Packet addNewCharEntry(MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.ADD_NEW_CHAR_ENTRY);
      p.writeByte(0);
      addCharEntry(p, chr, false);
      return p;
   }

   /**
    * State :
    * 0x00 = success
    * 0x06 = Trouble logging into the game?
    * 0x09 = Unknown error
    * 0x0A = Could not be processed due to too many connection requests to the server.
    * 0x12 = invalid bday
    * 0x14 = incorrect pic
    * 0x16 = Cannot delete a guild master.
    * 0x18 = Cannot delete a character with a pending wedding.
    * 0x1A = Cannot delete a character with a pending world transfer.
    * 0x1D = Cannot delete a character that has a family.
    *
    * @param cid
    * @param state
    * @return
    */
   public static Packet deleteCharResponse(int cid, int state) {
      final OutPacket p = OutPacket.create(SendOpcode.DELETE_CHAR_RESPONSE);
      p.writeInt(cid);
      p.writeByte(state);
      return p;
   }

   /**
    * Gets the response to a relog request.
    *
    * @return The relog response packet.
    */
   public static Packet getRelogResponse() {
      final OutPacket p = OutPacket.create(SendOpcode.RELOG_RESPONSE);
      p.writeByte(1);//1 O.O Must be more types ):
      return p;
   }

   public static Packet selectWorld(int world) {
      final OutPacket p = OutPacket.create(SendOpcode.LAST_CONNECTED_WORLD);
      p.writeInt(world);//According to GMS, it should be the world that contains the most characters (most active)
      return p;
   }

   public static Packet sendRecommended(List<Pair<Integer, String>> worlds) {
      final OutPacket p = OutPacket.create(SendOpcode.RECOMMENDED_WORLD_MESSAGE);
      p.writeByte(worlds.size());//size
      for (Pair<Integer, String> world : worlds) {
         p.writeInt(world.getLeft());
         p.writeString(world.getRight());
      }
      return p;
   }

   public static Packet wrongPic() {
      final OutPacket p = OutPacket.create(SendOpcode.CHECK_SPW_RESULT);
      p.writeByte(0);
      return p;
   }

   public static void addCharEntry(OutPacket p, MapleCharacter chr, boolean viewall) {
      CCommon.addCharStats(p, chr);
      CCommon.addCharLook(p, chr, false);
      if (!viewall) {
         p.writeByte(0);
      }
      if (chr.isGM()
            || chr.isGmJob()) {  // thanks Daddy Egg (Ubaware), resinate for noticing GM jobs crashing on non-GM players account
         p.writeByte(0);
         return;
      }
      p.writeByte(1); // world rank enabled (next 4 ints are not sent if disabled) Short??
      p.writeInt(chr.getRank()); // world rank
      p.writeInt(chr.getRankMove()); // move (negative is downwards)
      p.writeInt(chr.getJobRank()); // job rank
      p.writeInt(chr.getJobRankMove()); // move (negative is downwards)
   }

   public static Packet requestPin() {
      return pinOperation((byte) 4);
   }

   public static Packet requestPinAfterFailure() {
      return pinOperation((byte) 2);
   }

   public static Packet registerPin() {
      return pinOperation((byte) 1);
   }

   public static Packet pinAccepted() {
      return pinOperation((byte) 0);
   }
}
