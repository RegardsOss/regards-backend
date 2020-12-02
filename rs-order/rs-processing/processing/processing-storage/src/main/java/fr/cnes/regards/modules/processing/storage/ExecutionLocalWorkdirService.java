/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import static fr.cnes.regards.modules.processing.exceptions.ProcessingException.mustWrap;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.WORKDIR_CREATION_ERROR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException;
import fr.cnes.regards.modules.processing.domain.service.IDownloadService;
import fr.cnes.regards.modules.processing.utils.Unit;
import io.vavr.collection.Seq;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * TODO : Class description
 *
 * @author Guillaume Andrieu
 *
 */
@Service
public class ExecutionLocalWorkdirService implements IExecutionLocalWorkdirService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionLocalWorkdirService.class);

    private final Path basePath;

    private final IDownloadService downloadService;

    public ExecutionLocalWorkdirService(@Qualifier("executionWorkdirParentPath") Path basePath,
            IDownloadService downloadService) {
        this.basePath = basePath;
        this.downloadService = downloadService;
    }

    @Override
    public Mono<ExecutionLocalWorkdir> makeWorkdir(PExecution exec) {
        return Mono.fromCallable(() -> {
            ExecutionLocalWorkdir executionLocalWorkdir = new ExecutionLocalWorkdir(
                    basePath.resolve(exec.getId().toString()));
            Files.createDirectories(executionLocalWorkdir.inputFolder());
            Files.createDirectories(executionLocalWorkdir.outputFolder());
            return executionLocalWorkdir;
        }).onErrorMap(mustWrap(), t -> new MakeWorkdirException(exec, "Unable to create workdir", t));
    }

    @Override
    public Mono<ExecutionLocalWorkdir> writeInputFilesToWorkdirInput(ExecutionLocalWorkdir workdir,
            Seq<PInputFile> inputFiles) {
        return Unit.fromCallable(() -> {
            Files.createDirectories(workdir.inputFolder());
        }).flatMapMany(x -> Flux.fromIterable(inputFiles)).parallel(8)
                .flatMap(f -> download(f, workdir.inputFolder().resolve(f.getLocalRelativePath()))).reduce((x, y) -> x)
                .map(x -> workdir).onErrorResume(t -> cleanupWorkdir(workdir).flatMap(x -> Mono.error(t)));
    }

    private Mono<Path> download(PInputFile src, Path dst) {
        return downloadService.download(src, dst).retry(2L) // Allow some noise on the network and retry a little
        ;
    }

    @Override
    public Mono<ExecutionLocalWorkdir> cleanupWorkdir(ExecutionLocalWorkdir workdir) {
        return Mono.fromCallable(() -> {
            try {
                FileSystemUtils.deleteRecursively(workdir.getBasePath());
            } catch (IOException e) {
                LOGGER.warn("Could not delete workdir {} following failure to download input files.",
                            workdir.getBasePath(), e);
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
