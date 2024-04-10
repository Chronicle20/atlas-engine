package drop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;

@Type("drops")
public class DropResource {
   @Id
   private String id;

   @JsonProperty("item_id")
   private Integer itemId;

   @JsonProperty("minimum_quantity")
   private Integer minimumQuantity;

   @JsonProperty("maximum_quantity")
   private Integer maximumQuantity;

   @JsonProperty("quest_id")
   private Short questId;

   @JsonProperty("chance")
   private Integer chance;

   public Integer getChance() {
      return chance;
   }

   public String getId() {
      return id;
   }

   public Integer getItemId() {
      return itemId;
   }

   public Integer getMaximumQuantity() {
      return maximumQuantity;
   }

   public Integer getMinimumQuantity() {
      return minimumQuantity;
   }

   public Short getQuestId() {
      return questId;
   }
}
