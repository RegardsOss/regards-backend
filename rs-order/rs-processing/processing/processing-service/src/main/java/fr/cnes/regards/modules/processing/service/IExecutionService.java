package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PExecutionStep;
import fr.cnes.regards.modules.processing.domain.PExecutionStepSequence;
import fr.cnes.regards.modules.processing.service.events.PExecutionRequestEvent;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface IExecutionService {

    Mono<PExecution> launchExecution(PExecutionRequestEvent request);

    Mono<PExecutionStepSequence> saveExecutionStep(PExecutionStep step);

    Mono<Duration> estimateDuration(PBatch batch, PExecutionRequestEvent request);

}
