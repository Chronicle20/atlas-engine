package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

public final class CharInfoRequestHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int updateTime = slea.readInt();
        int characterId = slea.readInt();
        byte petInfo = slea.readByte();
        c.getPlayer().getMap().getCharacterByOid(characterId).ifPresent(t -> showInfo(c, t));
    }

    private static void showInfo(MapleClient c, MapleCharacter target) {
        if (c.getPlayer().getId() != target.getId()) {
            target.exportExcludedItems(c);
        }
        c.announce(CWvsContext.charInfo(target));
    }
}
