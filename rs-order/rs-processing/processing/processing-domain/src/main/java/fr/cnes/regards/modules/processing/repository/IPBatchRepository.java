package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.modules.processing.domain.PBatch;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IPBatchRepository {

    Mono<PBatch> save(PBatch entity);

    Mono<PBatch> findById(UUID id);

}
