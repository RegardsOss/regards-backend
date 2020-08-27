package fr.cnes.regards.modules.processing.entity.projections;

import fr.cnes.regards.modules.processing.entity.FileParameters;
import fr.cnes.regards.modules.processing.entity.Steps;

import java.util.UUID;

public interface ExecutionBatchMonitoringProjection {

    UUID execId();

    UUID batchId();

    FileParameters parameters();

    Steps steps();

    String correlationId();

    String tenant();

    String userName();

    String process();

}
