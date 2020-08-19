package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import io.vavr.collection.List;
import lombok.Value;
import lombok.With;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;

@Value @With

public class PStep {

    ExecutionStatus status;
    OffsetDateTime time;
    String message;

    public PStep toUTC() {
        return this.withTime(time.withOffsetSameInstant(ZoneOffset.UTC));
    }

    public static PStep newStep(ExecutionStatus status, String message) {
        return new PStep(status, nowUtc(), message);
    }
    public static PStep newStep(ExecutionStatus status) {
        return newStep(status, "");
    }

    public static PStepSequence sequence(PStep... steps) {
        return new PStepSequence(List.of(steps));
    }
}
