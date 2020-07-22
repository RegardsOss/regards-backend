package fr.cnes.regards.modules.processing.testutils;

import io.vavr.control.Either;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.randomizers.AbstractRandomizer;

public class VavrEitherRandomizer<L, R> extends AbstractRandomizer<Either<L, R>> {

    private Randomizer<Boolean> booleanRandomizer;

    private Randomizer<? extends R> rightRandomizer;

    private Randomizer<? extends L> leftRandomizer;

    public VavrEitherRandomizer() {
    }

    public VavrEitherRandomizer(Randomizer<Boolean> booleanRandomizer,
            Randomizer<? extends R> rightRandomizer, Randomizer<? extends L> leftRandomizer) {
        this.booleanRandomizer = booleanRandomizer;
        this.rightRandomizer = rightRandomizer;
        this.leftRandomizer = leftRandomizer;
    }

    public Randomizer<Boolean> getBooleanRandomizer() {
        return booleanRandomizer;
    }

    public Randomizer<? extends R> getRightRandomizer() {
        return rightRandomizer;
    }

    public Randomizer<? extends L> getLeftRandomizer() {
        return leftRandomizer;
    }

    public void setBooleanRandomizer(Randomizer<Boolean> booleanRandomizer) {
        this.booleanRandomizer = booleanRandomizer;
    }

    public void setRightRandomizer(Randomizer<? extends R> rightRandomizer) {
        this.rightRandomizer = rightRandomizer;
    }

    public void setLeftRandomizer(Randomizer<? extends L> leftRandomizer) {
        this.leftRandomizer = leftRandomizer;
    }

    @Override public Either<L, R> getRandomValue() {
        if (booleanRandomizer.getRandomValue()) {
            return Either.right(rightRandomizer.getRandomValue());
        } else {
            return Either.left(leftRandomizer.getRandomValue());
        }
    }
}
