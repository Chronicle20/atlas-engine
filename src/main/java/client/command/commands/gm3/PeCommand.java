/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
   @Author: Arthur L - Refactored command content into modules
*/
package client.command.commands.gm3;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.MapleCharacter;
import client.MapleClient;
import client.command.Command;
import io.netty.buffer.Unpooled;
import net.MaplePacketHandler;
import net.PacketProcessor;
import net.packet.ByteBufInPacket;
import net.packet.InPacket;
import tools.HexTool;

public class PeCommand extends Command {
   private static final Logger log = LoggerFactory.getLogger(PeCommand.class);

   {
      setDescription("Handle synthesized packets from file, and handle them as if sent from a client");
   }

   @Override
   public void execute(MapleClient c, String[] params) {
      MapleCharacter player = c.getPlayer();
      String packet = "";
      try (BufferedReader br = Files.newBufferedReader(Path.of("pe.txt"))) {
         Properties packetProps = new Properties();
         packetProps.load(br);
         packet = packetProps.getProperty("pe");
      } catch (IOException ex) {
         ex.printStackTrace();
         player.yellowMessage("Failed to load pe.txt");
         return;
      }

      byte[] packetContent = HexTool.toBytes(packet);
      InPacket inPacket = new ByteBufInPacket(Unpooled.wrappedBuffer(packetContent));
      short packetId = inPacket.readShort();
      Optional<MaplePacketHandler> packetHandler = PacketProcessor.getProcessor(0, c.getChannel()).getHandler(packetId);
      if (packetHandler.isPresent() && packetHandler.get().validateState(c)) {
         try {
            player.yellowMessage("Receiving: " + packet);
            packetHandler.get().handlePacket(inPacket, c);
         } catch (final Throwable t) {
            final String chrInfo = player != null ? player.getName() + " on map " + player.getMapId() : "?";
            log.warn("Error in packet handler {}. Chr {}, account {}. Packet: {}", packetHandler.getClass().getSimpleName(),
                  chrInfo, c.getAccountName(), packet, t);
         }
      }
   }
}
