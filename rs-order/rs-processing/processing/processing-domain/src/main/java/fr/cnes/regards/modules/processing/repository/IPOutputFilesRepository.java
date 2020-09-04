package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface IPOutputFilesRepository {

    Flux<POutputFile> save(Flux<POutputFile> files);

    Flux<POutputFile> findByExecId(UUID execId);

}
