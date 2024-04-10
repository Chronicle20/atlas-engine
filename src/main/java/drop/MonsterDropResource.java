package drop;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;

@Type("monsters")
public class MonsterDropResource {
   @Id
   private String id;

   @JsonProperty("drops")
   private List<DropResource> drops;

   public List<DropResource> getDrops() {
      return drops;
   }

   public String getId() {
      return id;
   }
}
