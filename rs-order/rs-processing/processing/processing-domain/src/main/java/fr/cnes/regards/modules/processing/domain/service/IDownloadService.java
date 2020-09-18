package fr.cnes.regards.modules.processing.domain.service;

import fr.cnes.regards.modules.processing.domain.PInputFile;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public interface IDownloadService {

    Mono<Path> download(PInputFile file, Path dest);

}
