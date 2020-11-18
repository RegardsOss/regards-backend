package fr.cnes.regards.modules.processing.storage;

import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.processing.client.IReactiveStorageClient;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.service.IDownloadService;
import io.vavr.collection.Set;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException.mustWrap;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.EXTERNAL_DOWNLOAD_ERROR;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.INTERNAL_DOWNLOAD_ERROR;
import static fr.cnes.regards.modules.processing.utils.ReactorErrorTransformers.errorWithContextMono;

@Service
public class DownloadService implements IDownloadService {

    private final Proxy proxy;

    @Qualifier("nonProxyHosts")
    private final Set<String> nonProxyHosts;

    private final IReactiveStorageClient storageClient;

    @Autowired
    public DownloadService(Proxy proxy, Set<String> nonProxyHosts, IReactiveStorageClient storageClient) {
        this.proxy = proxy;
        this.nonProxyHosts = nonProxyHosts;
        this.storageClient = storageClient;
    }

    @Override public Mono<Path> download(PInputFile file, Path dest) {
        return createParentFolderIfNeeded(dest)
            .flatMap(d -> discriminateInternalExternal(file, d));
    }

    private Mono<Path> discriminateInternalExternal(PInputFile file, Path dest) {
        return file.getInternal()
            ? internalDownload(file.getChecksum(), dest)
            : externalDownload(file.getUrl(), dest);
    }

    private Mono<Path> createParentFolderIfNeeded(Path dest) {
        return Mono.fromCallable(() -> {
            Files.createDirectories(dest.getParent());
            return dest;
        });
    }

    private Mono<Path> internalDownload(String checksum, Path dest) {
        return Mono.fromCallable(() -> {
            Files.createDirectories(dest.getParent());
            Flux<DataBuffer> dataBufferFlux = storageClient.downloadFile(checksum);
            return DataBufferUtils.write(dataBufferFlux, dest, StandardOpenOption.WRITE);
        })
        .flatMap(voidMono -> voidMono.map(n -> dest))
        .onErrorResume(mustWrap(), errorWithContextMono(
            PExecution.class,
            (exec, t) -> new InternalDownloadException(
                exec,
                "Failed to download internal " + checksum + " into " + dest,
                t
            )
        ));
    }

    private Mono<Path> externalDownload(URL url, Path dest) {
        return Mono.fromCallable(() -> {
            try (InputStream is = DownloadUtils.getInputStreamThroughProxy(url, proxy, nonProxyHosts.toJavaSet(), 10_000)) {
                FileUtils.copyToFile(is, dest.toFile());
            }
            return dest;
        })
        .onErrorResume(mustWrap(), errorWithContextMono(
            PExecution.class,
            (exec, t) -> new ExternalDownloadException(
                exec,
                String.format("Failed to download external %s into %s", url, dest),
                t
            )
        ));
    }

    public static class InternalDownloadException extends ProcessingExecutionException {
        public InternalDownloadException(PExecution exec, String message, Throwable t) {
            super(INTERNAL_DOWNLOAD_ERROR, exec, message, t);
        }
    }

    public static class ExternalDownloadException extends ProcessingExecutionException {
        public ExternalDownloadException(PExecution exec, String message, Throwable t) {
            super(EXTERNAL_DOWNLOAD_ERROR, exec, message, t);
        }
    }
}
