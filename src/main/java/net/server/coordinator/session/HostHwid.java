package net.server.coordinator.session;

import static java.util.concurrent.TimeUnit.DAYS;

import java.time.Instant;

import net.server.Server;

record HostHwid(Hwid hwid, Instant expiry) {
   static HostHwid createWithDefaultExpiry(Hwid hwid) {
      return new HostHwid(hwid, getDefaultExpiry());
   }

   private static Instant getDefaultExpiry() {
      return Instant.ofEpochMilli(Server.getInstance().getCurrentTime() + DAYS.toMillis(7));
   }
}
