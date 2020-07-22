package fr.cnes.regards.modules.processing.storage;

import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

@Service
public class SharedStorageService implements ISharedStorageService {

    private final Path basePath;

    private Scheduler fileCopyScheduler = Schedulers.boundedElastic();

    @Autowired
    public SharedStorageService(@Qualifier("sharedStorageBasePath") Path basePath) {
        this.basePath = basePath;
    }

    @Override public Mono<Seq<POutputFile>> storeResult(ExecutionContext ctx, ExecutionLocalWorkdir workdir) {
        return Mono.defer(() -> {
            Path storageExecPath = basePath.resolve(ctx.getExec().getId().toString());
            Path execOutputPath = workdir.outputFolder();
            return Flux.<Mono<POutputFile>>create(sink -> {
                try {
                    Files.walkFileTree(execOutputPath, new SimpleFileVisitor<Path>() {
                        @Override public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes)
                                throws IOException {
                            sink.next(createOutputFile(path, storageExecPath));
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

    private Mono<POutputFile> createOutputFile(Path outputtedFile, Path storageExecPath) {
        return Mono.fromCallable(() -> {
            long size = outputtedFile.toFile().length();
            String checksum = ChecksumUtils.computeHexChecksum(outputtedFile, "MD5");
            Path storedFilePath = storageExecPath.resolve(checksum);
            Files.createDirectories(storageExecPath);
            Files.copy(outputtedFile, storedFilePath);
            return new POutputFile(
                outputtedFile.getFileName().toString(),
                checksum,
                storedFilePath.toUri().toURL(),
                size
            );
        })
        .subscribeOn(fileCopyScheduler);
    }
}
