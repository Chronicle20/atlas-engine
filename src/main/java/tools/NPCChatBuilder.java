package tools;

public class NPCChatBuilder {
   private final StringBuilder builder;

   public NPCChatBuilder() {
      builder = new StringBuilder();
   }

   public static NPCChatBuilder of() {
      return new NPCChatBuilder();
   }

   public String toString() {
      return builder.toString();
   }

   public NPCChatBuilder addText(String message) {
      builder.append(message);
      return this;
   }

   public NPCChatBuilder newLine() {
      builder.append("\r\n");
      return this;
   }

   public NPCChatBuilder openItem(int i) {
      builder.append(String.format("#L%d#", i));
      return this;
   }

   public NPCChatBuilder blueText() {
      builder.append("#b");
      return this;
   }

   public NPCChatBuilder closeItem() {
      builder.append("#l");
      return this;
   }

   public NPCChatBuilder blackText() {
      builder.append("#k");
      return this;
   }

   public NPCChatBuilder purpleText() {
      builder.append("#d");
      return this;
   }

   public NPCChatBuilder boldText() {
      builder.append("#e");
      return this;
   }

   public NPCChatBuilder greenText() {
      builder.append("#g");
      return this;
   }

   public NPCChatBuilder redText() {
      builder.append("#r");
      return this;
   }

   public NPCChatBuilder normalText() {
      builder.append("#n");
      return this;
   }

   public NPCChatBuilder showMap(int mapId) {
      builder.append(String.format("#m%d#", mapId));
      return this;
   }

   public NPCChatBuilder showNPC(int npcId) {
      builder.append(String.format("#p%d#", npcId));
      return this;
   }

   public NPCChatBuilder showItemName1(int itemId) {
      builder.append(String.format("#t%d#", itemId));
      return this;
   }

   public NPCChatBuilder showItemName2(int itemId) {
      builder.append(String.format("#z%d#", itemId));
      return this;
   }

   public NPCChatBuilder showCharacterName() {
      builder.append("#h #");
      return this;
   }

   public NPCChatBuilder showItemImage1(int itemId) {
      builder.append(String.format("#v%d#", itemId));
      return this;
   }

   public NPCChatBuilder showItemImage2(int itemId) {
      builder.append(String.format("#i%d#", itemId));
      return this;
   }

   public NPCChatBuilder showItemCount(int itemId) {
      builder.append(String.format("#c%d#", itemId));
      return this;
   }

   public NPCChatBuilder showMonsterName(int monsterId) {
      builder.append(String.format("#o%d#", monsterId));
      return this;
   }

   public NPCChatBuilder showSkillImage(int skillId) {
      builder.append(String.format("#s%d#", skillId));
      return this;
   }

   public NPCChatBuilder showProgressBar(int amount) {
      builder.append(String.format("#B%d#", amount));
      return this;
   }

   public NPCChatBuilder dimensionalMirrorOption(int index, String selection) {
      builder.append(String.format("#%d# %s", index, selection));
      return this;
   }
}
