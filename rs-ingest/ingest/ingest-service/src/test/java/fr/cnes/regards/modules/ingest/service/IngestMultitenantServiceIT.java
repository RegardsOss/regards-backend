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
package fr.cnes.regards.modules.ingest.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.integration.test.job.JobTestCleaner;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dao.*;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.DeletionRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.domain.sip.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.aip.scheduler.IngestRequestSchedulerService;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingChainService;
import fr.cnes.regards.modules.ingest.service.flow.IngestRequestFlowHandler;
import fr.cnes.regards.modules.ingest.service.notification.IAIPNotificationService;
import fr.cnes.regards.modules.ingest.service.plugin.AIPGenerationTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.ValidationTestPlugin;
import fr.cnes.regards.modules.ingest.service.settings.IIngestSettingsService;
import fr.cnes.regards.modules.test.IngestServiceIT;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.cnes.regards.modules.ingest.service.TestData.*;

/**
 * Overlay of the default class to manage context cleaning in non transactional testing
 *
 * @author Marc SORDI
 */
@TestPropertySource(properties = { "eureka.client.enabled=false",
                                   "regards.ingest.schedlock.timeout=1",
                                   "regards.ingest.schedule.request.initial.delay=100",
                                   "regards.ingest.aip.post-process.bulk.delay.init=100",
                                   "regards.ingest.aip.update.bulk.delay.init=100",
                                   "regards.ingest.aip.delete.bulk.delay.init=100",
                                   "regards.ingest.schedule.pending.initial.delay=100" },
                    locations = { "classpath:application-test.properties" })
public abstract class IngestMultitenantServiceIT extends AbstractMultitenantServiceIT {

    protected static final long TWO_SECONDS = 2000;

    protected static final long THREE_SECONDS = 3000;

    protected static final long FIVE_SECONDS = 5000;

    protected static final long TEN_SECONDS = 10000;

    protected final static String CHAIN_PP_LABEL = "ChainWithPostProcess";

    protected final static String CHAIN_PP_WITH_ERRORS_LABEL = "ChainWithPostProcessErrors";

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected IIngestRequestRepository ingestRequestRepository;

    @Autowired
    protected IOAISDeletionRequestRepository oaisDeletionRequestRepository;

    @Autowired
    protected IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    protected IAIPPostProcessRequestRepository aipPostProcessRequestRepository;

    @Autowired
    protected ISIPRepository sipRepository;

    @Autowired
    protected IAIPRepository aipRepository;

    @Autowired
    protected IJobInfoService jobInfoService;

    @Autowired
    protected IngestServiceIT ingestServiceTest;

    @Autowired
    protected IIngestProcessingChainService ingestProcessingService;

    @Autowired
    protected IIngestSettingsService ingestSettingsService;

    @Autowired
    protected IAIPNotificationService notificationService;

    @Autowired
    protected IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    @Autowired
    protected IJobService jobService;

    @Autowired
    private IngestRequestFlowHandler requestFlowHandler;

    /**
     * Can be null if profile nojobs
     */
    @Autowired(required = false)
    private JobTestCleaner jobTestCleaner;

    @Before
    public void init() throws Exception {
        LOGGER.info("-------------> Test initialization !!!");
        // clear AMQP queues and repositories
        ingestServiceTest.init();
        jobService.cleanAndRestart();

        // simulate application started and ready
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // override this method to custom action performed before
        doInit();
    }

    /**
     * Custom test initialization to override
     */
    protected void doInit() throws Exception {
        // Override to init something
    }

    @After
    public void after() throws Exception {
        if (jobTestCleaner != null) {
            jobTestCleaner.cleanJob();
        }
        // override this method to custom action performed after
        doAfter();
    }

    /**
     * Custom test cleaning to override
     */
    protected void doAfter() throws Exception {
        // Override to init something
    }

    /**
     * Creates a new {@link IngestRequestFlowItem}
     */
    protected IngestRequestFlowItem createSipEvent(String providerId,
                                                   List<String> tags,
                                                   String storage,
                                                   String session,
                                                   String sessionOwner,
                                                   List<String> categories) {

        SIP sip = create(providerId, tags);
        return ingestServiceTest.createSipEvent(sip,
                                                storage,
                                                session,
                                                sessionOwner,
                                                categories,
                                                Optional.empty(),
                                                VersioningMode.INC_VERSION);
    }

    protected SIP create(String providerId, List<String> tags) {
        String fileName = String.format("file-%s.dat", providerId);
        SIP sip = SIP.build(EntityType.DATA, providerId);
        sip.withDataObject(DataType.RAWDATA,
                           Paths.get("src", "test", "resources", "data", fileName),
                           "MD5",
                           UUID.randomUUID().toString());
        sip.withSyntax(MediaType.APPLICATION_JSON);
        sip.registerContentInformation();
        if ((tags != null) && !tags.isEmpty()) {
            sip.withContextTags(tags.toArray(new String[0]));
        }

        // Add creation event
        sip.withEvent(String.format("SIP %s generated", providerId));

        return sip;
    }

    protected IngestProcessingChain createChainWithPostProcess(String label, Class<?> postProcessPluginClass)
        throws ModuleException {
        IngestProcessingChain newChain = new IngestProcessingChain();
        newChain.setDescription(label);
        newChain.setName(label);

        PluginConfiguration validation = PluginConfiguration.build(ValidationTestPlugin.class, null, Sets.newHashSet());
        validation.setIsActive(true);
        validation.setLabel("validationPlugin_ipst");
        newChain.setValidationPlugin(validation);

        PluginConfiguration generation = PluginConfiguration.build(AIPGenerationTestPlugin.class,
                                                                   null,
                                                                   Sets.newHashSet());
        generation.setIsActive(true);
        generation.setLabel("generationPlugin_ipst");
        newChain.setGenerationPlugin(generation);

        PluginConfiguration postprocess = PluginConfiguration.build(postProcessPluginClass, null, Sets.newHashSet());
        postprocess.setIsActive(true);
        postprocess.setLabel("postprocess test plugin");

        newChain.setPostProcessingPlugin(postprocess);

        return ingestProcessingService.createNewChain(newChain);
    }

    protected void publishSIPEvent(SIP sip,
                                   String storage,
                                   String session,
                                   String sessionOwner,
                                   List<String> categories) {
        publishSIPEvent(sip, Lists.newArrayList(storage), session, sessionOwner, categories, Optional.empty());
    }

    protected void handleSipEventsWithoutAmqp(Collection<SIP> sips,
                                              List<String> storages,
                                              String session,
                                              String sessionOwner,
                                              List<String> categories,
                                              Optional<String> chainLabel) {

        List<StorageMetadata> storagesMeta = storages.stream().map(StorageMetadata::build).collect(Collectors.toList());
        IngestMetadataDto mtd = IngestMetadataDto.build(sessionOwner,
                                                        session,
                                                        null,
                                                        chainLabel.orElse(IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL),
                                                        Sets.newHashSet(categories),
                                                        VersioningMode.INC_VERSION,
                                                        null,
                                                        storagesMeta);

        List<IngestRequestFlowItem> events = new ArrayList<>(sips.size());
        for (SIP sip : sips) {
            events.add(IngestRequestFlowItem.build(mtd, sip));
        }
        requestFlowHandler.handleBatch(events);
    }

    protected void publishSIPEvent(SIP sip,
                                   List<String> storages,
                                   String session,
                                   String sessionOwner,
                                   List<String> categories,
                                   Optional<String> chainLabel) {
        publishSIPEvent(sip, storages, session, sessionOwner, categories, chainLabel, null);
    }

    protected void publishSIPEvent(SIP sip,
                                   List<String> storages,
                                   String session,
                                   String sessionOwner,
                                   List<String> categories,
                                   Optional<String> chainLabel,
                                   VersioningMode versioningMode) {
        publishSIPEvent(Sets.newHashSet(sip), storages, session, sessionOwner, categories, chainLabel, versioningMode);
    }

    protected void publishSIPEvent(Collection<SIP> sips,
                                   List<String> storages,
                                   String session,
                                   String sessionOwner,
                                   List<String> categories,
                                   Optional<String> chainLabel,
                                   VersioningMode versioningMode) {
        // Create event
        List<StorageMetadata> storagesMeta = storages.stream().map(StorageMetadata::build).collect(Collectors.toList());
        IngestMetadataDto mtd = IngestMetadataDto.build(sessionOwner,
                                                        session,
                                                        null,
                                                        chainLabel.orElse(IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL),
                                                        Sets.newHashSet(categories),
                                                        versioningMode,
                                                        null,
                                                        storagesMeta);
        ingestServiceTest.sendIngestRequestEvent(sips, mtd);
    }

    public boolean initDefaultNotificationSettings() {
        return ingestSettingsService.isActiveNotification();
    }

    public void initRandomData(int nbSIP, IngestRequestSchedulerService schedulerService) {
        for (int i = 0; i < nbSIP; i++) {
            publishSIPEvent(create(UUID.randomUUID().toString(), getRandomTags()),
                            getRandomStorage().get(0),
                            getRandomSession(),
                            getRandomSessionOwner(),
                            getRandomCategories());
        }
        if (schedulerService != null) {
            waitSipCount(nbSIP);
            schedulerService.scheduleRequests();
        }
        // Wait for SIP ingestion
        ingestServiceTest.waitForIngestion(nbSIP, TEN_SECONDS * nbSIP, SIPState.STORED);
        // Wait for all requests to finish in case of no notification else delete requests
        if (!initDefaultNotificationSettings()) {
            ingestServiceTest.waitAllRequestsFinished(nbSIP * 1000L);
        } else {
            ingestRequestRepository.deleteAll();
            Assert.assertEquals("All ingest requests should have been deleted", 0L, ingestRequestRepository.count());
        }
    }

    // simulation notification success when required
    public void mockNotificationSuccess(String type) {
        List<? extends AbstractRequest> requests;
        switch (type) {
            case RequestTypeConstant.INGEST_VALUE:
                requests = ingestRequestRepository.findAll();
                IngestRequest ingestRequest;
                for (AbstractRequest request : requests) {
                    ingestRequest = (IngestRequest) request;
                    Assert.assertEquals(IngestRequestStep.LOCAL_TO_BE_NOTIFIED, ingestRequest.getStep());
                    Assert.assertEquals(InternalRequestState.RUNNING, ingestRequest.getState());
                }
                notificationService.handleNotificationSuccess(Sets.newHashSet(requests));
                Assert.assertEquals("Ingest requests were not deleted as expected",
                                    0L,
                                    ingestRequestRepository.count());

                break;
            case RequestTypeConstant.OAIS_DELETION_VALUE:
                requests = oaisDeletionRequestRepository.findAll();
                OAISDeletionRequest deletionRequest;
                for (AbstractRequest request : requests) {
                    deletionRequest = (OAISDeletionRequest) request;
                    Assert.assertEquals(DeletionRequestStep.LOCAL_TO_BE_NOTIFIED, deletionRequest.getStep());
                    Assert.assertEquals(InternalRequestState.RUNNING, deletionRequest.getState());
                }
                notificationService.handleNotificationSuccess(Sets.newHashSet(requests));
                Assert.assertEquals("Deletion requests were not deleted as expected",
                                    0L,
                                    oaisDeletionRequestRepository.count());
                break;
            case RequestTypeConstant.UPDATE_VALUE:
                requests = aipUpdateRequestRepository.findAll();
                AIPUpdateRequest updateRequest;
                for (AbstractRequest request : requests) {
                    updateRequest = (AIPUpdateRequest) request;
                    Assert.assertEquals(AIPUpdateRequestStep.LOCAL_TO_BE_NOTIFIED, updateRequest.getStep());
                    Assert.assertEquals(InternalRequestState.RUNNING, updateRequest.getState());
                }
                notificationService.handleNotificationSuccess(Sets.newHashSet(requests));
                Assert.assertEquals("Update requests were not deleted as expected",
                                    0L,
                                    aipUpdateRequestRepository.count());
                break;
            default:
                break;
        }
    }

    public void waitJobDone(JobInfo jobInfo, JobStatus jobStatus, long timeout) {
        Assert.assertNotNull("Job info should not be null", jobInfo);
        this.ingestServiceTest.waitJobDone(jobInfo, jobStatus, timeout);
    }

    public JobInfo waitJobCreated(String jobClassName, long timeout) {
        try {
            Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return !this.jobInfoService.retrieveJobs(jobClassName, PageRequest.of(0, 1)).isEmpty();
            });
        } catch (ConditionTimeoutException e) {
            Assert.fail(String.format("Fail after waiting for new job %s", jobClassName));
        }
        return this.jobInfoService.retrieveJobs(jobClassName, PageRequest.of(0, 1), JobStatus.values())
                                  .getContent()
                                  .get(0);
    }

    public void waitSipCount(long sipCount) {
        waitSipCount(5, sipCount);
    }

    public void waitSipCount(int timeout, long sipCount) {
        Awaitility.await().atMost(timeout, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return ingestRequestRepository.count() == sipCount;
        });
    }
}
