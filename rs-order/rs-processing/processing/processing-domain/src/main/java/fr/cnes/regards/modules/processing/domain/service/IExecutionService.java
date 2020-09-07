package fr.cnes.regards.modules.processing.domain.service;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IExecutionService {

    Mono<PExecution> launchExecution(PExecutionRequestEvent request);

    Mono<PExecution> runExecutable(UUID execId);

    void scheduledTimeoutNotify();

}
