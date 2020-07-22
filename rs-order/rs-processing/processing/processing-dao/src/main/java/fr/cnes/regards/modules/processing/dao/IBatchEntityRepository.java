package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.processing.entities.BatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@InstanceEntity
@Repository
public interface IBatchEntityRepository extends ReactiveCrudRepository<BatchEntity, UUID> {

}
