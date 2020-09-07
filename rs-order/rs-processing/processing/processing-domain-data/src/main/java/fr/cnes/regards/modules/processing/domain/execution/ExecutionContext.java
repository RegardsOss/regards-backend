package fr.cnes.regards.modules.processing.domain.execution;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutionEventNotifier;
import fr.cnes.regards.modules.processing.domain.storage.ExecutionLocalWorkdir;
import fr.cnes.regards.modules.processing.domain.storage.IExecutionLocalWorkdirService;
import fr.cnes.regards.modules.processing.domain.storage.ISharedStorageService;
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
