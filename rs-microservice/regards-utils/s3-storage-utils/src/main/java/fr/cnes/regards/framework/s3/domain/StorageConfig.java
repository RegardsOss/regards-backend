package fr.cnes.regards.framework.s3.domain;

import io.vavr.control.Option;
import io.vavr.control.Try;
import software.amazon.awssdk.utils.StringUtils;

import java.net.URL;
import java.util.Objects;
import java.util.function.Function;

public class StorageConfig {

    private String endpoint;

    private String region;

    private String key;

    private String secret;

    private String bucket;

    private String rootPath;

    private Integer maxRetriesNumber;

    private Integer retryBackOffBaseDuration;

    private Integer retryBackOffMaxDuration;

    public StorageConfig(StorageConfigBuilder storageConfigBuilder) {
        this.endpoint = storageConfigBuilder.endpoint;
        this.key = storageConfigBuilder.key;
        this.region = storageConfigBuilder.region;
        this.secret = storageConfigBuilder.secret;
        this.rootPath = storageConfigBuilder.rootPath;
        this.bucket = storageConfigBuilder.bucket;
        this.maxRetriesNumber = storageConfigBuilder.maxRetriesNumber;
        this.retryBackOffBaseDuration = storageConfigBuilder.retryBackOffBaseDuration;
        this.retryBackOffMaxDuration = storageConfigBuilder.retryBackOffMaxDuration;
    }

    public static class StorageConfigBuilder {

        private String endpoint;

        private String region;

        private String key;

        private String secret;

        private String bucket;

        private String rootPath;

        private Integer maxRetriesNumber = 5;

        private Integer retryBackOffBaseDuration = 5;

        private Integer retryBackOffMaxDuration = 15;

        private StorageConfigBuilder(String endpoint, String region, String key, String secret) {
            this.endpoint = endpoint;
            this.region = region;
            this.key = key;
            this.secret = secret;
        }

        public StorageConfigBuilder(S3Server s3Server) {
            this.endpoint = s3Server.getEndpoint();
            this.key = s3Server.getKey();
            this.region = s3Server.getRegion();
            this.secret = s3Server.getSecret();
            this.bucket = s3Server.getBucket();
        }

        public StorageConfigBuilder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        public StorageConfigBuilder rootPath(String rootPath) {
            this.rootPath = rootPath;
            return this;
        }

        public StorageConfigBuilder maxRetriesNumber(int maxRetriesNumber) {
            this.maxRetriesNumber = maxRetriesNumber;
            return this;
        }

        public StorageConfigBuilder retryBackOffBaseDuration(int retryBackOffBaseDuration) {
            this.retryBackOffBaseDuration = retryBackOffBaseDuration;
            return this;
        }

        public StorageConfigBuilder retryBackOffMaxDuration(int retryBackOffMaxDuration) {
            this.retryBackOffMaxDuration = retryBackOffMaxDuration;
            return this;
        }

        public StorageConfig build() {
            return new StorageConfig(this);
        }
    }

    public static StorageConfigBuilder builder(String endpoint, String region, String key, String secret) {
        return new StorageConfigBuilder(endpoint, region, key, secret);
    }

    public static StorageConfigBuilder builder(S3Server s3Server) {
        return new StorageConfigBuilder(s3Server);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getRegion() {
        return region;
    }

    public String getKey() {
        return key;
    }

    public String getSecret() {
        return secret;
    }

    public String getBucket() {
        return bucket;
    }

    public String getRootPath() {
        return rootPath;
    }

    public Integer getMaxRetriesNumber() {
        return maxRetriesNumber;
    }

    public Integer getRetryBackOffBaseDuration() {
        return retryBackOffBaseDuration;
    }

    public Integer getRetryBackOffMaxDuration() {
        return retryBackOffMaxDuration;
    }

    public String entryKey(String suffix) {
        return normalizedRootPath() + suffix;
    }

    private String normalizedRootPath() {
        return Option.of(rootPath)
                     .filter(StringUtils::isNotBlank)
                     .map(s -> s.endsWith("/") ? s : s + "/")
                     .getOrElse("");
    }

    public URL entryKeyUrl(String entryKey) {
        return Try.of(() -> new URL(String.format("%s/%s/%s", endpoint, bucket, entryKey)))
                  .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StorageConfig that = (StorageConfig) o;
        return endpoint.equals(that.endpoint) && region.equals(that.region) && key.equals(that.key) && secret.equals(
            that.secret) && bucket.equals(that.bucket) && Objects.equals(rootPath, that.rootPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, region, key, secret, bucket, rootPath);
    }

    @Override
    public String toString() {
        return "StorageConfig{"
               + "endpoint='"
               + endpoint
               + '\''
               + ", region='"
               + region
               + '\''
               + ", key='"
               + key
               + '\''
               + ", secret='"
               + secret
               + '\''
               + ", bucket='"
               + bucket
               + '\''
               + ", rootPath='"
               + rootPath
               + '\''
               + '}';
    }
}
