package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.TemporaryStatType;
import net.packet.InPacket;

public final class TouchMonsterDamageHandler extends AbstractDealDamageHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      MapleCharacter chr = c.getPlayer();
      if (chr.getEnergyBar() == 15000 || chr.getBuffedValue(TemporaryStatType.BODY_PRESSURE) != null) {
         applyAttack(parseDamage(p, chr, false, false), c.getPlayer(), 1);
      }
   }
}
