package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CMob;
import constants.game.GameConstants;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import server.life.MapleMonster;
import server.life.MonsterInformationProvider;
import server.maps.MapleMap;
import tools.FilePrinter;

public class FieldDamageMobHandler extends AbstractMaplePacketHandler {

   private static void performDamage(MapleClient c, int dmg, MapleMonster mob) {
      MapleMap map = c.getPlayer().getMap();
      if (dmg < 0 || dmg > GameConstants.MAX_FIELD_MOB_DAMAGE) {
         FilePrinter.printError(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt",
               c.getPlayer().getName() + " tried to use an obstacle on mapid " + map.getId() + " to attack "
                     + MonsterInformationProvider.getInstance().getMobNameFromId(mob.getId()) + " with damage " + dmg);
         return;
      }

      map.broadcastMessage(c.getPlayer(), CMob.damageMonster(mob.getObjectId(), dmg), true);
      map.damageMonster(c.getPlayer(), mob, dmg);
   }

   @Override
   public final void handlePacket(InPacket p, MapleClient c) {
      int objectId = p.readInt();
      int dmg = p.readInt();

      MapleCharacter chr = c.getPlayer();
      MapleMap map = chr.getMap();

      if (map.getEnvironment().isEmpty()) {   // no environment objects activated to actually hit the mob
         FilePrinter.printError(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt",
               c.getPlayer().getName() + " tried to use an obstacle on mapid " + map.getId() + " to attack.");
         return;
      }

      map.getMonsterByOid(objectId).ifPresent(m -> performDamage(c, dmg, m));
   }
}
