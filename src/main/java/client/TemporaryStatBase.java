package client;

import tools.data.output.LittleEndianWriter;

public class TemporaryStatBase {
    public final boolean bDynamicTermSet;
    public int nOption;
    public int rOption;
    public long tLastUpdated;
    public int usExpireTerm;

    public TemporaryStatBase(boolean bDynamicTermSet) {
        this.nOption = 0;
        this.rOption = 0;
        this.tLastUpdated = System.currentTimeMillis();
        this.bDynamicTermSet = bDynamicTermSet;
    }

    public TemporaryStatBase(int nOption, int rOption, boolean bDynamicTermSet) {
        this.nOption = nOption;
        this.rOption = rOption;
        this.tLastUpdated = System.currentTimeMillis();
        this.bDynamicTermSet = bDynamicTermSet;
    }

    public void EncodeForClient(LittleEndianWriter lew) {
        lew.writeInt(nOption);
        lew.writeInt(rOption);
        lew.writeTime(tLastUpdated);
        if (bDynamicTermSet) {
            lew.writeShort(usExpireTerm);
        }
    }
}
