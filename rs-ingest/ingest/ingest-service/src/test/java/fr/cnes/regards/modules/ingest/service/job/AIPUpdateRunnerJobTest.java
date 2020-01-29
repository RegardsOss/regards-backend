/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.flow.StorageResponseFlowHandler;
import fr.cnes.regards.modules.ingest.service.schedule.AIPUpdateJobScheduler;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;

/**
 * Test {@link AIPUpdateRunnerJob}
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=update_oais_job",
        "regards.amqp.enabled=true", "regards.ingest.aip.update.bulk.delay=100000000", "eureka.client.enabled=false" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock" })
public class AIPUpdateRunnerJobTest extends IngestMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AIPUpdatesCreatorJobIT.class);

    @Autowired
    private StorageClientMock storageClient;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private AIPUpdateJobScheduler updateFlowHandler;

    @Autowired
    private StorageResponseFlowHandler storageListener;

    @Autowired
    private IJobService jobService;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    private static final List<String> CATEGORIES_0 = Lists.newArrayList("CATEGORY", "CATEGORY00", "CATEGORY01");

    private static final List<String> CATEGORIES_1 = Lists.newArrayList("CATEGORY1");

    private static final List<String> CATEGORIES_2 = Lists.newArrayList("CATEGORY2");

    private static final List<String> TAG_0 = Lists.newArrayList("toto", "tata");

    private static final List<String> TAG_1 = Lists.newArrayList("toto", "tutu");

    private static final List<String> TAG_2 = Lists.newArrayList("plop", "ping");

    private static final List<String> TAG_3 = Lists.newArrayList("toto");

    private static final String STORAGE_1 = "AWS";

    private static final String STORAGE_2 = "Azure";

    private static final String STORAGE_3 = "Pentagon";

    private static final String SESSION_OWNER_0 = "NASA";

    private static final String SESSION_OWNER_1 = "CNES";

    private static final String SESSION_0 = OffsetDateTime.now().toString();

    private static final String SESSION_1 = OffsetDateTime.now().minusDays(4).toString();

    @Override
    public void doInit() {
        simulateApplicationReadyEvent();
        // Re-set tenant because above simulation clear it!
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        abstractRequestRepository.deleteAll();
        jobInfoRepository.deleteAll();
    }

    public void initData() {

        long nbSIP = 6;
        publishSIPEvent(create("1", TAG_0), STORAGE_1, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("3", TAG_1), Lists.newArrayList(STORAGE_2, STORAGE_3), SESSION_0, SESSION_OWNER_0,
                        CATEGORIES_0);

        // useless entities for this test
        publishSIPEvent(create("6", TAG_0), STORAGE_2, SESSION_1, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("2", TAG_0), STORAGE_1, SESSION_0, SESSION_OWNER_1, CATEGORIES_1);
        publishSIPEvent(create("4", TAG_1), STORAGE_1, SESSION_1, SESSION_OWNER_1, CATEGORIES_1);
        publishSIPEvent(create("5", TAG_1), STORAGE_2, SESSION_1, SESSION_OWNER_1, CATEGORIES_0);
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000, SIPState.STORED);
        // Wait STORE_META request over
        ingestServiceTest.waitAllRequestsFinished(nbSIP * 5000);
    }

    /**
     * Helper method to wait for DB ingestion
     * @param expectedTasks expected count of task in db
     * @param timeout in ms
     * @throws InterruptedException
     */
    public void waitForUpdateTaskCreated(long expectedTasks, long timeout) {
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

    protected void runAndWaitJob(Collection<JobInfo> jobs) {
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        try {
            Iterator<JobInfo> it = jobs.iterator();
            List<RunnableFuture<Void>> list = Lists.newArrayList();
            while (it.hasNext()) {
                list.add(jobService.runJob(it.next(), tenant));
            }
            for (RunnableFuture<Void> futur : list) {
                futur.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        } finally {
            runtimeTenantResolver.forceTenant(tenant);
        }
    }

    /**
     * Test to add/remove TAGS and categories to an existing list of AIPs.
     * @throws ModuleException
     */
    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_420"), @Requirement("REGARDS_DSL_STO_AIP_430"),
            @Requirement("REGARDS_DSL_STO_AIP_210") })
    @Purpose("Check that specific informations can be updated in AIP properties")
    public void testUpdateJob() throws ModuleException {
        storageClient.setBehavior(true, true);
        initData();
        aipService.registerUpdatesCreator(AIPUpdateParametersDto
                .build(SearchAIPsParameters.build().withSession(SESSION_0).withSessionOwner(SESSION_OWNER_0), TAG_2,
                       TAG_3, CATEGORIES_2, CATEGORIES_0, Lists.newArrayList(STORAGE_3)));
        long nbSipConcerned = 2;
        long nbTasksPerSip = 5;
        waitForUpdateTaskCreated(nbSipConcerned * nbTasksPerSip, 10_000);
        JobInfo updateJob = updateFlowHandler.getUpdateJob();
        runAndWaitJob(Lists.newArrayList(updateJob));
        Pageable pageRequest = PageRequest.of(0, 200);

        Page<AIPEntity> aips = aipService
                .findByFilters(SearchAIPsParameters.build().withSession(SESSION_0).withSessionOwner(SESSION_OWNER_0),
                               pageRequest);
        List<AIPEntity> aipsContent = aips.getContent();
        for (AIPEntity aip : aipsContent) {
            Assert.assertEquals(3, aip.getTags().size());
            // TAG_3 are not existing anymore on entities
            Assert.assertFalse(aip.getTags().stream().anyMatch(tag -> TAG_3.contains(tag)));
            Assert.assertEquals(1, aip.getCategories().size());
            // Only one category remaining
            Assert.assertEquals(CATEGORIES_2.get(0), aip.getCategories().iterator().next());
            // No more STORAGE_3
            Assert.assertFalse(aip.getStorages().contains(STORAGE_3));
        }
    }

    @Test
    public void testUpdateAIPFileLocationJob() throws InterruptedException {

        storageClient.setBehavior(true, true);
        initData();

        Page<AIPEntity> aips = aipService
                .findByFilters(SearchAIPsParameters.build().withSession(SESSION_0).withSessionOwner(SESSION_OWNER_0),
                               PageRequest.of(0, 200));
        AIPEntity toUpdate = aips.getContent().get(0);
        String providerId = toUpdate.getProviderId();
        Assert.assertEquals("Before adding the new location the data object should contains only one location", 1,
                            toUpdate.getAip().getProperties().getContentInformations().get(0).getDataObject()
                                    .getLocations().size());
        String toUpdateChecksum = toUpdate.getAip().getProperties().getContentInformations().get(0).getDataObject()
                .getChecksum();

        LOGGER.info("Updating AIP {} and file {}", toUpdate.getAipId(), toUpdateChecksum);

        Set<RequestInfo> requests = Sets.newHashSet();
        String newStorageLocation = "somewhere";
        Collection<RequestResultInfoDTO> successRequests = Sets.newHashSet();
        successRequests.add(RequestResultInfoDTO
                .build("groupId", toUpdateChecksum, newStorageLocation, null, Sets.newHashSet("someone"),
                       simulatefileReference(toUpdateChecksum, toUpdate.getAipId()), null));
        requests.add(RequestInfo.build("groupId", successRequests, Sets.newHashSet()));

        storageListener.onCopySuccess(requests);

        JobInfo updateJob = updateFlowHandler.getUpdateJob();
        runAndWaitJob(Lists.newArrayList(updateJob));

        // Check that the new location is added to the AIP in DB.
        aips = aipService.findByFilters(SearchAIPsParameters.build().withProviderId(providerId), PageRequest.of(0, 10));
        AIPEntity updateAIP = aips.getContent().get(0);
        Assert.assertEquals("After adding the new location the data object should contains two locations", 2, updateAIP
                .getAip().getProperties().getContentInformations().get(0).getDataObject().getLocations().size());
        Assert.assertTrue("New location is not added to the AIP dataobject",
                          updateAIP.getAip().getProperties().getContentInformations().get(0).getDataObject()
                                  .getLocations().stream()
                                  .anyMatch(l -> l.getStorage().contentEquals(newStorageLocation)));

    }

    protected FileReference simulatefileReference(String checksum, String owner) {
        FileReferenceMetaInfo meta = new FileReferenceMetaInfo(checksum, "MD5", "file.name", 10L, MediaType.TEXT_PLAIN);
        return new FileReference(owner, meta, new FileLocation("somewhere", "file:///somewhere/file.name"));
    }
}
