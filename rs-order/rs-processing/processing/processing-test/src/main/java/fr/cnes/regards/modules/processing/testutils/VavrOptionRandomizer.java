package fr.cnes.regards.modules.processing.testutils;

import io.vavr.control.Option;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.randomizers.AbstractRandomizer;

public class VavrOptionRandomizer<T> extends AbstractRandomizer<Option<T>> {

    private Randomizer<Boolean> booleanRandomizer;

    private Randomizer<? extends T> valueRandomizer;

    public VavrOptionRandomizer(Randomizer<Boolean> booleanRandomizer,
            Randomizer<? extends T> valueRandomizer) {
        super();
        this.booleanRandomizer = booleanRandomizer;
        this.valueRandomizer = valueRandomizer;
    }

    public VavrOptionRandomizer() {
    }

    public Randomizer<Boolean> getBooleanRandomizer() {
        return booleanRandomizer;
    }

    public void setBooleanRandomizer(Randomizer<Boolean> booleanRandomizer) {
        this.booleanRandomizer = booleanRandomizer;
    }

    public Randomizer<? extends T> getValueRandomizer() {
        return valueRandomizer;
    }

    public void setValueRandomizer(Randomizer<? extends T> valueRandomizer) {
        this.valueRandomizer = valueRandomizer;
    }

    @Override public Option<T> getRandomValue() {
        if (booleanRandomizer.getRandomValue()) {
            return Option.of(valueRandomizer.getRandomValue());
        } else {
            return Option.none();
        }
    }
}
