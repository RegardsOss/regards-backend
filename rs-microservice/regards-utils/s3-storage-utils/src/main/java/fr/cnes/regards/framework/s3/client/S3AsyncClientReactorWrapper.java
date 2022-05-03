package fr.cnes.regards.framework.s3.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import fr.cnes.regards.framework.s3.domain.StorageConfig;
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
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Comparator;
import java.util.concurrent.*;
import java.util.function.Supplier;

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
            .setUncaughtExceptionHandler(
                    (t, e) -> LOGGER.error("Uncaught on thread {}: {}", t.getName(), e.getMessage(), e)).build();

    static final ExecutorService executor = Executors.newCachedThreadPool(threadFactory);

    public S3AsyncClientReactorWrapper(StorageConfig config) {
        super(3, config, S3AsyncClientReactorWrapper::createS3Client);
    }

    private static <T> Mono<T> fromFutureSupplier(Supplier<CompletableFuture<T>> futSupplier) {
        return Mono.fromFuture(futSupplier).onErrorMap(CompletionException.class, Throwable::getCause);
    }

    private static S3AsyncClient createS3Client(StorageConfig config) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(config.getKey(), config.getSecret());

        return S3AsyncClient.builder().endpointOverride(URI.create(config.getEndpoint()))
                .region(Region.of(config.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .asyncConfiguration(
                        b -> b.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, executor))
                .build();
    }

    @Override
    public Mono<Boolean> exists(String bucket, String key) {
        return withClient(client -> {
            HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucket).key(key).build();
            return fromFutureSupplier(() -> client.headObject(request)).map(any -> {
                LOGGER.info("File ({}) in bucket ({}) exists", key, bucket);
                return true;
            }).onErrorResume(NoSuchKeyException.class, t -> {
                LOGGER.error("File ({}) in bucket ({}) does not exist", key, bucket, t);
                return Mono.just(false);
            }).onErrorMap(SdkClientException.class, S3ClientException::new);
        });
    }

    public Mono<ResponseAndStream> readContentFlux(String bucket, String key, boolean failIfMissing) {
        return withClient(client -> {
            GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();
            return fromFutureSupplier(() -> client.getObject(request, new GetResponseAndStream())).onErrorMap(
                            SdkClientException.class, S3ClientException::new)
                    .onErrorMap(S3Exception.class, this::wrapS3Exception);
        });
    }

    public Flux<String> listObjects(String bucket, String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();

        return withClient(client -> Flux.from(client.listObjectsV2Paginator(request))
                .onErrorMap(CompletionException.class, Throwable::getCause)
                .concatMap(r -> Flux.fromIterable(r.contents())).map(S3Object::key)
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

        PutObjectRequest putRequest = PutObjectRequest.builder().bucket(bucket).key(path).contentMD5(md5b64)
                .contentLength(length).build();
        AsyncRequestBody requestBody = AsyncRequestBody.fromBytes(content);

        return withClient(client -> fromFutureSupplier(() -> client.putObject(putRequest, requestBody))).log(
                        "putContent").onErrorMap(SdkClientException.class, S3ClientException::new)
                .onErrorMap(S3Exception.class, this::wrapS3Exception);
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
            CreateMultipartUploadRequest initRequest = CreateMultipartUploadRequest.builder().bucket(bucket)
                    .storageClass((String) null).key(path).build();
            return fromFutureSupplier(() -> client.createMultipartUpload(initRequest)).map(resp -> resp.uploadId())
                    .doOnNext(uploadId -> LOGGER.debug("Multipart {} - Initiated multipart upload to {}/{}", uploadId,
                                                       bucket, path));
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
    public Mono<UploadedPart> uploadMultipartFilePart(String bucket, String path, String uploadId, int partId,
            byte[] partBytes) {
        LOGGER.debug("Multipart {} - Uploading part {} of multipart upload to {}/{}...", uploadId, partId, bucket,
                     path);

        UploadPartRequest uploadRequest = UploadPartRequest.builder().bucket(bucket).key(path).uploadId(uploadId)
                .partNumber(partId).build();
        AsyncRequestBody requestBody = AsyncRequestBody.fromBytes(partBytes);

        return withClient(client -> fromFutureSupplier(() -> client.uploadPart(uploadRequest, requestBody)).map(
                resp -> new UploadedPart(CompletedPart.builder().eTag(resp.eTag()).partNumber(partId).build(),
                                         partBytes.length)).doOnNext(etag -> LOGGER.debug(
                "Multipart {} - Finished uploading part {} of multipart upload to {}/{}, got tag {}", uploadId, partId,
                bucket, path, etag)));
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
    public Mono<String> completeMultipartUpload(String bucket, String path, String uploadId,
            List<CompletedPart> parts) {
        List<CompletedPart> sortedParts = parts.sorted(Comparator.comparing(CompletedPart::partNumber));
        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder().bucket(bucket)
                .key(path).uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(sortedParts.toJavaList()).build()).build();

        LOGGER.debug("Multipart {} - Completing multipart upload for {}/{}...", uploadId, bucket, path);
        return withClient(client -> fromFutureSupplier(() -> client.completeMultipartUpload(completeRequest)).doOnNext(
                resp -> LOGGER.debug("Multipart {} - Completed multipart upload for {}/{} ", uploadId, bucket,
                                     path))).map(CompleteMultipartUploadResponse::eTag);
    }

    public Mono<String> abortMultipartUpload(String bucket, String path, String uploadId) {

        AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder().bucket(bucket).key(path)
                .uploadId(uploadId).build();

        LOGGER.debug("Multipart {} - Completing multipart upload for {}/{}...", uploadId, bucket, path);
        return withClient(client -> fromFutureSupplier(() -> client.abortMultipartUpload(abortRequest)).doOnNext(
                resp -> LOGGER.debug("Multipart {} - Aborted multipart upload for {}/{} ", uploadId, bucket,
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