package fr.cnes.regards.modules.processing.domain.service;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IExecutionService {

    Mono<PExecution> launchExecution(PExecutionRequestEvent request);

    void scheduledTimeoutNotify();

    Mono<ExecutionContext> createContext(UUID execId);

    default Mono<PExecution> runExecutable(UUID execId) {
        return createContext(execId)
                .flatMap(ctx -> ctx.getProcess().getEngine().run(ctx));
    }

}
