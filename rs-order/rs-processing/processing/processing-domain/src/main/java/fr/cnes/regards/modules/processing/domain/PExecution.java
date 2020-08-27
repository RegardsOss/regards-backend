package fr.cnes.regards.modules.processing.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import io.vavr.collection.Seq;
import lombok.Value;
import lombok.With;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;

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

    @JsonIgnore
    int version;

    @JsonIgnore
    boolean persisted;

    public PExecution addStep(PStep step) {
        return withSteps(this.steps.append(step)).withLastUpdated(step.getTime());
    }

    public PExecution withAuditDates() {
        if (persisted) { return this; }
        OffsetDateTime now = nowUtc();
        return this.withCreated(now).withLastUpdated(now);
    }

    public PExecution asNew() { return this.withId(UUID.randomUUID()).withPersisted(false).withVersion(0).withAuditDates(); }
}
