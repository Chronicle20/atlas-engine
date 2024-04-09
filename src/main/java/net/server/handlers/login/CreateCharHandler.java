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
package net.server.handlers.login;

import client.MapleClient;
import client.creator.novice.BeginnerCreator;
import client.creator.novice.LegendCreator;
import client.creator.novice.NoblesseCreator;
import connection.packets.CLogin;
import constants.character.creation.TemplateFactory;
import net.AbstractMaplePacketHandler;
import tools.FilePrinter;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class CreateCharHandler extends AbstractMaplePacketHandler {

    private final static Set<Integer> IDs = new HashSet<>(Arrays.asList(1302000, 1312004, 1322005, 1442079,// weapons
            1040002, 1040006, 1040010, 1041002, 1041006, 1041010, 1041011, 1042167,// bottom
            1060002, 1060006, 1061002, 1061008, 1062115, // top
            1072001, 1072005, 1072037, 1072038, 1072383,// shoes
            30000, 30010, 30020, 30030, 31000, 31040, 31050,// hair
            20000, 20001, 20002, 21000, 21001, 21002, 21201, 20401, 20402, 21700, 20100  //face
            //#NeverTrustStevenCode
    ));

    private static boolean isLegal(Integer toCompare) {
        return IDs.contains(toCompare);
    }


    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String name = slea.readMapleAsciiString();
        int job = slea.readInt();
        short nSubJob = slea.readShort();

        int face = slea.readInt();
        if (!TemplateFactory.getInstance()
                .validFace(face)) {
            packetError(c, name, String.format("Account [%s] attempted to use a [%s] : [%d] that should not be available during character creation. Possible packet exploit.", c.getAccountName(), "FACE", face));
            return;
        }

        int hair = slea.readInt();
        if (!TemplateFactory.getInstance()
                .validHair(hair)) {
            packetError(c, name, String.format("Account [%s] attempted to use a [%s] : [%d] that should not be available during character creation. Possible packet exploit.", c.getAccountName(), "HAIR", hair));
            return;
        }

        int top = slea.readInt();
        if (!TemplateFactory.getInstance()
                .validTop(top)) {
            packetError(c, name, String.format("Account [%s] attempted to use a [%s] : [%d] that should not be available during character creation. Possible packet exploit.", c.getAccountName(), "TOP", top));
            return;
        }

        int bottom = slea.readInt();
        if (!TemplateFactory.getInstance()
                .validBottom(bottom)) {
            packetError(c, name, String.format("Account [%s] attempted to use a [%s] : [%d] that should not be available during character creation. Possible packet exploit.", c.getAccountName(), "BOTTOM", bottom));
            return;
        }

        int shoes = slea.readInt();
        if (!TemplateFactory.getInstance()
                .validShoe(shoes)) {
            packetError(c, name, String.format("Account [%s] attempted to use a [%s] : [%d] that should not be available during character creation. Possible packet exploit.", c.getAccountName(), "SHOE", shoes));
            return;
        }

        int weapon = slea.readInt();
        if (!TemplateFactory.getInstance()
                .validWeapon(weapon)) {
            packetError(c, name, String.format("Account [%s] attempted to use a [%s] : [%d] that should not be available during character creation. Possible packet exploit.", c.getAccountName(), "WEAPON", weapon));
            return;
        }

        int status;
        if (job == 0) { // Knights of Cygnus
            status = NoblesseCreator.createCharacter(c, name, face, hair, 0, top, bottom, shoes, weapon, c.getGender());
        } else if (job == 1) { // Adventurer
            status = BeginnerCreator.createCharacter(c, name, face, hair, 0, top, bottom, shoes, weapon, c.getGender());
        } else if (job == 2) { // Aran
            status = LegendCreator.createCharacter(c, name, face, hair, 0, top, bottom, shoes, weapon, c.getGender());
        } else {
            c.announce(CLogin.deleteCharResponse(0, 9));
            return;
        }

        if (status == -2) {
            c.announce(CLogin.deleteCharResponse(0, 9));
        }
    }

    private void packetError(MapleClient c, String name, String reason) {
        FilePrinter.printError(FilePrinter.EXPLOITS + name + ".txt", reason);
        c.disconnect(true, false);
    }
}