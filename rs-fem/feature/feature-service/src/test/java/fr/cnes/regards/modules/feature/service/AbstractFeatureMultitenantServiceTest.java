package fr.cnes.regards.modules.feature.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureDeletionRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureUpdateRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureNotificationRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.AbstractRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.domain.settings.FeatureNotificationSettings;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.FeatureFile;
import fr.cnes.regards.modules.feature.dto.FeatureFileAttributes;
import fr.cnes.regards.modules.feature.dto.FeatureFileLocation;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.flow.FeatureCreationRequestEventHandler;
import fr.cnes.regards.modules.feature.service.flow.FeatureDeletionRequestEventHandler;
import fr.cnes.regards.modules.feature.service.flow.FeatureUpdateRequestEventHandler;
import fr.cnes.regards.modules.feature.service.flow.NotificationRequestEventHandler;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class AbstractFeatureMultitenantServiceTest extends AbstractMultitenantServiceTest {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureMultitenantServiceTest.class);

    private static final String RESOURCE_PATH = "fr/cnes/regards/modules/feature/service/";

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

    @Autowired(required = false)
    private IAmqpAdmin amqpAdmin;

    @Autowired(required = false)
    private IRabbitVirtualHostAdmin vhostAdmin;

    @Before
    public void before() throws Exception {
        this.featureCreationRequestRepo.deleteAllInBatch();
        this.featureUpdateRequestRepo.deleteAllInBatch();
        this.featureDeletionRequestRepo.deleteAllInBatch();
        this.featureRepo.deleteAllInBatch();
        this.notificationRequestRepo.deleteAllInBatch();
        simulateApplicationReadyEvent();
        doInit();
    }

    /**
     * Custom test initialization to override
     * @throws Exception
     */
    protected void doInit() throws Exception {
        // Override to init something
    }


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
                    Thread.sleep(1000);
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
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.error(String.format("Thread interrupted %s expected in database, %s really ",
                                               expected,
                                               entityCount));
                    Assert.fail(String.format("Thread interrupted {} expected in database, {} really ",
                                              expected,
                                              entityCount));

                }
            } else {
                LOGGER.error(String.format("Thread interrupted %s expected in database, %s really ",
                                           expected,
                                           entityCount));
                Assert.fail("Timeout");
            }
        } while (true);
    }

    public String mockModelClient(String filename) {
        return mockModelClient(filename, cps, factory, getDefaultTenant(), modelAttrAssocClientMock);
    }

    /**
     * Mock model client importing model specified by its filename
     * @param filename model filename found using {@link Class#getResourceAsStream(String)}
     * @return mocked model name
     */
    public String mockModelClient(String filename, IComputationPluginService cps,
            MultitenantFlattenedAttributeAdapterFactory factory, String tenant,
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
                resources.add(new EntityModel<ModelAttrAssoc>(assoc));
                if (modelName == null) {
                    modelName = assoc.getModel().getName();
                }
            }

            // Property factory registration
            factory.registerAttributes(tenant, atts);

            // Mock client
            List<EntityModel<Model>> models = new ArrayList<EntityModel<Model>>();
            Model mockModel = Mockito.mock(Model.class);
            Mockito.when(mockModel.getName()).thenReturn(modelName);
            models.add(new EntityModel<Model>(mockModel));
            Mockito.when(modelClientMock.getModels(null)).thenReturn(ResponseEntity.ok(models));
            Mockito.when(modelAttrAssocClientMock.getModelAttrAssocs(modelName))
                    .thenReturn(ResponseEntity.ok(resources));

            return modelName;
        } catch (IOException | ImportException e) {
            String errorMessage = "Cannot import model";
            LOGGER.debug(errorMessage);
            throw new AssertionError(errorMessage);
        }
    }

    protected List<FeatureCreationRequestEvent> initFeatureCreationRequestEvent(int featureNumberToCreate,
            boolean override) {
        List<FeatureCreationRequestEvent> events = new ArrayList<>();
        FeatureCreationRequestEvent toAdd;
        Feature featureToAdd;
        FeatureFile file;
        String model = mockModelClient("feature_model_01.xml",
                                       cps,
                                       factory,
                                       this.getDefaultTenant(),
                                       modelAttrAssocClientMock);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // Skip
        }

        // create events to publish
        for (int i = 0; i < featureNumberToCreate; i++) {
            file = FeatureFile.build(FeatureFileAttributes.build(DataType.DESCRIPTION,
                                                                 new MimeType("mime"),
                                                                 "toto",
                                                                 1024l,
                                                                 "MD5",
                                                                 "checksum"),
                                     FeatureFileLocation.build("www.google.com", "GPFS"));
            featureToAdd = Feature.build("id" + i,
                                         "owner",
                                         null,
                                         IGeometry.point(IGeometry.position(10.0, 20.0)),
                                         EntityType.DATA,
                                         model).withFiles(file);
            featureToAdd.addProperty(IProperty.buildString("data_type", "TYPE01"));
            featureToAdd.addProperty(IProperty.buildObject("file_characterization",
                                                           IProperty.buildBoolean("valid", Boolean.TRUE)));

            toAdd = FeatureCreationRequestEvent.build("owner",
                                                      FeatureCreationSessionMetadata.build("owner",
                                                                                           "session",
                                                                                           PriorityLevel.NORMAL,
                                                                                           Lists.emptyList(),
                                                                                           override),
                                                      featureToAdd);
            toAdd.setRequestId(String.valueOf(i));
            toAdd.setFeature(featureToAdd);
            toAdd.setRequestDate(OffsetDateTime.now().minusDays(1));

            events.add(toAdd);
        }
        return events;
    }

    public IComputationPluginService getCps() {
        return cps;
    }

    public IModelAttrAssocClient getModelAttrAssocClientMock() {
        return modelAttrAssocClientMock;
    }

    public MultitenantFlattenedAttributeAdapterFactory getFactory() {
        return factory;
    }

    protected List<FeatureDeletionRequestEvent> prepareDeletionTestData(String deletionOwner,
            boolean prepareFeatureWithFiles, Integer featureToCreateNumber) throws InterruptedException {
        List<FeatureCreationRequestEvent> events = initFeatureCreationRequestEvent(featureToCreateNumber, true);

        if (!prepareFeatureWithFiles) {
            // remove files inside features
            events.stream().forEach(event -> event.getFeature().setFiles(null));
        }

        this.featureCreationService.registerRequests(events);

        assertEquals(featureToCreateNumber.intValue(), this.featureCreationRequestRepo.count());

        this.featureCreationService.scheduleRequests();
        // if they are several page to create
        for (int i = 0; i < (featureToCreateNumber % properties.getMaxBulkSize()); i++) {
            this.featureCreationService.scheduleRequests();
        }

        int cpt = 0;
        long featureNumberInDatabase;
        do {
            featureNumberInDatabase = this.featureRepo.count();
            Thread.sleep(1000);
            cpt++;
        } while ((cpt < 100) && (featureNumberInDatabase != featureToCreateNumber));

        assertEquals(featureToCreateNumber.intValue(), this.featureRepo.count());

        // in that case all features hasn't been saved
        if (cpt == 100) {
            fail("Doesn't have all features at the end of time");
        }

        mockNotificationSuccess();
        // if they are several page to create
        for (int i = 0; i < (featureToCreateNumber % properties.getMaxBulkSize()); i++) {
            mockNotificationSuccess();
        }

        // get urn from feature we just created
        List<FeatureUniformResourceName> entityCreatedUrn = this.featureRepo.findAll().stream()
                .map(feature -> feature.getUrn()).collect(Collectors.toList());

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

    @After
    public void after() throws Exception {
        subscriber.unsubscribeFrom(FeatureCreationRequestEvent.class);
        subscriber.unsubscribeFrom(FeatureDeletionRequestEvent.class);
        subscriber.unsubscribeFrom(FeatureUpdateRequestEvent.class);
        subscriber.unsubscribeFrom(FeatureNotificationRequestEvent.class);
        subscriber.unsubscribeFrom(NotificationRequestEvent.class);
        cleanAMQPQueues(FeatureCreationRequestEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(FeatureUpdateRequestEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(FeatureDeletionRequestEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(NotificationRequestEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
        doAfter();
    }

    /**
     * Custom test cleaning to override
     * @throws Exception
     */
    protected void doAfter() throws Exception {
        // Override to init something
    }

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

    protected void waitForErrorState(JpaRepository<? extends AbstractRequest, ?> repo)

            throws InterruptedException {
        int cpt = 0;

        // we will expect that all feature reference remain in database with the error state
        do {
            Thread.sleep(1000);
            if (cpt == 60) {
                fail("Timeout");
            }
            cpt++;
        } while (!repo.findAll().stream().allMatch(request -> RequestState.ERROR.equals(request.getState())));
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

    public boolean initNotificationSettings(boolean state){
        // Set notification to true/false
        FeatureNotificationSettings notificationSettings = featureSettingsNotificationService.retrieve();
        notificationSettings.setActiveNotification(state);
        try {
            featureSettingsNotificationService.update(notificationSettings);
        } catch (EntityNotFoundException e) {
            LOGGER.error("Notification settings not initialized properly");
        }
        return state;
    }
}
