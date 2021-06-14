package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.service.update.AgentSnapshotJobService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.*;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.AbstractRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.flow.FeatureCreationRequestEventHandler;
import fr.cnes.regards.modules.feature.service.flow.FeatureDeletionRequestEventHandler;
import fr.cnes.regards.modules.feature.service.flow.FeatureUpdateRequestEventHandler;
import fr.cnes.regards.modules.feature.service.flow.NotificationRequestEventHandler;
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
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import fr.cnes.regards.modules.storage.client.FileRequestGroupEventHandler;
import fr.cnes.regards.modules.storage.domain.event.FileRequestsGroupEvent;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fr.cnes.regards.framework.amqp.event.Target.ONE_PER_MICROSERVICE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class AbstractFeatureMultitenantServiceTest extends AbstractMultitenantServiceTest {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureMultitenantServiceTest.class);

    private static final String RESOURCE_PATH = "fr/cnes/regards/modules/feature/service/";

    protected final String sessionStepName = (String) ReflectionTestUtils.getField(FeatureSessionNotifier.class, "GLOBAL_SESSION_STEP");

    protected String owner = "owner";
    protected String session = "session";

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
    protected IRuntimeTenantResolver runtimeTenantResolver;
    @Autowired
    protected FeatureConfigurationProperties properties;
    @Autowired
    protected FeatureCreationService featureCreationService;
    @Autowired
    protected IFeatureDeletionService featureDeletionService;
    @Autowired
    protected ISubscriber subscriber;
    @Autowired
    protected IPublisher publisher;
    @Autowired
    protected IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepo;
    @Autowired
    protected IFeatureNotificationService featureNotificationService;
    @Autowired
    protected IFeatureNotificationSettingsService featureSettingsNotificationService;
    @Autowired
    protected IFeatureRequestService featureRequestService;
    @Autowired
    protected ISessionStepRepository sessionStepRepository;
    @Autowired
    protected IStepPropertyUpdateRequestRepository stepPropertyUpdateRequestRepository;
    @Autowired
    protected AgentSnapshotJobService agentSnapshotJobService;

    @Autowired
    private IJobInfoRepository jobInfoRepository;
    @Autowired(required = false)
    private IAmqpAdmin amqpAdmin;
    @Autowired(required = false)
    private IRabbitVirtualHostAdmin vhostAdmin;

    // ------------------------
    // TO CLEAN TESTS
    // ------------------------

    @Before
    public void before() throws Exception {
        cleanRepo();
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();
        setNotificationSetting(true);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        doInit();
    }

    protected void doInit() throws Exception {
        // Override to init something
    }

    @After
    public void after() throws Exception {
        cleanQueues();
        setNotificationSetting(true);
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
        this.featureRepo.deleteAllInBatch();
        this.notificationRequestRepo.deleteAllInBatch();
        this.jobInfoRepository.deleteAll();
        stepPropertyUpdateRequestRepository.deleteAll();
        sessionStepRepository.deleteAll();
    }

    public void cleanQueues() {
        subscriber.unsubscribeFrom(FeatureCreationRequestEvent.class);
        subscriber.unsubscribeFrom(FeatureDeletionRequestEvent.class);
        subscriber.unsubscribeFrom(FeatureUpdateRequestEvent.class);
        subscriber.unsubscribeFrom(FeatureNotificationRequestEvent.class);
        subscriber.unsubscribeFrom(NotificationRequestEvent.class);
        subscriber.unsubscribeFrom(FileRequestsGroupEvent.class);
        cleanAMQPQueues(FeatureCreationRequestEventHandler.class, ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(FeatureUpdateRequestEventHandler.class, ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(FeatureDeletionRequestEventHandler.class, ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(NotificationRequestEventHandler.class, ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(FileRequestGroupEventHandler.class, ONE_PER_MICROSERVICE_TYPE);
    }

    // ------------------------
    // WAIT FUNCTIONS
    // ------------------------

    /**
     * Wait until feature are properly created
     * @param expected expected feature number
     * @param from feature updated after from date. May be <code>null</code>.
     * @param timeout timeout in milliseconds
     */
    protected void waitFeature(long expected, @Nullable OffsetDateTime from, long timeout) {
        long end = System.currentTimeMillis() + timeout;
        // Wait
        long entityCount;
        do {
            if (from != null) {
                entityCount = featureRepo.countByLastUpdateGreaterThan(from);
            } else {
                entityCount = featureRepo.count();
            }
            LOGGER.trace("{} feature(s) created in database", entityCount);
            if (entityCount == expected) {
                break;
            }
            long now = System.currentTimeMillis();
            if (end > now) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    Assert.fail("Thread interrupted");
                }
            } else {
                Assert.fail("Timeout only " + entityCount + " feature in database instead of " + expected);
            }
        } while (true);
    }

    /**
     * Wait until feature creation request(s) are properly deleted
     * @param expected expected request number
     * @param timeout timeout in milliseconds
     */
    protected void waitCreationRequestDeletion(long expected, long timeout) {
        waitRequest(featureCreationRequestRepo, expected, timeout);
    }

    /**
     * Wait until feature update request(s) are properly deleted
     * @param expected expected request number
     * @param timeout timeout in milliseconds
     */
    protected void waitUpdateRequestDeletion(long expected, long timeout) {
        waitRequest(featureUpdateRequestRepo, expected, timeout);
    }

    protected void waitRequest(JpaRepository<?, ?> repo, long expected, long timeout) {
        long end = System.currentTimeMillis() + timeout;
        // Wait
        long entityCount;
        do {
            entityCount = repo.count();
            LOGGER.trace("{} request(s) remain(s) in database", entityCount);
            if (entityCount == expected) {
                break;
            }
            long now = System.currentTimeMillis();
            if (end > now) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.error(String.format("Thread interrupted %s expected in database, %s really ", expected,
                                               entityCount));
                    Assert.fail(String.format("Thread interrupted {} expected in database, {} really ", expected,
                                              entityCount));

                }
            } else {
                LOGGER.error(String.format("Thread interrupted %s expected in database, %s really ", expected,
                                           entityCount));
                Assert.fail("Timeout");
            }
        } while (true);
    }

    protected void waitForErrorState(JpaRepository<? extends AbstractRequest, ?> repo) throws InterruptedException {

        int cpt = 0;
        // we will expect that all feature reference remain in database with the error state
        do {
            TimeUnit.MILLISECONDS.sleep(1000);
            if (cpt == 60) {
                fail("Timeout");
            }
            cpt++;
        } while (!repo.findAll().stream().allMatch(request -> RequestState.ERROR.equals(request.getState())));
    }

    protected void waitForStep(JpaRepository<? extends AbstractRequest, ?> repository, FeatureRequestStep step, int count, int timeout) throws InterruptedException {
        Instant start = Instant.now();
        while (repository.findAll().stream().filter(item -> step.equals(item.getStep())).count() != count) {
            TimeUnit.SECONDS.sleep(1);
            if (Instant.now().isAfter(start.plusSeconds(timeout))) {
                fail("Timeout reached waiting for FeatureRequest : " + step);
            }
        }
    }

    protected void waitForSate(JpaRepository<? extends AbstractRequest, ?> repository, RequestState state, int count, int timeout) throws InterruptedException {
        Instant start = Instant.now();
        while (repository.findAll().stream().filter(item -> state.equals(item.getState())).count() != count) {
            TimeUnit.SECONDS.sleep(1);
            if (Instant.now().isAfter(start.plusSeconds(timeout))) {
                fail("Timeout reached waiting for FeatureState  : " + state);
            }
        }
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
    public String mockModelClient(String filename, IComputationPluginService cps, MultitenantFlattenedAttributeAdapterFactory factory, String tenant,
                                  IModelAttrAssocClient modelAttrAssocClientMock
    ) {

        try (InputStream input = new ClassPathResource(RESOURCE_PATH + filename).getInputStream()) {
            // Import model
            Iterable<ModelAttrAssoc> assocs = XmlImportHelper.importModel(input, cps);

            // Translate to resources and attribute models and extract model name
            String modelName = null;
            List<AttributeModel> atts = new ArrayList<>();
            List<EntityModel<ModelAttrAssoc>> resources = new ArrayList<>();
            for (ModelAttrAssoc assoc : assocs) {
                atts.add(assoc.getAttribute());
                resources.add(new EntityModel<>(assoc));
                if (modelName == null) {
                    modelName = assoc.getModel().getName();
                }
            }

            // Property factory registration
            factory.registerAttributes(tenant, atts);

            // Mock client
            List<EntityModel<Model>> models = new ArrayList<>();
            Model mockModel = Mockito.mock(Model.class);
            Mockito.when(mockModel.getName()).thenReturn(modelName);
            models.add(new EntityModel<Model>(mockModel));
            Mockito.when(modelClientMock.getModels(null)).thenReturn(ResponseEntity.ok(models));
            Mockito.when(modelAttrAssocClientMock.getModelAttrAssocs(modelName)).thenReturn(ResponseEntity.ok(resources));

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

    protected void mockFeatureCreationStorageSuccess() {
        // mock rs-storage response success for file storage
        Pageable pageToRequest = PageRequest.of(0, properties.getMaxBulkSize());
        Page<FeatureCreationRequest> fcrPage;
        do {
            // find first page of requests to handle
            fcrPage = featureCreationRequestRepo.findByStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED, pageToRequest);
            // simulate storage response
            featureRequestService.handleStorageSuccess(fcrPage.stream().map(AbstractFeatureRequest::getGroupId).collect(Collectors.toSet()));
            // get next page of requests if present
            if (fcrPage.hasNext()) {
                fcrPage.nextPageable();
            }
        } while (fcrPage.hasNext());
    }

    protected void mockNotificationSuccess() {
        Page<AbstractFeatureRequest> requestsToSend = abstractFeatureRequestRepo
                .findByStepAndRequestDateLessThanEqual(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                                                       OffsetDateTime.now().plusDays(1),
                                                       PageRequest.of(0, properties.getMaxBulkSize(),
                                                                      Sort.by(Sort.Order.asc("priority"),
                                                                              Sort.Order.asc("requestDate"))));
        if (!requestsToSend.isEmpty()) {
            featureNotificationService.sendToNotifier();
            //simulate that notification has been handle with success
            featureNotificationService.handleNotificationSuccess(requestsToSend.toSet());
        }
        for (int i = 1; i < requestsToSend.getTotalPages(); i++) {
            requestsToSend = abstractFeatureRequestRepo
                    .findByStepAndRequestDateLessThanEqual(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                                                           OffsetDateTime.now().plusDays(1),
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
        Pageable pageable = PageRequest.of(0, properties.getMaxBulkSize(), Sort.by(Sort.Order.asc("priority"), Sort.Order.asc("requestDate")));
        do {
            requestsToSend = abstractFeatureRequestRepo.findByStepAndRequestDateLessThanEqual(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED, OffsetDateTime.now().plusDays(1), pageable);
            if (!requestsToSend.isEmpty()) {
                featureNotificationService.sendToNotifier();
                featureNotificationService.handleNotificationError(requestsToSend.toSet(), FeatureRequestStep.REMOTE_NOTIFICATION_ERROR);
            }
            pageable = requestsToSend.nextPageable();
        } while (requestsToSend.hasNext());
    }

    // ------------------------
    // UTILS
    // ------------------------

    /**
     * Internal method to clean AMQP queues, if actives
     */
    public void cleanAMQPQueues(Class<? extends IHandler<?>> handler, Target target) {
        if (vhostAdmin != null) {
            // Re-set tenant because above simulation clear it!

            // Purge event queue
            try {
                vhostAdmin.bind(AmqpConstants.AMQP_MULTITENANT_MANAGER);
                amqpAdmin.purgeQueue(amqpAdmin.getSubscriptionQueueName(handler, target), false);
            } catch (AmqpIOException e) {
                //todo
            } finally {
                vhostAdmin.unbind();
            }
        }
    }

    /**
     * Create features
     * @param nbFeatures number of features to create
     */
    protected void initData(int nbFeatures) {
        List<FeatureCreationRequestEvent> events = initFeatureCreationRequestEvent(nbFeatures, true);
        this.featureCreationService.registerRequests(events);
        this.featureCreationService.scheduleRequests();
        waitFeature(nbFeatures, null, nbFeatures * 1000);
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

    protected List<FeatureCreationRequestEvent> initFeatureCreationRequestEvent(int featureNumberToCreate, boolean override) {

        List<FeatureCreationRequestEvent> events = new ArrayList<>();
        FeatureCreationRequestEvent toAdd;
        Feature featureToAdd;
        FeatureFile file;
        String model = mockModelClient("feature_model_01.xml", cps, factory, this.getDefaultTenant(), modelAttrAssocClientMock);

        try {
            TimeUnit.MILLISECONDS.sleep(5000);
        } catch (InterruptedException e) {
            // Skip
        }

        // create events to publish
        for (int i = 0; i < featureNumberToCreate; i++) {
            file = FeatureFile.build(
                    FeatureFileAttributes.build(DataType.DESCRIPTION, new MimeType("mime"), "toto", 1024L, "MD5", "checksum"),
                    FeatureFileLocation.build("www.google.com", "GPFS"));

            featureToAdd = Feature.build("id" + i, owner, null, IGeometry.point(IGeometry.position(10.0, 20.0)), EntityType.DATA, model).withFiles(file);
            featureToAdd.addProperty(IProperty.buildString("data_type", "TYPE01"));
            featureToAdd.addProperty(IProperty.buildObject("file_characterization", IProperty.buildBoolean("valid", Boolean.TRUE)));

            toAdd = FeatureCreationRequestEvent.build(owner,
                                                      FeatureCreationSessionMetadata.build(owner, session, PriorityLevel.NORMAL, Lists.emptyList(), override),
                                                      featureToAdd);
            toAdd.setRequestId(String.valueOf(i));
            toAdd.setFeature(featureToAdd);
            toAdd.setRequestDate(OffsetDateTime.now().minusDays(1));

            events.add(toAdd);
        }
        return events;
    }

    protected List<FeatureCreationRequestEvent> prepareCreationTestData(boolean prepareFeatureWithFiles, int featureToCreateNumber, boolean isToNotify, boolean override)
            throws InterruptedException {

        List<FeatureCreationRequestEvent> events = initFeatureCreationRequestEvent(featureToCreateNumber, override);

        if (!prepareFeatureWithFiles) {
            // remove files inside features
            events.stream().forEach(event -> event.getFeature().setFiles(null));
        }

        this.featureCreationService.registerRequests(events);
        assertEquals(featureToCreateNumber, this.featureCreationRequestRepo.count());

        this.featureCreationService.scheduleRequests();
        // if they are several page to create
        for (int i = 0; i < (featureToCreateNumber % properties.getMaxBulkSize()); i++) {
            this.featureCreationService.scheduleRequests();
        }

        int cpt = 0;
        long featureNumberInDatabase;
        do {
            featureNumberInDatabase = this.featureRepo.count();
            TimeUnit.MILLISECONDS.sleep(1000);
            cpt++;
        } while ((cpt < 100) && (featureNumberInDatabase != featureToCreateNumber));

        assertEquals(featureToCreateNumber, this.featureRepo.count());

        // in that case all features hasn't been saved
        if (cpt == 100) {
            fail("Doesn't have all features at the end of time");
        }

        if (prepareFeatureWithFiles) {
            mockFeatureCreationStorageSuccess();
        }

        if (isToNotify) {
            mockNotificationSuccess();
            // if they are several page to create
            for (int i = 0; i < (featureToCreateNumber % properties.getMaxBulkSize()); i++) {
                mockNotificationSuccess();
            }
            Assert.assertEquals("Not all requests to be notified were deleted", 0L, featureCreationRequestRepo.count());
        }

        return events;
    }

    protected List<FeatureDeletionRequestEvent> prepareDeletionTestData(String deletionOwner, boolean prepareFeatureWithFiles, Integer featureToCreateNumber, boolean isToNotify)
            throws InterruptedException {

        // create features
        prepareCreationTestData(prepareFeatureWithFiles, featureToCreateNumber, isToNotify, true);

        // get urn from feature we just created
        List<FeatureUniformResourceName> entityCreatedUrn = this.featureRepo.findAll().stream().map(FeatureEntity::getUrn).collect(Collectors.toList());

        // preparation of the FeatureDeletionRequestEvent
        List<FeatureDeletionRequestEvent> deletionEvents = entityCreatedUrn.stream()
                .map(urn -> FeatureDeletionRequestEvent.build(deletionOwner, urn, PriorityLevel.NORMAL))
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
        urns.forEach(u -> events.add(FeatureNotificationRequestEvent.build("notifier", u, PriorityLevel.NORMAL)));
        return events;
    }

    protected List<FeatureUpdateRequestEvent> prepareUpdateRequests(List<FeatureUniformResourceName> urns) {
        return featureRepo.findByUrnIn(urns).stream().map(f -> FeatureUpdateRequestEvent
                .build("test", FeatureMetadata.build(PriorityLevel.NORMAL), f.getFeature())).map(e -> {
                    e.getFeature().getProperties().clear();
                    return e;
                }).collect(Collectors.toList());
    }

    protected SessionStep getSessionStep() {
        Optional<SessionStep> sessionStepOptional = sessionStepRepository.findBySourceAndSessionAndStepId(owner, session, sessionStepName);
        Assertions.assertTrue(sessionStepOptional.isPresent());
        return sessionStepOptional.get();
    }

    protected void checkRequests(int expected, Predicate<StepPropertyUpdateRequest> predicate, List<StepPropertyUpdateRequest> requestList
    ) {
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

    protected void computeSessionStep() throws InterruptedException {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        SessionStep sessionStep;
        boolean done = false;
        do {
            agentSnapshotJobService.scheduleJob();
            TimeUnit.SECONDS.sleep(1);
            sessionStep = sessionStepRepository.findAll().stream().findFirst().orElse(null);
            if (sessionStep != null &&
                    stepPropertyUpdateRequestRepository.countBySourceAndDateGreaterThanAndDateLessThanEqual(owner, sessionStep.getLastUpdateDate(), start) == 0) {
                done = true;
            }

        } while (!done && OffsetDateTime.now(ZoneOffset.UTC).isBefore(start.plusSeconds(60)));
    }

}
