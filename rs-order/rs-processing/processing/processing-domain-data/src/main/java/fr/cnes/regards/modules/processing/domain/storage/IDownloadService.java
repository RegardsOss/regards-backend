package fr.cnes.regards.modules.processing.domain.storage;

import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public interface IDownloadService {

    Mono<Path> download(ExecutionFileParameterValue file, Path dest);

}
