package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class CancelDebuffHandler extends AbstractMaplePacketHandler {//TIP: BAD STUFF LOL!

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
        /*List<MapleDisease> diseases = c.getPlayer().getDiseases();
         List<MapleDisease> diseases_ = new ArrayList<MapleDisease>();
         for (MapleDisease disease : diseases) {
         List<MapleDisease> disease_ = new ArrayList<MapleDisease>();
         disease_.add(disease);
         diseases_.add(disease);
         c.sendPacket(MaplePacketCreator.cancelDebuff(disease_));
         c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.cancelForeignDebuff(c.getPlayer().getId(), disease_), false);
         }
         for (MapleDisease disease : diseases_) {
         c.getPlayer().removeDisease(disease);
         }*/
   }
}