package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import io.vavr.collection.Seq;
import lombok.Value;
import lombok.With;

import java.time.Duration;
import java.util.UUID;

@Value @With

public class PExecution {

    UUID id;

    UUID batchId;

    Duration expectedDuration;

    Seq<ExecutionFileParameterValue> inputFiles;

    boolean persisted;

    public PExecution asNew() { return this.withId(UUID.randomUUID()).withPersisted(false); }

}
