package constants.character.creation;


import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class TemplateFactory {
    private static TemplateFactory instance;

    private final Collection<CharacterCreationTemplate> templates;

    private TemplateFactory() {
        templates = List.of(new MaleTemplate(), new FemaleTemplate(), new PremiumMaleTemplate(), new PremiumFemaleTemplate(), new OrientMaleTemplate(), new OrientFemaleTemplate(), new EvanMaleTemplate(), new EvanFemaleTemplate(), new DualMaleTemplate(), new DualFemaleTemplate());
    }

    public static TemplateFactory getInstance() {
        if (instance == null) {
            // If instance is null, create a new instance
            instance = new TemplateFactory();
        }
        return instance;
    }

    public boolean validFace(int faceId) {
        return validChoice(faceId, CharacterCreationTemplate::getFaces);
    }

    public boolean validHair(int hairId) {
        return validChoice(hairId, CharacterCreationTemplate::getHair);
    }

    public boolean validTop(int topId) {
        return validChoice(topId, CharacterCreationTemplate::getTops);
    }

    public boolean validBottom(int bottomId) {
        return validChoice(bottomId, CharacterCreationTemplate::getBottoms);
    }

    public boolean validShoe(int shoeId) {
        return validChoice(shoeId, CharacterCreationTemplate::getShoes);
    }

    public boolean validWeapon(int weaponId) {
        return validChoice(weaponId, CharacterCreationTemplate::getWeapons);
    }

    private boolean validChoice(int id, Function<CharacterCreationTemplate, Collection<Integer>> getter) {
        return templates.stream()
                .map(getter)
                .flatMap(Collection::stream)
                .anyMatch(i -> i == id);
    }
}
