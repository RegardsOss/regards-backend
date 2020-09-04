package fr.cnes.regards.modules.processing.utils;

import org.jeasy.random.EasyRandom;
import org.jeasy.random.api.Randomizer;

public interface TypedRandomizer<T> {

    Class<T> type();
    Randomizer<T> randomizer(EasyRandom generator);

}
