/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
    Copyleft (L) 2016 - 2019 RonanLana

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

/*
   @Author: Arthur L - Refactored command content into modules
*/
package client.command.commands.gm3;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.command.Command;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;

import java.util.List;

public class DebuffCommand extends Command {
    {
        setDescription("");
    }

    @Override
    public void execute(MapleClient c, String[] params) {
        MapleCharacter player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !debuff SLOW|SEDUCE|ZOMBIFY|CONFUSE|STUN|POISON|SEAL|DARKNESS|WEAKEN|CURSE");
            return;
        }

        MapleDisease disease = null;
        MobSkill skill = switch (params[0].toUpperCase()) {
           case "SLOW" -> {
              disease = MapleDisease.SLOW;
              yield MobSkillFactory.getMobSkill(126, 7);
           }
           case "SEDUCE" -> {
              disease = MapleDisease.SEDUCE;
              yield MobSkillFactory.getMobSkill(128, 7);
           }
           case "ZOMBIFY" -> {
              disease = MapleDisease.ZOMBIFY;
              yield MobSkillFactory.getMobSkill(133, 1);
           }
           case "CONFUSE" -> {
              disease = MapleDisease.CONFUSE;
              yield MobSkillFactory.getMobSkill(132, 2);
           }
           case "STUN" -> {
              disease = MapleDisease.STUN;
              yield MobSkillFactory.getMobSkill(123, 7);
           }
           case "POISON" -> {
              disease = MapleDisease.POISON;
              yield MobSkillFactory.getMobSkill(125, 5);
           }
           case "SEAL" -> {
              disease = MapleDisease.SEAL;
              yield MobSkillFactory.getMobSkill(120, 1);
           }
           case "DARKNESS" -> {
              disease = MapleDisease.DARKNESS;
              yield MobSkillFactory.getMobSkill(121, 1);
           }
           case "WEAKEN" -> {
              disease = MapleDisease.WEAKEN;
              yield MobSkillFactory.getMobSkill(122, 1);
           }
           case "CURSE" -> {
              disease = MapleDisease.CURSE;
              yield MobSkillFactory.getMobSkill(124, 1);
           }
           default -> null;
        };

       if (disease == null) {
            player.yellowMessage("Syntax: !debuff SLOW|SEDUCE|ZOMBIFY|CONFUSE|STUN|POISON|SEAL|DARKNESS|WEAKEN|CURSE");
            return;
        }

        for (MapleMapObject mmo : player.getMap().getMapObjectsInRange(player.getPosition(), 777777.7, List.of(MapleMapObjectType.PLAYER))) {
            MapleCharacter chr = (MapleCharacter) mmo;

            if (chr.getId() != player.getId()) {
                chr.giveDebuff(disease, skill);
            }
        }
    }
}
