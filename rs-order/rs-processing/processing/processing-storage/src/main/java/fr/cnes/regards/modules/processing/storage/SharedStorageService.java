package fr.cnes.regards.modules.processing.storage;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException;
import fr.cnes.regards.modules.processing.domain.exception.ProcessingOutputFileException;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.OffsetDateTime;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException.mustWrap;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.DELETE_OUTPUTFILE_ERROR;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.STORE_OUTPUTFILE_ERROR;
import static fr.cnes.regards.modules.processing.utils.ReactorErrorTransformers.errorWithContextMono;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.fromEpochMillisUTC;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;
import static java.nio.file.Files.*;

@Service
public class SharedStorageService implements ISharedStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedStorageService.class);

    static final Scheduler fileCopyScheduler = Schedulers.boundedElastic();

    private final Path basePath;

    @Autowired
    public SharedStorageService(@Qualifier("sharedStorageBasePath") Path basePath) {
        this.basePath = basePath;
    }

    @Override public Mono<Seq<POutputFile>> storeResult(ExecutionContext ctx) {
        return Mono.defer(() -> {
            Path storageExecPath = basePath.resolve(ctx.getExec().getId().toString());
            ExecutionLocalWorkdir workdir = ctx.getWorkdir();
            Path execOutputPath = workdir.outputFolder();
            return Flux.<Mono<POutputFile>>create(sink -> {
                try {
                    walkFileTree(execOutputPath, new SimpleFileVisitor<Path>() {
                        @Override public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes)
                                throws IOException {
                            sink.next(createOutputFile(ctx.getExec().getId(), path, storageExecPath));
                            return FileVisitResult.CONTINUE;
                        }
                        @Override public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                            if (execOutputPath.equals(path)) {
                                sink.complete();
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
                catch(Exception e) {
                    sink.error(e);
                }
            })
            .flatMap(mono -> mono)
            .collect(List.collector());
        });
    }

    @Override public Mono<POutputFile> delete(POutputFile outFile) {
        LOGGER.info("outFile={} - Deleting", outFile.getId());
        return Mono.fromCallable(() -> {
            Path path = Paths.get(outFile.getUrl().toURI());
            FileUtils.forceDelete(path.toFile());
            return outFile;
        })
        .onErrorResume(t -> {
            DeleteOutputfileException err = new DeleteOutputfileException(outFile, "Failed to delete file", t);
            LOGGER.error(err.getMessage());
            return Mono.just(outFile);
        });
    }

    private Mono<POutputFile> createOutputFile(UUID execId, Path outputtedFile, Path storageExecPath) {
        return Mono.fromCallable(() -> {
            long size = outputtedFile.toFile().length();
            POutputFile.Digest checksum = checksum(outputtedFile);
            Path storedFilePath = storageExecPath.resolve(checksum.getValue());
            createDirectories(storageExecPath);
            copy(outputtedFile, storedFilePath);
            return new POutputFile(
                UUID.randomUUID(),
                execId,
                outputtedFile.getFileName().toString(),
                checksum,
                storedFilePath.toUri().toURL(),
                size,
                creationTime(storedFilePath),
                false,
                false,
                false
            );
        })
        .onErrorResume(mustWrap(), errorWithContextMono(
            PExecution.class,
            (exec, t) -> new StoreOutputfileException(
                exec,
                String.format("Failed to store %s in %s", outputtedFile, storageExecPath),
                t
            )
        ))
        .subscribeOn(fileCopyScheduler);
    }

    private POutputFile.Digest checksum(Path outputtedFile) throws IOException {
        String value = Files.asByteSource(outputtedFile.toFile()).hash(Hashing.sha256()).toString();
        return new POutputFile.Digest("SHA-256", value);
    }

    private OffsetDateTime creationTime(Path storedFilePath) {
        try {
            return fromEpochMillisUTC(storedFilePath.toFile().lastModified());
        }
        catch(Exception e) {
            // Really not grave if we don't have the actual date...
            return nowUtc();
        }
    }


    public static class StoreOutputfileException extends ProcessingExecutionException {
        public StoreOutputfileException(PExecution exec, String message,
                Throwable throwable) {
            super(STORE_OUTPUTFILE_ERROR, exec, message, throwable);
        }
    }

    public static class DeleteOutputfileException extends ProcessingOutputFileException {
        public DeleteOutputfileException(POutputFile outFile, String message, Throwable throwable) {
            super(DELETE_OUTPUTFILE_ERROR, outFile, message, throwable);
        }
    }
}
