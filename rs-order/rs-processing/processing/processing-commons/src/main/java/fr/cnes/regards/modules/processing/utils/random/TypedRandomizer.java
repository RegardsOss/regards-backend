package fr.cnes.regards.modules.processing.utils.random;

import org.jeasy.random.EasyRandom;
import org.jeasy.random.api.Randomizer;

/**
 * This interface is meant to be used BY TESTS, and is a target for ServiceLoader, so that
 * other components can independently declare new randomizers to be loaded
 * by RandomUtils in the processing-test module. It is just easier to declare it here in
 * order to prevent circular dependencies in maven modules.
 *
 * @param <T> the generic type
 */
public interface TypedRandomizer<T> {

    Class<T> type();
    Randomizer<T> randomizer(EasyRandom generator);

}
