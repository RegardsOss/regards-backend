package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.processing.entity.OutputFileEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@InstanceEntity
@Repository
public interface IOutputFileEntityRepository extends ReactiveCrudRepository<OutputFileEntity, UUID> {

    Flux<OutputFileEntity> findByExecId(UUID execId);

}
