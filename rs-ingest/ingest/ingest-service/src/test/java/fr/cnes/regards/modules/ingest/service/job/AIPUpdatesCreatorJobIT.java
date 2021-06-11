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
package fr.cnes.regards.modules.ingest.service.job;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateState;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateTagTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateTaskType;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=update_scanner_job", "regards.amqp.enabled=true",
                "regards.ingest.aip.update.bulk.delay=100000000",
                "eureka.client.enabled=false", "regards.ingest.request.schedule.delay=100000000" },
        locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock" })
public class AIPUpdatesCreatorJobIT extends IngestMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AIPUpdateRunnerJobTest.class);

    @Autowired
    private StorageClientMock storageClient;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private IAIPService aipService;


    private static final List<String> CATEGORIES_0 = Lists.newArrayList("CATEGORY");

    private static final List<String> CATEGORIES_1 = Lists.newArrayList("CATEGORY1");

    private static final List<String> CATEGORIES_2 = Lists.newArrayList("CATEGORY2");

    private static final List<String> TAG_0 = Lists.newArrayList("toto", "tata");

    private static final List<String> TAG_1 = Lists.newArrayList("toto", "tutu");

    private static final List<String> TAG_2 = Lists.newArrayList("plop", "ping");

    private static final String STORAGE_1 = "AWS";

    private static final String STORAGE_2 = "Azure";

    private static final String STORAGE_3 = "Pentagon";

    private static final String SESSION_OWNER_0 = "NASA";

    private static final String SESSION_OWNER_1 = "CNES";

    private static final String SESSION_0 = OffsetDateTime.now().toString();

    private static final String SESSION_1 = OffsetDateTime.now().minusDays(4).toString();

    public void initData() {

        long nbSIP = 6;
        publishSIPEvent(create(UUID.randomUUID().toString(), TAG_0), STORAGE_1, SESSION_0, SESSION_OWNER_0,
                        CATEGORIES_0);
        publishSIPEvent(create(UUID.randomUUID().toString(), TAG_0), STORAGE_1, SESSION_0, SESSION_OWNER_1,
                        CATEGORIES_1);
        publishSIPEvent(create(UUID.randomUUID().toString(), TAG_1), STORAGE_1, SESSION_0, SESSION_OWNER_0,
                        CATEGORIES_0);
        publishSIPEvent(create(UUID.randomUUID().toString(), TAG_1), STORAGE_1, SESSION_1, SESSION_OWNER_1,
                        CATEGORIES_1);
        publishSIPEvent(create(UUID.randomUUID().toString(), TAG_1), STORAGE_2, SESSION_1, SESSION_OWNER_1,
                        CATEGORIES_0);
        publishSIPEvent(create(UUID.randomUUID().toString(), TAG_0), STORAGE_2, SESSION_1, SESSION_OWNER_0,
                        CATEGORIES_0);
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000, SIPState.STORED);
        // Wait STORE_META request over
        // delete requests, if notification are active mock success of notifications to delete ingest requests
        if(!initDefaultNotificationSettings()) {
            ingestServiceTest.waitAllRequestsFinished(nbSIP * 5000);
        } else {
            mockNotificationSuccess(RequestTypeConstant.INGEST_VALUE);
            ingestServiceTest.waitDuring(TWO_SECONDS * nbSIP);
        }
    }

    /**
     * Helper method to wait for DB ingestion
     * @param expectedTasks expected count of task in db
     * @param timeout in ms
     * @throws InterruptedException
     */
    public void waitForTaskCreated(long expectedTasks, long timeout) {
        long end = System.currentTimeMillis() + timeout;
        // Wait
        long taskCount;
        do {
            taskCount = aipUpdateRequestRepository.count();
            LOGGER.debug("{} UpdateRequest(s) created in database", taskCount);
            if (taskCount == expectedTasks) {
                break;
            }
            long now = System.currentTimeMillis();
            if (end > now) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Assert.fail("Thread interrupted");
                }
            } else {
                Assert.fail("Timeout");
            }
        } while (true);
    }

    @Test
    public void testScanJob() throws ModuleException {
        storageClient.setBehavior(true, true);
        initData();
        aipService.registerUpdatesCreator(AIPUpdateParametersDto
                .build(SearchAIPsParameters.build().withSession(SESSION_0).withSessionOwner(SESSION_OWNER_0), TAG_2,
                       TAG_1, CATEGORIES_2, CATEGORIES_1, Lists.newArrayList(STORAGE_3)));
        long nbSipConcerned = 2;
        long nbTasksPerSip = 5;
        waitForTaskCreated(nbSipConcerned * nbTasksPerSip, 10_000);
    }

    @Test
    public void testScanJobWithPending() throws ModuleException {
        storageClient.setBehavior(true, true);
        initData();
        generateFakeRunningTasks();

        aipService.registerUpdatesCreator(AIPUpdateParametersDto
                .build(SearchAIPsParameters.build().withSession(SESSION_0).withSessionOwner(SESSION_OWNER_0), TAG_2,
                       TAG_1, CATEGORIES_2, CATEGORIES_1, Lists.newArrayList(STORAGE_3)));
        long nbInitialTasks = 6;
        long nbSipConcerned = 2;
        long nbTasksPerSip = 5;
        waitForTaskCreated((nbSipConcerned * nbTasksPerSip) + nbInitialTasks, 10_000);

        Pageable pageRequest = PageRequest.of(0, 200);
        Page<AIPUpdateRequest> blocked = aipUpdateRequestRepository.findAllByState(InternalRequestState.BLOCKED,
                                                                                   pageRequest);
        Assert.assertEquals(nbSipConcerned * nbTasksPerSip, blocked.getTotalElements());
    }

    private void generateFakeRunningTasks() {
        // Init some running tasks
        List<AIPEntity> aips = aipRepository.findAll();
        for (AIPEntity aip : aips) {
            List<AbstractAIPUpdateTask> updateTasks = Lists.newArrayList(AIPUpdateTagTask
                    .build(AIPUpdateTaskType.ADD_TAG, AIPUpdateState.READY, Lists.newArrayList("TOTO", "TITI")));
            List<AIPUpdateRequest> requests = AIPUpdateRequest.build(aip, updateTasks, false);
            for (AIPUpdateRequest r : requests) {
                r.setState(InternalRequestState.RUNNING);
            }
            aipUpdateRequestRepository.saveAll(requests);
        }
    }
}
