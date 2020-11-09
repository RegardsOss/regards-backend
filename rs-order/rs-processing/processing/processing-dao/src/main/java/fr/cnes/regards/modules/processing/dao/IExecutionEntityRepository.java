package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@InstanceEntity
@Repository
public interface IExecutionEntityRepository
        extends ReactiveCrudRepository<ExecutionEntity, UUID> {

    /**
     * We look for executions whose last recorded step is RUNNING, and its difference between recording time
     * and now is greater than the duration declared in the corresponding execution.
     */
    @Query(" SELECT * "
    + " FROM t_execution AS E "
    + " WHERE E.current_status = 'RUNNING' "
    + "   AND EXTRACT(EPOCH FROM now()) - EXTRACT(EPOCH FROM E.last_updated) > (E.timeout_after_millis / 1000) "
    )
    Flux<ExecutionEntity> getTimedOutExecutions();

    Flux<ExecutionEntity> findByTenantAndCurrentStatusIn(
            String tenant,
            List<ExecutionStatus> status,
            Pageable page
    );

    Flux<ExecutionEntity> findByTenantAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
            String tenant,
            List<ExecutionStatus> status,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable page
    );

    Flux<ExecutionEntity> findByTenantAndUserEmailAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
            String tenant,
            String userEmail,
            List<ExecutionStatus> status,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable page
    );

    Flux<PExecution> findByProcessBusinessIdAndCurrentStatusIn(
            UUID processBusinessId,
            List<ExecutionStatus> nonFinalStatusList
    );
}
