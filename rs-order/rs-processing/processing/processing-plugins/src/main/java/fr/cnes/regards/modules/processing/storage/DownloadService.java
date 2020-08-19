package fr.cnes.regards.modules.processing.storage;

import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import io.vavr.collection.Set;
import lombok.Data;
import lombok.Value;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static fr.cnes.regards.framework.utils.file.DownloadUtils.getInputStreamThroughProxy;

@Data @Service
public class DownloadService implements IDownloadService {

    private final Proxy proxy;

    @Qualifier("nonProxyHosts")
    private final Set<String> nonProxyHosts;

    private final IStorageRestClient storageClient;

    @Override public Mono<Path> download(ExecutionFileParameterValue file, Path dest) {
        return createParentFolderIfNeeded(dest)
            .flatMap(d -> discriminateInternalExternal(file, d));
    }

    private Mono<Path> discriminateInternalExternal(ExecutionFileParameterValue file, Path dest) {
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
            try (InputStream is = storageClient.downloadFile(checksum).body().asInputStream()) {
                FileUtils.copyToFile(is, dest.toFile());
            }
            return dest;
        });
    }

    private Mono<Path> externalDownload(URL url, Path dest) {
        return Mono.fromCallable(() -> {
            try (InputStream is = getInputStreamThroughProxy(url, proxy, nonProxyHosts.toJavaSet(), 10_000)) {
                FileUtils.copyToFile(is, dest.toFile());
            }
            return dest;
        });
    }

}
