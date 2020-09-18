package fr.cnes.regards.modules.processing.storage;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import io.vavr.collection.Seq;
import reactor.core.publisher.Mono;

public interface IExecutionLocalWorkdirService {

    Mono<ExecutionLocalWorkdir> makeWorkdir(PExecution exec);

    Mono<ExecutionLocalWorkdir> writeInputFilesToWorkdirInput(ExecutionLocalWorkdir workdir, Seq<PInputFile> inputFiles);

    Mono<ExecutionLocalWorkdir> cleanupWorkdir(ExecutionLocalWorkdir workdir);

}
