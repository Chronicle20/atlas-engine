package scripting.event;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.Invocable;
import javax.script.ScriptEngine;

import org.slf4j.LoggerFactory;

import net.server.channel.Channel;
import scripting.AbstractScriptManager;
import scripting.SynchronizedInvocable;

public class EventScriptManager extends AbstractScriptManager {

   private static final org.slf4j.Logger log = LoggerFactory.getLogger(EventScriptManager.class);

   private static final String INJECTED_VARIABLE_NAME = "em";
   private static EventEntry fallback;
   private final Map<String, EventEntry> events = new ConcurrentHashMap<>();
   private boolean active = false;

   public EventScriptManager(Channel channel, String[] scripts) {
      super();
      for (String script : scripts) {
         if (!script.isEmpty()) {
            events.put(script, initializeEventEntry(script, channel));
         }
      }

      init();
      fallback = events.remove("0_EXAMPLE");
   }

   public EventManager getEventManager(String event) {
      EventEntry entry = events.get(event);
      if (entry == null) {
         return fallback.em;
      }
      return entry.em;
   }

   public boolean isActive() {
      return active;
   }

   public final void init() {
      for (EventEntry entry : events.values()) {
         try {
            entry.iv.invokeFunction("init", (Object) null);
         } catch (Exception ex) {
            log.error("Error on script: {}", entry.em.getName());
            log.error("Exception: ", ex);
         }
      }

      active = events.size() > 1; // bootup loads only 1 script
   }

   private void reloadScripts() {
      Set<Entry<String, EventEntry>> eventEntries = new HashSet<>(events.entrySet());
      if (eventEntries.isEmpty()) {
         return;
      }

      Channel channel = eventEntries.iterator().next().getValue().em.getChannelServer();
      for (Entry<String, EventEntry> entry : eventEntries) {
         String script = entry.getKey();
         events.put(script, initializeEventEntry(script, channel));
      }
   }

   private EventEntry initializeEventEntry(String script, Channel channel) {
      ScriptEngine engine = getScriptEngine("event/" + script + ".js").orElseThrow();
      Invocable iv = SynchronizedInvocable.of((Invocable) engine);
      EventManager eventManager = new EventManager(channel, iv, script);
      engine.put(INJECTED_VARIABLE_NAME, eventManager);
      return new EventEntry(iv, eventManager);
   }

   public void cancel() {
      active = false;
      for (EventEntry entry : events.values()) {
         entry.em.cancel();
      }
   }

   public void dispose() {
      if (events.isEmpty()) {
         return;
      }

      Set<EventEntry> eventEntries = new HashSet<>(events.values());
      events.clear();

      active = false;
      for (EventEntry entry : eventEntries) {
         entry.em.cancel();
      }
   }

   private static class EventEntry {
      public Invocable iv;
      public EventManager em;

      public EventEntry(Invocable iv, EventManager em) {
         this.iv = iv;
         this.em = em;
      }
   }
}
