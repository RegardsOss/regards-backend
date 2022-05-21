package fr.cnes.regards.framework.s3.client;

import com.google.common.collect.ImmutableMap;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Try;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.function.Supplier;

public class S3Rule extends ExternalResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Rule.class);

    private final Supplier<String> host;

    private final Supplier<String> region;

    private final Supplier<String> key;

    private final Supplier<String> secret;

    private final Supplier<String> bucket;

    private Map<String, List<Tuple2<String, InputStream>>> objects;

    public S3Rule(Supplier<String> host,
                  Supplier<String> key,
                  Supplier<String> secret,
                  Supplier<String> region,
                  Supplier<String> bucket) {
        this.host = host;
        this.key = key;
        this.secret = secret;
        this.region = region;
        this.bucket = bucket;
    }

    protected S3Client makeS3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(key.get(), secret.get());

        return S3Client.builder()
                       .endpointOverride(URI.create(host.get()))
                       .region(Region.of(region.get()))
                       .credentialsProvider(StaticCredentialsProvider.create(credentials))
                       .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                       .build();
    }

    @Override
    public void before() {
        try {
            S3Client s3Client = makeS3Client();

            objects = ImmutableMap.of(bucket.get(), List.empty());

            for (Map.Entry<String, List<Tuple2<String, InputStream>>> entry : objects.entrySet()) {
                String bucketName = entry.getKey();
                if (s3Client.listBuckets().buckets().stream().noneMatch(bucket -> bucket.name().equals(bucketName))) {
                    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                }

                if (!entry.getValue().isEmpty()) {
                    for (Tuple2<String, InputStream> t : entry.getValue()) {
                        String path = t._1;
                        InputStream content = t._2;

                        byte[] bytes = IOUtils.toByteArray(content);
                        byte[] md5 = DigestUtils.md5(bytes);
                        String md5b64 = new String(Base64.encodeBase64(md5));
                        PutObjectRequest request = PutObjectRequest.builder()
                                                                   .bucket(bucketName)
                                                                   .key(path)
                                                                   .contentMD5(md5b64)
                                                                   .contentLength((long) bytes.length)
                                                                   .build();
                        try (InputStream bis = new ByteArrayInputStream(bytes)) {
                            s3Client.putObject(request, RequestBody.fromInputStream(bis, bytes.length));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to init S3 buckets and objects before rule", e);
        }
    }

    @Override
    public void after() {
        Try.ofCallable(this::makeS3Client)
           .flatMapTry(s3Client -> Try.run(() -> objects.keySet()
                                                        .stream()
                                                        .peek(bucket -> s3Client.listObjectsV2Paginator(
                                                                                    ListObjectsV2Request.builder().bucket(bucket).build())
                                                                                .contents()
                                                                                .stream()
                                                                                .forEach(s3Object -> s3Client.deleteObject(
                                                                                    DeleteObjectRequest.builder()
                                                                                                       .bucket(bucket)
                                                                                                       .key(s3Object.key())
                                                                                                       .build())))
                                                        .forEach(bucket -> s3Client.deleteBucket(DeleteBucketRequest.builder()
                                                                                                                    .bucket(
                                                                                                                        bucket)
                                                                                                                    .build()))))
           .onFailure(e -> LOGGER.error("Failed to clean S3 buckets after rule", e));
    }
}

