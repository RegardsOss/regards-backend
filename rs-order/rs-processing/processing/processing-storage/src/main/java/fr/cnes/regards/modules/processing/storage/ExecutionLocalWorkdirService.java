package fr.cnes.regards.modules.processing.storage;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException;
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

import static fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException.mustWrap;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.WORKDIR_CREATION_ERROR;

@Service
public class ExecutionLocalWorkdirService implements IExecutionLocalWorkdirService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionLocalWorkdirService.class);

    private final Path basePath;

    private final DownloadService downloadService;

    public ExecutionLocalWorkdirService(@Qualifier("executionWorkdirParentPath") Path basePath, DownloadService downloadService) {
        this.basePath = basePath;
        this.downloadService = downloadService;
    }

    public Mono<ExecutionLocalWorkdir> makeWorkdir(PExecution exec) {
        return Mono.fromCallable(() -> {
            ExecutionLocalWorkdir executionLocalWorkdir = new ExecutionLocalWorkdir(
                    basePath.resolve(exec.getId().toString()));
            Files.createDirectories(executionLocalWorkdir.inputFolder());
            Files.createDirectories(executionLocalWorkdir.outputFolder());
            return executionLocalWorkdir;
        })
        .onErrorMap(mustWrap(), t -> new MakeWorkdirException(exec, "Unable to create workdir", t));
    }

    public Mono<ExecutionLocalWorkdir> writeInputFilesToWorkdirInput(
            ExecutionLocalWorkdir workdir,
            Seq<ExecutionFileParameterValue> inputFiles
    ) {
        return Unit.fromCallable(() -> {
                Files.createDirectories(workdir.inputFolder());
            })
            .flatMapMany(x -> Flux.fromIterable(inputFiles))
            .parallel(8)
            .flatMap(f -> download(f, workdir.inputFolder().resolve(f.getLocalRelativePath())))
            .reduce((x,y) -> x)
            .map(x -> workdir)
            .onErrorResume(t -> cleanupWorkdir(workdir).flatMap(x -> Mono.error(t)));
    }

    private Mono<Path> download(ExecutionFileParameterValue src, Path dst) {
        return downloadService
            .download(src, dst)
            .retry(2L) // Allow some noise on the network and retry a little
            ;
    }

    public Mono<ExecutionLocalWorkdir> cleanupWorkdir(ExecutionLocalWorkdir workdir) {
        return Mono.fromCallable(() -> {
            try {
                FileSystemUtils.deleteRecursively(workdir.getBasePath());
            } catch (IOException e) {
                LOGGER.warn("Could not delete workdir {} following failure to download input files.", workdir.getBasePath(), e);
            }
            return workdir;
        });
    }

    public static class MakeWorkdirException extends ProcessingExecutionException {
        public MakeWorkdirException(PExecution exec, String message, Throwable t) {
            super(WORKDIR_CREATION_ERROR, exec, message, t);
        }
    }

}
