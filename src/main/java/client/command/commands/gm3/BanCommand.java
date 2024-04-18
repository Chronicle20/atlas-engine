package client.command.commands.gm3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ban.BanProcessor;
import client.MapleCharacter;
import client.MapleClient;
import client.command.Command;
import connection.packets.CField;
import connection.packets.CWvsContext;
import net.server.Server;
import server.TimerManager;
import tools.DatabaseConnection;

public class BanCommand extends Command {
   private final static Logger log = LoggerFactory.getLogger(BanCommand.class);

   {
      setDescription("");
   }

   @Override
   public void execute(MapleClient c, String[] params) {
      MapleCharacter player = c.getPlayer();
      if (params.length < 2) {
         player.yellowMessage("Syntax: !ban <IGN> <Reason> (Please be descriptive)");
         return;
      }
      String ign = params[0];
      String reason = joinStringFrom(params, 1);
      MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(ign).orElse(null);
      if (target != null) {
         String readableTargetName = MapleCharacter.makeMapleReadable(target.getName());
         String ip = target.getClient().getRemoteAddress();
         //Ban ip
         PreparedStatement ps;
         try {
            Connection con = DatabaseConnection.getConnection();
            if (ip.matches("/[0-9]{1,3}\\..*")) {
               ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?, ?)");
               ps.setString(1, ip);
               ps.setString(2, String.valueOf(target.getClient().getAccID()));

               ps.executeUpdate();
               ps.close();
            }

            con.close();
         } catch (SQLException ex) {
            log.error("Error occured while banning IP address");
            log.error("{}'s IP was not banned: {}", target.getName(), ip);
         }
         target.getClient().banMacs();
         reason = c.getPlayer().getName() + " banned " + readableTargetName + " for " + reason + " (IP: " + ip + ") " + "(MAC: "
               + c.getMacs() + ")";
         target.ban(reason);
         target.yellowMessage("You have been banned by #b" + c.getPlayer().getName() + " #k.");
         target.yellowMessage("Reason: " + reason);
         c.sendPacket(CField.getGMEffect(4, (byte) 0));
         final MapleCharacter rip = target;
         TimerManager.getInstance().schedule(() -> rip.getClient().disconnect(false, false), 5000);
         Server.getInstance().broadcastMessage(c.getWorld(), CWvsContext.serverNotice(6, "[RIP]: " + ign + " has been banned."));
      } else if (BanProcessor.getInstance().ban(ign, reason, false)) {
         c.sendPacket(CField.getGMEffect(4, (byte) 0));
         Server.getInstance().broadcastMessage(c.getWorld(), CWvsContext.serverNotice(6, "[RIP]: " + ign + " has been banned."));
      } else {
         c.sendPacket(CField.getGMEffect(6, (byte) 1));
      }
   }
}
