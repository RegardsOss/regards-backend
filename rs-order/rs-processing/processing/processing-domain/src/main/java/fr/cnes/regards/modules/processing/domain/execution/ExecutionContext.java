package fr.cnes.regards.modules.processing.domain.execution;

import fr.cnes.regards.modules.processing.domain.*;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutionEventNotifier;
import fr.cnes.regards.modules.processing.storage.ExecutionLocalWorkdir;
import fr.cnes.regards.modules.processing.storage.IExecutionLocalWorkdirService;
import fr.cnes.regards.modules.processing.storage.ISharedStorageService;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import lombok.Value;
import lombok.With;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

@Value
public class ExecutionContext {

    IExecutionLocalWorkdirService workdirService;
    ISharedStorageService storageService;

    @With PExecution exec;
    PBatch batch;
    PProcess process;

    ExecutionLocalWorkdir workdir;

    IExecutionEventNotifier eventNotifier;

    public Mono<ExecutionContext> sendEvent(Supplier<ExecutionEvent> event) {
        return getEventNotifier()
                .notifyEvent(event.get())
                .map(this::withExec);
    }

}
