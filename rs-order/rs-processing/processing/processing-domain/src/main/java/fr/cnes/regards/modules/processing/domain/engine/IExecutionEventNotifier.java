package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.step.PStepFinal;
import fr.cnes.regards.modules.processing.domain.step.PStepIntermediary;
import io.vavr.Function1;
import io.vavr.collection.Seq;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent.event;

/**
 * This interface is given to an IExecutable to allow the executable to notify events for its execution.
 */
public interface IExecutionEventNotifier extends Function1<ExecutionEvent, Mono<PExecution>> {

    default Mono<PExecution> notifyEvent(ExecutionEvent event) {
        return apply(event);
    }

    default  Mono<PExecution> notifyEvent(PStepFinal step) {
        return apply(event(step));
    }

    default  Mono<PExecution> notifyEvent(PStepIntermediary step) {
        return apply(event(step));
    }

    default  Mono<PExecution> notifyEvent(PStepFinal step, Seq<POutputFile> files) {
        return apply(event(step, files));
    }
}
