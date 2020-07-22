package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PExecutionStep;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IPExecutionRepository {

    Mono<PExecution> save(PExecution entity);

    Mono<PExecution> findById(UUID id);

    Flux<PExecution> getTimedOutExecutions();

}
