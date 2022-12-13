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
package fr.cnes.regards.framework.s3.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import fr.cnes.regards.framework.s3.domain.StorageCommand.Check;
import fr.cnes.regards.framework.s3.domain.StorageCommand.Delete;
import fr.cnes.regards.framework.s3.domain.StorageCommand.Read;
import fr.cnes.regards.framework.s3.domain.StorageCommand.Write;
import fr.cnes.regards.framework.s3.domain.StorageCommandResult;
import fr.cnes.regards.framework.s3.domain.StorageCommandResult.*;
import fr.cnes.regards.framework.s3.domain.StorageConfig;
import fr.cnes.regards.framework.s3.domain.StorageEntry;
import fr.cnes.regards.framework.s3.domain.multipart.MultipartReport;
import fr.cnes.regards.framework.s3.domain.multipart.ResponseAndStream;
import fr.cnes.regards.framework.s3.domain.multipart.UploadedPart;
import fr.cnes.regards.framework.s3.exception.MultipartException;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Scheduler;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;

import static fr.cnes.regards.framework.s3.utils.FluxByteBufferHarmonizer.harmonize;
import static org.slf4j.LoggerFactory.getLogger;

public class S3HighLevelReactiveClient {

    private static final Logger LOGGER = getLogger(S3HighLevelReactiveClient.class);

    protected final Scheduler scheduler;

    protected final int maxBytesPerPart;

    /**
     * Number of reactor flux message handles in a batch for multipart uploads.
     * Increase memory usage as each part is read in memory.
     * maxBytesPerPart is the amount a bytes in memory for each part.
     */
    private final int reactorPreFetch;

    private final Cache<StorageConfig, S3AsyncClientReactorWrapper> configManagers = Caffeine.newBuilder()
                                                                                             .expireAfterWrite(Duration.ofMinutes(
                                                                                                 5))
                                                                                             .evictionListener(
                                                                                                 S3HighLevelReactiveClient::onClientCacheEviction)
                                                                                             .build();

    public S3HighLevelReactiveClient(Scheduler scheduler, int maxBytesPerPart, int reactorPreFetch) {
        this.scheduler = scheduler;
        this.maxBytesPerPart = maxBytesPerPart;
        this.reactorPreFetch = reactorPreFetch;
    }

    private static void onClientCacheEviction(StorageConfig config,
                                              S3AsyncClientReactorWrapper client,
                                              RemovalCause cause) {
        client.close();
    }

    protected S3AsyncClientReactorWrapper getClient(StorageConfig config) {
        return configManagers.get(config, S3AsyncClientReactorWrapper::new);
    }

    public Mono<ReadResult> read(Read readCmd) {
        return readingCallableMono(readCmd).subscribeOn(scheduler)
                                           .onErrorResume(t -> Mono.just(new StorageCommandResult.UnreachableStorage(
                                               readCmd,
                                               t)))
                                           .log("storage.s3.read",
                                                Level.FINE,
                                                SignalType.ON_SUBSCRIBE,
                                                SignalType.ON_NEXT,
                                                SignalType.ON_ERROR);
    }

    public Mono<WriteResult> write(Write writeCmd) {
        return writeMono(writeCmd);
    }

    public Mono<DeleteResult> delete(Delete deleteCmd) {
        return deleteMono(deleteCmd).subscribeOn(scheduler)
                                    .onErrorResume(t -> Mono.just(new StorageCommandResult.UnreachableStorage(deleteCmd,
                                                                                                              t)))
                                    .log("storage.s3.delete",
                                         Level.FINE,
                                         SignalType.ON_SUBSCRIBE,
                                         SignalType.ON_NEXT,
                                         SignalType.ON_ERROR);
    }

    public Mono<DeleteResult> deleteWithPrefix(Delete deleteCmd) {
        return deleteMonoWithPrefix(deleteCmd).subscribeOn(scheduler)
                                              .onErrorResume(t -> Mono.just(new StorageCommandResult.UnreachableStorage(
                                                  deleteCmd,
                                                  t)))
                                              .log("storage.s3.delete",
                                                   Level.FINE,
                                                   SignalType.ON_SUBSCRIBE,
                                                   SignalType.ON_NEXT,
                                                   SignalType.ON_ERROR);
    }

    public Mono<CheckResult> check(Check checkCmd) {
        StorageConfig config = checkCmd.getConfig();
        String archivePath = checkCmd.getEntryKey();
        String bucket = config.getBucket();
        return getClient(config).exists(bucket, archivePath)
                                .<CheckResult>map(exists -> exists ?
                                    new StorageCommandResult.CheckPresent(checkCmd) :
                                    new StorageCommandResult.CheckAbsent(checkCmd))
                                .onErrorResume(t -> Mono.just(new StorageCommandResult.UnreachableStorage(checkCmd,
                                                                                                          t)));
    }

    public Mono<Optional<String>> eTag(Check checkCmd) {
        StorageConfig config = checkCmd.getConfig();
        String archivePath = checkCmd.getEntryKey();
        String bucket = config.getBucket();
        return getClient(config).eTag(bucket, archivePath);
    }

    private Mono<ReadResult> readingCallableMono(Read readCmd) {
        StorageConfig config = readCmd.getConfig();
        String bucket = config.getBucket();
        String entryKey = readCmd.getEntryKey();
        return getClient(config).readContentFlux(bucket, entryKey, true)
                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                                                .filter(t -> !(t instanceof NoSuchKeyException))
                                                .doBeforeRetry(x -> LOGGER.info("Retrying read entry {}", entryKey))
                                                .maxBackoff(Duration.ofSeconds(3)))
                                .map(ras -> (ReadResult) new ReadingPipe(readCmd,
                                                                         Mono.just(getStorageEntry(config,
                                                                                                   entryKey,
                                                                                                   ras))))
                                .onErrorResume(t -> t instanceof NoSuchKeyException ?
                                    Mono.just(new ReadNotFound(readCmd)) :
                                    Mono.just(new UnreachableStorage(readCmd, t)));
    }

    private static StorageEntry getStorageEntry(StorageConfig config, String entryKey, ResponseAndStream ras) {
        Long size = ras.getResponse().contentLength();
        String etag = ras.getResponse().eTag();
        LOGGER.debug("Reading entry={} size={} eTag={}", entryKey, size, etag);
        StorageEntry storageEntry = StorageEntry.builder()
                                                .config(config)
                                                .fullPath(entryKey)
                                                .checksum(Option.of(Tuple.of("eTag", etag)))
                                                .size(Option.of(size))
                                                .data(ras.getStream())
                                                .build();
        return storageEntry;
    }

    public Mono<WriteResult> writeMono(Write writeCmd) {
        return Mono.defer(() -> {
            StorageEntry entry = writeCmd.getEntry();
            return entry.getSize()
                        .filter(s -> s <= maxBytesPerPart)
                        .map(size -> storeSmallEntry(writeCmd))
                        .getOrElse(() -> storeMultipartEntry(writeCmd))
                        .log("storage.s3.write", Level.FINE)
                        .flatMap(size -> Mono.<WriteResult>just(new WriteSuccess(writeCmd, size)))
                        .transform(dealWithWriteEntryErrors(writeCmd));
        });
    }

    protected Function<Mono<WriteResult>, Mono<WriteResult>> dealWithWriteEntryErrors(Write writeCmd) {
        return writeResultMono -> writeResultMono.doOnError(t -> LOGGER.error("Command {} - error {}",
                                                                              writeCmd.getCmdId(),
                                                                              t.getMessage(),
                                                                              t))
                                                 .onErrorResume(MultipartException.class,
                                                                cme -> Mono.just(new WriteFailure(writeCmd)))
                                                 .onErrorResume(t -> Mono.just(new StorageCommandResult.UnreachableStorage(
                                                     writeCmd,
                                                     t)))
                                                 .log("storage.s3.write",
                                                      Level.FINE,
                                                      SignalType.ON_SUBSCRIBE,
                                                      SignalType.ON_NEXT,
                                                      SignalType.ON_ERROR);
    }

    protected Mono<Long> storeMultipartEntry(Write writeCmd) {
        StorageConfig config = writeCmd.getConfig();
        String bucket = config.getBucket();
        StorageEntry entry = writeCmd.getEntry();
        String key = entry.getFullPath();

        return Mono.defer(() -> getClient(config).initiateMultipartUpload(bucket, key)
                                                 .flatMap(uploadId -> uploadThenCompleteMultipartEntry(entry,
                                                                                                       config,
                                                                                                       bucket,
                                                                                                       key,
                                                                                                       uploadId)))
                   .subscribeOn(scheduler);
    }

    protected Mono<Long> uploadThenCompleteMultipartEntry(StorageEntry entry,
                                                          StorageConfig config,
                                                          String bucket,
                                                          String key,
                                                          String uploadId) {
        return entry.getData()
                    .limitRate(reactorPreFetch)
                    .publishOn(scheduler)
                    .transform(harmonize(maxBytesPerPart, reactorPreFetch))
                    .zipWithIterable(Stream.range(1, Integer.MAX_VALUE))
                    .concatMap(part -> uploadPart(config, bucket, key, uploadId, part), reactorPreFetch)
                    .reduce(new MultipartReport(), MultipartReport::accumulate)
                    .flatMap(report -> completeMultipartEntry(config, bucket, key, uploadId, report))
                    .onErrorResume(t -> getClient(config).abortMultipartUpload(bucket, key, uploadId)
                                                         .flatMap(any -> Mono.error(new MultipartException(t, entry))));
    }

    private Mono<Long> completeMultipartEntry(StorageConfig config,
                                              String bucket,
                                              String key,
                                              String uploadId,
                                              MultipartReport report) {
        return getClient(config).completeMultipartUpload(bucket, key, uploadId, report.getCompleted())
                                .map(any -> report.getAccumulatedSize());
    }

    private Publisher<? extends UploadedPart> uploadPart(StorageConfig config,
                                                         String bucket,
                                                         String key,
                                                         String uploadId,
                                                         Tuple2<ByteBuffer, Integer> part) {
        int partNum = part.getT2();
        byte[] partData = bufToArr(part.getT1());
        return getClient(config).uploadMultipartFilePart(bucket, key, uploadId, partNum, partData);
    }

    protected Mono<Long> storeSmallEntry(Write writeCmd) {
        StorageConfig config = writeCmd.getConfig();
        String bucket = config.getBucket();
        StorageEntry entry = writeCmd.getEntry();
        String key = entry.getFullPath();

        return dataToByteArray(entry.getData()).flatMap(bytes -> getClient(config).putContent(bucket, key, bytes)
                                                                                  .map(r -> (long) bytes.length));
    }

    /**
     * Must be used only for small entries, because it will load all the data in RAM
     */
    private Mono<byte[]> dataToByteArray(Flux<ByteBuffer> data) {
        return data.publishOn(scheduler)
                   .collect(List.collector())
                   .map(bbs -> bbs.foldLeft(new ByteArrayOutputStream(), this::appendByteBufferToOutputStream)
                                  .toByteArray());
    }

    private ByteArrayOutputStream appendByteBufferToOutputStream(ByteArrayOutputStream out, ByteBuffer bb) {
        byte[] arr = bufToArr(bb);
        out.write(arr, 0, arr.length);
        return out;
    }

    protected Mono<DeleteResult> deleteMonoWithPrefix(Delete deleteCmd) {
        return Mono.defer(() -> {
            StorageConfig config = deleteCmd.getConfig();
            String archivePath = deleteCmd.getEntryKey();
            String bucket = config.getBucket();

            IS3ClientReactorWrapper client = getClient(config);

            return deleteArchiveContent(deleteCmd, bucket, archivePath, client).transform(deleteErrorManagement(
                deleteCmd));
        });
    }

    protected Mono<DeleteResult> deleteMono(Delete deleteCmd) {
        return Mono.defer(() -> {
            StorageConfig config = deleteCmd.getConfig();
            String key = deleteCmd.getEntryKey();
            String bucket = config.getBucket();

            IS3ClientReactorWrapper client = getClient(config);

            return deleteContent(deleteCmd, bucket, key, client).transform(deleteErrorManagement(deleteCmd));
        });
    }

    private Function<Mono<DeleteResult>, Publisher<DeleteResult>> deleteErrorManagement(Delete deleteCmd) {
        return mdr -> mdr.retryWhen(Retry.backoff(5, Duration.ofSeconds(5))
                                         .jitter(0.2d)
                                         .maxBackoff(Duration.ofMinutes(5)))
                         .onErrorResume(t -> Mono.just(new DeleteFailure(deleteCmd)));
    }

    private Mono<DeleteResult> deleteArchiveContent(Delete deleteCmd,
                                                    String bucket,
                                                    String key,
                                                    IS3ClientReactorWrapper client) {
        return client.deleteContentWithPrefix(bucket, key)
                     .log("storage.s3.delete", Level.FINER, SignalType.ON_NEXT, SignalType.ON_ERROR)
                     .then(Mono.<DeleteResult>just(new DeleteSuccess(deleteCmd)))
                     .switchIfEmpty(Mono.<DeleteResult>just(new DeleteSuccess(deleteCmd)));
    }

    private Mono<DeleteResult> deleteContent(Delete deleteCmd,
                                             String bucket,
                                             String key,
                                             IS3ClientReactorWrapper client) {
        DeleteResult successResult = new DeleteSuccess(deleteCmd);
        return client.deleteContent(bucket, key)
                     .log("storage.s3.delete", Level.FINER, SignalType.ON_NEXT, SignalType.ON_ERROR)
                     .map(response -> successResult)
                     .doOnError(t -> LOGGER.error(t.getMessage(), t))
                     .switchIfEmpty(Mono.just(successResult));
    }

    protected byte[] bufToArr(ByteBuffer bb) {
        byte[] arr = new byte[bb.remaining()];
        bb.get(arr);
        return arr;
    }

}

