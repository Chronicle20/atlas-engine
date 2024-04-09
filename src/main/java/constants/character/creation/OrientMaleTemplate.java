package constants.character.creation;

import java.util.Collection;
import java.util.List;

public class OrientMaleTemplate implements CharacterCreationTemplate {
    public Collection<Integer> getFaces() {
        return List.of(20100, 20401, 20402);
    }

    @Override
    public Collection<Integer> getHair() {
        return List.of(30030, 30027, 30000);
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