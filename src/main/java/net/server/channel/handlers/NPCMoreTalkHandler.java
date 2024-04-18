package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestScriptManager;

public final class NPCMoreTalkHandler extends AbstractMaplePacketHandler {
   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      byte lastMsg = p.readByte();
      byte action = p.readByte(); // 00 = end chat, 01 == follow
      int selection = -1;

      if (lastMsg == 0) {
         // CScriptMan::OnSay
      } else if (lastMsg == 1) {
         // CScriptMan::OnSayImage
      } else if (lastMsg == 2 || lastMsg == 13) {
         // CScriptMan::OnAskYesNo
      } else if (lastMsg == 3 || lastMsg == 14) {
         // CScriptMan::OnAskText
         // CScriptMan::OnAskBoxText
         if (action != 1) {
            if (c.getQM() != null) {
               c.getQM().dispose();
               return;
            }
            c.getCM().dispose();
            return;
         }

         // Potential decode string
         String text = p.readString();
         if (c.getQM() == null) {
            c.getCM().setGetText(text);
            NPCScriptManager.getInstance().action(c, action, lastMsg, -1);
            return;
         }

         c.getQM().setGetText(text);
         if (c.getQM().isStart()) {
            QuestScriptManager.getInstance().start(c, action, lastMsg, -1);
            return;
         }

         QuestScriptManager.getInstance().end(c, action, lastMsg, -1);
         return;
      } else if (lastMsg == 4 || lastMsg == 5 || lastMsg == 15) {
         // CScriptMan::OnAskNumber
         // CScriptMan::OnAskMenu
         // CScriptMan::OnAskSlideMenu
         if (action == 1) {
            selection = p.readInt();
         }
      } else if (lastMsg == 8) {
         // CScriptMan::OnAskAvatar
      } else if (lastMsg == 9) {
         // CScriptMan::OnAskMembershopAvatar
      } else if (lastMsg == 10 || lastMsg == 11) {
         // CScriptMan::OnAskPet
         // CScriptMan::OnAskPetAll
         // crazy
         return;
      }

      if (c.getQM() != null) {
         if (c.getQM().isStart()) {
            QuestScriptManager.getInstance().start(c, action, lastMsg, selection);
            return;
         }

         QuestScriptManager.getInstance().end(c, action, lastMsg, selection);
         return;
      }

      if (c.getCM() != null) {
         NPCScriptManager.getInstance().action(c, action, lastMsg, selection);
         return;
      }
   }
}