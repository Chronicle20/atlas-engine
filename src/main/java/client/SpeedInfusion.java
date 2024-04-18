package client;

import net.packet.OutPacket;

public class SpeedInfusion extends TwoStateTemporaryStat {
   public int tCurrentTime;

   public SpeedInfusion() {
      super(false);
      tCurrentTime = 0;
      usExpireTerm = 0;
   }

   @Override
   public void EncodeForClient(OutPacket p) {
      super.EncodeForClient(p);
      p.writeTime(tCurrentTime);
      p.writeShort(usExpireTerm);
   }
}
