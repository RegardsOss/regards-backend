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
package fr.cnes.regards.modules.ingest.service.job;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.DisseminationInfo;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.aip.scheduler.AIPUpdateRequestScheduler;
import fr.cnes.regards.modules.ingest.service.flow.StorageResponseFlowHandler;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link AIPUpdateRunnerJob}
 *
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=update_oais_job",
                                   "regards.amqp.enabled=true",
                                   "regards.ingest.aip.update.bulk.delay=100000000",
                                   "eureka.client.enabled=false" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock" })
public class AIPUpdateRunnerJobIT extends IngestMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPUpdatesCreatorJobIT.class);

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

    private static final String LABEL_0 = "LABEL0";

    private static final String LABEL_1 = "LABEL1";

    private static final String LABEL_2 = "LABEL2";

    private static final OffsetDateTime INITIAL_DATE = OffsetDateTime.now();

    boolean isToNotify;

    @Autowired
    private StorageClientMock storageClient;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private AIPUpdateRequestScheduler aipUpdateRequestScheduler;

    @Autowired
    private StorageResponseFlowHandler storageListener;

    @Autowired
    private IJobService jobService;

    @Override
    public void doInit() {
        // Notification
        this.isToNotify = initDefaultNotificationSettings();
    }

    public void initData() throws InterruptedException {

        long nbSIP = 6;
        publishSIPEvent(create(UUID.randomUUID().toString(), TAG_0),
                        STORAGE_1,
                        SESSION_0,
                        SESSION_OWNER_0,
                        CATEGORIES_0);
        publishSIPEvent(create(UUID.randomUUID().toString(), TAG_1),
                        Lists.newArrayList(STORAGE_2, STORAGE_3),
                        SESSION_0,
                        SESSION_OWNER_0,
                        Lists.newArrayList(CATEGORIES_0),
                        Optional.empty());

        // useless entities for this test
        publishSIPEvent(create(UUID.randomUUID().toString(), TAG_0),
                        STORAGE_2,
                        SESSION_1,
                        SESSION_OWNER_0,
                        CATEGORIES_0);
        publishSIPEvent(create(UUID.randomUUID().toString(), TAG_0),
                        STORAGE_1,
                        SESSION_0,
                        SESSION_OWNER_1,
                        CATEGORIES_1);
        publishSIPEvent(create(UUID.randomUUID().toString(), TAG_1),
                        STORAGE_1,
                        SESSION_1,
                        SESSION_OWNER_1,
                        CATEGORIES_1);
        publishSIPEvent(create(UUID.randomUUID().toString(), TAG_1),
                        STORAGE_2,
                        SESSION_1,
                        SESSION_OWNER_1,
                        CATEGORIES_0);
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000, SIPState.STORED);

        if (!isToNotify) {
            // Wait STORE_META request over
            ingestServiceTest.waitAllRequestsFinished(nbSIP * 5000);
        } else {
            notificationService.handleNotificationSuccess(Sets.newHashSet(ingestRequestRepository.findAll()));
        }

        // Check init datas contains the storage to remove in this test
        Page<AIPEntity> aips = aipService.findByFilters(new SearchAIPsParameters(), PageRequest.of(0, 200));
        List<AIPEntity> aipsContent = aips.getContent();

        ArrayList<DisseminationInfo> disseminationInfo = Lists.newArrayList(new DisseminationInfo(LABEL_0,
                                                                                                  INITIAL_DATE,
                                                                                                  null),
                                                                            new DisseminationInfo(LABEL_1,
                                                                                                  INITIAL_DATE,
                                                                                                  null));

        for (AIPEntity aip : aipsContent) {
            LOGGER.info("Intial AIP {}/{} tags : {}, categories : {}, storages : {}",
                        aip.getProviderId(),
                        aip.getState(),
                        aip.getTags(),
                        aip.getCategories(),
                        aip.getStorages());
            aip.setDisseminationInfos(disseminationInfo);
            aipService.save(aip);
        }
    }

    /**
     * Helper method to wait for DB ingestion
     *
     * @param expectedTasks expected count of task in db
     * @param timeout       in ms
     */
    public void waitForUpdateTaskCreated(long expectedTasks, long timeout) {
        Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return aipUpdateRequestRepository.count() == expectedTasks;
        });
    }

    /**
     * Test to add/remove TAGS and categories to an existing list of AIPs.
     */
    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_420"),
                    @Requirement("REGARDS_DSL_STO_AIP_430"),
                    @Requirement("REGARDS_DSL_STO_AIP_210") })
    @Purpose("Check that specific informations can be updated in AIP properties")
    public void testUpdateJob() throws ModuleException, InterruptedException {
        storageClient.setBehavior(true, true);
        initData();

        LOGGER.info("TAGS ADD : {}, REMOVE {}", TAG_2, TAG_3);
        LOGGER.info("CATEGORIES ADD : {}, REMOVE {}", CATEGORIES_2, CATEGORIES_0);
        LOGGER.info("STORAGES REMOVE : {}", STORAGE_3);
        LOGGER.info("DISSEMINATION INFO UPDATED FOR LABEL : {}", LABEL_0);

        OffsetDateTime firstAckDate = OffsetDateTime.now();

        List<DisseminationInfo> disseminationInfos = Lists.newArrayList(new DisseminationInfo(LABEL_0,
                                                                                              null,
                                                                                              firstAckDate),
                                                                        new DisseminationInfo(LABEL_1,
                                                                                              OffsetDateTime.now(),
                                                                                              null),
                                                                        new DisseminationInfo(LABEL_2,
                                                                                              OffsetDateTime.now(),
                                                                                              null));

        aipService.registerUpdatesCreator(AIPUpdateParametersDto.build(new SearchAIPsParameters().withSession(SESSION_0)
                                                                                                 .withSessionOwner(
                                                                                                     SESSION_OWNER_0),
                                                                       TAG_2,
                                                                       TAG_3,
                                                                       CATEGORIES_2,
                                                                       CATEGORIES_0,
                                                                       Lists.newArrayList(STORAGE_3),
                                                                       disseminationInfos));
        long nbSipConcerned = 2;
        long nbTasksPerSip = 6;
        waitForUpdateTaskCreated(nbSipConcerned * nbTasksPerSip, 10_000);
        // Wait job scheduled
        JobInfo updateJob = aipUpdateRequestScheduler.scheduleJob();
        // Wait job done
        waitJobDone(updateJob, JobStatus.SUCCEEDED, 5_000);

        Page<AIPEntity> aips = aipService.findByFilters(new SearchAIPsParameters().withSession(SESSION_0)
                                                                                  .withSessionOwner(SESSION_OWNER_0),
                                                        PageRequest.of(0, 200));
        Collection<AIPEntity> aipsContent = aips.getContent();
        for (AIPEntity aip : aipsContent) {
            Assert.assertEquals(3, aip.getTags().size());
            // TAG_3 are not existing anymore on entities
            Assert.assertFalse(aip.getTags().stream().anyMatch(tag -> TAG_3.contains(tag)));
            Assert.assertEquals(1, aip.getCategories().size());
            // Only one category remaining
            Assert.assertEquals(CATEGORIES_2.get(0), aip.getCategories().iterator().next());
            // No more STORAGE_3
            Assert.assertFalse(aip.getStorages().contains(STORAGE_3));

            // Dissemination infos are updated
            Assert.assertEquals(aip.getDisseminationInfos().size(), 3);
            DisseminationInfo disseminationInfo0 = aip.getDisseminationInfos()
                                                      .stream()
                                                      .filter(aipInfo -> aipInfo.getLabel().equals(LABEL_0))
                                                      .findFirst()
                                                      .get();
            DisseminationInfo disseminationInfo1 = aip.getDisseminationInfos()
                                                      .stream()
                                                      .filter(aipInfo -> aipInfo.getLabel().equals(LABEL_1))
                                                      .findFirst()
                                                      .get();
            DisseminationInfo disseminationInfo2 = aip.getDisseminationInfos()
                                                      .stream()
                                                      .filter(aipInfo -> aipInfo.getLabel().equals(LABEL_2))
                                                      .findFirst()
                                                      .get();
            Assert.assertFalse(disseminationInfo0.getAckDate() == null);
            Assert.assertTrue(disseminationInfo1.getAckDate() == null);
            Assert.assertFalse(disseminationInfo0.getDate().isAfter(INITIAL_DATE));
            Assert.assertTrue(disseminationInfo1.getDate().isAfter(INITIAL_DATE));
            Assert.assertFalse(disseminationInfo2.getDate() == null);
        }
    }

    @Test
    public void testUpdateAIPFileLocationJob() throws InterruptedException {
        ingestServiceTest.waitAllRequestsFinished(20_000);
        storageClient.setBehavior(true, true);
        initData();

        Page<AIPEntity> aips = aipService.findByFilters(new SearchAIPsParameters().withSession(SESSION_0)
                                                                                  .withSessionOwner(SESSION_OWNER_0)
                                                                                  .withStoragesIncluded(List.of(
                                                                                      STORAGE_1)),
                                                        PageRequest.of(0, 200));
        AIPEntity toUpdate = aips.getContent().get(0);
        String providerId = toUpdate.getProviderId();
        Assert.assertEquals("Before adding the new location the data object should contains only one location",
                            1,
                            toUpdate.getAip()
                                    .getProperties()
                                    .getContentInformations()
                                    .get(0)
                                    .getDataObject()
                                    .getLocations()
                                    .size());
        String toUpdateChecksum = toUpdate.getAip()
                                          .getProperties()
                                          .getContentInformations()
                                          .get(0)
                                          .getDataObject()
                                          .getChecksum();

        LOGGER.info("Updating AIP {} and file {}", toUpdate.getAipId(), toUpdateChecksum);

        Set<RequestInfo> requests = Sets.newHashSet();
        String newStorageLocation = "somewhere";
        Collection<RequestResultInfoDTO> successRequests = Sets.newHashSet();
        Collection<String> owners = Sets.newHashSet(toUpdate.getAipId());
        successRequests.add(RequestResultInfoDTO.build("groupId",
                                                       toUpdateChecksum,
                                                       newStorageLocation,
                                                       null,
                                                       owners,
                                                       simulatefileReference(toUpdateChecksum, toUpdate.getAipId()),
                                                       null));
        requests.add(RequestInfo.build("groupId", successRequests, Sets.newHashSet()));

        storageListener.onCopySuccess(requests);

        JobInfo updateJob = aipUpdateRequestScheduler.scheduleJob();
        Assert.assertNotNull("One update job should be scheduled", updateJob);
        waitJobDone(updateJob, JobStatus.SUCCEEDED, 5_000);

        // Check that the new location is added to the AIP in DB.
        aips = aipService.findByFilters(new SearchAIPsParameters().withProviderIdsIncluded(List.of(providerId)),
                                        PageRequest.of(0, 10));
        AIPEntity updateAIP = aips.getContent().get(0);
        Assert.assertEquals("After adding the new location the data object should contains two locations",
                            2,
                            updateAIP.getAip()
                                     .getProperties()
                                     .getContentInformations()
                                     .get(0)
                                     .getDataObject()
                                     .getLocations()
                                     .size());
        Assert.assertTrue("New location is not added to the AIP dataobject",
                          updateAIP.getAip()
                                   .getProperties()
                                   .getContentInformations()
                                   .get(0)
                                   .getDataObject()
                                   .getLocations()
                                   .stream()
                                   .anyMatch(l -> l.getStorage().contentEquals(newStorageLocation)));

    }

    protected FileReference simulatefileReference(String checksum, String owner) {
        return new FileReference(owner,
                                 new FileReferenceMetaInfo(checksum, "MD5", "file.name", 10L, MediaType.TEXT_PLAIN),
                                 new FileLocation("somewhere", "file:///somewhere/file.name", false));
    }

}
