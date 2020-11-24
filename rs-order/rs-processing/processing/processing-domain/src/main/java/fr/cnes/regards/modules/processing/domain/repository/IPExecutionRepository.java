package fr.cnes.regards.modules.processing.domain.repository;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface IPExecutionRepository {

    Mono<PExecution> create(PExecution execution);

    Mono<PExecution> update(PExecution execution);

    Mono<PExecution> findById(UUID id);

    Flux<PExecution> getTimedOutExecutions();

    Flux<PExecution> findByTenantAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
            String tenant,
            List<ExecutionStatus> status,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable page
    );

    Mono<Integer> countByTenantAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
            String tenant,
            List<ExecutionStatus> status,
            OffsetDateTime from,
            OffsetDateTime to
    );

    Flux<PExecution> findByTenantAndUserEmailAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
            String tenant,
            String userEmail,
            List<ExecutionStatus> status,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable page
    );

    Mono<Integer> countByTenantAndUserEmailAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
            String tenant,
            String userEmail,
            List<ExecutionStatus> status,
            OffsetDateTime from,
            OffsetDateTime to
    );

    Mono<Integer> countByProcessBusinessIdAndStatusIn(UUID processBusinessId, Seq<ExecutionStatus> nonFinalStatusList);


}
