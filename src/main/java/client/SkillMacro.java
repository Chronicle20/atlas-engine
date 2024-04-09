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
package client;

public record SkillMacro(int skill1, int skill2, int skill3, String name, int shout, int position) {

    public SkillMacro setSkill1(int skill) {
        return new SkillMacro(skill, skill2, skill3, name, shout, position);
    }

    public SkillMacro setSkill2(int skill) {
        return new SkillMacro(skill1, skill, skill3, name, shout, position);
    }

    public SkillMacro setSkill3(int skill) {
        return new SkillMacro(skill1, skill2, skill, name, shout, position);
    }
}
