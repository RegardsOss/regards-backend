package fr.cnes.regards.modules.processing.storage;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import fr.cnes.regards.modules.processing.utils.Unit;
import io.vavr.collection.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ExecutionLocalWorkdirService implements IExecutionLocalWorkdirService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionLocalWorkdirService.class);

    private final Path basePath;

    private final DownloadService downloadService;

    public ExecutionLocalWorkdirService(@Qualifier("executionWorkdirParentPath") Path basePath, DownloadService downloadService) {
        this.basePath = basePath;
        this.downloadService = downloadService;
    }

    public Mono<ExecutionLocalWorkdir> makeWorkdir(ExecutionContext ctx) {
        return Mono.fromCallable(() -> {
            ExecutionLocalWorkdir executionLocalWorkdir = new ExecutionLocalWorkdir(
                    basePath.resolve(ctx.getExec().getId().toString()));
            Files.createDirectories(executionLocalWorkdir.inputFolder());
            Files.createDirectories(executionLocalWorkdir.outputFolder());
            return executionLocalWorkdir;
        });
    }

    public Mono<Unit> writeInputFilesToWorkdirInput(ExecutionLocalWorkdir workdir, Seq<ExecutionFileParameterValue> inputFiles) {
        return Mono.fromCallable(() -> {
                Files.createDirectories(workdir.inputFolder());
                return Unit.UNIT;
            })
            .flatMapMany(x -> Flux.fromIterable(inputFiles))
            .parallel(8)
            .flatMap(f -> download(f, workdir.inputFolder().resolve(f.getLocalRelativePath())))
            .reduce((x,y) -> x)
            .doOnError(t -> cleanupWorkdir(workdir));
    }

    private Mono<Unit> download(ExecutionFileParameterValue src, Path dst) {
        return downloadService
            .download(src, dst)
            .retry(2L) // Allow some noise on the network and retry a little
            .map(x -> Unit.UNIT)
            .doOnError(t -> {
                LOGGER.error("Failed to download input file {} to path {}: {} - {}",
                    src, dst, t.getClass().getSimpleName(), t.getMessage());
            });
    }

    public Mono<Unit> cleanupWorkdir(ExecutionLocalWorkdir workdir) {
        return Mono.fromCallable(() -> {
            try {
                FileSystemUtils.deleteRecursively(workdir.getBasePath());
            } catch (IOException e) {
                LOGGER.warn("Could not delete workdir {} following failure to download input files.", workdir.getBasePath(), e);
            }
            return Unit.UNIT;
        });
    }

}
