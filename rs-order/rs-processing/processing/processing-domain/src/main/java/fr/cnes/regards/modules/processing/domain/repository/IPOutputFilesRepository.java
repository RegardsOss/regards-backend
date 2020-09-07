package fr.cnes.regards.modules.processing.domain.repository;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import io.vavr.collection.List;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface IPOutputFilesRepository {

    Flux<POutputFile> save(Flux<POutputFile> files);

    Flux<POutputFile> findByExecId(UUID execId);

    Flux<POutputFile> findByIdIn(List<UUID> ids);

    Flux<POutputFile> findByDownloadedIsTrueAndDeletedIsFalse();
}
