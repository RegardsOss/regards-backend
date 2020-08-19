package fr.cnes.regards.modules.processing.entities.projections;

import fr.cnes.regards.modules.processing.entities.FileParameters;
import fr.cnes.regards.modules.processing.entities.Steps;

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
