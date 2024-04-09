package constants.character.creation;

import java.util.Collection;
import java.util.List;

public class OrientFemaleTemplate implements CharacterCreationTemplate {
    public Collection<Integer> getFaces() {
        return List.of(21700, 21201, 21002);
    }

    @Override
    public Collection<Integer> getHair() {
        return List.of(31002, 31047, 31057);
    }

    @Override
    public Collection<Integer> getTops() {
        return List.of(1042167);
    }

    @Override
    public Collection<Integer> getBottoms() {
        return List.of(1062115);
    }

    @Override
    public Collection<Integer> getShoes() {
        return List.of(1072383);
    }

    @Override
    public Collection<Integer> getWeapons() {
        return List.of(1442079);
    }
}