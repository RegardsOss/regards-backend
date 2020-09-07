package fr.cnes.regards.modules.processing.domain.storage;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import io.vavr.collection.Seq;
import reactor.core.publisher.Mono;

public interface IExecutionLocalWorkdirService {

    Mono<ExecutionLocalWorkdir> makeWorkdir(PExecution exec);

    Mono<ExecutionLocalWorkdir> writeInputFilesToWorkdirInput(ExecutionLocalWorkdir workdir, Seq<ExecutionFileParameterValue> inputFiles);

    Mono<ExecutionLocalWorkdir> cleanupWorkdir(ExecutionLocalWorkdir workdir);

}
