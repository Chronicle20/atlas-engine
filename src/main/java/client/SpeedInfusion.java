package client;

import tools.data.output.LittleEndianWriter;

public class SpeedInfusion extends TwoStateTemporaryStat {
    public int tCurrentTime;

    public SpeedInfusion() {
        super(false);
        tCurrentTime = 0;
        usExpireTerm = 0;
    }

    @Override
    public void EncodeForClient(LittleEndianWriter lew) {
        super.EncodeForClient(lew);
        lew.writeTime(tCurrentTime);
        lew.writeShort(usExpireTerm);
    }
}
