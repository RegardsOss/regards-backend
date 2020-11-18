package fr.cnes.regards.modules.processing.entity.mapping;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.entity.BatchEntity;

public class BatchMapperTest extends AbstractDomainEntityMapperTest<PBatch, BatchEntity> {

    @Override DomainEntityMapper.Batch makeMapper() { return new BatchMapper(); }
    @Override Class<PBatch> domainClass() { return PBatch.class; }
    @Override Class<BatchEntity> entityClass() { return BatchEntity.class; }

}
