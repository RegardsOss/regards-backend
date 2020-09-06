package fr.cnes.regards.modules.processing.utils.random;

import fr.cnes.regards.modules.processing.utils.random.TypedRandomizer;
import fr.cnes.regards.modules.processing.utils.random.VavrWrappersRegistry;
import io.github.xshadov.easyrandom.vavr.VavrRandomizerRegistry;
import io.vavr.collection.List;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ServiceLoader;

public interface RandomUtils {

    EasyRandom RANDOM = easyRandom();

    static <T> List<T> randomList(Class<T> type, int num) {
        return List.ofAll(RANDOM.objects(type, num));
    }

    static <T> T randomInstance(Class<T> type) {
        return RANDOM.nextObject(type);
    }

    static EasyRandom easyRandom() {
        VavrWrappersRegistry vavrWrappersRegistry = new VavrWrappersRegistry();
        VavrRandomizerRegistry vavrRandomizerRegistry = new VavrRandomizerRegistry();

        EasyRandomParameters parameters = new EasyRandomParameters();
        parameters.randomizerRegistry(vavrWrappersRegistry);
        parameters.randomizerRegistry(vavrRandomizerRegistry);

        EasyRandom generator = new EasyRandom(parameters);
        vavrRandomizerRegistry.setEasyRandom(generator);
        vavrWrappersRegistry.setEasyRandom(generator);

        parameters.collectionSizeRange(0, 10)
            .randomize(Duration.class, () -> Duration.ofSeconds(generator.nextInt(3600 * 24 * 10)))
            .randomize(OffsetDateTime.class, () -> getOffsetDateTime(generator))
            .randomize(LocalDateTime.class, () -> getLocalDateTime(generator));

        ServiceLoader<TypedRandomizer> loader = ServiceLoader.load(TypedRandomizer.class);
        loader.iterator().forEachRemaining(tr -> parameters.randomize(tr.type(), tr.randomizer(generator)));

        return generator;
    }

    static LocalDateTime getLocalDateTime(EasyRandom generator) {
        return LocalDateTime.now().withNano(0).minusSeconds(generator.nextInt(3600 * 24 * 10));
    }

    static OffsetDateTime getOffsetDateTime(EasyRandom generator) {
        return OffsetDateTime.now().withNano(0).minusSeconds(generator.nextInt(3600 * 24 * 10))
                .withOffsetSameInstant(ZoneOffset.UTC);
    }
}
