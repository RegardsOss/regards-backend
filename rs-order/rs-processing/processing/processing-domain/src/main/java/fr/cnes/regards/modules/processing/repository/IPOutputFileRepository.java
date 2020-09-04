package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IPOutputFileRepository {

    Mono<POutputFile> create(POutputFile file);

    Mono<POutputFile> setDeleted(POutputFile file);

    Mono<POutputFile> findByChecksum(String checksum);

    Mono<POutputFile> findByExecutionID(UUID execId);

}
