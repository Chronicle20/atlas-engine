package constants.character.creation;

import java.util.Collection;

public interface CharacterCreationTemplate {
    Collection<Integer> getFaces();

    Collection<Integer> getHair();

    Collection<Integer> getTops();

    Collection<Integer> getBottoms();

    Collection<Integer> getShoes();

    Collection<Integer> getWeapons();
}
