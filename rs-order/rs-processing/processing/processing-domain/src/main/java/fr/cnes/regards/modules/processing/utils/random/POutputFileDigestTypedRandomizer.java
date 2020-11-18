package fr.cnes.regards.modules.processing.utils.random;

import com.google.auto.service.AutoService;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.api.Randomizer;

import java.io.IOException;

@AutoService(TypedRandomizer.class)
public class POutputFileDigestTypedRandomizer implements TypedRandomizer<POutputFile.Digest> {
    @Override public Class<POutputFile.Digest> type() {
        return POutputFile.Digest.class;
    }

    @Override public Randomizer<POutputFile.Digest> randomizer(EasyRandom generator) {
        return () -> new POutputFile.Digest("SHA-256", createDigestValue(generator));
    }

    public String createDigestValue(EasyRandom generator) {
        try {
            return ByteSource.wrap(generator.nextObject(String.class).getBytes()).hash(Hashing.sha256()).toString();
        }
        catch(Exception e) { return "wups"; }
    }
}
