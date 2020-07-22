package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.processing.entities.BatchEntity;
import fr.cnes.regards.modules.processing.entities.ExecutionEntity;
import io.vavr.Tuple2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@InstanceEntity
@Repository
public interface IExecutionEntityRepository extends ReactiveCrudRepository<ExecutionEntity, UUID> {

    /**
     * We look for executions whose last recorded step is RUNNING, and its difference between recording time
     * and now is greater than the duration declared in the corresponding execution.
     */
    @Query(
        "WITH L as (SELECT S.execution_id, MAX(S.id) last_row_id FROM t_execution_step AS S GROUP BY S.execution_id) " +
        " SELECT  E.* FROM t_execution AS E " +
        " INNER JOIN t_execution_step AS S ON S.execution_id = E.id " +
        " INNER JOIN L ON L.last_row_id = S.id " +
        " WHERE S.status = 'RUNNING' " +
        "   AND EXTRACT(EPOCH FROM now()) - EXTRACT(EPOCH FROM S.time) > (E.timeoutaftermillis / 1000) "
    )
    Flux<ExecutionEntity> getTimedOutExecutions();

}
