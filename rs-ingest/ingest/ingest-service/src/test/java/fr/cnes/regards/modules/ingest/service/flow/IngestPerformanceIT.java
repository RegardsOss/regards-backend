/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.flow;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import fr.cnes.regards.modules.ingest.service.request.IOAISDeletionService;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import fr.cnes.regards.modules.sessionmanager.client.SessionNotificationPublisher;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;

/**
 * Test SIP flow handling
 *
 * @author Marc SORDI
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=sipflow", "regards.amqp.enabled=true",
                "regards.scheduler.pool.size=4", "regards.ingest.maxBulkSize=100", "eureka.client.enabled=false",
                "regards.aips.save-metadata.bulk.delay=100", "regards.ingest.aip.delete.bulk.delay=100" },
        locations = { "classpath:application-test.properties" })
@ActiveProfiles({ "testAmqp", "StorageClientMock" })
public class IngestPerformanceIT extends IngestMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestPerformanceIT.class);

    private static final List<String> CATEGORIES = Lists.newArrayList("CATEGORY");

    private static final String PROVIDER_PREFIX = "provider";

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private SessionNotificationPublisher sessionNotifier;

    @Autowired
    private IOAISDeletionService deletionService;

    @Autowired
    private StorageClientMock storageClientMock;

    @Autowired
    private IRequestService requestService;

    @Override
    public void doInit() {

        simulateApplicationReadyEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @Override
    protected void doAfter() throws Exception {
        // WARNING : clean context manually because Spring doesn't do it between tests
        subscriber.unsubscribeFrom(IngestRequestFlowItem.class);
    }

    @Test
    public void generateAndPublish() {

        // STORAGE BEHAVIOR
        storageClientMock.setBehavior(true, true);

        long start = System.currentTimeMillis();
        long existingItems = 0;
        long maxloops = 10;
        String session = "session";// OffsetDateTime.now().toString();
        for (long i = 0; i < maxloops; i++) {
            SIP sip = create(PROVIDER_PREFIX + i, null);
            // Create event
            publishSIPEvent(sip, "fake", session, "source", CATEGORIES);
        }

        // Wait
        ingestServiceTest.waitForIngestion(maxloops, maxloops * 10000, SIPState.STORED);

        LOGGER.info("END TEST : {} SIP(s) INGESTED in {} ms", maxloops + existingItems,
                    System.currentTimeMillis() - start);

        sessionNotifier.debugSession();

        // Delete products
        OAISDeletionPayloadDto dto = OAISDeletionPayloadDto.build(SessionDeletionMode.BY_STATE)
                .withProviderId(PROVIDER_PREFIX + "0");
        deletionService.registerOAISDeletionCreator(dto);

        ingestServiceTest.waitForIngestion(1, 100000, SIPState.DELETED);
        sessionNotifier.debugSession();

        // TODO
        // session notif assertion
    }

    @Test
    public void generateWithErrorAndDelete() {

        // STORAGE BEHAVIOR
        storageClientMock.setBehavior(false, true);

        String providerId = "requestError01";
        SIP sip = create(providerId, null);
        publishSIPEvent(sip, "fake", "errorSession", "source", CATEGORIES);

        // Wait
        ingestServiceTest.waitForAIP(1, 30_000, AIPState.GENERATED);
        ingestServiceTest.waitForIngestRequest(1, 30_000, InternalRequestState.ERROR);

        sessionNotifier.debugSession();

        // Remove request
        SearchRequestsParameters filters = SearchRequestsParameters.build().withProviderIds(providerId);
        requestService.scheduleRequestDeletionJob(filters);

        // Wait
        ingestServiceTest.waitForIngestRequest(0, 30_000, null);

        sessionNotifier.debugSession();

        // TODO
        // session notif assertion
    }
}
