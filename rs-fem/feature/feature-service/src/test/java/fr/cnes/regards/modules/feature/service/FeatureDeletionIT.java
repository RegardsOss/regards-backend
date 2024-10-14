/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityWithDisseminationRepository;
import fr.cnes.regards.modules.feature.domain.FeatureDisseminationInfo;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Kevin Marchois
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_deletion",
                                   "regards.amqp.enabled=true",
                                   "regards.feature.max.bulk.size=10" },
                    locations = { "classpath:regards_perf.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties", })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "noFemHandler" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FeatureDeletionIT extends AbstractFeatureMultitenantServiceIT {

    private static final String DELETION_OWNER = "deleter";

    @Captor
    private ArgumentCaptor<List<NotificationRequestEvent>> recordsCaptor;

    private boolean isToNotify;

    @Autowired
    private IFeatureEntityWithDisseminationRepository featureWithDisseminationRepository;

    @Override
    public void doInit() {
        // Check if notifications are required
        isToNotify = initDefaultNotificationSettings();
    }

    /**
     * Nominal test case of deletion create feature then send delete request
     * we will test that the {@link FeatureDeletionRequest}
     * are deleted and all FeatureEntity are deleted too
     * because they have not files
     */
    @Test
    public void test_feature_deletion_request_without_files() throws InterruptedException {
        Mockito.clearInvocations(publisher);
        // Given : mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisher).publish(Mockito.any(NotificationRequestEvent.class));
        long featureNumberInDatabase;
        int cpt = 0;
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(DELETION_OWNER,
                                                                           false,
                                                                           properties.getMaxBulkSize(),
                                                                           this.isToNotify);

        featureDeletionService.registerRequests(events);

        // When
        featureDeletionService.scheduleRequests();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return featureRepo.count() == 0;
        });

        // In this case all features have not been deleted
        if (cpt == 100) {
            fail("All features have not been deleted.");
        }

        // Then
        if (isToNotify) {
            mockNotificationSuccess();
            // the publisher must be called 2 times one for feature creation and one for feature deletion
            Mockito.verify(publisher, Mockito.times(2)).publish(recordsCaptor.capture());
            // each call concern properties.getMaxBulkSize().intValue() features
            assertEquals(properties.getMaxBulkSize().intValue(), recordsCaptor.getAllValues().get(0).size());
            assertEquals(properties.getMaxBulkSize().intValue(), recordsCaptor.getAllValues().get(1).size());
        }
        assertEquals(0, featureRepo.count());
    }

    /**
     * Nominal test case of deletion create feature then send delete request
     * we will test that the {@link FeatureDeletionRequest} have their step to
     * REMOTE_STORAGE_DELETEION_REQUESTED and all FeatureEntity are still in database
     * because they have files
     */
    @Test
    public void test_feature_deletion_request_with_files() throws InterruptedException {
        Mockito.clearInvocations(publisher);
        // Given
        int nbFeature = properties.getMaxBulkSize();

        // mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisher).publish(Mockito.any(NotificationRequestEvent.class));

        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(DELETION_OWNER, true, nbFeature, isToNotify);
        this.featureDeletionService.registerRequests(events);

        // When
        this.featureDeletionService.scheduleRequests();
        waitForStep(featureDeletionRequestRepo,
                    FeatureRequestStep.REMOTE_STORAGE_DELETION_REQUESTED,
                    nbFeature,
                    10_000);

        // Then
        assertEquals(properties.getMaxBulkSize().intValue(), this.featureRepo.count());
        // the publisher has been called because of storage successes (feature creation with files)
        Mockito.verify(publisher, Mockito.times(1)).publish(recordsCaptor.capture());
    }

    /**
     * Test priority level for feature deletion we will schedule properties.getMaxBulkSize() {@link FeatureDeletionRequestEvent}
     * with priority set to average plus properties.getMaxBulkSize() /2 {@link FeatureDeletionRequestEvent}
     * with {@link PriorityLevel} to average
     */
    @Test
    public void testFeaturePriority() throws InterruptedException {
        // Given
        int nbFeaturesBatch1 = properties.getMaxBulkSize();
        int nbFeaturesBatch2 = properties.getMaxBulkSize() / 2;
        int nbFeatures = nbFeaturesBatch1 + nbFeaturesBatch2;
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(DELETION_OWNER,
                                                                           true,
                                                                           nbFeatures,
                                                                           this.isToNotify);
        this.featureDeletionService.registerRequests(events);

        // When
        this.featureDeletionService.scheduleRequests();

        waitFeature(nbFeaturesBatch2, null, 30_000);

        if (isToNotify) {
            // first feature batch has been successfully deleted, now let simulate notification success
            mockNotificationSuccess();
        }
        // Then : there should remain properties.getMaxBulkSize / 2 request to be handled (scheduleRequest only
        // schedule properties.getMaxBulkSize requests)
        List<FeatureDeletionRequest> notScheduled = this.featureDeletionRequestRepo.findAll();
        assertEquals(nbFeaturesBatch2, notScheduled.size());
        assertTrue(notScheduled.stream().allMatch(request -> PriorityLevel.NORMAL.equals(request.getPriority())));
    }

    @Test
    public void testRetrieveRequests() throws InterruptedException {
        // Given
        int nbValid = properties.getMaxBulkSize();
        OffsetDateTime start = OffsetDateTime.now();
        // Register valid requests
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(DELETION_OWNER, true, nbValid, false);
        this.featureDeletionService.registerRequests(events);

        // When
        RequestsPage<FeatureRequestDTO> results = featureRequestService.findAll(FeatureRequestTypeEnum.DELETION,
                                                                                new SearchFeatureRequestParameters(),
                                                                                PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(nbValid, results.getContent().size());
        Assert.assertEquals(nbValid, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());

        // When
        results = featureRequestService.findAll(FeatureRequestTypeEnum.DELETION,
                                                new SearchFeatureRequestParameters().withStatesIncluded(List.of(
                                                    RequestState.ERROR)),
                                                PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());

        // When
        results = featureRequestService.findAll(FeatureRequestTypeEnum.DELETION,
                                                new SearchFeatureRequestParameters().withStatesIncluded(List.of(
                                                                                        RequestState.GRANTED))
                                                                                    .withLastUpdateAfter(OffsetDateTime.now()
                                                                                                                       .plusSeconds(
                                                                                                                           5)),
                                                PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());

        // When
        results = featureRequestService.findAll(FeatureRequestTypeEnum.DELETION,
                                                new SearchFeatureRequestParameters().withLastUpdateAfter(start)
                                                                                    .withLastUpdateBefore(OffsetDateTime.now()
                                                                                                                        .plusSeconds(
                                                                                                                            5)),
                                                PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(nbValid, results.getContent().size());
        Assert.assertEquals(nbValid, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());

        // When
        results = featureRequestService.findAll(FeatureRequestTypeEnum.DELETION,
                                                new SearchFeatureRequestParameters().withProviderIdsIncluded(List.of(
                                                    "id1")),
                                                PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(1, results.getContent().size());
        Assert.assertEquals(1, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());

        // When
        results = featureRequestService.findAll(FeatureRequestTypeEnum.DELETION,
                                                new SearchFeatureRequestParameters().withProviderIdsIncluded(List.of(
                                                    "id2")),
                                                PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(1, results.getContent().size());
        Assert.assertEquals(1, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());
    }

    @Test
    public void testDeleteRequests() throws InterruptedException {
        // Given
        int nbValid = properties.getMaxBulkSize();
        // Register valid requests
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(DELETION_OWNER, true, nbValid, false);
        featureDeletionService.registerRequests(events);

        // Simulate all requests to scheduled
        featureDeletionService.findRequests(new SearchFeatureRequestParameters(), PageRequest.of(0, nbValid * 2))
                              .forEach(r -> {
                                  r.setStep(FeatureRequestStep.LOCAL_SCHEDULED);
                                  this.featureDeletionRequestRepo.save(r);
                              });

        // When : try delete all requests.
        RequestHandledResponse response = this.featureDeletionService.deleteRequests(new SearchFeatureRequestParameters());
        LOGGER.info(response.getMessage());

        // Then
        Assert.assertEquals("There should be 0 requests deleted as all request are processing",
                            0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as all request are processing",
                            0,
                            response.getTotalRequested());

        // When
        response = featureDeletionService.deleteRequests(new SearchFeatureRequestParameters().withStatesIncluded(List.of(
            RequestState.GRANTED)));
        LOGGER.info(response.getMessage());

        // Then
        Assert.assertEquals("There should be 0 requests deleted as selection set on GRANTED Requests",
                            0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as selection set on GRANTED Requests",
                            0,
                            response.getTotalRequested());

        // When : simulate all requests to scheduled
        featureDeletionService.findRequests(new SearchFeatureRequestParameters(), PageRequest.of(0, nbValid * 2))
                              .forEach(r -> {
                                  r.setStep(FeatureRequestStep.REMOTE_STORAGE_ERROR);
                                  this.featureDeletionRequestRepo.save(r);
                              });

        response = featureDeletionService.deleteRequests(new SearchFeatureRequestParameters());
        LOGGER.info(response.getMessage());

        // Then
        Assert.assertEquals("invalid number of granted delete requests", nbValid, response.getTotalHandled());
        Assert.assertEquals("invalid number of granted delete requests", nbValid, response.getTotalRequested());
    }

    @Test
    public void testRetryRequests() throws InterruptedException {
        // Given
        int nbValid = properties.getMaxBulkSize();
        // Register valid requests
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(DELETION_OWNER, true, nbValid, false);
        this.featureDeletionService.registerRequests(events);

        // When : try delete all requests.
        RequestHandledResponse response = featureDeletionService.retryRequests(new SearchFeatureRequestParameters().withStatesIncluded(
            List.of(RequestState.ERROR)));
        LOGGER.info(response.getMessage());

        // Then
        Assert.assertEquals("There should be 0 requests retryed as request are not in ERROR state",
                            0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to retry as request are not in ERROR state",
                            0,
                            response.getTotalRequested());

        // When
        response = featureDeletionService.retryRequests(new SearchFeatureRequestParameters().withStatesIncluded(List.of(
            RequestState.GRANTED)));
        LOGGER.info(response.getMessage());

        // Then
        Assert.assertEquals("invalid number of GRANTED Requests", nbValid, response.getTotalHandled());
        Assert.assertEquals("invalid number GRANTED Requests", nbValid, response.getTotalRequested());

    }

    @Test
    public void test1SessionNotifierWithFiles() throws InterruptedException {
        // Given : create and Delete One with Files
        List<FeatureDeletionRequestEvent> eventWithFiles = prepareDeletionTestData(owner, true, 1, true);
        featureDeletionService.registerRequests(eventWithFiles);

        // When
        featureDeletionService.scheduleRequests();
        TimeUnit.SECONDS.sleep(5);
        mockNotificationSuccess();
        waitRequest(featureDeletionRequestRepo, 0, 20000);

        // Then
        checkOneDeletion();
    }

    @Test
    public void test1SessionNotifierWithoutFiles() throws InterruptedException {
        // Given : create and Delete One without files
        List<FeatureDeletionRequestEvent> eventWithoutFiles = prepareDeletionTestData(owner, false, 1, true);
        featureDeletionService.registerRequests(eventWithoutFiles);

        // When
        featureDeletionService.scheduleRequests();
        TimeUnit.SECONDS.sleep(5);
        mockNotificationSuccess();
        waitRequest(featureDeletionRequestRepo, 0, 20000);

        // Then
        checkOneDeletion();
    }

    @Test
    public void test1SessionNotifierWithoutNotification() throws InterruptedException, EntityException {
        // Given
        setNotificationSetting(false);

        // Create and Delete One without files
        List<FeatureDeletionRequestEvent> eventWithoutFiles = prepareDeletionTestData(owner, false, 1, false);
        featureDeletionService.registerRequests(eventWithoutFiles);
        TimeUnit.SECONDS.sleep(5);
        // When
        featureDeletionService.scheduleRequests();
        waitRequest(featureDeletionRequestRepo, 0, 20000);

        // Then
        checkOneDeletion();
    }

    @Test
    public void test1SessionNotifierWithRetry() throws InterruptedException {

        createRequestWithError();

        // Retry
        featureDeletionService.retryRequests(new SearchFeatureRequestParameters());
        mockNotificationSuccess();
        waitRequest(featureDeletionRequestRepo, 0, 20000);

        // Compute Session step
        computeSessionStep(13, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(8, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(5, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(2, property("referencedProducts"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(1, property("deleteRequests"), requests);
        checkRequests(1, property("deletedProducts"), requests);
        checkRequests(4, property("runningDeleteRequests"), requests);
        checkRequests(2, property("inErrorDeleteRequests"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(2, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(7, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(0, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "deleteRequests", sessionStepProperties);
        checkKey(1, "deletedProducts", sessionStepProperties);
        checkKey(0, "runningDeleteRequests", sessionStepProperties);
        checkKey(0, "inErrorDeleteRequests", sessionStepProperties);
    }

    @Test
    public void test1SessionNotifierWithDelete() throws InterruptedException {

        createRequestWithError();

        // Retry
        featureDeletionService.deleteRequests(new SearchFeatureRequestParameters());
        mockNotificationSuccess();
        waitRequest(featureDeletionRequestRepo, 0, 20000);

        // Compute Session step
        computeSessionStep(12, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(7, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(5, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(2, property("referencedProducts"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(2, property("deleteRequests"), requests);
        checkRequests(2, property("runningDeleteRequests"), requests);
        checkRequests(1, property("deletedProducts"), requests);
        checkRequests(2, property("inErrorDeleteRequests"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(2, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        Assertions.assertEquals(0, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(7, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(0, "referencedProducts", sessionStepProperties);
        checkKey(1, "deletedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(0, "deleteRequests", sessionStepProperties);
        checkKey(0, "runningDeleteRequests", sessionStepProperties);
        checkKey(0, "inErrorDeleteRequests", sessionStepProperties);
    }

    @Test
    public void testDeletionWithCreationPendingWithURN() {
        // Given
        List<FeatureCreationRequestEvent> featureCreationRequestEvents = initFeatureCreationRequestEvent(1,
                                                                                                         false,
                                                                                                         false);
        FeatureUniformResourceName urn = FeatureUniformResourceName.build(FeatureIdentifier.FEATURE,
                                                                          EntityType.DATA,
                                                                          "tenant",
                                                                          UUID.randomUUID(),
                                                                          1);
        featureCreationRequestEvents.get(0).getFeature().setUrn(urn);
        featureCreationService.registerRequests(featureCreationRequestEvents);

        FeatureDeletionRequestEvent featureDeletionRequestEvent = FeatureDeletionRequestEvent.build(owner,
                                                                                                    urn,
                                                                                                    PriorityLevel.NORMAL);

        featureDeletionService.registerRequests(Collections.singletonList(featureDeletionRequestEvent));

        // When
        int nbScheduledRequests = featureDeletionService.scheduleRequests();

        // Then
        Assert.assertEquals(0, nbScheduledRequests);
        Set<FeatureDeletionRequest> notScheduled = featureDeletionRequestRepo.findByStepIn(Collections.singletonList(
            FeatureRequestStep.LOCAL_DELAYED), OffsetDateTime.now());
        assertEquals(1, notScheduled.size());
    }

    @Test
    public void testDeletionWithCreationPendingWithoutURN() throws InterruptedException {
        // Given
        prepareCreationTestData(false, 1, false, false, false);

        FeatureUniformResourceName urn = featureCreationRequestRepo.findAll().get(0).getUrn();

        FeatureDeletionRequestEvent featureDeletionRequestEvent = FeatureDeletionRequestEvent.build(owner,
                                                                                                    urn,
                                                                                                    PriorityLevel.NORMAL);

        featureDeletionService.registerRequests(Collections.singletonList(featureDeletionRequestEvent));

        // When
        featureDeletionService.scheduleRequests();

        // Then
        Set<FeatureDeletionRequest> notScheduled = featureDeletionRequestRepo.findByStepIn(Collections.singletonList(
            FeatureRequestStep.LOCAL_DELAYED), OffsetDateTime.now());
        assertEquals(1, notScheduled.size());
    }

    @Test
    public void test_feature_deletion_request_blocked_with_files() throws InterruptedException {
        // Given : mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisher).publish(Mockito.any(NotificationRequestEvent.class));
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(DELETION_OWNER, true, 1, isToNotify);

        featureDeletionService.registerRequests(events);

        // Create case in order to block the feature deletion request
        FeatureEntity featureEntity = featureWithDisseminationRepository.findAll().get(0);
        FeatureDisseminationInfo featureDisseminationInfo = createFeatureDisseminationInfo(featureEntity.getUrn(),
                                                                                           "RecipientLabel",
                                                                                           true);
        featureDisseminationInfo.setBlocking(true);

        featureEntity.getDisseminationsInfo().add(featureDisseminationInfo);
        featureWithDisseminationRepository.save(featureEntity);

        // When
        Assert.assertEquals(0L,featureDeletionService.scheduleRequests());

        // Then
        Assert.assertEquals(1L, featureDeletionRequestRepo.findByStep(FeatureRequestStep.WAITING_BLOCKING_DISSEMINATION,
                                                                  OffsetDateTime.now().plusDays(1)).size());
    }

    @Test
    public void test_feature_deletion_request_blocked_without_files() throws InterruptedException {
        // Given : mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisher).publish(Mockito.any(NotificationRequestEvent.class));
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(DELETION_OWNER, false, 1, isToNotify);

        featureDeletionService.registerRequests(events);

        // Create case in order to block the feature deletion request
        FeatureEntity featureEntity = featureWithDisseminationRepository.findAll().get(0);
        FeatureDisseminationInfo featureDisseminationInfo = createFeatureDisseminationInfo(featureEntity.getUrn(),
                                                                                           "RecipientLabel",
                                                                                           true);
        featureDisseminationInfo.setBlocking(true);

        featureEntity.getDisseminationsInfo().add(featureDisseminationInfo);
        featureWithDisseminationRepository.save(featureEntity);

        // When
        Assert.assertEquals(0L,featureDeletionService.scheduleRequests());

        // Then
        Assert.assertEquals(1L, featureDeletionRequestRepo.findByStep(FeatureRequestStep.WAITING_BLOCKING_DISSEMINATION,
                                                                      OffsetDateTime.now().plusDays(1)).size());
    }

    // ---------------------
    // -- UTILITY METHODS --
    // ---------------------

    private void checkOneDeletion() throws InterruptedException {
        // Compute Session step
        computeSessionStep(9, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(6, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(3, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(2, property("referencedProducts"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(1, property("deleteRequests"), requests);
        checkRequests(1, property("deletedProducts"), requests);
        checkRequests(2, property("runningDeleteRequests"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(2, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(6, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(0, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "deleteRequests", sessionStepProperties);
        checkKey(1, "deletedProducts", sessionStepProperties);
        checkKey(0, "runningDeleteRequests", sessionStepProperties);
    }

    private void createRequestWithError() throws InterruptedException {
        // Create and Delete One with Files, fail on notification
        List<FeatureDeletionRequestEvent> eventWithFiles = prepareDeletionTestData(owner, true, 1, true);
        featureDeletionService.registerRequests(eventWithFiles);
        TimeUnit.SECONDS.sleep(5);
        featureDeletionService.scheduleRequests();
        waitForStep(featureDeletionRequestRepo, FeatureRequestStep.LOCAL_TO_BE_NOTIFIED, 1, 10_000);
        mockNotificationError();
        waitForSate(featureDeletionRequestRepo, RequestState.ERROR, 1, 20);

        // Compute Session step
        computeSessionStep(10, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(7, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(3, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(2, property("referencedProducts"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(1, property("deleteRequests"), requests);
        checkRequests(1, property("deletedProducts"), requests);
        checkRequests(2, property("runningDeleteRequests"), requests);
        checkRequests(1, property("inErrorDeleteRequests"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(2, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        Assertions.assertEquals(0, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(7, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(0, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "deleteRequests", sessionStepProperties);
        checkKey(1, "deletedProducts", sessionStepProperties);
        checkKey(0, "runningDeleteRequests", sessionStepProperties);
        checkKey(1, "inErrorDeleteRequests", sessionStepProperties);
    }

}
