package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import lombok.Value;
import lombok.With;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value @With

public class PExecution {

    UUID id;

    UUID batchId;

    Duration expectedDuration;

    Seq<ExecutionFileParameterValue> inputFiles;

    Seq<PStep> steps;

    String tenant;

    String userName;

    UUID processBusinessId;

    String processName;

    OffsetDateTime created;

    OffsetDateTime lastUpdated;

    int version;

    transient boolean persisted;

    public static PExecution create(
            UUID batchId,
            Duration expectedDuration,
            Seq<ExecutionFileParameterValue> inputFiles,
            String tenant,
            String userName,
            UUID processBusinessId,
            String processName
    ) {
        PStep registered = PStep.registered("");
        return new PExecution(
            UUID.randomUUID(),
            batchId,
            expectedDuration,
            inputFiles,
            List.of(registered),
            tenant,
            userName,
            processBusinessId,
            processName,
            registered.getTime(),
            registered.getTime(),
            0,
            false
        );
    }

    public PExecution addStep(PStep step) {
        return withSteps(this.steps.append(step)).withLastUpdated(step.getTime());
    }

}
