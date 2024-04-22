package gift;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;

@Type("choices")
public class ChoiceResource {
   @Id
   private String id;

   @JsonProperty("option_1")
   private Integer option1;

   @JsonProperty("option_2")
   private Integer option2;

   @JsonProperty("option_3")
   private Integer option3;

   public Integer getOption1() {
      return option1;
   }

   public Integer getOption2() {
      return option2;
   }

   public Integer getOption3() {
      return option3;
   }
}