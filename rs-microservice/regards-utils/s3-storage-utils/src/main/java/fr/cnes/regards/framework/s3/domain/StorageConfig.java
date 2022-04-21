package fr.cnes.regards.framework.s3.domain;

import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.Builder;
import lombok.Value;
import software.amazon.awssdk.utils.StringUtils;

import java.net.URL;
import java.util.function.Function;

@Value
@Builder
public class StorageConfig {

    String endpoint;

    String region;

    String key;

    String secret;

    String bucket;

    String rootPath;

    public String entryKey(String suffix) {
        return normalizedRootPath() + suffix;
    }

    private String normalizedRootPath() {
        return Option.of(rootPath).filter(StringUtils::isNotBlank).map(s -> s.endsWith("/") ? s : s + "/")
                .getOrElse("");
    }

    public URL entryKeyUrl(String entryKey) {
        return Try.of(() -> new URL(String.format("%s/%s/%s", endpoint, bucket, entryKey)))
                .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
    }
}
