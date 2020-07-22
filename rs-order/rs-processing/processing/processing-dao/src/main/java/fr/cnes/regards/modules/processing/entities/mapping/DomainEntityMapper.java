package fr.cnes.regards.modules.processing.entities.mapping;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PExecutionStep;
import fr.cnes.regards.modules.processing.entities.BatchEntity;
import fr.cnes.regards.modules.processing.entities.ExecutionEntity;
import fr.cnes.regards.modules.processing.entities.ExecutionStepEntity;
import reactor.core.publisher.Mono;

public interface DomainEntityMapper {

    BatchEntity toEntity(PBatch batch);

    Mono<PBatch> toDomain(BatchEntity batch);


    ExecutionEntity toEntity(PExecution exec);

    Mono<PExecution> toDomain(ExecutionEntity exec);


    ExecutionStepEntity toEntity(PExecutionStep step);

    Mono<PExecutionStep> toDomain(ExecutionStepEntity step);

}

