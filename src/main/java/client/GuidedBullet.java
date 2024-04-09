package client;

import tools.data.output.LittleEndianWriter;

public class GuidedBullet extends TemporaryStatBase {
    public int dwMobId;

    public GuidedBullet() {
        super(false);
        this.dwMobId = 0;
    }

    @Override
    public void EncodeForClient(LittleEndianWriter lew) {
        super.EncodeForClient(lew);
        lew.writeInt(dwMobId);
    }
}
