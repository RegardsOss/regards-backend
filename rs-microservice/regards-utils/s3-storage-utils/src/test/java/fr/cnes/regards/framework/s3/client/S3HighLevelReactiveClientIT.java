package fr.cnes.regards.framework.s3.client;

import fr.cnes.regards.framework.s3.S3Rule;
import fr.cnes.regards.framework.s3.domain.StorageCommand;
import fr.cnes.regards.framework.s3.domain.StorageCommandID;
import fr.cnes.regards.framework.s3.domain.StorageConfig;
import fr.cnes.regards.framework.s3.domain.StorageEntry;
import fr.cnes.regards.framework.s3.exception.ChecksumDoesntMatchException;
import fr.cnes.regards.framework.s3.utils.BytesConverterUtils;
import fr.cnes.regards.framework.test.integration.RegardsActiveProfileResolver;
import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import io.vavr.Tuple;
import io.vavr.control.Option;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@SpringBootConfiguration
@ActiveProfiles(resolver = RegardsActiveProfileResolver.class)
public class S3HighLevelReactiveClientIT {

    private final static Logger LOGGER = getLogger(S3HighLevelReactiveClientIT.class);

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

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testWriteReadDeleteSmall() {
        String rootPath = "some/root/path";

        StorageConfig config = StorageConfig.builder(s3Host, region, key, secret)
                                            .bucket(bucket)
                                            .rootPath(rootPath)
                                            .build();

        S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(Schedulers.immediate(), 1024, 10);

        Flux<ByteBuffer> buffers = DataBufferUtils.read(new ClassPathResource("small.txt"),
                                                        new DefaultDataBufferFactory(),
                                                        1024).map(DataBuffer::asByteBuffer);
        StorageCommandID cmdId = new StorageCommandID("askId", UUID.randomUUID());

        String checksum = "706126bf6d8553708227dba90694e81c";

        String entryKey = config.entryKey("small.txt");
        long size = 427L;
        StorageEntry entry = StorageEntry.builder()
                                         .checksum(Option.of(Tuple.of("MD5", checksum)))
                                         .config(config)
                                         .size(Option.some(size))
                                         .fullPath(entryKey)
                                         .data(buffers)
                                         .build();

        client.check(StorageCommand.check(config, cmdId, entryKey)).block().matchCheckResult(present -> {
            fail("Should be absent");
            return false;
        }, absent -> true, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        });

        client.write(StorageCommand.write(config, cmdId, entryKey, entry, checksum))
              .block()
              .matchWriteResult(success -> {
                  assertThat(success.getSize()).isEqualTo(size);
                  assertThat(success.getChecksum()).isEqualTo(checksum);
                  return true;
              }, unreachableStorage -> {
                  fail("s3 unreachable");
                  return false;
              }, failure -> {
                  fail(failure.toString());
                  return false;
              });

        client.check(StorageCommand.check(config, cmdId, entryKey))
              .block()
              .matchCheckResult(present -> true, absent -> {
                  fail("Should be present");
                  return false;
              }, unreachableStorage -> {
                  fail("s3 unreachable");
                  return false;
              });

        Optional<String> eTag = client.eTag(StorageCommand.check(config, cmdId, entryKey)).block();
        Assert.assertTrue("Missing etag property", eTag.isPresent());
        Assert.assertEquals("Invalid etag. Does not match expected checksum", checksum, eTag.get());

        Optional<Long> contentLength = client.contentLength(StorageCommand.check(config, cmdId, entryKey)).block();
        Assert.assertTrue("Missing contentLength property", contentLength.isPresent());
        Assert.assertEquals("Invalid contentLength. Does not match expected contentLength",
                            size,
                            contentLength.get().longValue());

        client.read(StorageCommand.read(config, cmdId, entryKey)).block().matchReadResult(pipe -> {
            pipe.getEntry().doOnNext(e -> {
                byte[] readContent = readBytes(e);
                LOGGER.info("entry: {}", readContent.length);
                assertThat(readContent).hasSize((int) size);

                MessageDigest readDigest = null;
                try {
                    readDigest = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException ex) {
                    fail();
                }
                readDigest.update(readContent);
                assertThat(BytesConverterUtils.bytesToHex(readDigest.digest())).isEqualTo(checksum);
            }).block();
            return true;
        }, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        }, failure -> {
            fail(failure.toString());
            return false;
        });

        client.delete(StorageCommand.delete(config, cmdId, entryKey))
              .block()
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
    public void testWriteSmallWrongChecksum() {
        String rootPath = "some/root/path";

        StorageConfig config = StorageConfig.builder(s3Host, region, key, secret)
                                            .bucket(bucket)
                                            .rootPath(rootPath)
                                            .build();

        S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(Schedulers.immediate(), 1024, 10);

        Flux<ByteBuffer> buffers = DataBufferUtils.read(new ClassPathResource("small.txt"),
                                                        new DefaultDataBufferFactory(),
                                                        1024).map(DataBuffer::asByteBuffer);
        StorageCommandID cmdId = new StorageCommandID("askId", UUID.randomUUID());

        String checksum = "abcd1234abcd1234";

        String entryKey = config.entryKey("small.txt");
        long size = 427L;
        StorageEntry entry = StorageEntry.builder()
                                         .checksum(Option.of(Tuple.of("MD5", checksum)))
                                         .config(config)
                                         .size(Option.some(size))
                                         .fullPath(entryKey)
                                         .data(buffers)
                                         .build();

        client.check(StorageCommand.check(config, cmdId, entryKey)).block().matchCheckResult(present -> {
            fail("Should be absent");
            return false;
        }, absent -> true, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        });

        client.write(StorageCommand.write(config, cmdId, entryKey, entry, checksum))
              .block()
              .matchWriteResult(success -> {
                  fail("This should fail because the checksum is not the right one");
                  return false;
              }, unreachableStorage -> {
                  fail("s3 unreachable");
                  return false;
              }, failure -> {
                  assertThat(failure.getCause().getClass()).isEqualTo(ChecksumDoesntMatchException.class);
                  return true;
              });
    }

    @Test
    public void testWriteSmallNoChecksum() {
        String rootPath = "some/root/path";

        StorageConfig config = StorageConfig.builder(s3Host, region, key, secret)
                                            .bucket(bucket)
                                            .rootPath(rootPath)
                                            .build();

        S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(Schedulers.immediate(), 1024, 10);

        Flux<ByteBuffer> buffers = DataBufferUtils.read(new ClassPathResource("small.txt"),
                                                        new DefaultDataBufferFactory(),
                                                        1024).map(DataBuffer::asByteBuffer);
        StorageCommandID cmdId = new StorageCommandID("askId", UUID.randomUUID());

        String entryKey = config.entryKey("small.txt");
        long size = 427L;
        StorageEntry entry = StorageEntry.builder()
                                         .config(config)
                                         .size(Option.some(size))
                                         .fullPath(entryKey)
                                         .data(buffers)
                                         .build();

        client.check(StorageCommand.check(config, cmdId, entryKey)).block().matchCheckResult(present -> {
            fail("Should be absent");
            return false;
        }, absent -> true, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        });

        client.write(StorageCommand.write(config, cmdId, entryKey, entry)).block().matchWriteResult(success -> {
            return true;
        }, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        }, failure -> {
            fail("write error");
            return false;
        });
    }

    @Test
    public void testWriteReadDeleteBig() throws IOException, NoSuchAlgorithmException {
        String rootPath = "some/root/path";

        StorageConfig config = StorageConfig.builder(s3Host, region, key, secret)
                                            .bucket(bucket)
                                            .rootPath(rootPath)
                                            .build();

        S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(Schedulers.immediate(), 5 * 1024 * 1024, 10);

        long size = 10L * 1024L * 1024L + 512L;

        File tmpFile = tmp.newFile("big.txt");

        Random random = new Random();
        byte[] content = new byte[(int) size];
        random.nextBytes(content);
        Files.write(tmpFile.toPath(), content);
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(content);
        String checksum = BytesConverterUtils.bytesToHex(digest.digest());
        Flux<ByteBuffer> buffers = Flux.just(ByteBuffer.wrap(content));

        StorageCommandID cmdId = new StorageCommandID("askId", UUID.randomUUID());

        String entryKey = config.entryKey("big.txt");

        StorageEntry entry = StorageEntry.builder()
                                         .checksum(Option.of(Tuple.of("MD5", checksum)))
                                         .config(config)
                                         .size(Option.some(size))
                                         .fullPath(entryKey)
                                         .data(buffers)
                                         .build();

        client.write(StorageCommand.write(config, cmdId, entryKey, entry, checksum))
              .block()
              .matchWriteResult(success -> {
                  assertThat(success.getSize()).isEqualTo(size);
                  assertThat(success.getChecksum()).isEqualTo(checksum);
                  return true;
              }, unreachableStorage -> {
                  fail("s3 unreachable");
                  return false;
              }, failure -> {
                  fail(failure.toString());
                  return false;
              });

        client.check(StorageCommand.check(config, cmdId, entryKey))
              .block()
              .matchCheckResult(present -> true, absent -> {
                  fail("Should be present");
                  return false;
              }, unreachableStorage -> {
                  fail("s3 unreachable");
                  return false;
              });

        client.read(StorageCommand.read(config, cmdId, entryKey)).block().matchReadResult(pipe -> {
            pipe.getEntry().doOnNext(e -> {
                byte[] readContent = readBytes(e);
                LOGGER.info("entry: {}", readContent.length);
                assertThat(readContent).hasSize((int) size);

                MessageDigest readDigest = null;
                try {
                    readDigest = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException ex) {
                    fail();
                }
                readDigest.update(readContent);
                assertThat(BytesConverterUtils.bytesToHex(readDigest.digest())).isEqualTo(checksum);
            }).block();
            return true;
        }, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        }, failure -> {
            fail(failure.toString());
            return false;
        });

        client.delete(StorageCommand.delete(config, cmdId, entryKey))
              .block()
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
    public void testWriteBigWrongChecksum() throws IOException, NoSuchAlgorithmException {
        String rootPath = "some/root/path";

        StorageConfig config = StorageConfig.builder(s3Host, region, key, secret)
                                            .bucket(bucket)
                                            .rootPath(rootPath)
                                            .build();

        S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(Schedulers.immediate(), 5 * 1024 * 1024, 10);

        long size = 10L * 1024L * 1024L + 512L;

        File tmpFile = tmp.newFile("big.txt");

        Random random = new Random();
        byte[] content = new byte[(int) size];
        random.nextBytes(content);
        Files.write(tmpFile.toPath(), content);
        String checksum = "abcd1234abcd1234";
        Flux<ByteBuffer> buffers = Flux.just(ByteBuffer.wrap(content));

        StorageCommandID cmdId = new StorageCommandID("askId", UUID.randomUUID());

        String entryKey = config.entryKey("big.txt");

        StorageEntry entry = StorageEntry.builder()
                                         .checksum(Option.of(Tuple.of("MD5", checksum)))
                                         .config(config)
                                         .size(Option.some(size))
                                         .fullPath(entryKey)
                                         .data(buffers)
                                         .build();

        client.write(StorageCommand.write(config, cmdId, entryKey, entry, checksum))
              .block()
              .matchWriteResult(success -> {
                  fail("This should fail because the checksum is not the right one");
                  return false;
              }, unreachableStorage -> {
                  fail("s3 unreachable");
                  return false;
              }, failure -> {
                  assertThat(failure.getCause().getClass()).isEqualTo(ChecksumDoesntMatchException.class);
                  return true;
              });

    }

    @Test
    public void testWriteBigNoChecksum() throws IOException, NoSuchAlgorithmException {
        String rootPath = "some/root/path";

        StorageConfig config = StorageConfig.builder(s3Host, region, key, secret)
                                            .bucket(bucket)
                                            .rootPath(rootPath)
                                            .build();

        S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(Schedulers.immediate(), 5 * 1024 * 1024, 10);

        long size = 10L * 1024L * 1024L + 512L;

        File tmpFile = tmp.newFile("big.txt");

        Random random = new Random();
        byte[] content = new byte[(int) size];
        random.nextBytes(content);
        Files.write(tmpFile.toPath(), content);
        Flux<ByteBuffer> buffers = Flux.just(ByteBuffer.wrap(content));

        StorageCommandID cmdId = new StorageCommandID("askId", UUID.randomUUID());

        String entryKey = config.entryKey("big.txt");

        StorageEntry entry = StorageEntry.builder()
                                         .config(config)
                                         .size(Option.some(size))
                                         .fullPath(entryKey)
                                         .data(buffers)
                                         .build();

        client.write(StorageCommand.write(config, cmdId, entryKey, entry)).block().matchWriteResult(success -> {
            return true;
        }, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        }, failure -> {
            fail("write failure");
            return false;
        });

    }

    private byte[] readBytes(StorageEntry e) {
        DataBuffer buffer = DataBufferUtils.join(e.getData().map(bb -> new DefaultDataBufferFactory().wrap(bb)))
                                           .block();
        byte[] content = new byte[buffer.readableByteCount()];
        buffer.read(content);
        return content;
    }

}
