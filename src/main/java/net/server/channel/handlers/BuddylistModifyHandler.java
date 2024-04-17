package net.server.channel.handlers;

import buddy.BuddyProcessor;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

public class BuddylistModifyHandler extends AbstractMaplePacketHandler {

   @Override
   public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
      int mode = slea.readByte();
      if (mode == 0) { // refresh
         BuddyProcessor.getInstance().refreshBuddies(c.getPlayer());
         return;
      }

      if (mode == 1) { // add
         String addName = slea.readMapleAsciiString();
         String group = slea.readMapleAsciiString();
         BuddyProcessor.getInstance().addBuddy(c.getPlayer(), addName, group);
         return;
      }

      if (mode == 2) { // accept buddy
         int otherCid = slea.readInt();
         BuddyProcessor.getInstance().acceptBuddy(c.getPlayer(), otherCid);
         return;
      }

      if (mode == 3) { // delete
         int otherCid = slea.readInt();
         BuddyProcessor.getInstance().deleteBuddy(c.getPlayer(), otherCid);
      }
   }
}
