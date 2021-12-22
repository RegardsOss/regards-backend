/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.processing.storage;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.engine.IOutputToInputMapper;
import fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException;
import fr.cnes.regards.modules.processing.domain.exception.ProcessingOutputFileException;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingException.mustWrap;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.DELETE_OUTPUTFILE_ERROR;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.STORE_OUTPUTFILE_ERROR;
import static fr.cnes.regards.modules.processing.utils.ReactorErrorTransformers.errorWithContextMono;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.fromEpochMillisUTC;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileVisitResult;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.walkFileTree;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.OffsetDateTime;
import java.util.UUID;
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

/**
 * This class is the implementation for {@link ISharedStorageService}.
 *
 * @author gandrieu
 */
@Service
public class SharedStorageService implements ISharedStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedStorageService.class);

    static final Scheduler fileCopyScheduler = Schedulers.boundedElastic();

    private final Path basePath;

    @Autowired
    public SharedStorageService(@Qualifier("sharedStorageBasePath") Path basePath) {
        this.basePath = basePath;
    }

    @Override
    public Mono<Seq<POutputFile>> storeResult(ExecutionContext ctx, ExecutionLocalWorkdir workdir) {
        return Mono.defer(() -> {
            Path storageExecPath = getStorageExecPath(ctx.getExec().getId());
            Path execOutputPath = workdir.outputFolder();
            IOutputToInputMapper ioMapper = ctx.getProcess().getMapper();
            LOGGER.info("Storing outputs from {} in {}", execOutputPath, storageExecPath);

            return Flux.<Mono<POutputFile>>create(sink -> {
                try {
                    walkFileTree(execOutputPath, new SimpleFileVisitor<Path>() {

                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes)
                                throws IOException {
                            Mono<POutputFile> outputFile = storeOutputFile(ctx.getExec().getId(), path,
                                                                           workdir.outputFolder().relativize(path)
                                                                                   .normalize().toString(),
                                                                           List.empty()).map(
                                    out -> ioMapper.mapInputCorrelationIds(ctx, out));
                            sink.next(outputFile);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                            if (execOutputPath.equals(path)) {
                                sink.complete();
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Failed to store results", e);
                    sink.error(e);
                }
            }).flatMap(mono -> mono).collect(List.collector());
        });
    }

    @Override
    public Mono<POutputFile> storeOutputFile(UUID execId, Path outputFileOldPath, String outputFilename,
            List<String> inputFileCorrelationIds) {
        return Mono.fromCallable(() -> {
                    long size = outputFileOldPath.toFile().length();
                    Path storedFilePath = storeFile(outputFileOldPath, execId);
                    return createOutputFile(execId, storedFilePath, outputFilename, inputFileCorrelationIds, size);
                }).onErrorResume(mustWrap(), errorWithContextMono(PExecution.class,
                                                                  (exec, t) -> new StoreOutputfileException(exec, String.format(
                                                                          "Failed to store %s in %s", outputFileOldPath,
                                                                          getStorageExecPath(execId)), t)))
                .subscribeOn(fileCopyScheduler);
    }

    @Override
    public POutputFile createOutputFile(UUID execId, Path storedFilePath, String outputFilename,
            List<String> inputFileCorrelationIds, long size) throws MalformedURLException {
        return new POutputFile(UUID.randomUUID(), execId, outputFilename,
                               new POutputFile.Digest("SHA-256", storedFilePath.getFileName().toString()),
                               storedFilePath.toUri().toURL(), size, inputFileCorrelationIds,
                               creationTime(storedFilePath), false, false, false);
    }

    @Override
    public Path storeFile(Path filePathToCopy, UUID executionContextId) throws IOException {
        String checksum = Files.asByteSource(filePathToCopy.toFile()).hash(Hashing.sha256()).toString();
        Path storageExecPath = getStorageExecPath(executionContextId);
        Path storedFilePath = storageExecPath.resolve(checksum);

        LOGGER.debug("Storing outputs from {} to {}", filePathToCopy, storedFilePath);
        createDirectories(storageExecPath);
        copy(filePathToCopy, storedFilePath);
        return storedFilePath;
    }

    @Override
    public Mono<POutputFile> deleteFile(POutputFile outFile) {
        LOGGER.info("outFile={} - Deleting", outFile.getId());
        return Mono.fromCallable(() -> {
            Path path = Paths.get(outFile.getUrl().toURI());
            FileUtils.forceDelete(path.toFile());
            return outFile;
        }).onErrorResume(t -> {
            DeleteOutputfileException err = new DeleteOutputfileException(outFile, "Failed to delete file", t);
            LOGGER.error(err.getMessage());
            return Mono.just(outFile);
        });
    }

    private OffsetDateTime creationTime(Path storedFilePath) {
        try {
            return fromEpochMillisUTC(storedFilePath.toFile().lastModified());
        } catch (RuntimeException e) {
            // Really not grave if we don't have the actual date...
            LOGGER.debug("Could not get the actual date", e);
            return nowUtc();
        }
    }

    public static class StoreOutputfileException extends ProcessingExecutionException {

        public StoreOutputfileException(PExecution exec, String message, Throwable throwable) {
            super(STORE_OUTPUTFILE_ERROR, exec, message, throwable);
        }
    }

    public static class DeleteOutputfileException extends ProcessingOutputFileException {

        public DeleteOutputfileException(POutputFile outFile, String message, Throwable throwable) {
            super(DELETE_OUTPUTFILE_ERROR, outFile, message, throwable);
        }
    }

    public Path getStorageExecPath(UUID executionContextId) {
        return basePath.resolve(executionContextId.toString());
    }
}