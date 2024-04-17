package constants;
public class SkillConstants {

	public static boolean isEliteExempted(int skillid){
      return switch (skillid) {
         case 2022631, 2022632, 2022633 ->
               true;
         default -> false;
      };
   }

	public static boolean isLegendarySpirit(int skillID){
		return skillID % 10000000 == 1003;
	}
}
