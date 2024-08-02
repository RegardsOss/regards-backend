/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.framework.s3.test;

import fr.cnes.regards.framework.s3.client.S3HighLevelReactiveClient;
import fr.cnes.regards.framework.s3.domain.*;
import fr.cnes.regards.framework.s3.dto.StorageConfigDto;
import fr.cnes.regards.framework.s3.utils.StorageConfigUtils;
import fr.cnes.regards.modules.fileaccess.plugin.domain.FileStorageWorkingSubset;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Utils method to manage files on a S3 server
 *
 * @author Thibaud Michaudel
 **/
public final class S3FileTestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3FileTestUtils.class);

    private static final String THREAD_PREFIX = "s3-reactive-client-test";

    private static final int MULTIPART_THRESHOLD_MB = 5;

    private static final int MULTIPART_UPLOAD_PREFETCH = 1;

    /**
     * Store files on a s3 server
     *
     * @param workingSet         the files to be stored
     * @param s3Server           the s3Server where the files will be stored
     * @param fileIdentification define the name of the file to be used in the s3Server, see {@link FileIdentificationEnum}
     */
    public static void store(FileStorageWorkingSubset workingSet,
                             S3Server s3Server,
                             FileIdentificationEnum fileIdentification) {
        Stream.ofAll(workingSet.getFileReferenceRequests()).flatMap(requestDto -> Try.of(() -> {
            FileStorageRequestAggregation request = FileStorageRequestAggregation.fromDto(requestDto);
            LOGGER.info("Start storing {}", request.getOriginUrl());
            URL sourceUrl = new URL(request.getOriginUrl());

            request.getMetaInfo().setFileSize(FileTestUtils.getFileSize(sourceUrl));

            Flux<ByteBuffer> buffers = DataBufferUtils.readInputStream(sourceUrl::openStream,
                                                                       new DefaultDataBufferFactory(),
                                                                       MULTIPART_THRESHOLD_MB * 1024 * 1024)
                                                      .map(DataBuffer::asByteBuffer);

            StorageConfigDto storageConfig = new StorageConfigBuilder(s3Server).rootPath(request.getStorageSubDirectory())
                                                                               .build();

            StorageCommandID cmdId = new StorageCommandID(request.getJobId(), UUID.randomUUID());

            String entryKey = null;
            switch (fileIdentification) {
                case CHECKSUM:
                    entryKey = request.getMetaInfo().getChecksum();
                    break;
                case FILENAME:
                    entryKey = Paths.get(StorageConfigUtils.entryKey(storageConfig, request.getOriginUrl()))
                                    .getFileName()
                                    .toString();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown file identification");
            }

            StorageEntry storageEntry = StorageEntry.builder()
                                                    .config(storageConfig)
                                                    .fullPath(new File(storageConfig.getRootPath(),
                                                                       entryKey).toString())
                                                    .checksum(entryChecksum(request))
                                                    .size(entrySize(request))
                                                    .data(buffers)
                                                    .build();

            StorageCommand.Write writeCmd = new StorageCommand.Write.Impl(storageConfig,
                                                                          cmdId,
                                                                          entryKey,
                                                                          storageEntry,
                                                                          request.getMetaInfo().getChecksum());

            Scheduler scheduler = Schedulers.newParallel(THREAD_PREFIX, 10);
            int maxBytesPerPart = MULTIPART_THRESHOLD_MB * 1024 * 1024;
            try (S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(scheduler,
                                                                                  maxBytesPerPart,
                                                                                  MULTIPART_UPLOAD_PREFETCH)) {

                return client.write(writeCmd)
                             .flatMap(writeResult -> writeResult.matchWriteResult(Mono::just,
                                                                                  unreachable -> Mono.error(new RuntimeException(
                                                                                      "Unreachable endpoint")),
                                                                                  failure -> Mono.error(new RuntimeException(
                                                                                      "Write failure in S3 storage"))))
                             .doOnError(t -> {
                                 LOGGER.error("End storing {}", request.getOriginUrl(), t);
                             })
                             .doOnSuccess(success -> {
                                 LOGGER.info("End storing {}", request.getOriginUrl());
                             })
                             .block();
            }
        }));
    }

    public static void deleteAllFilesFromRoot(StorageConfigDto s3Server, String rootPath) {
        try (S3HighLevelReactiveClient client = getS3HighLevelReactiveClient()) {
            client.deleteWithPrefix(StorageCommand.delete(s3Server,
                                                          new StorageCommandID("", UUID.randomUUID()),
                                                          rootPath))
                  .flatMap(r -> r.matchDeleteResult(Mono::just,
                                                    unreachable -> Mono.error(new RuntimeException(String.format(
                                                        "Unreachable [endpoint: %s] : %s [bucket: %s]",
                                                        s3Server.getEndpoint(),
                                                        unreachable.getThrowable().getMessage(),
                                                        s3Server.getBucket()))),
                                                    failure -> Mono.error(new RuntimeException(String.format(
                                                        "Delete failure [bucket: %s] [endpoint: %s]",
                                                        s3Server.getBucket(),
                                                        s3Server.getEndpoint())))))

                  .doOnError(t -> LOGGER.error("Failed [bucket: {}] to delete file {} [endpoint: {}]:",
                                               s3Server.getBucket(),
                                               rootPath,
                                               s3Server.getEndpoint(),
                                               t))
                  .doOnSuccess(success -> LOGGER.info("Success [bucket: {}] end deleting of file {} [endpoint: {}]",
                                                      s3Server.getBucket(),
                                                      rootPath,
                                                      s3Server.getEndpoint()))
                  .block();
        }
    }

    private static Option<Long> entrySize(FileStorageRequestAggregation request) {
        return Option.some(request.getMetaInfo().getFileSize());
    }

    private static Option<Tuple2<String, String>> entryChecksum(FileStorageRequestAggregation request) {
        return Option.some(Tuple.of(request.getMetaInfo().getAlgorithm(), request.getMetaInfo().getChecksum()));
    }

    private S3FileTestUtils() {
    }

    private static S3HighLevelReactiveClient getS3HighLevelReactiveClient() {
        Scheduler scheduler = Schedulers.newParallel("s3-reactive-client", 10);
        int maxBytesPerPart = 5 * 1024 * 1024;
        S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(scheduler, maxBytesPerPart, 10);
        return client;
    }
}
