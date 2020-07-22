package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.modules.processing.domain.PExecutionStep;
import fr.cnes.regards.modules.processing.domain.PExecutionStepSequence;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IPExecutionStepRepository {

    Mono<PExecutionStep> save(PExecutionStep step);

    Mono<PExecutionStepSequence> findAllForExecution(UUID execId);

}
