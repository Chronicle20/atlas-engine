package constants.character.creation;

import java.util.Collection;
import java.util.List;

public class EvanMaleTemplate implements CharacterCreationTemplate {
    public Collection<Integer> getFaces() {
        return List.of(20100, 20401, 20402);
    }

    @Override
    public Collection<Integer> getHair() {
        return List.of(30030, 30027, 30000);
    }

    @Override
    public Collection<Integer> getTops() {
        return List.of(1042180);
    }

    @Override
    public Collection<Integer> getBottoms() {
        return List.of(1060138);
    }

    @Override
    public Collection<Integer> getShoes() {
        return List.of(1072418);
    }

    @Override
    public Collection<Integer> getWeapons() {
        return List.of(1302132);
    }
}