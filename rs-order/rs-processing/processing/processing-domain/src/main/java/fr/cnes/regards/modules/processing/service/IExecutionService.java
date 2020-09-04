package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.*;
import fr.cnes.regards.modules.processing.domain.engine.IExecutionEventNotifier;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import fr.cnes.regards.modules.processing.events.PExecutionRequestEvent;
import io.vavr.collection.Seq;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

public interface IExecutionService {

    Mono<ExecutionContext> createContext(PExecution exec, PBatch batch, PProcess process);

    Mono<PExecution> launchExecution(PExecutionRequestEvent request);

    Mono<Duration> estimateDuration(PBatch batch, Seq<ExecutionFileParameterValue> inputFiles);

    Mono<PExecution> runExecutable(UUID execId);

    IExecutionEventNotifier notifierFor(PExecution exec);

}
