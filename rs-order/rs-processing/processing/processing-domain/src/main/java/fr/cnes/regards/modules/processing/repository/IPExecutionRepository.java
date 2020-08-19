package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface IPExecutionRepository {

    Mono<PExecution> save(PExecution execution);

    Mono<PExecution> findById(UUID id);

    Flux<PExecution> getTimedOutExecutions();

    Flux<PExecution> findByTenantAndCurrentStatusIn(
            String tenant,
            List<ExecutionStatus> status,
            Pageable page
    );

}
