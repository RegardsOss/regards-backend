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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import fr.cnes.regards.framework.s3.domain.GlacierFileStatus;
import fr.cnes.regards.framework.s3.domain.RestorationStatus;
import fr.cnes.regards.framework.s3.domain.StorageConfig;
import fr.cnes.regards.framework.s3.domain.StorageEntry;
import fr.cnes.regards.framework.s3.domain.multipart.GetResponseAndStream;
import fr.cnes.regards.framework.s3.domain.multipart.ResponseAndStream;
import fr.cnes.regards.framework.s3.domain.multipart.UploadedPart;
import fr.cnes.regards.framework.s3.exception.S3ClientException;
import io.vavr.collection.List;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An asynchronous storage client wrapper. This client is a wrapper of S3 asynchronous {@link S3AsyncClient} that
 * provides helper methods, cache and try facilities among other stuff.
 * <p>
 * You should always use this wrapper instead of instantiating and handling a {@link S3AsyncClient} yourself.
 * <p>
 */
public class S3AsyncClientReactorWrapper extends S3ClientReloader<S3AsyncClient> implements IS3ClientReactorWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3AsyncClientReactorWrapper.class);

    static final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("s3-async-client-%d")
                                                                         .setUncaughtExceptionHandler((t, e) -> LOGGER.error(
                                                                             "Uncaught on thread {}: {}",
                                                                             t.getName(),
                                                                             e.getMessage(),
                                                                             e))
                                                                         .build();

    public static final Pattern HEAD_RESPONSE_RESTORE_ON_GOING_PATTERN = Pattern.compile(
        "^.*ongoing-request=\"([^\"]*)\"" + ".*$");

    public static final Pattern HEAD_RESPONSE_RESTORE_EXPIRE_PATTERN = Pattern.compile("^.*expiry-date=\"([^\"]*)\".*$");

    private static final Set<ZoneId> PREFERRED_ZONES = Set.of(ZoneId.of("Europe/Paris"), ZoneId.of("America/Havana"));

    public static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder().appendPattern(
                                                                                                  "E, dd MMM yyyy " + "HH:mm:ss [")
                                                                                              .appendZoneText(TextStyle.SHORT,
                                                                                                              PREFERRED_ZONES)
                                                                                              .appendPattern("]")
                                                                                              .toFormatter(Locale.ENGLISH);

    static final ExecutorService executor = Executors.newCachedThreadPool(threadFactory);

    public S3AsyncClientReactorWrapper(StorageConfig config) {
        super(3, config, S3AsyncClientReactorWrapper::createS3Client);
    }

    private static <T> Mono<T> fromFutureSupplier(Supplier<CompletableFuture<T>> futSupplier) {
        return Mono.fromFuture(futSupplier).onErrorMap(CompletionException.class, Throwable::getCause);
    }

    private static ClientOverrideConfiguration createRetryConfiguration(StorageConfig config) {
        BackoffStrategy backoffStrategy = EqualJitterBackoffStrategy.builder()
                                                                    .baseDelay(Duration.ofSeconds(config.getRetryBackOffBaseDuration()))
                                                                    .maxBackoffTime(Duration.ofSeconds(config.getRetryBackOffMaxDuration()))
                                                                    .build();
        return ClientOverrideConfiguration.builder()
                                          .retryPolicy(RetryPolicy.builder()
                                                                  .backoffStrategy(backoffStrategy)
                                                                  .throttlingBackoffStrategy(backoffStrategy)
                                                                  .numRetries(config.getMaxRetriesNumber())
                                                                  .build())
                                          .build();
    }

    private static S3AsyncClient createS3Client(StorageConfig config) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(config.getKey(), config.getSecret());

        return S3AsyncClient.builder()
                            .endpointOverride(URI.create(config.getEndpoint()))
                            .region(Region.of(config.getRegion()))
                            .credentialsProvider(StaticCredentialsProvider.create(credentials))
                            .serviceConfiguration(S3Configuration.builder().build())
                            .asyncConfiguration(b -> b.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR,
                                                                      executor))
                            .overrideConfiguration(createRetryConfiguration(config))
                            .build();
    }

    @Override
    public Mono<Boolean> exists(String bucket, String key) {
        return withClient(client -> {
            HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucket).key(key).build();
            return fromFutureSupplier(() -> client.headObject(request)).map(any -> {
                LOGGER.debug("File ({}) in bucket ({}) exists", key, bucket);
                return true;
            }).onErrorResume(NoSuchKeyException.class, t -> {
                LOGGER.debug("File ({}) in bucket ({}) does not exist", key, bucket);
                return Mono.just(false);
            }).onErrorMap(SdkClientException.class, S3ClientException::new);
        });
    }

    @Override
    public Mono<GlacierFileStatus> isFileAvailable(String bucket, String key, @Nullable String standardStorageClass) {
        return withClient(client -> {
            HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucket).key(key).build();
            return fromFutureSupplier(() -> client.headObject(request)).map(headResponse -> checkHeadRestoreState(
                headResponse,
                standardStorageClass,
                key,
                bucket)).onErrorResume(NoSuchKeyException.class, t -> {
                LOGGER.debug("File ({}) in bucket ({}) does not exist", key, bucket);
                return Mono.error(t);
            }).onErrorMap(SdkClientException.class, S3ClientException::new);
        });
    }

    /**
     * Check if the S3 head request response indicates that the file is restored, available for download and return its size.
     */
    public static GlacierFileStatus checkHeadRestoreState(HeadObjectResponse response,
                                                          String standardStorageClass,
                                                          String key,
                                                          String bucket) {
        RestorationStatus status = RestorationStatus.NOT_AVAILABLE;
        ZonedDateTime expirationDate = null;

        if (checkIfFileIsInStandardStorageClass(response, standardStorageClass)) {
            LOGGER.debug("File ({}) in bucket ({}) is in STANDARD storage class", key, bucket);
            status = RestorationStatus.AVAILABLE;
        } else {
            LOGGER.debug("File ({}) in bucket ({}) is in GLACIER storage class", key, bucket);
            // Check if a restoration request is running.
            Boolean restoreRequestPending = checkRestoreRequestOnGoingStatus(response);

            Boolean restoreRequestExpired = null;
            try {
                expirationDate = getRestoreRequestExpiration(response);
                if (expirationDate != null) {
                    // If restoration request is done, check expiration date.
                    restoreRequestExpired = expirationDate.isBefore(ZonedDateTime.now());
                } else {
                    restoreRequestExpired = false;
                }
                if (restoreRequestPending != null) {
                    if (!restoreRequestPending && !restoreRequestExpired) {
                        // If restore request exists, is not pending and not expired, file is available for download.
                        status = RestorationStatus.AVAILABLE;
                    } else if (!restoreRequestPending) {
                        // If restore request exists, is not pending and expired, file is no longer available for
                        // download.
                        status = RestorationStatus.EXPIRED;
                    } else {
                        // If restore request exists and is pending, file is not yet available for download.
                        status = RestorationStatus.RESTORE_PENDING;
                    }
                }
            } catch (DateTimeParseException e) {
                LOGGER.error("Head response from S3 Server is malformed. Date format for expiration date is not "
                             + "in expected format", e);
            }
        }
        String logMsg = String.format("File (%s) in bucket (%s) restore status = %s", key, bucket, status);
        if (expirationDate != null) {
            logMsg = String.format("File (%s) in bucket (%s) restore status = %s (expiration date: %s)",
                                   key,
                                   bucket,
                                   status,
                                   expirationDate);
        }
        LOGGER.info(logMsg);

        return new GlacierFileStatus(status, response.contentLength(), expirationDate);
    }

    /**
     * Check from the given s3 head response, if a restore request exists, is running or is done.
     *
     * @return null if no restoration request exists; otherwise True if a request exists and is pending else False.
     */
    private static Boolean checkRestoreRequestOnGoingStatus(HeadObjectResponse response) {
        if (response.restore() != null) {
            Matcher matcher = HEAD_RESPONSE_RESTORE_ON_GOING_PATTERN.matcher(response.restore().toLowerCase());
            if (matcher.find() && matcher.group(1) != null) {
                return Boolean.parseBoolean(matcher.group(1));
            }
        }
        return null;
    }

    /**
     * Return the expiration date from the given S3 head response.
     *
     * @return the expiration date; otherwise null if the expiration date does not exist with
     * {@link HEAD_RESPONSE_RESTORE_EXPIRE_PATTERN} pattern in response.
     * @throws DateTimeParseException if date is not wellformed in response.
     */
    private static ZonedDateTime getRestoreRequestExpiration(HeadObjectResponse response)
        throws DateTimeParseException {
        ZonedDateTime expirationZoneDateTime = null;
        if (response.restore() == null) {
            return expirationZoneDateTime;
        }
        Matcher matcher = HEAD_RESPONSE_RESTORE_EXPIRE_PATTERN.matcher(response.restore());
        if (matcher.find() && matcher.group(1) != null) {
            expirationZoneDateTime = ZonedDateTime.parse(matcher.group(1), DATE_TIME_FORMATTER);
        }
        return expirationZoneDateTime;
    }

    /**
     * Check from the given S3 head response, if file is stored on a STANDARD storage class.
     */
    private static Boolean checkIfFileIsInStandardStorageClass(HeadObjectResponse response,
                                                               @Nullable String standardStorageClass) {
        return response.storageClass() == null || response.storageClass()
                                                          .name()
                                                          .equals(standardStorageClass == null ?
                                                                      StorageClass.STANDARD.name() :
                                                                      standardStorageClass);
    }

    @Override
    public Mono<Optional<String>> eTag(String bucket, String key) {
        return getHeadObjectResponse(bucket, key).flatMap(optional -> optional.map(headObjectResponse -> Mono.just(
            // Etag specification indicates that the string is quoted so remove quotes to get eTag content value
            Optional.of(headObjectResponse.eTag().replace("\"", "")))).orElse(Mono.just(Optional.empty())));
    }

    @Override
    public Mono<Optional<Long>> contentLength(String bucket, String key) {
        return getHeadObjectResponse(bucket, key).flatMap(optional -> optional.map(headObjectResponse -> Mono.just(
            Optional.of(headObjectResponse.contentLength()))).orElse(Mono.just(Optional.empty())));
    }

    private Mono<Optional<HeadObjectResponse>> getHeadObjectResponse(String bucket, String key) {
        return withClient(client -> {
            HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucket).key(key).build();
            return fromFutureSupplier(() -> client.headObject(request)).map(response -> {
                LOGGER.debug("File ({}) in bucket ({}) exists", key, bucket);
                return Optional.of(response);
            }).onErrorResume(NoSuchKeyException.class, t -> {
                LOGGER.debug("File ({}) in bucket ({}) does not exist", key, bucket, t);
                return Mono.just(Optional.empty());
            }).onErrorMap(SdkClientException.class, S3ClientException::new);
        });
    }

    public Mono<ResponseAndStream> readContentFlux(String bucket, String key, boolean failIfMissing) {
        return withClient(client -> {
            GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();
            return fromFutureSupplier(() -> client.getObject(request, new GetResponseAndStream())).onErrorMap(
                SdkClientException.class,
                S3ClientException::new);
        });
    }

    public Mono<RestoreObjectResponse> restore(String bucket, String key, Integer days) {
        return withClient(client -> {
            RestoreObjectRequest request = RestoreObjectRequest.builder()
                                                               .bucket(bucket)
                                                               .key(key)
                                                               .restoreRequest(r -> r.days(days))
                                                               .build();
            return fromFutureSupplier(() -> client.restoreObject(request)).onErrorMap(SdkClientException.class,
                                                                                      S3ClientException::new);
        });
    }

    public Flux<String> listObjects(String bucket, String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();

        return withClient(client -> Flux.from(client.listObjectsV2Paginator(request))
                                        .onErrorMap(CompletionException.class, Throwable::getCause)
                                        .concatMap(r -> Flux.fromIterable(r.contents()))
                                        .map(S3Object::key)
                                        .onErrorMap(S3Exception.class, this::wrapS3Exception));
    }

    public Mono<PutObjectResponse> putContent(String bucket, String path, InputStream content) {
        try {
            return putContent(bucket, path, IOUtils.toByteArray(content));
        } catch (IOException e) {
            return Mono.error(new S3ClientException(e));
        }
    }

    public Mono<PutObjectResponse> putContent(String bucket, String path, byte[] content) {
        byte[] md5 = DigestUtils.md5(content);
        String md5b64 = new String(Base64.encodeBase64(md5));
        long length = content.length;

        LOGGER.debug("Writing to storage : bucket={} path={} length={} md5={}", bucket, path, length, md5b64);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                                                      .bucket(bucket)
                                                      .key(path)
                                                      .contentMD5(md5b64)
                                                      .contentLength(length)
                                                      .build();
        AsyncRequestBody requestBody = AsyncRequestBody.fromBytes(content);

        return withClient(client -> fromFutureSupplier(() -> client.putObject(putRequest, requestBody))).log(
                                                                                                            "putContent")
                                                                                                        .onErrorMap(
                                                                                                            SdkClientException.class,
                                                                                                            S3ClientException::new)
                                                                                                        .onErrorMap(
                                                                                                            S3Exception.class,
                                                                                                            this::wrapS3Exception);
    }

    /**
     * Send a request to initiate a multipart upload for given path,
     * according to @see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html">S3 multipart upload spec</a>
     * <br />
     * Note: this method should be followed by multiple calls to {@link #uploadMultipartFilePart(String, String, String, int, byte[])}
     *
     * @param path the path to the final stored object
     * @return the uploadId
     */
    public Mono<String> initiateMultipartUpload(String bucket, String path) {
        return withClient(client -> {
            LOGGER.debug("Initiating multipart upload to {}/{}...", bucket, path);
            CreateMultipartUploadRequest initRequest = CreateMultipartUploadRequest.builder()
                                                                                   .bucket(bucket)
                                                                                   .storageClass((String) null)
                                                                                   .key(path)
                                                                                   .build();
            return fromFutureSupplier(() -> client.createMultipartUpload(initRequest)).map(resp -> resp.uploadId())
                                                                                      .doOnNext(uploadId -> LOGGER.debug(
                                                                                          "Multipart {} - Initiated multipart upload to {}/{}",
                                                                                          uploadId,
                                                                                          bucket,
                                                                                          path));
        });
    }

    /**
     * Upload a part of a multipart file, according to
     *
     * @param path      the path to the final stored object
     * @param uploadId  the uploadId returned by the {@link #initiateMultipartUpload(String, String)} request
     * @param partId    the id of the part to upload. Ids need not to be consecutive, however they should be ordered
     *                  (earlier file parts should have lower ids than parts at the end of the file)
     * @param partBytes the actual content of the part
     * @return the entity tag corresponding to the part that has been uploaded.
     * Needed for {@link #completeMultipartUpload(String, String, String, List)}.
     * @see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html">S3 multipart upload spec</a>
     * <br />
     * Note: you should make sure that you have previously initiated the multipart upload by calling
     * {@link #initiateMultipartUpload(String, String)} and you should call
     * {@link #completeMultipartUpload(String, String, String, List)} once you have sent the last part.
     */
    public Mono<UploadedPart> uploadMultipartFilePart(String bucket,
                                                      String path,
                                                      String uploadId,
                                                      int partId,
                                                      byte[] partBytes) {
        LOGGER.debug("Multipart {} - Uploading part {} of multipart upload to {}/{}...",
                     uploadId,
                     partId,
                     bucket,
                     path);

        byte[] md5 = DigestUtils.md5(partBytes);
        String md5b64 = new String(Base64.encodeBase64(md5));

        UploadPartRequest uploadRequest = UploadPartRequest.builder()
                                                           .bucket(bucket)
                                                           .contentMD5(md5b64)
                                                           .key(path)
                                                           .uploadId(uploadId)
                                                           .partNumber(partId)
                                                           .build();
        AsyncRequestBody requestBody = AsyncRequestBody.fromBytes(partBytes);

        return withClient(client -> fromFutureSupplier(() -> client.uploadPart(uploadRequest,
                                                                               requestBody)).map(resp -> new UploadedPart(
                                                                                                CompletedPart.builder().eTag(resp.eTag()).partNumber(partId).build(),
                                                                                                requestBody.contentLength().orElse(0L)))
                                                                                            .doOnNext(etag -> LOGGER.debug(
                                                                                                "Multipart {} - Finished uploading part {} of multipart upload to {}/{}, got tag {}",
                                                                                                uploadId,
                                                                                                partId,
                                                                                                bucket,
                                                                                                path,
                                                                                                etag)));
    }

    /**
     * Upload a part to S3 and update the given MessageDigest with the part data in order to compute the checksum
     * when the upload is complete.
     * <p>
     * This only work if the upload is done sequentially (the parts are sent in order), ie if parallel upload is not
     * used. @see{{@link S3HighLevelReactiveClient#uploadThenCompleteMultipartEntry(StorageEntry, StorageConfig, String, String, String)}}
     *
     * @param bucket    the bucket the file is uploaded to
     * @param path      the path in the bucket
     * @param uploadId  the multipartId of the part for s3
     * @param partId    the number of the part, this is used by S3 to order the part when building the file
     * @param partBytes the data to upload
     * @param digest    The MessageDigest to update in order to compute the file checksum
     * @return the UploadedPart metadata
     */
    public Mono<UploadedPart> uploadMultipartFilePartWithDigest(String bucket,
                                                                String path,
                                                                String uploadId,
                                                                int partId,
                                                                byte[] partBytes,
                                                                MessageDigest digest) {
        LOGGER.debug("Multipart {} - Uploading part {} of multipart upload to {}/{}...",
                     uploadId,
                     partId,
                     bucket,
                     path);

        byte[] md5 = DigestUtils.md5(partBytes);
        String md5b64 = new String(Base64.encodeBase64(md5));

        UploadPartRequest uploadRequest = UploadPartRequest.builder()
                                                           .bucket(bucket)
                                                           .contentMD5(md5b64)
                                                           .key(path)
                                                           .uploadId(uploadId)
                                                           .partNumber(partId)
                                                           .build();
        AsyncRequestBody requestBody = AsyncRequestBody.fromBytes(partBytes);

        /*
         * WARNING : This only works because we don't use parallel upload. If we do parallel upload, the parts are
         * then not sent sequentially, and it is then impossible to compute the checksum.
         * If we change our reactor algorithm to use parallel upload (with ParallelFlux), it would not be possible to
         * compute the checksum on the fly anymore.
         */

        digest.update(partBytes);
        return withClient(client -> fromFutureSupplier(() -> client.uploadPart(uploadRequest,
                                                                               requestBody)).map(resp -> new UploadedPart(
                                                                                                CompletedPart.builder().eTag(resp.eTag()).partNumber(partId).build(),
                                                                                                requestBody.contentLength().orElse(0L)))
                                                                                            .doOnNext(etag -> LOGGER.debug(
                                                                                                "Multipart {} - Finished uploading part {} of multipart upload to {}/{}, got tag {}",
                                                                                                uploadId,
                                                                                                partId,
                                                                                                bucket,
                                                                                                path,
                                                                                                etag)));
    }

    /**
     * Send a request to complete a multipart upload for given path,
     * according to @see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html">S3 multipart upload spec</a>
     * <br />
     * Note: this method MUST BE call after all parts of a multipart upload have been uploaded using
     * {@link #uploadMultipartFilePart(String, String, String, int, byte[])}. Not calling this method will result
     * in an unclosed file which will be both invalid and paid for.
     *
     * @param path     the path to the final stored object
     * @param uploadId the uploadId returned by the {@link #initiateMultipartUpload(String, String)} request
     * @param parts    the list of entity tags of all uploaded parts
     * @return the etag of the multipart content
     */
    public Mono<String> completeMultipartUpload(String bucket,
                                                String path,
                                                String uploadId,
                                                List<CompletedPart> parts) {
        List<CompletedPart> sortedParts = parts.sorted(Comparator.comparing(CompletedPart::partNumber));
        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                                                                                       .bucket(bucket)
                                                                                       .key(path)
                                                                                       .uploadId(uploadId)
                                                                                       .multipartUpload(
                                                                                           CompletedMultipartUpload.builder()
                                                                                                                   .parts(
                                                                                                                       sortedParts.toJavaList())
                                                                                                                   .build())
                                                                                       .build();

        LOGGER.debug("Multipart {} - Completing multipart upload for {}/{}...", uploadId, bucket, path);
        return withClient(client -> fromFutureSupplier(() -> client.completeMultipartUpload(completeRequest)).doOnNext(
            resp -> LOGGER.debug("Multipart {} - Completed multipart upload for {}/{} ", uploadId, bucket, path))).map(
            CompleteMultipartUploadResponse::eTag);
    }

    public Mono<String> abortMultipartUpload(String bucket, String path, String uploadId) {

        AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                                                                              .bucket(bucket)
                                                                              .key(path)
                                                                              .uploadId(uploadId)
                                                                              .build();

        LOGGER.debug("Multipart {} - Completing multipart upload for {}/{}...", uploadId, bucket, path);
        return withClient(client -> fromFutureSupplier(() -> client.abortMultipartUpload(abortRequest)).doOnNext(resp -> LOGGER.debug(
            "Multipart {} - Aborted multipart upload for {}/{} ",
            uploadId,
            bucket,
            path))).map(any -> uploadId);
    }

    public Mono<DeleteObjectResponse> deleteContent(String bucket, String path) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucket).key(path).build();
        return withClient(client -> fromFutureSupplier(() -> client.deleteObject(deleteObjectRequest)));
    }

    public Flux<DeleteObjectResponse> deleteContentWithPrefix(String bucket, String prefix) {
        return listObjects(bucket, prefix).flatMap(x -> deleteContent(bucket, x));
    }

    private S3ClientException wrapS3Exception(S3Exception e) {
        return new S3ClientException(e.getMessage(), e.statusCode(), e);
    }
}
