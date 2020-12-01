package fr.cnes.regards.modules.processing.domain.service;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import io.vavr.collection.List;
import reactor.core.publisher.Flux;

import java.net.URL;
import java.util.UUID;

public interface IOutputFileService {

    Flux<POutputFile> markDownloaded(List<URL> urls);

    void scheduledDeleteDownloadedFiles();

}
