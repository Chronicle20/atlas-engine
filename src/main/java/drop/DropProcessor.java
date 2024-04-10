package drop;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;

public class DropProcessor {
   private static DropProcessor instance = null;

   private final Map<Integer, List<DropEntry>> dropCache;

   private final String serviceUrl;

   private DropProcessor() {
      dropCache = new HashMap<>();
      serviceUrl = System.getenv("DROP_INFORMATION_SERVICE_URL");
   }

   public static synchronized DropProcessor getInstance() {
      if (instance == null) {
         instance = new DropProcessor();
      }

      return instance;
   }

   public List<DropEntry> getDropsForMonster(int monsterId) {
      if (dropCache.containsKey(monsterId)) {
         return dropCache.get(monsterId);
      }

      List<DropEntry> entries = Collections.emptyList();
      // Make a request to the API endpoint
      String apiUrl = String.format("%s/monsters/%d", serviceUrl, monsterId);
      try {
         URL url = new URL(apiUrl);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         connection.setRequestMethod("GET");

         int responseCode = connection.getResponseCode();
         if (responseCode == HttpURLConnection.HTTP_OK) {
            ResourceConverter converter = new ResourceConverter(MonsterDropResource.class, DropResource.class);
            JSONAPIDocument<MonsterDropResource> dropDocumentCollection =
                  converter.readDocument(connection.getInputStream(), MonsterDropResource.class);
            entries = Optional.ofNullable(dropDocumentCollection.get())
                  .map(MonsterDropResource::getDrops)
                  .orElse(Collections.emptyList())
                  .stream()
                  .map(this::transform)
                  .toList();
         } else {
            System.out.println("Error: HTTP request failed with status code " + responseCode);
         }

         connection.disconnect();
      } catch (IOException e) {
         e.printStackTrace();
      }

      dropCache.put(monsterId, entries);
      return entries;
   }

   public List<Integer> getMonsterIdsWhoDrop(int itemId) {
      List<Integer> entries = Collections.emptyList();
      // Make a request to the API endpoint
      String apiUrl = String.format("%s/monsters?filter[drops.item_id]=%d", serviceUrl, itemId);
      try {
         URL url = new URL(apiUrl);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         connection.setRequestMethod("GET");

         int responseCode = connection.getResponseCode();
         if (responseCode == HttpURLConnection.HTTP_OK) {
            ResourceConverter converter = new ResourceConverter(MonsterDropResource.class, DropResource.class);
            JSONAPIDocument<List<MonsterDropResource>> dropDocumentCollection =
                  converter.readDocumentCollection(connection.getInputStream(), MonsterDropResource.class);
            entries = Objects.requireNonNull(dropDocumentCollection.get())
                  .stream()
                  .map(MonsterDropResource::getId)
                  .map(Integer::parseInt)
                  .toList();
         } else {
            System.out.println("Error: HTTP request failed with status code " + responseCode);
         }

         connection.disconnect();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return entries;
   }

   private DropEntry transform(DropResource dropResource) {
      return new DropEntry(dropResource.getItemId(), dropResource.getChance(), dropResource.getMinimumQuantity(),
            dropResource.getMaximumQuantity(), dropResource.getQuestId());
   }
}
