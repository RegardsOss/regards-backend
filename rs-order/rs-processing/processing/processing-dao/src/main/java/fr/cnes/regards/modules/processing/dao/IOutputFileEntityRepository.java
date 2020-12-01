package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.processing.entity.OutputFileEntity;
import io.vavr.Tuple2;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.net.URL;
import java.util.List;
import java.util.UUID;

@InstanceEntity
@Repository
public interface IOutputFileEntityRepository extends ReactiveCrudRepository<OutputFileEntity, UUID> {

    Flux<OutputFileEntity> findByExecId(UUID execId);

    Flux<OutputFileEntity> findByUrlIn(List<URL> urls);

    Flux<OutputFileEntity> findByDownloadedIsTrueAndDeletedIsFalse();
}
