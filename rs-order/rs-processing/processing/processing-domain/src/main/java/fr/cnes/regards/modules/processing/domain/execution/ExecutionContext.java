package fr.cnes.regards.modules.processing.domain.execution;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PProcess;
import lombok.Value;

@Value
public class ExecutionContext {

    PExecution exec;
    PBatch batch;
    PProcess process;

}
