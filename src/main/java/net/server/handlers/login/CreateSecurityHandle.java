package net.server.handlers.login;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.Random;

public class CreateSecurityHandle extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.announce(writePacket(slea, c));
    }

    @Override
    public boolean validateState(MapleClient c) {
        return true;
    }

    private byte[] writePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String[] LoginScreen = {"MapLogin", "MapLogin1"};

        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);
        mplew.writeShort(connection.constants.SendOpcode.LOGIN_AUTH.getValue());
        int index = new Random().nextInt(2);
        mplew.writeMapleAsciiString(LoginScreen[index]);
        return mplew.getPacket();
    }
}
