package net.server.channel.handlers;

import java.util.Optional;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import net.server.coordinator.world.MapleInviteCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator.InviteType;

public final class FamilyAddHandler extends AbstractMaplePacketHandler {
   private static void handlePacket(MapleClient c, MapleCharacter addChr) {
      MapleCharacter chr = c.getPlayer();
      if (addChr == chr) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }

      if (addChr.getMap() != chr.getMap() || (addChr.isHidden()) && chr.gmLevel() < addChr.gmLevel()) {
         c.sendPacket(CWvsContext.sendFamilyMessage(69, 0));
         return;
      }

      if (addChr.getLevel() <= 10) {
         c.sendPacket(CWvsContext.sendFamilyMessage(77, 0));
         return;
      }

      if (Math.abs(addChr.getLevel() - chr.getLevel()) > 20) {
         c.sendPacket(CWvsContext.sendFamilyMessage(72, 0));
         return;
      }

      if (addChr.getFamily().isPresent() && addChr.getFamily() == chr.getFamily()) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }

      if (MapleInviteCoordinator.hasInvite(InviteType.FAMILY, addChr.getId())) {
         c.sendPacket(CWvsContext.sendFamilyMessage(73, 0));
         return;
      }

      if (chr.getFamily().isPresent() && addChr.getFamily().isPresent()
            && addChr.getFamily().get().getTotalGenerations() + chr.getFamily().get().getTotalGenerations()
            > YamlConfig.config.server.FAMILY_MAX_GENERATIONS) {
         c.sendPacket(CWvsContext.sendFamilyMessage(76, 0));
         return;
      }

      MapleInviteCoordinator.createInvite(InviteType.FAMILY, chr, addChr, addChr.getId());
      addChr.sendPacket(CWvsContext.sendFamilyInvite(chr.getId(), chr.getName()));
      chr.dropMessage("The invite has been sent.");
      c.sendPacket(CWvsContext.enableActions());
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (!YamlConfig.config.server.USE_FAMILY_SYSTEM) {
         return;
      }
      String toAdd = p.readString();
      Optional<MapleCharacter> addChr = c.getChannelServer().getPlayerStorage().getCharacterByName(toAdd);
      if (addChr.isEmpty()) {
         c.sendPacket(CWvsContext.sendFamilyMessage(65, 0));
         return;
      }

      handlePacket(c, addChr.get());
   }
}
