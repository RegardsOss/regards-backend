package fr.cnes.regards.modules.processing.domain.step;

import com.google.auto.service.AutoService;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.utils.TypedRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.api.Randomizer;

@AutoService(TypedRandomizer.class)
public class PStepTypedRandomizer implements TypedRandomizer<PStep> {
    @Override public Class<PStep> type() {
        return PStep.class;
    }

    @Override public Randomizer<PStep> randomizer(EasyRandom generator) {
        return () -> generator.nextBoolean()
                ? generator.nextObject(PStepFinal.class)
                : generator.nextObject(PStepIntermediary.class);
    }
}
