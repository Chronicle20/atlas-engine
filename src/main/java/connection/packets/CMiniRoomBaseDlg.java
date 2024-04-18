package connection.packets;

import java.util.List;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import net.server.channel.handlers.PlayerInteractionHandler;
import server.MapleTrade;
import server.maps.MapleHiredMerchant;
import server.maps.MapleMiniGame;
import server.maps.MaplePlayerShop;
import server.maps.MaplePlayerShopItem;
import tools.Pair;

public class CMiniRoomBaseDlg {
   public static Packet getPlayerShopChat(MapleCharacter chr, String chat, boolean owner) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.CHAT.getCode());
      p.writeByte(PlayerInteractionHandler.Action.CHAT_THING.getCode());
      p.writeByte(owner ? 0 : 1);
      p.writeString(chr.getName() + " : " + chat);
      return p;
   }

   public static Packet getPlayerShopNewVisitor(MapleCharacter chr, int slot) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.VISIT.getCode());
      p.writeByte(slot);
      CCommon.addCharLook(p, chr, false);
      p.writeString(chr.getName());
      return p;
   }

   public static Packet getPlayerShopRemoveVisitor(int slot) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.EXIT.getCode());
      if (slot != 0) {
         p.writeShort(slot);
      }
      return p;
   }

   public static Packet getTradePartnerAdd(MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.VISIT.getCode());
      p.writeByte(1);
      CCommon.addCharLook(p, chr, false);
      p.writeString(chr.getName());
      return p;
   }

   public static Packet tradeInvite(MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.INVITE.getCode());
      p.writeByte(3);
      p.writeString(chr.getName());
      p.writeBytes(new byte[]{(byte) 0xB7, (byte) 0x50, 0, 0});
      return p;
   }

   public static Packet getTradeMesoSet(byte number, int meso) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.SET_MESO.getCode());
      p.writeByte(number);
      p.writeInt(meso);
      return p;
   }

   public static Packet getTradeItemAdd(byte number, Item item) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.SET_ITEMS.getCode());
      p.writeByte(number);
      p.writeByte(item.getPosition());
      CCommon.addItemInfo(p, item, true);
      return p;
   }

   public static Packet getPlayerShopItemUpdate(MaplePlayerShop shop) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.UPDATE_MERCHANT.getCode());
      p.writeByte(shop.getItems()
            .size());
      for (MaplePlayerShopItem item : shop.getItems()) {
         p.writeShort(item.getBundles());
         p.writeShort(item.getItem()
               .getQuantity());
         p.writeInt(item.getPrice());
         CCommon.addItemInfo(p, item.getItem(), true);
      }
      return p;
   }

   public static Packet getPlayerShopOwnerUpdate(MaplePlayerShop.SoldItem item, int position) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.UPDATE_PLAYERSHOP.getCode());
      p.writeByte(position);
      p.writeShort(item.quantity());
      p.writeString(item.buyer());

      return p;
   }

   public static Packet getPlayerShop(MaplePlayerShop shop, boolean owner) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.ROOM.getCode());
      p.writeByte(4);
      p.writeByte(4);
      p.writeByte(owner ? 0 : 1);

      if (owner) {
         List<MaplePlayerShop.SoldItem> sold = shop.getSold();
         p.writeByte(sold.size());
         for (MaplePlayerShop.SoldItem s : sold) {
            p.writeInt(s.itemid());
            p.writeShort(s.quantity());
            p.writeInt(s.mesos());
            p.writeString(s.buyer());
         }
      } else {
         p.writeByte(0);
      }

      CCommon.addCharLook(p, shop.getOwner(), false);
      p.writeString(shop.getOwner()
            .getName());

      MapleCharacter[] visitors = shop.getVisitors();
      for (int i = 0; i < 3; i++) {
         if (visitors[i] != null) {
            p.writeByte(i + 1);
            CCommon.addCharLook(p, visitors[i], false);
            p.writeString(visitors[i].getName());
         }
      }

      p.writeByte(0xFF);
      p.writeString(shop.getDescription());
      List<MaplePlayerShopItem> items = shop.getItems();
      p.writeByte(0x10);  //TODO SLOTS, which is 16 for most stores...slotMax
      p.writeByte(items.size());
      for (MaplePlayerShopItem item : items) {
         p.writeShort(item.getBundles());
         p.writeShort(item.getItem()
               .getQuantity());
         p.writeInt(item.getPrice());
         CCommon.addItemInfo(p, item.getItem(), true);
      }
      return p;
   }

   public static Packet getTradeStart(MapleClient c, MapleTrade trade, byte number) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.ROOM.getCode());
      p.writeByte(3);
      p.writeByte(2);
      p.writeByte(number);
      if (number == 1) {
         p.writeByte(0);
         CCommon.addCharLook(p, trade.getPartner()
               .getChr(), false);
         p.writeString(trade.getPartner()
               .getChr()
               .getName());
      }
      p.writeByte(number);
      CCommon.addCharLook(p, c.getPlayer(), false);
      p.writeString(c.getPlayer()
            .getName());
      p.writeByte(0xFF);
      return p;
   }

   public static Packet getTradeConfirmation() {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.CONFIRM.getCode());
      return p;
   }

   /**
    * Possible values for <code>operation</code>:<br> 2: Trade cancelled, by the
    * other character<br> 7: Trade successful<br> 8: Trade unsuccessful<br>
    * 9: Cannot carry more one-of-a-kind items<br> 12: Cannot trade on different maps<br>
    * 13: Cannot trade, game files damaged<br>
    *
    * @param number
    * @param operation
    * @return
    */
   public static Packet getTradeResult(byte number, byte operation) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.EXIT.getCode());
      p.writeByte(number);
      p.writeByte(operation);
      return p;
   }

   public static Packet getMiniGame(MapleClient c, MapleMiniGame minigame, boolean owner, int piece) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.ROOM.getCode());
      p.writeByte(1);
      p.writeByte(0);
      p.writeByte(owner ? 0 : 1);
      p.writeByte(0);
      CCommon.addCharLook(p, minigame.getOwner(), false);
      p.writeString(minigame.getOwner()
            .getName());
      minigame.getVisitor()
            .ifPresent(v -> writeMiniGameVisitorLook(p, v));
      p.writeByte(0xFF);
      p.writeByte(0);
      p.writeInt(1);
      p.writeInt(minigame.getOwner()
            .getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, true));
      p.writeInt(minigame.getOwner()
            .getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, true));
      p.writeInt(minigame.getOwner()
            .getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, true));
      p.writeInt(minigame.getOwnerScore());
      minigame.getVisitor()
            .ifPresent(v -> writeMiniGameVisitorScore(minigame, p, v, 1));
      p.writeByte(0xFF);
      p.writeString(minigame.getDescription());
      p.writeByte(piece);
      p.writeByte(0);
      return p;
   }

   public static Packet getMiniGameReady(MapleMiniGame game) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.READY.getCode());
      return p;
   }

   public static Packet getMiniGameUnReady(MapleMiniGame game) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.UN_READY.getCode());
      return p;
   }

   public static Packet getMiniGameStart(MapleMiniGame game, int loser) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.START.getCode());
      p.writeByte(loser);
      return p;
   }

   public static Packet getMiniGameSkipOwner(MapleMiniGame game) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.SKIP.getCode());
      p.writeByte(0x01);
      return p;
   }

   public static Packet getMiniGameRequestTie(MapleMiniGame game) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.REQUEST_TIE.getCode());
      return p;
   }

   public static Packet getMiniGameDenyTie(MapleMiniGame game) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.ANSWER_TIE.getCode());
      return p;
   }

   /**
    * 1 = Room already closed  2 = Can't enter due full cappacity 3 = Other requests at this minute
    * 4 = Can't do while dead 5 = Can't do while middle event 6 = This character unable to do it
    * 7, 20 = Not allowed to trade anymore 9 = Can only trade on same map 10 = May not open store near portal
    * 11, 14 = Can't start game here 12 = Can't open store at this channel 13 = Can't estabilish miniroom
    * 15 = Stores only an the free market 16 = Lists the rooms at FM (?) 17 = You may not enter this store
    * 18 = Owner undergoing store maintenance 19 = Unable to enter tournament room 21 = Not enough mesos to enter
    * 22 = Incorrect password
    *
    * @param status
    * @return
    */
   public static Packet getMiniRoomError(int status) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.ROOM.getCode());
      p.writeByte(0);
      p.writeByte(status);
      return p;
   }

   public static Packet getMiniGameSkipVisitor(MapleMiniGame game) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeShort(PlayerInteractionHandler.Action.SKIP.getCode());
      return p;
   }

   public static Packet getMiniGameMoveOmok(MapleMiniGame game, int move1, int move2, int move3) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.MOVE_OMOK.getCode());
      p.writeInt(move1);
      p.writeInt(move2);
      p.writeByte(move3);
      return p;
   }

   public static Packet getMiniGameNewVisitor(MapleMiniGame minigame, MapleCharacter chr, int slot) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.VISIT.getCode());
      p.writeByte(slot);
      CCommon.addCharLook(p, chr, false);
      p.writeString(chr.getName());
      p.writeInt(1);
      p.writeInt(chr.getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, true));
      p.writeInt(chr.getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, true));
      p.writeInt(chr.getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, true));
      p.writeInt(minigame.getVisitorScore());
      return p;
   }

   public static Packet getMiniGameRemoveVisitor() {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.EXIT.getCode());
      p.writeByte(1);
      return p;
   }

   public static Packet getMiniGameResult(MapleMiniGame game, int tie, int result, int forfeit) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.GET_RESULT.getCode());

      int matchResultType;
      if (tie == 0 && forfeit != 1) {
         matchResultType = 0;
      } else if (tie != 0) {
         matchResultType = 1;
      } else {
         matchResultType = 2;
      }

      p.writeByte(matchResultType);
      p.writeBool(result == 2); // host/visitor wins

      boolean omok = game.isOmok();
      if (matchResultType == 1) {
         p.writeByte(0);
         p.writeShort(0);
         p.writeInt(game.getOwner()
               .getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, omok)); // wins
         p.writeInt(game.getOwner()
               .getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, omok)); // ties
         p.writeInt(game.getOwner()
               .getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, omok)); // losses
         p.writeInt(game.getOwnerScore()); // points

         p.writeInt(0); // unknown
         p.writeInt(game.getVisitor()
               .map(v -> v.getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, omok))
               .orElse(0)); // wins
         p.writeInt(game.getVisitor()
               .map(v -> v.getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, omok))
               .orElse(0)); // ties
         p.writeInt(game.getVisitor()
               .map(v -> v.getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, omok))
               .orElse(0)); // losses
         p.writeInt(game.getVisitorScore()); // points
         p.writeByte(0);
      } else {
         p.writeInt(0);
         p.writeInt(game.getOwner()
               .getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, omok)); // wins
         p.writeInt(game.getOwner()
               .getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, omok)); // ties
         p.writeInt(game.getOwner()
               .getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, omok)); // losses
         p.writeInt(game.getOwnerScore()); // points
         p.writeInt(0);
         p.writeInt(game.getVisitor()
               .map(v -> v.getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, omok))
               .orElse(0)); // wins
         p.writeInt(game.getVisitor()
               .map(v -> v.getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, omok))
               .orElse(0)); // ties
         p.writeInt(game.getVisitor()
               .map(v -> v.getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, omok))
               .orElse(0)); // losses
         p.writeInt(game.getVisitorScore()); // points
      }

      return p;
   }

   public static Packet getMiniGameOwnerWin(MapleMiniGame game, boolean forfeit) {
      return getMiniGameResult(game, 0, 1, forfeit ? 1 : 0);
   }

   public static Packet getMiniGameVisitorWin(MapleMiniGame game, boolean forfeit) {
      return getMiniGameResult(game, 0, 2, forfeit ? 1 : 0);
   }

   public static Packet getMiniGameTie(MapleMiniGame game) {
      return getMiniGameResult(game, 1, 3, 0);
   }

   public static Packet getMiniGameClose(boolean visitor, int type) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.EXIT.getCode());
      p.writeBool(visitor);
      p.writeByte(type); /* 2 : CRASH 3 : The room has been closed 4 : You have left the room 5 : You have been expelled  */
      return p;
   }

   public static Packet getMatchCard(MapleClient c, MapleMiniGame minigame, boolean owner, int piece) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.ROOM.getCode());
      p.writeByte(2);
      p.writeByte(2);
      p.writeByte(owner ? 0 : 1);
      p.writeByte(0);
      CCommon.addCharLook(p, minigame.getOwner(), false);
      p.writeString(minigame.getOwner()
            .getName());
      minigame.getVisitor()
            .ifPresent(v -> writeMiniGameVisitorLook(p, v));
      p.writeByte(0xFF);
      p.writeByte(0);
      p.writeInt(2);
      p.writeInt(minigame.getOwner()
            .getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, false));
      p.writeInt(minigame.getOwner()
            .getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, false));
      p.writeInt(minigame.getOwner()
            .getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, false));

      //set vs
      p.writeInt(minigame.getOwnerScore());
      minigame.getVisitor()
            .ifPresent(v -> writeMiniGameVisitorScore(minigame, p, v, 2));
      p.writeByte(0xFF);
      p.writeString(minigame.getDescription());
      p.writeByte(piece);
      p.writeByte(0);
      return p;
   }

   public static Packet getMatchCardStart(MapleMiniGame game, int loser) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.START.getCode());
      p.writeByte(loser);

      int last;
      if (game.getMatchesToWin() > 10) {
         last = 30;
      } else if (game.getMatchesToWin() > 6) {
         last = 20;
      } else {
         last = 12;
      }

      p.writeByte(last);
      for (int i = 0; i < last; i++) {
         p.writeInt(game.getCardId(i));
      }
      return p;
   }

   public static Packet getMatchCardNewVisitor(MapleMiniGame minigame, MapleCharacter chr, int slot) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.VISIT.getCode());
      p.writeByte(slot);
      CCommon.addCharLook(p, chr, false);
      p.writeString(chr.getName());
      p.writeInt(1);
      p.writeInt(chr.getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, false));
      p.writeInt(chr.getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, false));
      p.writeInt(chr.getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, false));
      p.writeInt(minigame.getVisitorScore());
      return p;
   }

   public static Packet getMatchCardSelect(MapleMiniGame game, int turn, int slot, int firstslot, int type) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.SELECT_CARD.getCode());
      p.writeByte(turn);
      if (turn == 1) {
         p.writeByte(slot);
      } else if (turn == 0) {
         p.writeByte(slot);
         p.writeByte(firstslot);
         p.writeByte(type);
      }
      return p;
   }

   public static Packet getPlayerShopChat(MapleCharacter chr, String chat, byte slot) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.CHAT.getCode());
      p.writeByte(PlayerInteractionHandler.Action.CHAT_THING.getCode());
      p.writeByte(slot);
      p.writeString(chr.getName() + " : " + chat);
      return p;
   }

   public static Packet getTradeChat(MapleCharacter chr, String chat, boolean owner) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.CHAT.getCode());
      p.writeByte(PlayerInteractionHandler.Action.CHAT_THING.getCode());
      p.writeByte(owner ? 0 : 1);
      p.writeString(chr.getName() + " : " + chat);
      return p;
   }

   public static Packet getHiredMerchant(MapleCharacter chr, MapleHiredMerchant hm, boolean firstTime) {//Thanks Dustin
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.ROOM.getCode());
      p.writeByte(0x05);
      p.writeByte(0x04);
      p.writeShort(hm.getVisitorSlotThreadsafe(chr) + 1);
      p.writeInt(hm.getItemId());
      p.writeString("Hired Merchant");

      MapleCharacter[] visitors = hm.getVisitors();
      for (int i = 0; i < 3; i++) {
         if (visitors[i] != null) {
            p.writeByte(i + 1);
            CCommon.addCharLook(p, visitors[i], false);
            p.writeString(visitors[i].getName());
         }
      }
      p.writeByte(-1);
      if (hm.isOwner(chr)) {
         List<Pair<String, Byte>> msgList = hm.getMessages();

         p.writeShort(msgList.size());
         for (Pair<String, Byte> stringBytePair : msgList) {
            p.writeString(stringBytePair
                  .getLeft());
            p.writeByte(stringBytePair
                  .getRight());
         }
      } else {
         p.writeShort(0);
      }
      p.writeString(hm.getOwner());
      if (hm.isOwner(chr)) {
         p.writeShort(0);
         p.writeShort(hm.getTimeOpen());
         p.writeByte(firstTime ? 1 : 0);
         List<MapleHiredMerchant.SoldItem> sold = hm.getSold();
         p.writeByte(sold.size());
         for (MapleHiredMerchant.SoldItem s : sold) {
            p.writeInt(s.itemid());
            p.writeShort(s.quantity());
            p.writeInt(s.mesos());
            p.writeString(s.buyer());
         }
         p.writeInt(chr.getMerchantMeso());//:D?
      }
      p.writeString(hm.getDescription());
      p.writeByte(0x10); //TODO SLOTS, which is 16 for most stores...slotMax
      p.writeInt(hm.isOwner(chr) ? chr.getMerchantMeso() : chr.getMeso());
      p.writeByte(hm.getItems()
            .size());
      if (hm.getItems()
            .isEmpty()) {
         p.writeByte(0);//Hmm??
      } else {
         for (MaplePlayerShopItem item : hm.getItems()) {
            p.writeShort(item.getBundles());
            p.writeShort(item.getItem()
                  .getQuantity());
            p.writeInt(item.getPrice());
            CCommon.addItemInfo(p, item.getItem(), true);
         }
      }
      return p;
   }

   public static Packet updateHiredMerchant(MapleHiredMerchant hm, MapleCharacter chr) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.UPDATE_MERCHANT.getCode());
      p.writeInt(hm.isOwner(chr) ? chr.getMerchantMeso() : chr.getMeso());
      p.writeByte(hm.getItems()
            .size());
      for (MaplePlayerShopItem item : hm.getItems()) {
         p.writeShort(item.getBundles());
         p.writeShort(item.getItem()
               .getQuantity());
         p.writeInt(item.getPrice());
         CCommon.addItemInfo(p, item.getItem(), true);
      }
      return p;
   }

   public static Packet hiredMerchantChat(String message, byte slot) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.CHAT.getCode());
      p.writeByte(PlayerInteractionHandler.Action.CHAT_THING.getCode());
      p.writeByte(slot);
      p.writeString(message);
      return p;
   }

   public static Packet hiredMerchantVisitorLeave(int slot) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.EXIT.getCode());
      if (slot != 0) {
         p.writeByte(slot);
      }
      return p;
   }

   public static Packet hiredMerchantOwnerLeave() {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.REAL_CLOSE_MERCHANT.getCode());
      p.writeByte(0);
      return p;
   }

   public static Packet hiredMerchantOwnerMaintenanceLeave() {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.REAL_CLOSE_MERCHANT.getCode());
      p.writeByte(5);
      return p;
   }

   public static Packet hiredMerchantMaintenanceMessage() {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.ROOM.getCode());
      p.writeByte(0x00);
      p.writeByte(0x12);
      return p;
   }

   public static Packet leaveHiredMerchant(int slot, int status2) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.EXIT.getCode());
      p.writeByte(slot);
      p.writeByte(status2);
      return p;
   }

   public static Packet hiredMerchantVisitorAdd(MapleCharacter chr, int slot) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(PlayerInteractionHandler.Action.VISIT.getCode());
      p.writeByte(slot);
      CCommon.addCharLook(p, chr, false);
      p.writeString(chr.getName());
      return p;
   }

   public static Packet shopErrorMessage(int error, int type) {
      final OutPacket p = OutPacket.create(SendOpcode.PLAYER_INTERACTION);
      p.writeByte(0x0A);
      p.writeByte(type);
      p.writeByte(error);
      return p;
   }

   private static void writeMiniGameVisitorLook(OutPacket p, MapleCharacter visitor) {
      p.writeByte(1);
      CCommon.addCharLook(p, visitor, false);
      p.writeString(visitor.getName());
   }

   private static void writeMiniGameVisitorScore(MapleMiniGame minigame, OutPacket p,
                                                 MapleCharacter visitor, int mode) {
      p.writeByte(1);
      p.writeInt(mode);
      p.writeInt(visitor.getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, true));
      p.writeInt(visitor.getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, true));
      p.writeInt(visitor.getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, true));
      p.writeInt(minigame.getVisitorScore());
   }
}
