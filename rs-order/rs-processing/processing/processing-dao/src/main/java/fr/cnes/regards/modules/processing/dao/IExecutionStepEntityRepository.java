package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.modules.processing.entities.ExecutionStepEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface IExecutionStepEntityRepository extends ReactiveCrudRepository<ExecutionStepEntity, Long> {

    @Query("SELECT * FROM t_execution_step WHERE execution_id = $1")
    Flux<ExecutionStepEntity> findAllByExecutionId(UUID execId);

}
