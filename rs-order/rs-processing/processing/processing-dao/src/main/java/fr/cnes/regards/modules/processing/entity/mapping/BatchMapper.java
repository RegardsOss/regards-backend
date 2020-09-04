package fr.cnes.regards.modules.processing.entity.mapping;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.FileStatsByDataset;
import fr.cnes.regards.modules.processing.entity.ParamValues;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import org.springframework.stereotype.Component;

@Component
public class BatchMapper implements DomainEntityMapper.Batch {

    @Override
    public BatchEntity toEntity(PBatch batch) {
        return new BatchEntity(
                batch.getId(),
                batch.getProcessBusinessId(),
                batch.getCorrelationId(),
                batch.getTenant(),
                batch.getUser(),
                batch.getUserRole(),
                batch.getProcessName(),
                new ParamValues(batch.getUserSuppliedParameters().toJavaList()),
                new FileStatsByDataset(batch.getFilesetsByDataset().toJavaMap()),
                batch.isPersisted()
        );
    }

    @Override
    public PBatch toDomain(BatchEntity entity) {
        return new PBatch(
                entity.getCorrelationId(),
                entity.getId(),
                entity.getProcessBusinessId(),
                entity.getProcessName(),
                entity.getTenant(),
                entity.getUserEmail(),
                entity.getUserRole(),
                List.ofAll(entity.getParameters().getValues()),
                HashMap.ofAll(entity.getFilesets().getMap()),
                entity.isPersisted()
        );
    }

}
