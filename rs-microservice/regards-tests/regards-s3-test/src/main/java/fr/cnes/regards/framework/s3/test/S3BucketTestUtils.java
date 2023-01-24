/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.s3.exception.S3ClientException;
import io.vavr.Tuple;
import io.vavr.control.Option;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Utils method to manage bucket in a S3 server
 *
 * @author Stephane Cortine
 */
public final class S3BucketTestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3BucketTestUtils.class);

    private static final String THREAD_PREFIX = "s3-reactive-client-test";

    private static final String ALGORITHM_MD5 = "MD5";

    private static final String FILE_PROTOCOL = "file:";

    private static final int MULTIPART_THRESHOLD_MB = 5;

    private static final int MULTIPART_UPLOAD_PREFETCH = 1;

    public static void createBucket(S3Server s3Server) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(s3Server.getKey(), s3Server.getSecret());
        S3Client s3Client = S3Client.builder()
                                    .endpointOverride(URI.create(s3Server.getEndpoint()))
                                    .region(Region.of(s3Server.getRegion()))
                                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                    .serviceConfiguration(S3Configuration.builder()
                                                                         .pathStyleAccessEnabled(true)
                                                                         .build())
                                    .build();

        if (s3Client.listBuckets().buckets().stream().noneMatch(bucket -> bucket.name().equals(s3Server.getBucket()))) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(s3Server.getBucket()).build());
        }
    }

    public static void deleteBucket(S3Server s3Server) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(s3Server.getKey(), s3Server.getSecret());
        S3Client s3Client = S3Client.builder()
                                    .endpointOverride(URI.create(s3Server.getEndpoint()))
                                    .region(Region.of(s3Server.getRegion()))
                                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                    .serviceConfiguration(S3Configuration.builder()
                                                                         .pathStyleAccessEnabled(true)
                                                                         .build())
                                    .build();

        if (s3Client.listBuckets().buckets().stream().anyMatch(bucket -> bucket.name().equals(s3Server.getBucket()))) {
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                                                                            .bucket(s3Server.getBucket())
                                                                            .build();
            ListObjectsV2Response listObjectsV2Response;
            do {
                listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
                for (S3Object s3Object : listObjectsV2Response.contents()) {
                    DeleteObjectRequest request = DeleteObjectRequest.builder()
                                                                     .bucket(s3Server.getBucket())
                                                                     .key(s3Object.key())
                                                                     .build();
                    s3Client.deleteObject(request);
                }
            } while (listObjectsV2Response.isTruncated());
            s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(s3Server.getBucket()).build());
        }
    }

    public static void store(String sourceFile, String rootPath, S3Server s3Server) throws IOException {
        URL sourceUrl = new URL(FILE_PROTOCOL + sourceFile);

        Path sourceFilePath = Path.of(sourceFile);

        String checksum;
        try (InputStream stream = Files.newInputStream(sourceFilePath)) {
            checksum = DigestUtils.md5Hex(stream).toUpperCase();
        }

        Flux<ByteBuffer> buffers = DataBufferUtils.readInputStream(sourceUrl::openStream,
                                                                   new DefaultDataBufferFactory(),
                                                                   MULTIPART_THRESHOLD_MB * 1024 * 1024)
                                                  .map(DataBuffer::asByteBuffer);

        StorageConfig storageConfig = buildStorageConfiguration(rootPath, s3Server);
        StorageCommandID cmdId = new StorageCommandID("test_id", UUID.randomUUID());
        String entryKey = storageConfig.entryKey(sourceFilePath.getFileName().toString());

        StorageEntry entry = StorageEntry.builder()
                                         .checksum(Option.of(Tuple.of(ALGORITHM_MD5, checksum)))
                                         .config(storageConfig)
                                         .size(Option.some(FileTestUtils.getFileSize(sourceUrl)))
                                         .fullPath(entryKey)
                                         .data(buffers)
                                         .build();

        Scheduler scheduler = Schedulers.newParallel(THREAD_PREFIX, 10);
        int maxBytesPerPart = MULTIPART_THRESHOLD_MB * 1024 * 1024;
        S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(scheduler,
                                                                         maxBytesPerPart,
                                                                         MULTIPART_UPLOAD_PREFETCH);

        StorageCommandResult.WriteResult result = client.write(StorageCommand.write(storageConfig,
                                                                                    cmdId,
                                                                                    entryKey,
                                                                                    entry)).block();
        if (result == null) {
            throw new S3ClientException(String.format("Invalid S3 write command result [bucket: %s] [endpoint: %s]",
                                                      s3Server.getBucket(),
                                                      s3Server.getEndpoint()));
        }
        result.matchWriteResult(success -> {
            LOGGER.info("Success [bucket: {}] end writing of file {} [endpoint: {}]",
                        s3Server.getBucket(),
                        sourceFile,
                        s3Server.getEndpoint());
            return true;
        }, unreachableStorage -> {
            throw new S3ClientException(String.format("Unreachable storage [bucket: %s] [endpoint: %s]",
                                                      s3Server.getBucket(),
                                                      s3Server.getEndpoint()));
        }, failure -> {
            throw new S3ClientException(String.format("Write failure [bucket: %s] [endpoint: %s]",
                                                      s3Server.getBucket(),
                                                      s3Server.getEndpoint()));
        });

    }

    public static boolean isPresent(String sourceFile, String rootPath, S3Server s3Server) {
        Path sourceFilePath = Path.of(sourceFile);

        StorageConfig storageConfig = buildStorageConfiguration(rootPath, s3Server);
        StorageCommandID cmdId = new StorageCommandID("test_id", UUID.randomUUID());
        String entryKey = storageConfig.entryKey(sourceFilePath.getFileName().toString());

        Scheduler scheduler = Schedulers.newParallel(THREAD_PREFIX, 10);
        int maxBytesPerPart = MULTIPART_THRESHOLD_MB * 1024 * 1024;
        S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(scheduler,
                                                                         maxBytesPerPart,
                                                                         MULTIPART_UPLOAD_PREFETCH);

        return client.check(StorageCommand.check(storageConfig, cmdId, entryKey))
                     .block()
                     .matchCheckResult(present -> true, absent -> false, unreachableStorage -> {
                         throw new S3ClientException(String.format("Unreachable storage [bucket: %s] [endpoint: %s]",
                                                                   s3Server.getBucket(),
                                                                   s3Server.getEndpoint()));
                     });
    }

    private static StorageConfig buildStorageConfiguration(String rootPath, S3Server s3Server) {
        return StorageConfig.builder()
                            .endpoint(s3Server.getEndpoint())
                            .bucket(s3Server.getBucket())
                            .key(s3Server.getKey())
                            .secret(s3Server.getSecret())
                            .region(s3Server.getRegion())
                            .rootPath(rootPath)
                            .build();
    }

    private S3BucketTestUtils() {
    }
}
