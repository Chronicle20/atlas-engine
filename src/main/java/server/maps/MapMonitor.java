package server.maps;

import java.util.concurrent.ScheduledFuture;

import server.TimerManager;

public class MapMonitor {
   private ScheduledFuture<?> monitorSchedule;
   private MapleMap map;
   private MaplePortal portal;

   public MapMonitor(final MapleMap map, String portal) {
      this.map = map;
      this.portal = map.getPortal(portal);
      this.monitorSchedule = TimerManager.getInstance().register(() -> {
         if (map.getCharacters().isEmpty()) {
            cancelAction();
         }
      }, 5000);
   }

   private void cancelAction() {
      if (monitorSchedule != null) {
         monitorSchedule.cancel(false);
         monitorSchedule = null;
      }

      map.killAllMonsters();
      map.clearDrops();
      if (portal != null) {
         portal.setPortalStatus(MaplePortal.OPEN);
      }
      map.resetReactors();

      map = null;
      portal = null;
   }
}
