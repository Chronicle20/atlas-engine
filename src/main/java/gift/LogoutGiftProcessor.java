package gift;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogoutGiftProcessor {
   private static final Logger log = LoggerFactory.getLogger(LogoutGiftProcessor.class);

   private static LogoutGiftProcessor instance = null;

   private final String serviceUrl;

   private LogoutGiftProcessor() {
      serviceUrl = System.getenv("LOGOUT_GIFT_SERVICE_URL");
   }

   public static synchronized LogoutGiftProcessor getInstance() {
      if (instance == null) {
         instance = new LogoutGiftProcessor();
      }

      return instance;
   }

   public List<LogoutGift> getGiftChoices(int worldId, int characterId) {
      // Make a request to the API endpoint
      String apiUrl = String.format("%s/worlds/%d/characters/%d/choices", serviceUrl, worldId, characterId);
      try {
         URL url = new URL(apiUrl);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         connection.setRequestMethod("POST");

         int responseCode = connection.getResponseCode();
         if (responseCode == HttpURLConnection.HTTP_OK) {
            ResourceConverter converter = new ResourceConverter(ChoiceResource.class);
            JSONAPIDocument<ChoiceResource> choiceCollection =
                  converter.readDocument(connection.getInputStream(), ChoiceResource.class);
            return Optional.ofNullable(choiceCollection.get())
                  .map(this::toGiftList)
                  .orElse(Collections.emptyList());
         } else {
            log.error("getGiftChoices HTTP request failed with status code {}", responseCode);
         }

         connection.disconnect();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return Collections.emptyList();
   }

   public void makeChoice(int worldId, int characterId) {
      // Make a request to the API endpoint
      String apiUrl = String.format("%s/worlds/%d/characters/%d/choices", serviceUrl, worldId, characterId);
      try {
         URL url = new URL(apiUrl);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         connection.setRequestMethod("DELETE");

         int responseCode = connection.getResponseCode();
         if (responseCode == HttpURLConnection.HTTP_OK) {
            return;
         } else {
            log.error("makeChoice HTTP request failed with status code {}", responseCode);
         }

         connection.disconnect();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private List<LogoutGift> toGiftList(ChoiceResource choiceCollection) {
      List<LogoutGift> gifts = new ArrayList<>();
      gifts.add(new LogoutGift(0, choiceCollection.getOption1()));
      gifts.add(new LogoutGift(0, choiceCollection.getOption2()));
      gifts.add(new LogoutGift(0, choiceCollection.getOption3()));
      return gifts;
   }
}
