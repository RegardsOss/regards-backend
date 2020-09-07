package fr.cnes.regards.modules.processing.domain.repository;

import fr.cnes.regards.modules.processing.domain.PBatch;
import jdk.nashorn.internal.objects.NativeArray;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.print.Pageable;
import java.util.UUID;

public interface IPBatchRepository {

    Mono<PBatch> save(PBatch entity);

    Mono<PBatch> findById(UUID id);

}
