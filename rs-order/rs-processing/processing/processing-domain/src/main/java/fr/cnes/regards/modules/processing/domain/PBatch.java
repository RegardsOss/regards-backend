package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.domain.parameters.ExecutionStringParameterValue;
import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import lombok.Value;
import lombok.With;

import java.util.UUID;

@Value @With

public class PBatch {

    String correlationId;

    UUID id;

    UUID processBusinessId;

    String processName;

    String tenant;

    String user;

    String userRole;

    Seq<ExecutionStringParameterValue> userSuppliedParameters;

    Map<String, FileSetStatistics> filesetsByDataset;

    boolean persisted;


    public PBatch asNew() { return this.withId(UUID.randomUUID()).withPersisted(false); }

}
