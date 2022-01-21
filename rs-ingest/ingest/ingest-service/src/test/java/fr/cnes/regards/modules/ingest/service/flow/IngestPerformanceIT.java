/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.request.IOAISDeletionService;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
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
                "regards.ingest.aip.delete.bulk.delay=100" },
        locations = { "classpath:application-test.properties" })
@ActiveProfiles({ "testAmqp", "StorageClientMock" })
@Ignore("Performance test")
public class IngestPerformanceIT extends IngestMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestPerformanceIT.class);

    private static final List<String> CATEGORIES = Lists.newArrayList("CATEGORY");

    private static final String PROVIDER_PREFIX = "provider";

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IOAISDeletionService deletionService;

    @Autowired
    private IAIPUpdateRequestRepository updateReqRepo;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private StorageClientMock storageClientMock;

    @Autowired
    private IRequestService requestService;

    @Override
    public void doInit() {
        simulateApplicationReadyEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    /**
     * Test scenario :
     * 1. Ingest Products
     * 2. Wait for ingestion ends
     * 3. Delete Products unitary
     * 4. Wait for  deletion ends
     */
    @Test
    public void generateAndPublish() {

        // STORAGE BEHAVIOR
        storageClientMock.setBehavior(true, true);

        long start = System.currentTimeMillis();
        long existingItems = 0;
        long maxloops = 10000;
        int maxSessions = 1;
        // 1. Populate catalog with products

        for (int s = 0; s < maxSessions; s++) {
            String session = "session" + s;// OffsetDateTime.now().toString();
            for (long i = 0; i < maxloops; i++) {
                SIP sip = create(PROVIDER_PREFIX + i, null);
                // Create event
                publishSIPEvent(sip, "fake", session, "source", CATEGORIES);
            }
            //            try {
            //                // FIXME remove only for specific concurrent tests
            //                Thread.sleep(30_000);
            //            } catch (InterruptedException e) {
            //                LOGGER.error(e.getMessage(), e);
            //            }
        }

        // 2. Wait
        ingestServiceTest.waitForIngestion(maxloops * maxSessions, maxloops * maxSessions * 10000, SIPState.STORED);

        LOGGER.info("END TEST : {} SIP(s) INGESTED in {} ms", (maxloops * maxSessions) + existingItems,
                    System.currentTimeMillis() - start);


        //        // 3. Delete products
        //        OAISDeletionPayloadDto dto = OAISDeletionPayloadDto.build(SessionDeletionMode.BY_STATE)
        //                .withProviderId(PROVIDER_PREFIX + "0");
        //        deletionService.registerOAISDeletionCreator(dto);
        //
        //        // 4. Wait
        //        ingestServiceTest.waitForIngestion(1, 100000, SIPState.DELETED);
        //        sessionNotifier.debugSession();
    }

    /**
     * Test scenario :
     * 1. Ingest Products
     * 2. Wait for ingestion ends
     * 3. Add some error requests
     * 4. Delete Products through OAISDeletionCreator
     * 5. Run second  time the deletion through OAISDeletionCreator
     * 6. Wait for  deletion ends
     */
    @Test
    public void deletionRequests() {

        // STORAGE BEHAVIOR
        storageClientMock.setBehavior(true, true);

        long start = System.currentTimeMillis();
        long nbStored = 100;
        long nbDeleted = 20;

        // 1. Populate catalog with products

        String session = "session";// OffsetDateTime.now().toString();
        for (long i = 0; i < nbStored; i++) {
            SIP sip = create(PROVIDER_PREFIX + i, null);
            // Create event
            publishSIPEvent(sip, "fake", session, "source", CATEGORIES);
        }
        // 2. Wait
        ingestServiceTest.waitForIngestion(nbStored, nbStored * 10000, SIPState.STORED);
        LOGGER.info("END TEST : {} SIP(s) INGESTED in {} ms", nbStored, System.currentTimeMillis() - start);

        // 3. Simulate an update request in error
        String sipToUpdate = sipRepository.findTopByProviderIdOrderByCreationDateDesc(PROVIDER_PREFIX + 0).getSipId();
        AIPEntity aip = aipService.findBySipId(sipToUpdate).iterator().next();
        AIPUpdateRequest updateRequest = new AIPUpdateRequest();
        updateRequest.setUpdateTask(null);
        updateRequest.setAip(aip);
        updateRequest.setCreationDate(OffsetDateTime.now());
        updateRequest.setSessionOwner(aip.getSessionOwner());
        updateRequest.setSession(aip.getSession());
        updateRequest.setProviderId(aip.getProviderId());
        updateRequest.setDtype(RequestTypeConstant.UPDATE_VALUE);
        updateRequest.setState(InternalRequestState.ERROR);
        updateReqRepo.save(updateRequest);

        // 4. Ask for product 1000 deletion
        OAISDeletionPayloadDto dto = OAISDeletionPayloadDto.build(SessionDeletionMode.IRREVOCABLY);
        for (int i = 0; i < nbDeleted; i++) {
            dto.withProviderId(PROVIDER_PREFIX + i);
        }
        deletionService.registerOAISDeletionCreator(dto);
        // 5. Send two times the same deletion request
        deletionService.registerOAISDeletionCreator(dto);

        // 6. Wait for all  deletion + new ingestion ends
        ingestServiceTest.waitAllRequestsFinished(180_000);
        ingestServiceTest.waitForIngestion(nbStored - nbDeleted, 100000, SIPState.STORED);
        ingestServiceTest.waitAllRequestsFinished(180_000);

    }

    /**
     * Test scenario :
     * 1. Ingest Products
     * 2. Wait for ingestion ends
     * 3. Update Products through AIPUpdatesCreatorRequest
     * 4. Run second  time the same update through AIPUpdatesCreatorRequest
     * 5. Wait for update ends
     */
    @Test
    public void updateRequests() {

        // STORAGE BEHAVIOR
        storageClientMock.setBehavior(true, true);

        long start = System.currentTimeMillis();
        long nbStored = 100;

        // 1. Populate catalog with 10_000 products (sip/aip)

        String session = "session";// OffsetDateTime.now().toString();
        for (long i = 0; i < nbStored; i++) {
            SIP sip = create(PROVIDER_PREFIX + i, null);
            // Create event
            publishSIPEvent(sip, "fake", session, "source", CATEGORIES);
        }
        // 2. Wait
        ingestServiceTest.waitForIngestion(nbStored, nbStored * 10000, SIPState.STORED);
        LOGGER.info("END TEST : {} SIP(s) INGESTED in {} ms", nbStored, System.currentTimeMillis() - start);

        // 3. Ask for product updates
        AIPUpdateParametersDto updateDto = AIPUpdateParametersDto
                .build(SearchAIPsParameters.build().withCategories(CATEGORIES.get(0)))
                .withAddCategories(Lists.newArrayList("new_cat"));
        aipService.registerUpdatesCreator(updateDto);
        // 4. Ask for same product updates
        aipService.registerUpdatesCreator(updateDto);

        // 5. Wait for all 1000 deletion + 500 new ingestion ends
        ingestServiceTest.waitAllRequestsFinished(180_000);
        ingestServiceTest.waitForIngestion(nbStored, 100000, SIPState.STORED);
        ingestServiceTest.waitAllRequestsFinished(180_000);
    }

    /**
     * Test scenario :
     * 1. Ingest Products
     * 2. Wait for ingestion ends
     * 3. Simulate ingestion errors
     * 4. Wait for ingestion errors ends
     * 5. Send products ingest requests
     * 6. Send products deletion request
     * 7. Send products ingest requests
     * 8. Send products update request
     * 9. Wait results of parallel results of 5,6,7 & 8
     */
    @Test
    public void testAllRequests() {

        // STORAGE BEHAVIOR
        storageClientMock.setBehavior(true, true);

        long start = System.currentTimeMillis();
        long nbStored = 1_000;
        long nbErrors = 200;
        long nbDeleted = 300;

        // 1. Populate catalog with products (sip/aip)
        String session = "session";// OffsetDateTime.now().toString();
        for (long i = 0; i < nbStored; i++) {
            SIP sip = create(PROVIDER_PREFIX + i, null);
            // Create event
            publishSIPEvent(sip, "fake", session, "source", CATEGORIES);
        }
        // 2. Wait ingestion ends
        ingestServiceTest.waitForIngestion(nbStored, nbStored * 10000, SIPState.STORED);
        LOGGER.info("END TEST : {} SIP(s) INGESTED in {} ms", nbStored, System.currentTimeMillis() - start);

        // 3. Simulate errors
        storageClientMock.setBehavior(true, false);
        for (long i = 0; i < nbErrors; i++) {
            SIP sip = create(PROVIDER_PREFIX + "_errors_" + i, null);
            // Create event
            publishSIPEvent(sip, "fake", session, "source", CATEGORIES);
        }
        // 4. Wait errors done
        ingestServiceTest.waitForIngestRequest(nbErrors, nbErrors * 10000, InternalRequestState.ERROR);
        storageClientMock.setBehavior(true, true);

        // 5. Ask for new products
        for (long i = 0; i < (nbStored / 4); i++) {
            SIP sip = create(PROVIDER_PREFIX + "new" + i, null);
            // Create event
            publishSIPEvent(sip, "fake", session, "source", CATEGORIES);
            nbStored++;
        }
        LOGGER.info("===============> Ingestion sents !!");

        // 6. Ask for product deletion
        OAISDeletionPayloadDto dto = OAISDeletionPayloadDto.build(SessionDeletionMode.BY_STATE);
        for (int i = 0; i < nbDeleted; i++) {
            dto.withProviderId(PROVIDER_PREFIX + i);
        }
        deletionService.registerOAISDeletionCreator(dto);
        LOGGER.info("===============> Deletion sents !!");

        // 7. Ask for new product without waiting ends of previous ingests
        for (long i = 0; i < (nbStored / 4); i++) {
            SIP sip = create(PROVIDER_PREFIX + "new" + i, null);
            // Create event
            publishSIPEvent(sip, "fake", session, "source", CATEGORIES);
            nbStored++;
        }
        LOGGER.info("===============> Ingestion sents !!");

        // 8. Ask for products update
        AIPUpdateParametersDto updateDto = AIPUpdateParametersDto
                .build(SearchAIPsParameters.build().withCategories(CATEGORIES.get(0)))
                .withAddCategories(Lists.newArrayList("new_cat"));
        aipService.registerUpdatesCreator(updateDto);
        LOGGER.info("===============> Update sents !!");

        // 9. Wait for all deletion and ingestion ends
        ingestServiceTest.waitForIngestion(nbDeleted, 100000, SIPState.DELETED);
        long count = nbStored - nbDeleted;
        ingestServiceTest.waitForIngestion(count, count * 1000, SIPState.STORED);
        ingestServiceTest.waitAllRequestsFinished(180_000);
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


        // Remove request
        SearchRequestsParameters filters = SearchRequestsParameters.build().withProviderIds(providerId);
        requestService.scheduleRequestDeletionJob(filters);

        // Wait
        ingestServiceTest.waitForIngestRequest(0, 30_000, null);

    }
}
