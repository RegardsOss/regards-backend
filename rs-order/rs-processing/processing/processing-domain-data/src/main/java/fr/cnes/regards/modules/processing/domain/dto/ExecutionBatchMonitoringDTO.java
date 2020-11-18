package fr.cnes.regards.modules.processing.domain.dto;

import fr.cnes.regards.modules.processing.domain.PStep;
import io.vavr.collection.List;
import lombok.Value;

import java.util.UUID;

@Value
public class ExecutionBatchMonitoringDTO {

    UUID batchId;
    UUID execId;

    String process;

    String correlationId;
    String tenant;
    String user;

    List<PStep> steps;

}
