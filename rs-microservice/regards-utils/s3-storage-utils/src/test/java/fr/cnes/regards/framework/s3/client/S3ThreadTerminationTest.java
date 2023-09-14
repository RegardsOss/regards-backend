/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.s3.S3Rule;
import fr.cnes.regards.framework.s3.domain.StorageCommand;
import fr.cnes.regards.framework.s3.domain.StorageCommandID;
import fr.cnes.regards.framework.s3.domain.StorageConfig;
import fr.cnes.regards.framework.s3.domain.StorageEntry;
import fr.cnes.regards.framework.test.integration.RegardsActiveProfileResolver;
import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import io.vavr.Tuple;
import io.vavr.control.Option;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Test verifying that s3 threads (schedulers, client ...) are terminated at the end of the process
 *
 * @author Thibaud Michaudel
 **/
@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@ActiveProfiles(resolver = RegardsActiveProfileResolver.class)
@Ignore("This test is meant to be used in debug to verify that there are no parasite threads still living at the end "
        + "of the process")
public class S3ThreadTerminationTest {

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
    public void test_s3_client_threads() {
        for (int i = 0; i < 10; i++) {
            S3HighLevelReactiveClient client = createClient(i);
            try {
                client.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        String debugHere = "debugHere";
    }

    public S3HighLevelReactiveClient createClient(int i) {
        Scheduler scheduler = Schedulers.newParallel("s3-reactive-client-" + i, 10);
        int maxBytesPerPart = 5 * 1024 * 1024;
        S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(scheduler, maxBytesPerPart, 10);
        StorageConfig config = StorageConfig.builder(s3Host, region, key, secret)
                                            .bucket(bucket)
                                            .rootPath("root_" + i)
                                            .build();
        String entryKey = config.entryKey("file");
        Flux<ByteBuffer> buffers = DataBufferUtils.read(new ClassPathResource("small.txt"),
                                                        new DefaultDataBufferFactory(),
                                                        1024).map(DataBuffer::asByteBuffer);
        StorageEntry entry = StorageEntry.builder()
                                         .checksum(Option.of(Tuple.of("MD5", "706126bf6d8553708227dba90694e81c")))
                                         .config(config)
                                         .size(Option.some(427L))
                                         .fullPath(entryKey)
                                         .data(buffers)
                                         .build();

        client.write(StorageCommand.write(config, new StorageCommandID("cmd-" + i, UUID.randomUUID()), entryKey, entry))
              .block();
        return client;
    }
}
