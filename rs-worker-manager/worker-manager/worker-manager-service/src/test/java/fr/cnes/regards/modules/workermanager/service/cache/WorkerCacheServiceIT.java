/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.service.cache;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.workermanager.dto.WorkerConfigDto;
import fr.cnes.regards.modules.workermanager.dto.events.in.WorkerHeartBeatEvent;
import fr.cnes.regards.modules.workermanager.service.config.ConfigManager;
import fr.cnes.regards.modules.workermanager.service.config.WorkerConfigCacheService;
import fr.cnes.regards.modules.workermanager.service.config.WorkerConfigService;
import org.assertj.core.util.Lists;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Test for {@link WorkerCacheService}
 *
 * @author Léo Mieulet
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=worker_cache_it" })
@ActiveProfiles({ "noscheduler" })
public class WorkerCacheServiceIT extends AbstractWorkerManagerServiceUtilsTest {

    private final String workerType1 = "workerType1";

    private final String workerType2 = "workerType2";

    private final String workerType3 = "workerType3";

    private final Set<String> contentTypes1 = Sets.newHashSet("contentTypes1-1", "contentTypes1-2");

    private final Set<String> contentTypes2 = Sets.newHashSet("contentTypes2-1");

    private final Set<String> contentTypes3 = Sets.newHashSet("contentTypes3", "contentTypes3-2");

    @Autowired
    private WorkerCacheService workerCacheService;

    @Autowired
    private WorkerConfigService workerConfigService;

    @Autowired
    private ConfigManager configManager;

    @Autowired
    private WorkerConfigCacheService workerConfigCacheService;

    @Test
    public void testRegisterWorkers() {
        // Save configuration used by this test
        workerConfigService.importConfiguration(Sets.newHashSet(new WorkerConfigDto(workerType1, contentTypes1),
                                                                new WorkerConfigDto(workerType2, contentTypes2),
                                                                new WorkerConfigDto(workerType3, contentTypes3)));

        String workerId1 = UUID.randomUUID().toString();
        String workerId2 = UUID.randomUUID().toString();
        String workerId3 = UUID.randomUUID().toString();

        workerCacheService.registerWorkers(
                Lists.list(new WorkerHeartBeatEvent(workerId1, workerType1, OffsetDateTime.now()),
                           new WorkerHeartBeatEvent(workerId2, workerType2, OffsetDateTime.now().minusDays(1))));

        contentTypes1.forEach(contentType -> Assert.assertEquals("Should get a worker", Optional.of(workerType1),
                                                                 workerCacheService.getWorkerTypeByContentType(
                                                                         contentType)));

        Assert.assertEquals("Should not accept request related to a worker that sent outdated heart beat",
                            Optional.empty(),
                            workerCacheService.getWorkerTypeByContentType(contentTypes2.stream().findFirst().get()));
        Assert.assertEquals("Should not find a worker", Optional.empty(),
                            workerCacheService.getWorkerTypeByContentType("notRecognizedContentType"));

        // Check receive new heartbeat + new instance for same workerType
        workerCacheService.registerWorkers(
                Lists.list(new WorkerHeartBeatEvent(workerId1, workerType1, OffsetDateTime.now()),
                           new WorkerHeartBeatEvent(workerId3, workerType2, OffsetDateTime.now())));
        contentTypes1.forEach(contentType -> Assert.assertEquals("Should get a worker", Optional.of(workerType1),
                                                                 workerCacheService.getWorkerTypeByContentType(
                                                                         contentType)));
    }

    @Test
    public void testConfigurationChanged() {

        // Save configuration used by this test
        workerConfigService.importConfiguration(Sets.newHashSet(new WorkerConfigDto(workerType1, contentTypes1),
                                                                new WorkerConfigDto(workerType2, contentTypes2),
                                                                new WorkerConfigDto(workerType3, contentTypes3)));

        String workerId1 = UUID.randomUUID().toString();

        workerCacheService.registerWorkers(
                Lists.list(new WorkerHeartBeatEvent(workerId1, workerType1, OffsetDateTime.now())));

        contentTypes1.forEach(contentType -> Assert.assertEquals("Should get a worker", Optional.of(workerType1),
                                                                 workerCacheService.getWorkerTypeByContentType(
                                                                         contentType)));
        // remove all worker config - which sends the event to clear WorkerConfig cache
        configManager.resetConfiguration();
        // we dont test events that clears the cache, so let's do it manually
        workerConfigCacheService.cleanCache();
        contentTypes1.forEach(
                contentType -> Assert.assertEquals("No workerConf defined, so request refused", Optional.empty(),
                                                   workerCacheService.getWorkerTypeByContentType(contentType)));
    }

    @Test
    @Ignore
    public void testCacheExpiration() throws InterruptedException {

        // Save configuration used by this test
        workerConfigService.importConfiguration(Sets.newHashSet(
                new WorkerConfigDto(workerType1, contentTypes1),
                new WorkerConfigDto(workerType2, contentTypes2)
        ));

        String workerId1 = UUID.randomUUID().toString();
        String workerId2 = UUID.randomUUID().toString();

        workerCacheService.registerWorkers(
                Lists.list(new WorkerHeartBeatEvent(workerId1, workerType1, OffsetDateTime.now()),
                           new WorkerHeartBeatEvent(workerId2, workerType2, OffsetDateTime.now().minusDays(1))));

        contentTypes1.forEach(contentType -> Assert.assertEquals("Should get a worker", Optional.of(workerType1),
                                                                 workerCacheService.getWorkerTypeByContentType(
                                                                         contentType)));

        // The cache will remove the last heartbeat
        //        Thread.sleep(workerCacheService.EXPIRE_IN_CACHE_DURATION * 1000);

        Awaitility.await().atMost(workerCacheService.expireInCacheDuration * 2, TimeUnit.SECONDS).until(() -> {
                                                                                                               runtimeTenantResolver.forceTenant(getDefaultTenant());
                                                                                                               return Optional.empty()
                                                                                                                       .equals(workerCacheService.getWorkerTypeByContentType(contentTypes1.iterator().next()));
                                                                                                           }

        );

        contentTypes1.forEach(contentType -> Assert.assertEquals("No more worker", Optional.empty(),
                                                                 workerCacheService.getWorkerTypeByContentType(
                                                                         contentType)));
    }

    @Test
    @Ignore
    public void testHandleManyRequests() {
        // Save configuration used by this test
        workerConfigService.importConfiguration(Sets.newHashSet(new WorkerConfigDto(workerType1, contentTypes1),
                                                                new WorkerConfigDto(workerType2, contentTypes2),
                                                                new WorkerConfigDto(workerType3, contentTypes3)));

        String workerId1 = UUID.randomUUID().toString();
        String workerId2 = UUID.randomUUID().toString();
        String workerId3 = UUID.randomUUID().toString();

        workerCacheService.registerWorkers(
                Lists.list(new WorkerHeartBeatEvent(workerId1, workerType1, OffsetDateTime.now()),
                           new WorkerHeartBeatEvent(workerId2, workerType2, OffsetDateTime.now()),
                           new WorkerHeartBeatEvent(workerId3, workerType3, OffsetDateTime.now())
                ));
        List<String> allContentTypeSet = Lists.newArrayList();
        allContentTypeSet.addAll(contentTypes1);
        allContentTypeSet.addAll(contentTypes2);
        allContentTypeSet.addAll(contentTypes3);

        long nbRequests = 0;
        OffsetDateTime before = OffsetDateTime.now();
        while (nbRequests < 100_000L) {
            int randomContentTypeI = new Random().nextInt(allContentTypeSet.size());
            String randomContentType = allContentTypeSet.get(randomContentTypeI);
            Assert.assertNotEquals("Should get a worker", Optional.empty(),
                                workerCacheService.getWorkerTypeByContentType(randomContentType));
            if (nbRequests % 10_000L == 0) {
                logger.info("Running requests {}", nbRequests);
            }
            nbRequests += 1;
        }

        int MAX_TEST_DURATION = 1; // 1 second
        Assert.assertTrue("Should take less than few seconds", before.plusSeconds(MAX_TEST_DURATION)
                .isAfter(OffsetDateTime.now()));
    }
}
