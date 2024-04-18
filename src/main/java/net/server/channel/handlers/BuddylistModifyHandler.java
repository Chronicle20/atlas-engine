package net.server.channel.handlers;

import buddy.BuddyProcessor;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public class BuddylistModifyHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int mode = p.readByte();
      if (mode == 0) { // refresh
         BuddyProcessor.getInstance().refreshBuddies(c.getPlayer());
         return;
      }

      if (mode == 1) { // add
         String addName = p.readString();
         String group = p.readString();
         BuddyProcessor.getInstance().addBuddy(c.getPlayer(), addName, group);
         return;
      }

      if (mode == 2) { // accept buddy
         int otherCid = p.readInt();
         BuddyProcessor.getInstance().acceptBuddy(c.getPlayer(), otherCid);
         return;
      }

      if (mode == 3) { // delete
         int otherCid = p.readInt();
         BuddyProcessor.getInstance().deleteBuddy(c.getPlayer(), otherCid);
      }
   }
}
