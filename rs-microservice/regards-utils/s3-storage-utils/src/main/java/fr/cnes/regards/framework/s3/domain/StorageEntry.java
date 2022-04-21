package fr.cnes.regards.framework.s3.domain;

import io.vavr.Tuple2;
import io.vavr.control.Option;
import lombok.Builder;
import lombok.Value;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;

/**
 * This class corresponds to an entry found, or to be written, in the S3 storage.
 */
@Value
@Builder
public class StorageEntry {

    StorageConfig config;

    /**
     * The full path corresponds to config root path + "suffix"
     */
    String fullPath;

    Option<Tuple2<String, String>> checksum;

    Option<Long> size;

    Flux<ByteBuffer> data;

}

