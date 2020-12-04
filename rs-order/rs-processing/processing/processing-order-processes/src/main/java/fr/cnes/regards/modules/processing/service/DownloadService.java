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
package fr.cnes.regards.modules.processing.service;

import static fr.cnes.regards.modules.processing.exceptions.ProcessingException.mustWrap;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.EXTERNAL_DOWNLOAD_ERROR;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.INTERNAL_DOWNLOAD_ERROR;
import static fr.cnes.regards.modules.processing.utils.ReactorErrorTransformers.errorWithContextMono;

import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException;
import fr.cnes.regards.modules.processing.domain.service.IDownloadService;
import fr.cnes.regards.modules.processing.order.OrderInputFileMetadata;
import fr.cnes.regards.modules.processing.order.OrderInputFileMetadataMapper;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import io.vavr.collection.Set;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * This class provides implementations for downloading files from storage or through a proxy.
 *
 * @author gandrieu
 */
@Service
@MultitenantTransactional
public class DownloadService implements IDownloadService {

    private static final OrderInputFileMetadataMapper mapper = new OrderInputFileMetadataMapper();

    private static final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    private final Proxy proxy;
    private final Set<String> nonProxyHosts;
    private final IStorageRestClient storageClient;
    private final IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    public DownloadService(Proxy proxy, @Qualifier("nonProxyHosts") Set<String> nonProxyHosts,
                           IStorageRestClient storageClient,
                           IRuntimeTenantResolver runtimeTenantResolver) {
        this.proxy = proxy;
        this.nonProxyHosts = nonProxyHosts;
        this.storageClient = storageClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public Mono<Path> download(PInputFile file, Path dest) {
        return createParentFolderIfNeeded(dest).flatMap(d -> discriminateInternalExternal(file, d));
    }

    private Mono<Path> discriminateInternalExternal(PInputFile file, Path dest) {
        boolean internal = mapper.fromMap(file.getMetadata()).map(OrderInputFileMetadata::getInternal).getOrElse(false);

        return internal ? internalDownload(file.getChecksum(), dest) : externalDownload(file.getUrl(), dest);
    }

    private Mono<Path> createParentFolderIfNeeded(Path dest) {
        return Mono.fromCallable(() -> {
            Files.createDirectories(dest.getParent());
            return dest;
        });
    }

    private Mono<Path> internalDownload(String checksum, Path dest) {
        return Mono.subscriberContext()
            .map(ctx -> ctx.get(PExecution.class))
            .map(PExecution::getTenant)
            .flatMap(tenant -> internalDownloadWithTenant(checksum, dest, tenant));
    }

    private Mono<Path> internalDownloadWithTenant(String checksum, Path dest, String tenant) {
        return Mono.fromCallable(() -> {
            Files.createDirectories(dest.getParent());
            Flux<DataBuffer> dataBufferFlux = downloadUsingStorageRestClient(tenant, checksum);
            return DataBufferUtils.write(dataBufferFlux, dest, StandardOpenOption.WRITE);
        })
        .flatMap(voidMono -> voidMono.map(n -> dest))
        .onErrorResume(mustWrap(),
                       errorWithContextMono(PExecution.class, (exec, t) -> new InternalDownloadException(exec,
                               "Failed to download internal " + checksum + " into " + dest, t)));
    }

    public Flux<DataBuffer> downloadUsingStorageRestClient(String tenant, String checksum) {
        return DataBufferUtils.readInputStream(
            () -> {
                try {
                    runtimeTenantResolver.forceTenant(tenant);
                    return storageClient.downloadFile(checksum).body().asInputStream();
                }
                finally {
                    runtimeTenantResolver.clearTenant();
                }
            },
            bufferFactory,
            4096
        );
    }

    private Mono<Path> externalDownload(URL url, Path dest) {
        return Mono.fromCallable(() -> {
            try (InputStream is = DownloadUtils.getInputStreamThroughProxy(url, proxy, nonProxyHosts.toJavaSet(),
                                                                           10_000)) {
                FileUtils.copyToFile(is, dest.toFile());
            }
            return dest;
        }).onErrorResume(mustWrap(),
                         errorWithContextMono(PExecution.class, (exec, t) -> new ExternalDownloadException(exec,
                                 String.format("Failed to download external %s into %s", url, dest), t)));
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
