package fr.cnes.regards.modules.processing.entity.mapping;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import fr.cnes.regards.modules.processing.entity.OutputFileEntity;
import fr.cnes.regards.modules.processing.entity.StepEntity;

public interface DomainEntityMapper<D, E> {

    E toEntity(D domain);

    D toDomain(E entity);

    interface Batch extends DomainEntityMapper<PBatch, BatchEntity> {}
    interface Execution extends DomainEntityMapper<PExecution, ExecutionEntity> {}
    interface OutputFile extends DomainEntityMapper<POutputFile, OutputFileEntity> {}
    interface Step extends DomainEntityMapper<PStep, StepEntity> {}

}

