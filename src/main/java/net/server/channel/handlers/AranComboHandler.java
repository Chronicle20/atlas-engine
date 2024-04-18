package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.SkillFactory;
import constants.skills.Aran;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public class AranComboHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      final MapleCharacter player = c.getPlayer();
      int skillLevel = player.getSkillLevel(Aran.COMBO_ABILITY);

      if (!player.isAran() || (skillLevel <= 0 && player.getJob().getId() != 2000)) {
         return;
      }

      final long currentTime = currentServerTime();
      short combo = player.getCombo();
      if ((currentTime - player.getLastCombo()) > 3000 && combo > 0) {
         combo = 0;
      }
      combo++;
      short finalCombo = combo;
      switch (combo) {
         case 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 -> {
            if (player.getJob().getId() != 2000 && (combo / 10) > skillLevel) {
               break;
            }
            SkillFactory.getSkill(Aran.COMBO_ABILITY).map(s -> s.getEffect(finalCombo / 10))
                  .ifPresent(e -> e.applyComboBuff(player, finalCombo));
         }
      }
      player.setCombo(combo);
      player.setLastCombo(currentTime);
   }
}
