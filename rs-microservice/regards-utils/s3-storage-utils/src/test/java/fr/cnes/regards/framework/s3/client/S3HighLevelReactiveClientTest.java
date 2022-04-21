package fr.cnes.regards.framework.s3.client;

import fr.cnes.regards.framework.s3.domain.StorageCommand;
import fr.cnes.regards.framework.s3.domain.StorageCommandID;
import fr.cnes.regards.framework.s3.domain.StorageConfig;
import fr.cnes.regards.framework.s3.domain.StorageEntry;
import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import io.vavr.Tuple;
import io.vavr.control.Option;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@SpringBootConfiguration
public class S3HighLevelReactiveClientTest {

    private static final Logger LOGGER = getLogger(S3HighLevelReactiveClientTest.class);

    @Value("${s3.server}")
    private String s3Host;

    @Value("${s3.key}")
    private String key;

    @Value("${s3.secret}")
    private String secret;

    @Value("${s3.region}")
    private String region;

    @Value("${s3.bucket}")
    private String bucket;

    @Rule
    public final S3Rule s3Rule = new S3Rule(() -> s3Host, () -> key, () -> secret, () -> region, () -> bucket);

    @Test
    public void testWriteReadDeleteSmall() {
        String rootPath = "some/root/path";

        StorageConfig config = StorageConfig.builder().endpoint(s3Host).bucket(bucket).region(region).key(key)
                .secret(secret).rootPath(rootPath).build();

        S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(Schedulers.immediate(), 1024);

        Flux<ByteBuffer> buffers = DataBufferUtils.read(new ClassPathResource("small.txt"),
                                                        new DefaultDataBufferFactory(), 1024)
                .map(DataBuffer::asByteBuffer);
        StorageCommandID cmdId = new StorageCommandID("askId", UUID.randomUUID());

        String entryKey = config.entryKey("small.txt");
        long size = 427L;
        StorageEntry entry = StorageEntry.builder()
                .checksum(Option.of(Tuple.of("MD5", "706126bf6d8553708227dba90694e81c"))).config(config)
                .size(Option.some(size)).fullPath(entryKey).data(buffers).build();

        client.check(StorageCommand.check(config, cmdId, entryKey)).block().matchCheckResult(present -> {
            fail("Should be absent");
            return false;
        }, absent -> true, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        });

        client.write(StorageCommand.write(config, cmdId, entryKey, entry)).block().matchWriteResult(success -> {
            assertThat(success.getSize()).isEqualTo(size);
            return true;
        }, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        }, failure -> {
            fail(failure.toString());
            return false;
        });

        client.check(StorageCommand.check(config, cmdId, entryKey)).block()
                .matchCheckResult(present -> true, absent -> {
                    fail("Should be present");
                    return false;
                }, unreachableStorage -> {
                    fail("s3 unreachable");
                    return false;
                });

        client.read(StorageCommand.read(config, cmdId, entryKey)).block().matchReadResult(pipe -> {
            pipe.getEntry().doOnNext(e -> LOGGER.info("entry: {}", readString(e))).block();
            return true;
        }, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        }, failure -> {
            fail(failure.toString());
            return false;
        });

        client.delete(StorageCommand.delete(config, cmdId, entryKey)).block()
                .matchDeleteResult(success -> true, unreachable -> {
                    fail("Delete failed: Unreachable");
                    return false;
                }, failure -> {
                    fail("Delete failed");
                    return false;
                });

        client.check(StorageCommand.check(config, cmdId, entryKey)).block().matchCheckResult(present -> {
            fail("Should be absent");
            return false;
        }, absent -> true, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        });

    }

    @Test
    public void testWriteBig() {
        String rootPath = "some/root/path";

        StorageConfig config = StorageConfig.builder().endpoint(s3Host).bucket(bucket).region(region).key(key)
                .secret(secret).rootPath(rootPath).build();

        S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(Schedulers.immediate(), 5 * 1024 * 1024);

        long size = 10L * 1024L * 1024L + 512L;

        Flux<ByteBuffer> buffers = Flux.just(ByteBuffer.wrap(new byte[(int) size]));

        StorageCommandID cmdId = new StorageCommandID("askId", UUID.randomUUID());

        String entryKey = config.entryKey("big.txt");

        StorageEntry entry = StorageEntry.builder()
                .checksum(Option.of(Tuple.of("MD5", "706126bf6d8553708227dba90694e81c"))).config(config)
                .size(Option.some(size)).fullPath(entryKey).data(buffers).build();

        client.write(StorageCommand.write(config, cmdId, entryKey, entry)).block().matchWriteResult(success -> {
            assertThat(success.getSize()).isEqualTo(size);
            return true;
        }, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        }, failure -> {
            fail(failure.toString());
            return false;
        });

        client.check(StorageCommand.check(config, cmdId, entryKey)).block()
                .matchCheckResult(present -> true, absent -> {
                    fail("Should be present");
                    return false;
                }, unreachableStorage -> {
                    fail("s3 unreachable");
                    return false;
                });

        client.read(StorageCommand.read(config, cmdId, entryKey)).block().matchReadResult(pipe -> {
            pipe.getEntry().doOnNext(e -> {
                int length = readString(e).length();
                LOGGER.info("entry: {}", length);
                assertThat(length).isEqualTo(size);
            }).block();
            return true;
        }, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        }, failure -> {
            fail(failure.toString());
            return false;
        });

        client.delete(StorageCommand.delete(config, cmdId, entryKey)).block()
                .matchDeleteResult(success -> true, unreachable -> {
                    fail("Delete failed: Unreachable");
                    return false;
                }, failure -> {
                    fail("Delete failed");
                    return false;
                });

        client.check(StorageCommand.check(config, cmdId, entryKey)).block().matchCheckResult(present -> {
            fail("Should be absent");
            return false;
        }, absent -> true, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        });

    }

    private String readString(StorageEntry e) {
        return DataBufferUtils.join(e.getData().map(bb -> new DefaultDataBufferFactory().wrap(bb))).block()
                .toString(StandardCharsets.UTF_8);
    }

}
