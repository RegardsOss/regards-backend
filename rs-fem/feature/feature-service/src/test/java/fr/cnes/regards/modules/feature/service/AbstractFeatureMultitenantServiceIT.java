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
package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.integration.test.job.JobTestCleaner;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.service.update.AgentSnapshotJobService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.*;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.AbstractRequest;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.request.IFeatureRequestService;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionNotifier;
import fr.cnes.regards.modules.feature.service.settings.IFeatureNotificationSettingsService;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.model.client.IModelClient;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.gson.MultitenantFlattenedAttributeAdapterFactory;
import fr.cnes.regards.modules.model.service.exception.ImportException;
import fr.cnes.regards.modules.model.service.xml.IComputationPluginService;
import fr.cnes.regards.modules.model.service.xml.XmlImportHelper;
import org.assertj.core.util.Lists;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Marc SORDI
 * @author Sébastien Binda
 */
@ContextConfiguration(classes = { JobTestCleaner.class })
public abstract class AbstractFeatureMultitenantServiceIT extends AbstractMultitenantServiceIT {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureMultitenantServiceIT.class);

    protected static final String RESOURCE_PATH = "fr/cnes/regards/modules/feature/service/";

    protected final String sessionStepName = (String) ReflectionTestUtils.getField(FeatureSessionNotifier.class,
                                                                                   "GLOBAL_SESSION_STEP");

    protected String owner = "owner";

    protected String session = "session";

    protected final String stepId = "feature";

    // Mock for test purpose
    @Autowired
    protected IComputationPluginService cps;

    @Autowired
    protected IModelAttrAssocClient modelAttrAssocClientMock;

    @Autowired
    protected IModelClient modelClientMock;

    @Autowired
    protected MultitenantFlattenedAttributeAdapterFactory factory;

    @Autowired
    protected IFeatureCreationRequestRepository featureCreationRequestRepo;

    @Autowired
    protected IFeatureEntityRepository featureRepo;

    @Autowired
    protected IFeatureUpdateRequestRepository featureUpdateRequestRepo;

    @Autowired
    protected IFeatureDeletionRequestRepository featureDeletionRequestRepo;

    @Autowired
    protected IFeatureNotificationRequestRepository notificationRequestRepo;

    @Autowired
    protected IFeatureSaveMetadataRequestRepository featureSaveMetadataRequestRepository;

    @Autowired
    protected IFeatureDisseminationInfoRepository featureDisseminationInfoRepository;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected FeatureConfigurationProperties properties;

    @Autowired
    protected FeatureCreationService featureCreationService;

    @Autowired
    protected FeatureUpdateService featureUpdateService;

    @Autowired
    protected IFeatureDeletionService featureDeletionService;

    @Autowired
    protected ISubscriber subscriber;

    @Autowired
    protected IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepo;

    @Autowired
    private IFeatureUpdateDisseminationRequestRepository featureUpdateDisseminationRequestRepository;

    @Autowired
    protected IFeatureNotificationService featureNotificationService;

    @Autowired
    protected IFeatureNotificationSettingsService featureSettingsNotificationService;

    @Autowired
    protected IFeatureRequestService featureRequestService;

    @Autowired
    protected ISessionStepRepository sessionStepRepository;

    @Autowired
    protected ISnapshotProcessRepository snapshotProcessRepository;

    @Autowired
    protected IStepPropertyUpdateRequestRepository stepPropertyUpdateRequestRepository;

    @Autowired
    protected AgentSnapshotJobService agentSnapshotJobService;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    protected MockStorageResponsesHelper mockStorageHelper;

    @Autowired
    private JobTestCleaner jobTestCleaner;

    protected String geoModelName;

    protected String geoModel100Name;

    protected String featureModelName;

    protected String mutationModelName;

    private List<EntityModel<Model>> models = new ArrayList<>();

    private Map<String, List<EntityModel<ModelAttrAssoc>>> modelResources = new HashMap<>();

    // ------------------------
    // TO CLEAN TESTS
    // ------------------------

    @Before
    public void before() throws Exception {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        cleanRepo();
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();
        setNotificationSetting(true);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Mock models
        geoModel100Name = geoModelName;
        featureModelName = mockModelClient("feature_model_01.xml");
        mutationModelName = mockModelClient("feature_mutation_model.xml");
        geoModelName = mockModelClient("model_geode_V1.0.0.xml");
        doInit();
    }

    protected void doInit() throws Exception {
        // Override to init something
    }

    @After
    public void after() throws Exception {
        setNotificationSetting(true);
        jobTestCleaner.cleanJob();
        doAfter();
    }

    protected void doAfter() throws Exception {
        // Override to init something
    }

    public void cleanRepo() {
        this.featureCreationRequestRepo.deleteAllInBatch();
        this.featureUpdateRequestRepo.deleteAllInBatch();
        this.featureDeletionRequestRepo.deleteAllInBatch();
        this.featureSaveMetadataRequestRepository.deleteAllInBatch();
        this.featureDisseminationInfoRepository.deleteAllInBatch();
        this.featureUpdateDisseminationRequestRepository.deleteAllInBatch();
        this.featureRepo.deleteAll();
        this.notificationRequestRepo.deleteAllInBatch();
        this.jobInfoRepository.deleteAll();
        this.stepPropertyUpdateRequestRepository.deleteAllInBatch();
        this.sessionStepRepository.deleteAllInBatch();
        this.snapshotProcessRepository.deleteAllInBatch();
    }

    // ------------------------
    // WAIT FUNCTIONS
    // ------------------------

    /**
     * Wait until feature are properly created
     *
     * @param expected expected feature number
     * @param from     feature updated after from date. May be <code>null</code>.
     * @param timeout  timeout in milliseconds
     */
    protected void waitFeature(long expected, @Nullable OffsetDateTime from, long timeout) {
        Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            if (from != null) {
                return expected == featureRepo.countByLastUpdateGreaterThan(from);
            } else {
                return expected == featureRepo.count();
            }
        });
    }

    /**
     * Wait until feature creation request(s) are properly deleted
     *
     * @param expected expected request number
     * @param timeout  timeout in milliseconds
     */
    protected void waitCreationRequestDeletion(long expected, long timeout) {
        waitRequest(featureCreationRequestRepo, expected, timeout);
    }

    /**
     * Wait until feature update request(s) are properly deleted
     *
     * @param expected expected request number
     * @param timeout  timeout in milliseconds
     */
    protected void waitUpdateRequestDeletion(long expected, long timeout) {
        waitRequest(featureUpdateRequestRepo, expected, timeout);
    }

    protected void waitRequest(JpaRepository<?, ?> repo, long expected, long timeout) {
        try {
            Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return repo.count() == expected;
            });
        } catch (Throwable e) {
            Assert.fail(String.format("Fails after waiting for %s requests from %s repository. Count=%s",
                                      expected,
                                      repo.getClass().getName(),
                                      repo.count()));
        }
    }

    protected void waitForErrorState(JpaRepository<? extends AbstractRequest, ?> repo) {
        try {
            Awaitility.await().atMost(Durations.ONE_MINUTE).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return repo.findAll().stream().allMatch(request -> RequestState.ERROR.equals(request.getState()));
            });
        } catch (Throwable e) {
            Assert.fail(String.format("Fails after waiting for all requests in error state from %s repository.",
                                      repo.getClass().getSimpleName()));
        }
    }

    protected void waitForStep(JpaRepository<? extends AbstractRequest, ?> repository,
                               FeatureRequestStep step,
                               int count,
                               int timeout) {
        Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return repository.findAll().stream().filter(item -> step.equals(item.getStep())).count() == count;
        });
    }

    protected void waitForSate(JpaRepository<? extends AbstractRequest, ?> repository,
                               RequestState state,
                               int count,
                               int timeout) {
        Awaitility.await().atMost(Durations.ONE_MINUTE).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return repository.findAll().stream().filter(item -> state.equals(item.getState())).count() == count;
        });
    }

    // ------------------------
    // MOCK FUNCTIONS
    // ------------------------

    public String mockModelClient(String filename) {
        return mockModelClient(filename, cps, factory, getDefaultTenant(), modelAttrAssocClientMock);
    }

    /**
     * Mock model client importing model specified by its filename
     *
     * @param filename model filename found using {@link Class#getResourceAsStream(String)}
     * @return mocked model name
     */
    public String mockModelClient(String filename,
                                  IComputationPluginService cps,
                                  MultitenantFlattenedAttributeAdapterFactory factory,
                                  String tenant,
                                  IModelAttrAssocClient modelAttrAssocClientMock) {

        try (InputStream input = new ClassPathResource(RESOURCE_PATH + filename).getInputStream()) {
            // Import model
            Iterable<ModelAttrAssoc> assocs = XmlImportHelper.importModel(input, cps);

            // Translate to resources and attribute models and extract model name
            String modelName = null;
            List<AttributeModel> atts = new ArrayList<>();
            List<EntityModel<ModelAttrAssoc>> resources = new ArrayList<>();
            for (ModelAttrAssoc assoc : assocs) {
                atts.add(assoc.getAttribute());
                resources.add(EntityModel.of(assoc));
                if (modelName == null) {
                    modelName = assoc.getModel().getName();
                }
            }

            // Property factory registration
            LOGGER.info("Registering {} attributes from model {}", atts.size(), modelName);
            factory.registerAttributes(tenant, atts);
            modelResources.put(modelName, resources);

            // Mock client

            Model mockModel = Mockito.mock(Model.class);
            Mockito.when(mockModel.getName()).thenReturn(modelName);
            models.add(EntityModel.of(mockModel));
            Mockito.when(modelClientMock.getModels(null)).thenReturn(ResponseEntity.ok(models));
            Mockito.when(modelAttrAssocClientMock.getModelAttrAssocs(modelName))
                   .thenReturn(ResponseEntity.ok(modelResources.get(modelName)));

            return modelName;
        } catch (IOException | ImportException e) {
            String errorMessage = "Cannot import model";
            LOGGER.debug(errorMessage);
            throw new AssertionError(errorMessage);
        }
    }

    public IModelAttrAssocClient getModelAttrAssocClientMock() {
        return modelAttrAssocClientMock;
    }

    protected void mockNotificationResponseSuccess() {
        Page<AbstractFeatureRequest> requestsToSend = abstractFeatureRequestRepo.findByStepAndRequestDateLessThanEqual(
            FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED,
            OffsetDateTime.now().plusDays(1),
            PageRequest.of(0,
                           properties.getMaxBulkSize(),
                           Sort.by(Sort.Order.asc("priority"), Sort.Order.asc("requestDate"))));
        if (!requestsToSend.isEmpty()) {
            //simulate that notification has been handle with success
            featureNotificationService.handleNotificationSuccess(requestsToSend.toSet());
        }
        for (int i = 1; i < requestsToSend.getTotalPages(); i++) {
            requestsToSend = abstractFeatureRequestRepo.findByStepAndRequestDateLessThanEqual(FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED,
                                                                                              OffsetDateTime.now()
                                                                                                            .plusDays(1),
                                                                                              requestsToSend.nextPageable());
            if (!requestsToSend.isEmpty()) {
                //simulate that notification has been handle with success
                featureNotificationService.handleNotificationSuccess(requestsToSend.toSet());
            }
        }
    }

    protected void mockNotificationSuccess() {
        Page<AbstractFeatureRequest> requestsToSend = abstractFeatureRequestRepo.findByStepAndRequestDateLessThanEqual(
            FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
            OffsetDateTime.now().plusDays(1),
            PageRequest.of(0,
                           properties.getMaxBulkSize(),
                           Sort.by(Sort.Order.asc("priority"), Sort.Order.asc("requestDate"))));
        if (!requestsToSend.isEmpty()) {
            featureNotificationService.sendToNotifier();
            //simulate that notification has been handle with success
            featureNotificationService.handleNotificationSuccess(requestsToSend.toSet());
        }
        for (int i = 1; i < requestsToSend.getTotalPages(); i++) {
            requestsToSend = abstractFeatureRequestRepo.findByStepAndRequestDateLessThanEqual(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                                                                                              OffsetDateTime.now()
                                                                                                            .plusDays(1),
                                                                                              requestsToSend.nextPageable());
            if (!requestsToSend.isEmpty()) {
                featureNotificationService.sendToNotifier();
                //simulate that notification has been handle with success
                featureNotificationService.handleNotificationSuccess(requestsToSend.toSet());
            }
        }
    }

    protected void mockNotificationError() {
        Page<AbstractFeatureRequest> requestsToSend;
        Pageable pageable = PageRequest.of(0,
                                           properties.getMaxBulkSize(),
                                           Sort.by(Sort.Order.asc("priority"), Sort.Order.asc("requestDate")));
        do {
            requestsToSend = abstractFeatureRequestRepo.findByStepAndRequestDateLessThanEqual(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                                                                                              OffsetDateTime.now()
                                                                                                            .plusDays(1),
                                                                                              pageable);
            if (!requestsToSend.isEmpty()) {
                featureNotificationService.sendToNotifier();
                featureNotificationService.handleNotificationError(requestsToSend.toSet(),
                                                                   FeatureRequestStep.REMOTE_NOTIFICATION_ERROR);
            }
            pageable = requestsToSend.nextPageable();
        } while (requestsToSend.hasNext());
    }

    // ------------------------
    // UTILS
    // ------------------------

    /**
     * Create features
     *
     * @param nbFeatures number of features to create
     */
    protected void initData(int nbFeatures) {
        List<FeatureCreationRequestEvent> events = initFeatureCreationRequestEvent(nbFeatures, true, false);
        this.featureCreationService.registerRequests(events);
        this.featureCreationService.scheduleRequests();

        int timeout = nbFeatures * 1000;
        // Timeout should not be less than 5000 ms
        if (timeout < 5000) {
            timeout = 5000;
        }
        waitFeature(nbFeatures, null, timeout);
    }

    public boolean initDefaultNotificationSettings() {
        return featureSettingsNotificationService.isActiveNotification();
    }

    public IComputationPluginService getCps() {
        return cps;
    }

    public MultitenantFlattenedAttributeAdapterFactory getFactory() {
        return factory;
    }

    protected List<FeatureCreationRequestEvent> initFeatureCreationRequestEvent(int featureNumberToCreate,
                                                                                boolean override,
                                                                                boolean updateIfExists) {
        return initFeatureCreationRequestEvent(featureNumberToCreate, override, updateIfExists, owner, session);
    }

    protected List<FeatureCreationRequestEvent> initFeatureCreationRequestEvent(int featureNumberToCreate,
                                                                                boolean override,
                                                                                boolean updateIfExists,
                                                                                String source,
                                                                                String session) {

        List<FeatureCreationRequestEvent> events = new ArrayList<>();
        FeatureCreationRequestEvent toAdd;
        Feature featureToAdd;
        FeatureFile file;

        // create events to publish
        for (int i = 0; i < featureNumberToCreate; i++) {
            file = FeatureFile.build(FeatureFileAttributes.build(DataType.DESCRIPTION,
                                                                 new MimeType("mime"),
                                                                 "toto",
                                                                 1024L,
                                                                 "MD5",
                                                                 "checksum"),
                                     FeatureFileLocation.build("http://www.google.com", "GPFS"));

            featureToAdd = Feature.build("id" + i,
                                         source,
                                         null,
                                         IGeometry.point(IGeometry.position(10.0, 20.0)),
                                         EntityType.DATA,
                                         featureModelName).withFiles(file);
            // data_type is configured to be not alterable. For creation with update if exists do not set this property.
            if (!updateIfExists) {
                featureToAdd.addProperty(IProperty.buildString("data_type", "TYPE01"));
            }
            featureToAdd.addProperty(IProperty.buildObject("file_characterization",
                                                           IProperty.buildBoolean("valid", Boolean.TRUE)));

            toAdd = FeatureCreationRequestEvent.build(source,
                                                      FeatureCreationSessionMetadata.build(source,
                                                                                           session,
                                                                                           PriorityLevel.NORMAL,
                                                                                           Lists.emptyList(),
                                                                                           override,
                                                                                           updateIfExists),
                                                      featureToAdd);
            toAdd.setRequestId(UUID.randomUUID().toString());
            toAdd.setFeature(featureToAdd);
            toAdd.setRequestDate(OffsetDateTime.now().minusDays(1));

            events.add(toAdd);
        }
        return events;
    }

    protected List<FeatureCreationRequestEvent> prepareCreationTestData(boolean prepareFeatureWithFiles,
                                                                        int featureToCreateNumber,
                                                                        boolean isToNotify,
                                                                        boolean override,
                                                                        boolean updateIfExists)
        throws InterruptedException {
        return prepareCreationTestData(prepareFeatureWithFiles,
                                       featureToCreateNumber,
                                       isToNotify,
                                       override,
                                       updateIfExists,
                                       owner,
                                       session);
    }

    protected List<FeatureCreationRequestEvent> prepareCreationTestData(boolean prepareFeatureWithFiles,
                                                                        int featureToCreateNumber,
                                                                        boolean isToNotify,
                                                                        boolean override,
                                                                        boolean updateIfExists,
                                                                        String source,
                                                                        String session) throws InterruptedException {

        List<FeatureCreationRequestEvent> events = initFeatureCreationRequestEvent(featureToCreateNumber,
                                                                                   override,
                                                                                   updateIfExists,
                                                                                   source,
                                                                                   session);

        if (!prepareFeatureWithFiles) {
            // remove files inside features
            events.forEach(event -> event.getFeature().setFiles(null));
        }

        this.featureCreationService.registerRequests(events);
        assertEquals(featureToCreateNumber, this.featureCreationRequestRepo.count());

        this.featureCreationService.scheduleRequests();
        // if they are several page to create
        for (long i = 0; i < (featureToCreateNumber / properties.getMaxBulkSize()); i++) {
            this.featureCreationService.scheduleRequests();
        }

        Awaitility.await().atMost(100, TimeUnit.SECONDS).pollInterval(300, TimeUnit.MILLISECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            long createdFeatures = featureRepo.findBySessionOwnerAndSession(source, session, Pageable.unpaged())
                                              .getTotalElements();
            LOGGER.info("{} / {} created features", createdFeatures, featureToCreateNumber);
            return createdFeatures == featureToCreateNumber;
        });

        if (prepareFeatureWithFiles) {
            mockStorageHelper.mockFeatureCreationStorageSuccess();
        }

        if (isToNotify) {
            mockNotificationSuccess();
            // if they are several page to create
            for (int i = 0; i < (featureToCreateNumber / properties.getMaxBulkSize()); i++) {
                mockNotificationSuccess();
            }
            Assert.assertEquals("Not all requests to be notified were deleted", 0L, featureCreationRequestRepo.count());
        }

        return events;
    }

    protected List<FeatureDeletionRequestEvent> prepareDeletionTestData(String deletionOwner,
                                                                        boolean prepareFeatureWithFiles,
                                                                        Integer featureToCreateNumber,
                                                                        boolean isToNotify)
        throws InterruptedException {

        // create features
        prepareCreationTestData(prepareFeatureWithFiles, featureToCreateNumber, isToNotify, true, false);

        // get urn from feature we just created
        List<FeatureUniformResourceName> entityCreatedUrn = this.featureRepo.findAll()
                                                                            .stream()
                                                                            .map(FeatureEntity::getUrn)
                                                                            .collect(Collectors.toList());

        // preparation of the FeatureDeletionRequestEvent
        List<FeatureDeletionRequestEvent> deletionEvents = entityCreatedUrn.stream()
                                                                           .map(urn -> FeatureDeletionRequestEvent.build(
                                                                               deletionOwner,
                                                                               urn,
                                                                               PriorityLevel.NORMAL))
                                                                           .collect(Collectors.toList());

        // if we have more than a page of request can handle we will upgrade the priority level of the request out of the page
        if (deletionEvents.size() > properties.getMaxBulkSize()) {
            // we will set all priority to low for the (featureToCreateNumber / 2) last event
            for (int i = properties.getMaxBulkSize(); i < featureToCreateNumber; i++) {
                deletionEvents.get(i).setPriority(PriorityLevel.HIGH);
            }
        }

        return deletionEvents;
    }

    protected List<FeatureNotificationRequestEvent> prepareNotificationRequests(Collection<FeatureUniformResourceName> urns) {
        List<FeatureNotificationRequestEvent> events = Lists.newArrayList();
        urns.forEach(u -> events.add(FeatureNotificationRequestEvent.build("notifier",
                                                                           u,
                                                                           PriorityLevel.NORMAL,
                                                                           new HashSet<String>())));
        return events;
    }

    protected List<FeatureUpdateRequestEvent> prepareUpdateRequests(List<FeatureUniformResourceName> urns) {
        return featureRepo.findCompleteByUrnIn(urns)
                          .stream()
                          .map(f -> FeatureUpdateRequestEvent.build("test",
                                                                    FeatureMetadata.build(PriorityLevel.NORMAL),
                                                                    f.getFeature()))
                          .map(e -> {
                              e.getFeature().getProperties().clear();
                              return e;
                          })
                          .collect(Collectors.toList());
    }

    protected SessionStep getSessionStep() {
        return getSessionStep(owner, session);
    }

    protected SessionStep getSessionStep(String source, String session) {
        Awaitility.await().atMost(Durations.FIVE_SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return sessionStepRepository.findBySourceAndSessionAndStepId(source, session, sessionStepName).isPresent();
        });
        Optional<SessionStep> sessionStepOptional = sessionStepRepository.findBySourceAndSessionAndStepId(source,
                                                                                                          session,
                                                                                                          sessionStepName);
        Assertions.assertTrue(sessionStepOptional.isPresent());
        return sessionStepOptional.get();
    }

    protected void checkRequests(int expected,
                                 Predicate<StepPropertyUpdateRequest> predicate,
                                 List<StepPropertyUpdateRequest> requestList) {
        Assertions.assertEquals(expected, requestList.stream().filter(predicate).count());
    }

    protected Predicate<StepPropertyUpdateRequest> property(String property) {
        return request -> request.getStepPropertyInfo().getProperty().equals(property);
    }

    protected Predicate<StepPropertyUpdateRequest> type(StepPropertyEventTypeEnum type) {
        return request -> request.getType().equals(type);
    }

    protected Predicate<StepPropertyUpdateRequest> inputRelated() {
        return request -> request.getStepPropertyInfo().isInputRelated();
    }

    protected Predicate<StepPropertyUpdateRequest> outputRelated() {
        return request -> request.getStepPropertyInfo().isOutputRelated();
    }

    protected void checkKey(int expected, String key, SessionStepProperties sessionStepProperties) {
        Assertions.assertTrue(sessionStepProperties.containsKey(key));
        Assertions.assertEquals(expected, Integer.valueOf(sessionStepProperties.get(key)));
    }

    protected void setNotificationSetting(boolean value) throws EntityException {
        featureSettingsNotificationService.setActiveNotification(value);
    }

    protected void computeSessionStep(int nbStepsRequired, int nbSnapshotProcessesRequired)
        throws InterruptedException {
        computeSessionStep(nbStepsRequired, nbSnapshotProcessesRequired, owner, session);
    }

    private boolean expectSteps(int nbStepsRequired) {
        this.runtimeTenantResolver.forceTenant(getDefaultTenant());
        return stepPropertyUpdateRequestRepository.findAll().size() == nbStepsRequired;
    }

    protected void computeSessionStep(int nbStepsRequired,
                                      int nbSnapshotProcessesRequired,
                                      String source,
                                      String session) throws InterruptedException {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(120);
        if (nbStepsRequired > 0) {
            try {
                Awaitility.await().atMost(Durations.ONE_MINUTE).until(() -> this.expectSteps(nbStepsRequired));
            } catch (ConditionTimeoutException e) {
                Assert.assertEquals(nbStepsRequired, stepPropertyUpdateRequestRepository.findAll().size());
            }
            try {
                Awaitility.await()
                          .atMost(Durations.ONE_MINUTE)
                          .until(() -> this.expectSnapshotProcess(nbSnapshotProcessesRequired));
            } catch (ConditionTimeoutException e) {
                Assert.assertEquals(nbSnapshotProcessesRequired, snapshotProcessRepository.findAll().size());
            }
        }
        agentSnapshotJobService.scheduleJob();

        // Check that sessionStep is correctly generated
        try {
            Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return sessionStepRepository.findBySourceAndSessionAndStepId(source, session, stepId)
                                            .filter(sessionStep -> sessionStep.getLastUpdateDate()
                                                                              .isBefore(end.truncatedTo(ChronoUnit.MICROS)))
                                            .map(sessionStep -> {
                                                LOGGER.info("""
                                                                1 session step found before {} from source "{}", session "{}", stepId "{}".
                                                                Searching if all corresponding step properties were processed.""",
                                                            end,
                                                            source,
                                                            session,
                                                            stepId);
                                                return findAllStepPropertiesBySourceSessionStepIdCreationDate(source,
                                                                                                              session,
                                                                                                              sessionStep.getLastUpdateDate(),
                                                                                                              start.truncatedTo(
                                                                                                                  ChronoUnit.MICROS)).size()
                                                       == 0;
                                            })
                                            .orElse(false);
            });
        } catch (ConditionTimeoutException e) {
            Assert.fail("The step properties were not processed in the expected amount of time");
        }

    }

    private boolean expectSnapshotProcess(int nbSnapshotProcessesRequired) {
        this.runtimeTenantResolver.forceTenant(getDefaultTenant());
        return snapshotProcessRepository.findAll().size() == nbSnapshotProcessesRequired;
    }

    private List<StepPropertyUpdateRequest> findAllStepPropertiesBySourceSessionStepIdCreationDate(String source,
                                                                                                   String session,
                                                                                                   OffsetDateTime creationDateBegin,
                                                                                                   OffsetDateTime creationDateEnd) {
        List<StepPropertyUpdateRequest> stepProperties = stepPropertyUpdateRequestRepository.findAll()
                                                                                            .stream()
                                                                                            .filter(stepProperty ->
                                                                                                        stepProperty.getSource()
                                                                                                                    .equals(
                                                                                                                        source)
                                                                                                        && stepProperty.getSession()
                                                                                                                       .equals(
                                                                                                                           session)
                                                                                                        && stepProperty.getStepId()
                                                                                                                       .equals(
                                                                                                                           stepId)
                                                                                                        && stepProperty.getCreationDate()
                                                                                                                       .isAfter(
                                                                                                                           creationDateBegin)
                                                                                                        && stepProperty.getCreationDate()
                                                                                                                       .isBefore(
                                                                                                                           creationDateEnd))
                                                                                            .toList();
        LOGGER.info("""
                        Step properties found for source "{}", session "{}", stepId "{}", creationDate between "{}" and "{}" :
                        {}
                        """, source, session, stepId, creationDateBegin, creationDateEnd, stepProperties);
        return stepProperties;

    }
}
