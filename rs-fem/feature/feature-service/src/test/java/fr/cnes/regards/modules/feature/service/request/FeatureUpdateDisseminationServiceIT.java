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
package fr.cnes.regards.modules.feature.service.request;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.*;
import fr.cnes.regards.modules.feature.domain.FeatureDisseminationInfo;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.ILightFeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationInfoType;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationRequest;
import fr.cnes.regards.modules.feature.dto.event.in.DisseminationAckEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceIT;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionProperty;
import fr.cnes.regards.modules.feature.service.settings.IFeatureNotificationSettingsService;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.dto.properties.ObjectProperty;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import fr.cnes.regards.modules.notifier.dto.out.Recipient;
import fr.cnes.regards.modules.notifier.dto.out.RecipientStatus;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_dissemination_it",
                                   "regards.amqp.enabled=true",
                                   "regards.feature.delay.before.processing=1",
                                   "spring.task.scheduling.pool.size=2",
                                   "regards.feature.metrics.enabled=true",
                                   "regards.feature.max.bulk.size=50",
                                   "regards.feature.delay.before.processing=1" },
                    locations = { "classpath:regards_perf.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles({ "testAmqp", "noscheduler", "noFemHandler" })
public class FeatureUpdateDisseminationServiceIT extends AbstractFeatureMultitenantServiceIT {

    private final String recipientLabelRequired = "recipientLabelRequired";

    private final String recipientLabelNotRequired = "recipientLabelNotRequired";

    @Autowired
    IStepPropertyUpdateRequestRepository stepPropertyUpdateRequestRepository;

    @Autowired
    private IFeatureCreationRequestRepository featureCreationRequestRepository;

    @Autowired
    private IFeatureEntityRepository featureRepo;

    @Autowired
    private IFeatureEntityWithDisseminationRepository featureWithDisseminationRepo;

    @Autowired
    private IFeatureUpdateRequestRepository featureUpdateRequestRepository;

    @Autowired
    private FeatureNotifierListener featureNotifierListener;

    @Autowired
    private FeatureUpdateDisseminationService featureUpdateDisseminationService;

    @Autowired
    private IFeatureNotificationSettingsService notificationSettingsService;

    @Autowired
    private IFeatureUpdateDisseminationRequestRepository featureUpdateDisseminationRequestRepository;

    private boolean isToNotify;

    @Override
    @Before
    public void before() throws Exception {
        super.before();
        this.isToNotify = initDefaultNotificationSettings();
    }

    @Test
    public void testUpdateFeatureWithAck() {
        FeatureUniformResourceName featureURN = initValidFeatureThatHasBeenNotifiedToTwoRecipients();

        // The recipient send back ACK
        List<DisseminationAckEvent> messages = Lists.newArrayList(new DisseminationAckEvent(featureURN.toString(),
                                                                                            recipientLabelRequired));
        featureUpdateDisseminationService.saveAckRequests(messages);

        // The FeatureUpdateDissemination request is saved
        List<FeatureUpdateDisseminationRequest> updateDisseminationRequests = featureUpdateDisseminationRequestRepository.findAll();
        Assert.assertEquals("should get one request", 1, updateDisseminationRequests.size());
        Assert.assertEquals("should use right recipient label",
                            recipientLabelRequired,
                            updateDisseminationRequests.get(0).getRecipientLabel());
        Assert.assertEquals("should use right request type",
                            FeatureUpdateDisseminationInfoType.ACK,
                            updateDisseminationRequests.get(0).getUpdateType());
        Assert.assertNull("should not use ack required", updateDisseminationRequests.get(0).getAckRequired());

        // The entity is still not updated
        List<FeatureEntity> featureEntities = featureWithDisseminationRepo.findAll();
        Assert.assertEquals("should get one feature entity", 1, featureEntities.size());
        Assert.assertTrue("should not be marked as disseminated", featureEntities.get(0).isDisseminationPending());

        Set<FeatureDisseminationInfo> featureDisseminationInfos = featureEntities.get(0).getDisseminationsInfo();
        FeatureDisseminationInfo featureDisseminationRequired = getFeatureDisseminationInfoByLabel(
            featureDisseminationInfos,
            recipientLabelRequired);
        FeatureDisseminationInfo featureDisseminationNotRequired = getFeatureDisseminationInfoByLabel(
            featureDisseminationInfos,
            recipientLabelNotRequired);

        Assert.assertNull("should be ack", featureDisseminationRequired.getAckDate());
        Assert.assertTrue(featureDisseminationRequired.isBlocking());
        Assert.assertNotNull("should be ack", featureDisseminationNotRequired.getAckDate());
        Assert.assertFalse(featureDisseminationNotRequired.isBlocking());

        // Process requests
        featureUpdateDisseminationService.handleRequests();

        // Request has been deleted
        updateDisseminationRequests = featureUpdateDisseminationRequestRepository.findAll();
        Assert.assertEquals("should all requests be handled", 0, updateDisseminationRequests.size());

        // Check Feature correctly updated
        featureEntities = featureWithDisseminationRepo.findAll();
        Assert.assertEquals("should get one feature entity", 1, featureEntities.size());
        Assert.assertFalse("should be marked as disseminated now", featureEntities.get(0).isDisseminationPending());

        featureDisseminationInfos = featureEntities.get(0).getDisseminationsInfo();
        Assert.assertEquals("should get two disseminations infos", 2, featureDisseminationInfos.size());

        featureDisseminationRequired = getFeatureDisseminationInfoByLabel(featureDisseminationInfos,
                                                                          recipientLabelRequired);

        featureDisseminationNotRequired = getFeatureDisseminationInfoByLabel(featureDisseminationInfos,
                                                                             recipientLabelNotRequired);

        Assert.assertNotNull("should be ack", featureDisseminationRequired.getAckDate());
        Assert.assertTrue(featureDisseminationRequired.isBlocking());
        Assert.assertNotNull("should be ack", featureDisseminationNotRequired.getAckDate());
        Assert.assertFalse(featureDisseminationNotRequired.isBlocking());

        // simulate feature update
        featureUpdateService.registerRequests(prepareUpdateRequests(Lists.newArrayList(featureURN)));
        List<AbstractFeatureRequest> abstractFeatureRequests = mockNotificationSent();

        String requestId = abstractFeatureRequests.stream().findFirst().get().getRequestId();
        String requestOwner = abstractFeatureRequests.stream().findFirst().get().getRequestOwner();
        String recipientLabelAnotherRequired = "recipientLabelAnotherRequired";
        Set<Recipient> recipients = Sets.newHashSet(new Recipient(recipientLabelRequired,
                                                                  RecipientStatus.SUCCESS,
                                                                  true,
                                                                  true),
                                                    new Recipient(recipientLabelAnotherRequired,
                                                                  RecipientStatus.SUCCESS,
                                                                  true,
                                                                  true),
                                                    new Recipient(recipientLabelNotRequired,
                                                                  RecipientStatus.SUCCESS,
                                                                  false,
                                                                  false));
        List<NotifierEvent> notifierEvents = Lists.newArrayList(new NotifierEvent(requestId,
                                                                                  requestOwner,
                                                                                  NotificationState.SUCCESS,
                                                                                  recipients,
                                                                                  OffsetDateTime.now()));
        featureNotifierListener.onRequestSuccess(notifierEvents);

        // the FeatureCreationRequest must be deleted
        assertEquals(0, featureCreationRequestRepository.count());

        // And 2 FeatureUpdateDissemination requests created
        updateDisseminationRequests = featureUpdateDisseminationRequestRepository.findAll();
        Assert.assertEquals("should requests exists", 3, updateDisseminationRequests.size());

        // Process requests
        featureUpdateDisseminationService.handleRequests();

        // Fetch entities with their dissemination
        featureEntities = featureWithDisseminationRepo.findAll();
        Assert.assertEquals("should get one feature entity", 1, featureEntities.size());
        featureDisseminationInfos = featureEntities.get(0).getDisseminationsInfo();
        Assert.assertEquals("should get two disseminations infos", 3, featureDisseminationInfos.size());

        featureDisseminationRequired = getFeatureDisseminationInfoByLabel(featureDisseminationInfos,
                                                                          recipientLabelRequired);
        featureDisseminationNotRequired = getFeatureDisseminationInfoByLabel(featureDisseminationInfos,
                                                                             recipientLabelNotRequired);
        FeatureDisseminationInfo featureDisseminationAnotherRequired = getFeatureDisseminationInfoByLabel(
            featureDisseminationInfos,
            recipientLabelAnotherRequired);

        // Ack date has been reinitialised
        Assert.assertNull("should not ack", featureDisseminationRequired.getAckDate());
        Assert.assertTrue(featureDisseminationRequired.isBlocking());
        Assert.assertNull("should not ack", featureDisseminationAnotherRequired.getAckDate());
        Assert.assertTrue(featureDisseminationAnotherRequired.isBlocking());
        Assert.assertNotNull("should be ack", featureDisseminationNotRequired.getAckDate());
        Assert.assertFalse(featureDisseminationNotRequired.isBlocking());

        checkSession(featureEntities.get(0).getSessionOwner(),
                     featureEntities.get(0).getSession(),
                     recipientLabelRequired,
                     1,
                     1,
                     13);
        checkSession(featureEntities.get(0).getSessionOwner(),
                     featureEntities.get(0).getSession(),
                     recipientLabelAnotherRequired,
                     1,
                     0,
                     13);
        checkSession(featureEntities.get(0).getSessionOwner(),
                     featureEntities.get(0).getSession(),
                     recipientLabelNotRequired,
                     0,
                     2,
                     13);
    }

    @Test
    public void testFeatureUpdateAndReceivingAndProcessingAckSimultaneously()
        throws InterruptedException, EntityException {
        notificationSettingsService.setActiveNotification(false);

        // create features
        int featureToCreateNumber = properties.getMaxBulkSize();
        List<FeatureCreationRequestEvent> events = prepareCreationTestData(false,
                                                                           featureToCreateNumber,
                                                                           false,
                                                                           true,
                                                                           false);

        List<FeatureEntity> allFeatureEntities = featureWithDisseminationRepo.findAll();
        String recipientLabel = "recipientLabel";
        for (FeatureEntity featureEntity : allFeatureEntities) {
            featureEntity.getDisseminationsInfo().add(new FeatureDisseminationInfo(recipientLabel, true));
        }
        featureWithDisseminationRepo.saveAll(allFeatureEntities);

        // create update requests
        List<FeatureUpdateRequestEvent> updateEvents = new ArrayList<>();
        updateEvents = events.stream()
                             .map(event -> FeatureUpdateRequestEvent.build("test",
                                                                           event.getMetadata(),
                                                                           event.getFeature()))
                             .collect(Collectors.toList());

        updateEvents.stream().forEach(event -> {
            event.getFeature().getProperties().clear();
            event.getFeature()
                 .setUrn(FeatureUniformResourceName.build(FeatureIdentifier.FEATURE,
                                                          EntityType.DATA,
                                                          getDefaultTenant(),
                                                          UUID.nameUUIDFromBytes(event.getFeature().getId().getBytes()),
                                                          1));
            event.getFeature()
                 .addProperty(IProperty.buildObject("file_characterization",
                                                    IProperty.buildBoolean("valid", Boolean.FALSE),
                                                    IProperty.buildDate("invalidation_date", OffsetDateTime.now())));
        });
        this.featureUpdateService.registerRequests(updateEvents);

        List<DisseminationAckEvent> disseminationAckEvents = new ArrayList<>();
        for (FeatureEntity featureEntity : allFeatureEntities) {
            disseminationAckEvents.add(new DisseminationAckEvent(featureEntity.getUrn().toString(), recipientLabel));
        }

        // we wait for delay before schedule
        Thread.sleep((this.properties.getDelayBeforeProcessing() * 1000) + 100);
        this.featureUpdateService.scheduleRequests();

        // save FeatureUpdateDissemination requests
        featureUpdateDisseminationService.saveAckRequests(disseminationAckEvents);

        // Process requests
        featureUpdateDisseminationService.handleRequests();

        List<FeatureUpdateDisseminationRequest> updateDisseminationRequests = featureUpdateDisseminationRequestRepository.findAll();
        Assert.assertNotEquals("requests cannot be handled right now as feature update requests are not handled",
                               0,
                               updateDisseminationRequests.size());

        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            featureUpdateDisseminationService.handleRequests();
            int nbFeatureUpdateDisseminationRequestRemaining = featureUpdateDisseminationRequestRepository.findAll()
                                                                                                          .size();
            LOGGER.info("{} remaining FeatureUpdateDissemination requests",
                        nbFeatureUpdateDisseminationRequestRemaining);
            return nbFeatureUpdateDisseminationRequestRemaining == 0;
        });

        List<ILightFeatureUpdateRequest> scheduled = this.featureUpdateRequestRepository.findRequestsToSchedule(0,
                                                                                                                properties.getMaxBulkSize());
        assertEquals(0, scheduled.size());

        featureWithDisseminationRepo.findAll().forEach(feature -> {
            assertNotNull(feature.getDisseminationsInfo().stream().findFirst().get().getAckDate());
            ObjectProperty file_characterization = (ObjectProperty) feature.getFeature()
                                                                           .getProperties()
                                                                           .stream()
                                                                           .filter(property -> property.getName()
                                                                                                       .equals(
                                                                                                           "file_characterization"))
                                                                           .findFirst()
                                                                           .get();
            IProperty validProperty = file_characterization.getValue()
                                                           .stream()
                                                           .filter(property -> property.getName().equals("valid"))
                                                           .findFirst()
                                                           .get();
            assertEquals(false, validProperty.getValue());
        });
    }

    // ---------------------
    // -- UTILITY METHODS --
    // ---------------------

    private void checkSession(String source,
                              String session,
                              String recipientLabel,
                              int running,
                              int done,
                              int totalTransactionOnSession) {

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            int nbStep = stepPropertyUpdateRequestRepository.findBySession(session).size();
            LOGGER.info("{} steps", nbStep);
            return nbStep == totalTransactionOnSession;
        });

        Map<String, List<StepPropertyUpdateRequest>> stepProperties = stepPropertyUpdateRequestRepository.findBySession(
                                                                                                             session)
                                                                                                         .stream()
                                                                                                         .filter(s -> s.getSource()
                                                                                                                       .equals(
                                                                                                                           source))
                                                                                                         .collect(
                                                                                                             Collectors.groupingBy(
                                                                                                                 s -> s.getStepPropertyInfo()
                                                                                                                       .getProperty()));

        checkSessionProperty(recipientLabel,
                             stepProperties,
                             FeatureSessionProperty.RUNNING_DISSEMINATION_PRODUCTS,
                             running);
        checkSessionProperty(recipientLabel, stepProperties, FeatureSessionProperty.DISSEMINATED_PRODUCTS, done);

    }

    private FeatureUniformResourceName initValidFeatureThatHasBeenNotifiedToTwoRecipients() {
        initData(1);

        assertEquals(1, featureCreationRequestRepository.count());
        mockStorageHelper.mockFeatureCreationStorageSuccess();

        List<AbstractFeatureRequest> abstractFeatureRequests = mockNotificationSent();

        assertFalse("should retrieve request", abstractFeatureRequests.isEmpty());

        String requestId = abstractFeatureRequests.stream().findFirst().get().getRequestId();
        String requestOwner = abstractFeatureRequests.stream().findFirst().get().getRequestOwner();
        HashSet<Recipient> recipients = Sets.newHashSet(new Recipient(recipientLabelRequired,
                                                                      RecipientStatus.SUCCESS,
                                                                      true,
                                                                      true),
                                                        new Recipient(recipientLabelNotRequired,
                                                                      RecipientStatus.SUCCESS,
                                                                      false,
                                                                      false));
        List<NotifierEvent> notifierEvents = Lists.newArrayList(new NotifierEvent(requestId,
                                                                                  requestOwner,
                                                                                  NotificationState.SUCCESS,
                                                                                  recipients,
                                                                                  OffsetDateTime.now()));
        featureNotifierListener.onRequestSuccess(notifierEvents);

        // the FeatureCreationRequest must be deleted
        assertEquals(0, featureCreationRequestRepository.count());

        List<FeatureUpdateDisseminationRequest> updateDisseminationRequests = featureUpdateDisseminationRequestRepository.findAll();
        Assert.assertEquals("should get two requests, one for each recipient", 2, updateDisseminationRequests.size());

        FeatureUpdateDisseminationRequest updateDisseminationRequestRequired = updateDisseminationRequests.stream()
                                                                                                          .filter(
                                                                                                              disseminationInfo -> disseminationInfo.getRecipientLabel()
                                                                                                                                                    .equals(
                                                                                                                                                        recipientLabelRequired))
                                                                                                          .findFirst()
                                                                                                          .get();
        FeatureUpdateDisseminationRequest updateDisseminationRequestNotRequired = updateDisseminationRequests.stream()
                                                                                                             .filter(
                                                                                                                 disseminationInfo -> disseminationInfo.getRecipientLabel()
                                                                                                                                                       .equals(
                                                                                                                                                           recipientLabelNotRequired))
                                                                                                             .findFirst()
                                                                                                             .get();

        FeatureUniformResourceName featureURN = abstractFeatureRequests.get(0).getUrn();
        assertEquals("should return the feature URN", featureURN, updateDisseminationRequestRequired.getUrn());
        assertEquals("should return the feature URN", featureURN, updateDisseminationRequestNotRequired.getUrn());

        assertTrue("should return the right value for <ACK required>",
                   updateDisseminationRequestRequired.getAckRequired());
        assertFalse("should return the right value for <ACK required>",
                    updateDisseminationRequestNotRequired.getAckRequired());

        assertEquals("should return the feature URN",
                     FeatureUpdateDisseminationInfoType.PUT,
                     updateDisseminationRequestRequired.getUpdateType());
        assertEquals("should return the feature URN",
                     FeatureUpdateDisseminationInfoType.PUT,
                     updateDisseminationRequestNotRequired.getUpdateType());

        featureUpdateDisseminationService.handleRequests();

        updateDisseminationRequests = featureUpdateDisseminationRequestRepository.findAll();
        Assert.assertEquals("should all requests be handled", 0, updateDisseminationRequests.size());

        return abstractFeatureRequests.get(0).getUrn();
    }

    private void checkSessionProperty(String recipientLabel,
                                      Map<String, List<StepPropertyUpdateRequest>> stepProperties,
                                      FeatureSessionProperty property,
                                      int expected) {
        String propertyName = this.featureUpdateDisseminationService.getSessionPropertyName(property, recipientLabel);
        int count = stepProperties.getOrDefault(propertyName, new ArrayList<>())
                                  .stream()
                                  .mapToInt(s -> s.getType() == StepPropertyEventTypeEnum.DEC ?
                                      -Integer.parseInt(s.getStepPropertyInfo().getValue()) :
                                      Integer.parseInt(s.getStepPropertyInfo().getValue()))
                                  .reduce(0, (total, value) -> total + value);
        Assert.assertEquals(String.format("Invalid number of %s requests in session", property), expected, count);
    }

    private FeatureDisseminationInfo getFeatureDisseminationInfoByLabel(Set<FeatureDisseminationInfo> featureDisseminationsInfos,
                                                                        String recipientLabel) {
        return featureDisseminationsInfos.stream()
                                         .filter(disseminationInfo -> disseminationInfo.getLabel()
                                                                                       .equals(recipientLabel))
                                         .findFirst()
                                         .get();
    }

    protected List<AbstractFeatureRequest> mockNotificationSent() {
        List<AbstractFeatureRequest> requestsToSend = abstractFeatureRequestRepo.findAll();
        if (!requestsToSend.isEmpty()) {
            featureNotificationService.sendToNotifier();
        }
        return requestsToSend;
    }
}