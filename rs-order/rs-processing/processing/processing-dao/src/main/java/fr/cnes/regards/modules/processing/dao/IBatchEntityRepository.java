package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@InstanceEntity
@Repository
public interface IBatchEntityRepository extends ReactiveCrudRepository<BatchEntity, UUID> {

}
