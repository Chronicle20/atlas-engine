package net.server.channel.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleClient;
import connection.packets.CUserRemote;
import constants.skills.Bishop;
import constants.skills.Bowmaster;
import constants.skills.Brawler;
import constants.skills.ChiefBandit;
import constants.skills.Corsair;
import constants.skills.DarkKnight;
import constants.skills.Evan;
import constants.skills.FPArchMage;
import constants.skills.FPMage;
import constants.skills.Gunslinger;
import constants.skills.Hero;
import constants.skills.ILArchMage;
import constants.skills.Marksman;
import constants.skills.NightWalker;
import constants.skills.Paladin;
import constants.skills.ThunderBreaker;
import constants.skills.WindArcher;
import net.AbstractMaplePacketHandler;
import net.packet.InPacket;

public final class SkillEffectHandler extends AbstractMaplePacketHandler {
   private static final Logger log = LoggerFactory.getLogger(SkillEffectHandler.class);

   @Override
   public void handlePacket(InPacket p, MapleClient c) {
      int skillId = p.readInt();
      int level = p.readByte();
      byte flags = p.readByte();
      int speed = p.readByte();
      byte aids = p.readByte();//Mmmk
      switch (skillId) {
         case FPMage.EXPLOSION:
         case FPArchMage.BIG_BANG:
         case ILArchMage.BIG_BANG:
         case Bishop.BIG_BANG:
         case Bowmaster.HURRICANE:
         case Marksman.PIERCING_ARROW:
         case ChiefBandit.CHAKRA:
         case Brawler.CORKSCREW_BLOW:
         case Gunslinger.GRENADE:
         case Corsair.RAPID_FIRE:
         case WindArcher.HURRICANE:
         case NightWalker.POISON_BOMB:
         case ThunderBreaker.CORKSCREW_BLOW:
         case Paladin.MONSTER_MAGNET:
         case DarkKnight.MONSTER_MAGNET:
         case Hero.MONSTER_MAGNET:
         case Evan.FIRE_BREATH:
         case Evan.ICE_BREATH:
            c.getPlayer().getMap()
                  .broadcastMessage(c.getPlayer(), CUserRemote.skillEffect(c.getPlayer(), skillId, level, flags, speed, aids),
                        false);
            return;
         default:
            log.debug("{} entered SkillEffectHandler without being handled using {}.", c.getPlayer(), skillId);
      }
   }
}