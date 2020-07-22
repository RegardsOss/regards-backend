package fr.cnes.regards.modules.processing.storage;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import fr.cnes.regards.modules.processing.utils.Unit;
import io.vavr.collection.Seq;
import reactor.core.publisher.Mono;

public interface IExecutionLocalWorkdirService {

    Mono<ExecutionLocalWorkdir> makeWorkdir(ExecutionContext ctx);

    Mono<Unit> writeInputFilesToWorkdirInput(ExecutionLocalWorkdir workdir, Seq<ExecutionFileParameterValue> inputFiles);

    Mono<Unit> cleanupWorkdir(ExecutionLocalWorkdir workdir);

}
