package fr.cnes.regards.modules.processing.domain.step;

import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;

import java.time.OffsetDateTime;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.toUtc;

public class PStepFinal extends PStep {
    public PStepFinal(ExecutionStatus status, OffsetDateTime time, String message) {
        super(status, toUtc(time), message);
        if (!status.isFinalStep()) {
            throw new IllegalStateException(String.format("A final step is build with a non-final status: %s", toString()));
        }
    }
    public PStepFinal withTime(OffsetDateTime time) { return new PStepFinal(status, time, message); }
}
