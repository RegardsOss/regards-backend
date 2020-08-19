package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.PStepSequence;
import fr.cnes.regards.modules.processing.service.events.PExecutionRequestEvent;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface IExecutionService {

    Mono<PExecution> launchExecution(PExecutionRequestEvent request);

    Mono<PExecution> addExecutionStep(PExecution exec, PStep step);

    Mono<Duration> estimateDuration(PBatch batch, PExecutionRequestEvent request);

}
