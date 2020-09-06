package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import io.vavr.collection.List;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface IOutputFileService {

    Flux<POutputFile> markDownloaded(List<UUID> ids);

    void scheduledDeleteDownloadedFiles();

}
