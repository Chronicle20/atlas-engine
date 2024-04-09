/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import server.maps.MapleHiredMerchant;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Optional;

/**
 * @author kevintjuh93 - :3
 */
public class RemoteStoreHandler extends AbstractMaplePacketHandler {
    private static Optional<MapleHiredMerchant> getMerchant(MapleClient c) {
        if (c.getPlayer().hasMerchant()) {
            return c.getWorldServer().getHiredMerchant(c.getPlayer().getId());
        }
        return Optional.empty();
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        Optional<MapleHiredMerchant> hm = getMerchant(c);
        if (hm.isEmpty() || !hm.get().isOwner(chr)) {
            chr.dropMessage(1, "You don't have a Merchant open.");
            c.announce(CWvsContext.enableActions());
            return;
        }

        if (hm.get().getChannel() == chr.getClient().getChannel()) {
            hm.get().visitShop(chr);
        } else {
            c.announce(CWvsContext.remoteChannelChange((byte) (hm.get().getChannel() - 1)));
        }
    }
}