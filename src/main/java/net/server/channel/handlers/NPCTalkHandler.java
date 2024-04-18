package net.server.channel.handlers;

import client.MapleClient;
import client.processor.npc.DueyProcessor;
import config.YamlConfig;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import scripting.npc.NPCScriptManager;
import server.life.MapleNPC;
import server.life.MaplePlayerNPC;
import server.maps.MapleMapObject;
import tools.FilePrinter;

public final class NPCTalkHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      if (!c.getPlayer().isAlive()) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }

      if (currentServerTime() - c.getPlayer().getNpcCooldown() < YamlConfig.config.server.BLOCK_NPC_RACE_CONDT) {
         c.sendPacket(CWvsContext.enableActions());
         return;
      }

      int oid = p.readInt();
      MapleMapObject obj = c.getPlayer().getMap().getMapObject(oid).orElse(null);
      if (obj instanceof MapleNPC npc) {
         if (YamlConfig.config.server.USE_DEBUG == true) {
            c.getPlayer().dropMessage(5, "Talking to NPC " + npc.getId());
         }

         if (npc.getId() == 9010009) {   //is duey
            DueyProcessor.dueySendTalk(c, false);
         } else {
            if (c.getCM() != null || c.getQM() != null) {
               c.sendPacket(CWvsContext.enableActions());
               return;
            }

            // Custom handling to reduce the amount of scripts needed.
            if (npc.getId() >= 9100100 && npc.getId() <= 9100200) {
               NPCScriptManager.getInstance().start(c, npc.getId(), "gachapon", null);
            } else if (npc.getName().endsWith("Maple TV")) {
               NPCScriptManager.getInstance().start(c, npc.getId(), "mapleTV", null);
            } else {
               boolean hasNpcScript = NPCScriptManager.getInstance().start(c, npc.getId(), oid, null);
               if (!hasNpcScript) {
                  if (!npc.hasShop()) {
                     FilePrinter.printError(FilePrinter.NPC_UNCODED,
                           "NPC " + npc.getName() + "(" + npc.getId() + ") is not coded.");
                     return;
                  } else if (c.getPlayer().getShop() != null) {
                     c.sendPacket(CWvsContext.enableActions());
                     return;
                  }

                  npc.sendShop(c);
               }
            }
         }
      } else if (obj instanceof MaplePlayerNPC pnpc) {
         NPCScriptManager nsm = NPCScriptManager.getInstance();

         if (pnpc.getScriptId() < 9977777 && !nsm.isNpcScriptAvailable(c, "" + pnpc.getScriptId())) {
            nsm.start(c, pnpc.getScriptId(), "rank_user", null);
         } else {
            nsm.start(c, pnpc.getScriptId(), null);
         }
      }
   }
}