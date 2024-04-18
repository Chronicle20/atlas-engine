package net.server.handlers.login;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import client.MapleClient;
import client.creator.novice.BeginnerCreator;
import client.creator.novice.LegendCreator;
import client.creator.novice.NoblesseCreator;
import connection.packets.CLogin;
import constants.character.creation.TemplateFactory;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;
import tools.FilePrinter;

public final class CreateCharHandler extends AbstractMaplePacketHandler {

   private final static Set<Integer> IDs = new HashSet<>(Arrays.asList(1302000, 1312004, 1322005, 1442079,// weapons
         1040002, 1040006, 1040010, 1041002, 1041006, 1041010, 1041011, 1042167,// bottom
         1060002, 1060006, 1061002, 1061008, 1062115, // top
         1072001, 1072005, 1072037, 1072038, 1072383,// shoes
         30000, 30010, 30020, 30030, 31000, 31040, 31050,// hair
         20000, 20001, 20002, 21000, 21001, 21002, 21201, 20401, 20402, 21700, 20100  //face
         //#NeverTrustStevenCode
   ));

   private static boolean isLegal(Integer toCompare) {
      return IDs.contains(toCompare);
   }

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      String name = p.readString();
      int job = p.readInt();
      short nSubJob = p.readShort();

      int face = p.readInt();
      if (!TemplateFactory.getInstance().validFace(face)) {
         packetError(c, name, String.format(
               "Account [%s] attempted to use a [%s] : [%d] that should not be available during character creation. Possible packet exploit.",
               c.getAccountName(), "FACE", face));
         return;
      }

      int hair = p.readInt();
      if (!TemplateFactory.getInstance().validHair(hair)) {
         packetError(c, name, String.format(
               "Account [%s] attempted to use a [%s] : [%d] that should not be available during character creation. Possible packet exploit.",
               c.getAccountName(), "HAIR", hair));
         return;
      }

      int top = p.readInt();
      if (!TemplateFactory.getInstance().validTop(top)) {
         packetError(c, name, String.format(
               "Account [%s] attempted to use a [%s] : [%d] that should not be available during character creation. Possible packet exploit.",
               c.getAccountName(), "TOP", top));
         return;
      }

      int bottom = p.readInt();
      if (!TemplateFactory.getInstance().validBottom(bottom)) {
         packetError(c, name, String.format(
               "Account [%s] attempted to use a [%s] : [%d] that should not be available during character creation. Possible packet exploit.",
               c.getAccountName(), "BOTTOM", bottom));
         return;
      }

      int shoes = p.readInt();
      if (!TemplateFactory.getInstance().validShoe(shoes)) {
         packetError(c, name, String.format(
               "Account [%s] attempted to use a [%s] : [%d] that should not be available during character creation. Possible packet exploit.",
               c.getAccountName(), "SHOE", shoes));
         return;
      }

      int weapon = p.readInt();
      if (!TemplateFactory.getInstance().validWeapon(weapon)) {
         packetError(c, name, String.format(
               "Account [%s] attempted to use a [%s] : [%d] that should not be available during character creation. Possible packet exploit.",
               c.getAccountName(), "WEAPON", weapon));
         return;
      }

      int status;
      if (job == 0) { // Knights of Cygnus
         status = NoblesseCreator.createCharacter(c, name, face, hair, 0, top, bottom, shoes, weapon, c.getGender());
      } else if (job == 1) { // Adventurer
         status = BeginnerCreator.createCharacter(c, name, face, hair, 0, top, bottom, shoes, weapon, c.getGender());
      } else if (job == 2) { // Aran
         status = LegendCreator.createCharacter(c, name, face, hair, 0, top, bottom, shoes, weapon, c.getGender());
      } else {
         c.sendPacket(CLogin.deleteCharResponse(0, 9));
         return;
      }

      if (status == -2) {
         c.sendPacket(CLogin.deleteCharResponse(0, 9));
      }
   }

   private void packetError(MapleClient c, String name, String reason) {
      FilePrinter.printError(FilePrinter.EXPLOITS + name + ".txt", reason);
      c.disconnect(true, false);
   }
}