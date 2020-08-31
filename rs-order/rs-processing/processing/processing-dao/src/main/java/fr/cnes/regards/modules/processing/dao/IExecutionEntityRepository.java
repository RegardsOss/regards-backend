package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import io.vavr.control.Option;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@InstanceEntity @Repository public interface IExecutionEntityRepository
        extends ReactiveCrudRepository<ExecutionEntity, UUID> {

    /**
     * We look for executions whose last recorded step is RUNNING, and its difference between recording time
     * and now is greater than the duration declared in the corresponding execution.
     */
    @Query(" SELECT * "
    + " FROM t_execution AS E "
    + " WHERE (steps#>>'{values,-1,status}') = 'RUNNING' "
    + "   AND EXTRACT(EPOCH FROM now()) - CAST(steps#>'{values,-1,epochTs}' AS BIGINT) / 1000 > (E.timeoutaftermillis / 1000) "
    )
    Flux<ExecutionEntity> getTimedOutExecutions();

    /*
    @Query(
    " SELECT"
        + " E.id as execId, E.fileParameters AS parameters, E.batch_id as batchId, E.steps as steps,"
        + " B.correlationId, B.process, B.userName "
    + " FROM t_execution AS E "
    + " INNER JOIN t_batch AS B ON B.id = E.batch_id "
    + " WHERE (steps#>>'{values,-1,status}') IN (:status) "
    + "   AND B.userName = :user "
    + "   AND B.tenant = :tenant "
    )*/
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

    Flux<ExecutionEntity> findByTenantAndUserNameAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
            String tenant,
            String userName,
            List<ExecutionStatus> status,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable page
    );
}
