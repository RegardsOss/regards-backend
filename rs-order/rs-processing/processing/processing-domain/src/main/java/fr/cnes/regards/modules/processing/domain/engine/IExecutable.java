package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import io.vavr.collection.Seq;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

public interface IExecutable {

    /**
     * An executable receives execution context, with parameters, and a handle to receive cancellation.
     *
     * @param context the execution parameters
     * @return a list of output files.
     */
    Mono<Seq<POutputFile>> execute(ExecutionContext context, FluxSink<PStep> stepSink);

}
