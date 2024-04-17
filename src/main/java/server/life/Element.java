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
package server.life;

public enum Element {
    NEUTRAL(0), PHYSICAL(1), FIRE(2, true), ICE(3, true), LIGHTING(4), POISON(5), HOLY(6, true), DARKNESS(7);

    private int value;
    private boolean special = false;

    Element(int v) {
        this.value = v;
    }

    Element(int v, boolean special) {
        this.value = v;
        this.special = special;
    }

    public static Element getFromChar(char c) {
       return switch (Character.toUpperCase(c)) {
          case 'F' -> FIRE;
          case 'I' -> ICE;
          case 'L' -> LIGHTING;
          case 'S' -> POISON;
          case 'H' -> HOLY;
          case 'D' -> DARKNESS;
          case 'P' -> NEUTRAL;
          default -> throw new IllegalArgumentException("unknown elemnt char " + c);
       };
    }

    public boolean isSpecial() {
        return special;
    }

    public int getValue() {
        return value;
    }
}
