package fr.cnes.regards.modules.processing.utils.random;

import io.vavr.control.Try;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.randomizers.AbstractRandomizer;

public class VavrTryRandomizer<T> extends AbstractRandomizer<Try<T>> {

    private Randomizer<Boolean> booleanRandomizer;

    private Randomizer<? extends T> valueRandomizer;

    public VavrTryRandomizer(Randomizer<Boolean> booleanRandomizer,
            Randomizer<? extends T> valueRandomizer) {
        super();
        this.booleanRandomizer = booleanRandomizer;
        this.valueRandomizer = valueRandomizer;
    }

    public VavrTryRandomizer() {
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

    @Override public Try<T> getRandomValue() {
        if (booleanRandomizer.getRandomValue()) {
            return Try.success(valueRandomizer.getRandomValue());
        } else {
            return Try.failure(new RandomTryCreationException());
        }
    }

    public static final class RandomTryCreationException extends Exception {}
}
