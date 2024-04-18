package client;

import net.packet.OutPacket;

public class GuidedBullet extends TemporaryStatBase {
   public int dwMobId;

   public GuidedBullet() {
      super(false);
      this.dwMobId = 0;
   }

   @Override
   public void EncodeForClient(OutPacket p) {
      super.EncodeForClient(p);
      p.writeInt(dwMobId);
   }
}
