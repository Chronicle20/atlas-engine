package connection.packets;

import connection.headers.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import server.events.gm.MapleSnowball;

public class CFieldSnowBall {
   public static Packet rollSnowBall(boolean entermap, int state, MapleSnowball ball0, MapleSnowball ball1) {
      final OutPacket p = OutPacket.create(SendOpcode.SNOWBALL_STATE);
      if (entermap) {
         p.skip(21);
      } else {
         p.writeByte(state);// 0 = move, 1 = roll, 2 is down disappear, 3 is up disappear
         p.writeInt(ball0.getSnowmanHP() / 75);
         p.writeInt(ball1.getSnowmanHP() / 75);
         p.writeShort(ball0.getPosition());//distance snowball down, 84 03 = max
         p.writeByte(-1);
         p.writeShort(ball1.getPosition());//distance snowball up, 84 03 = max
         p.writeByte(-1);
      }
      return p;
   }

   public static Packet hitSnowBall(int what, int damage) {
      final OutPacket p = OutPacket.create(SendOpcode.HIT_SNOWBALL);
      p.writeByte(what);
      p.writeInt(damage);
      return p;
   }

   /**
    * Sends a Snowball Message<br>
    * <p>
    * Possible values for <code>message</code>:<br> 1: ... Team's snowball has
    * passed the stage 1.<br> 2: ... Team's snowball has passed the stage
    * 2.<br> 3: ... Team's snowball has passed the stage 3.<br> 4: ... Team is
    * attacking the snowman, stopping the progress<br> 5: ... Team is moving
    * again<br>
    *
    * @param message
    */
   public static Packet snowballMessage(int team, int message) {
      final OutPacket p = OutPacket.create(SendOpcode.SNOWBALL_MESSAGE);
      p.writeByte(team);// 0 is down, 1 is up
      p.writeInt(message);
      return p;
   }

   public static Packet leftKnockBack() {
      return OutPacket.create(SendOpcode.LEFT_KNOCK_BACK);
   }
}
