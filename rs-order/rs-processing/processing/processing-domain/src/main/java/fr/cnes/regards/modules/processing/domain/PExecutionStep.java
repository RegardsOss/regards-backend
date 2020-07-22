package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import lombok.Value;
import lombok.With;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;

@Value @With

public class PExecutionStep {

    Long id;
    UUID executionId;
    ExecutionStatus status;
    OffsetDateTime time;
    String message;

    public PExecutionStep toUTC() {
        return this.withTime(time.withOffsetSameInstant(ZoneOffset.UTC));
    }

    public static PExecutionStep newStep(PExecution exec, ExecutionStatus status, String message) {
        return new PExecutionStep(null, exec.getId(), status, nowUtc(), message);
    }
    public static PExecutionStep newStep(PExecution exec, ExecutionStatus status) {
        return newStep(exec, status, "");
    }
}
