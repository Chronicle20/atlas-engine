package connection.packets;

import connection.constants.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

public class CFieldTournament {
   public static Packet Tournament__Tournament(byte nState, byte nSubState) {
      final OutPacket p = OutPacket.create(SendOpcode.TOURNAMENT);
      p.writeByte(nState);
      p.writeByte(nSubState);
      return p;
   }

   public static Packet Tournament__MatchTable(byte nState, byte nSubState) {
      return OutPacket.create(SendOpcode.TOURNAMENT_MATCH_TABLE);
   }

   public static Packet Tournament__SetPrize(byte bSetPrize, byte bHasPrize, int nItemID1, int nItemID2) {
      final OutPacket p = OutPacket.create(SendOpcode.TOURNAMENT_SET_PRIZE);
      //0 = "You have failed the set the prize. Please check the item number again."
      //1 = "You have successfully set the prize."
      p.writeByte(bSetPrize);
      p.writeByte(bHasPrize);

      if (bHasPrize != 0) {
         p.writeInt(nItemID1);
         p.writeInt(nItemID2);
      }
      return p;
   }

   public static Packet Tournament__UEW(byte nState) {
      final OutPacket p = OutPacket.create(SendOpcode.TOURNAMENT_UEW);
      //Is this a bitflag o.o ?
      //2 = "You have reached the finals by default."
      //4 = "You have reached the semifinals by default."
      //8 or 16 = "You have reached the round of %n by default." | Encodes nState as %n ?!
      p.writeByte(nState);
      return p;
   }
}
