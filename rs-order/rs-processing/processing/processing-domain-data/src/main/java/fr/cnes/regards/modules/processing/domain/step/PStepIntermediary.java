package fr.cnes.regards.modules.processing.domain.step;

import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;

import java.time.OffsetDateTime;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.toUtc;

public class PStepIntermediary extends PStep {
    public PStepIntermediary(ExecutionStatus status, OffsetDateTime time, String message) {
        super(status, toUtc(time), message);
        if (status.isFinalStep()) {
            throw new IllegalStateException(String.format("An intermediary step is build with a final status: %s", toString()));
        }
    }
    public PStepIntermediary withTime(OffsetDateTime time) { return new PStepIntermediary(status, time, message); }
}
