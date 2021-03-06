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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import org.elasticsearch.action.search.SearchTask;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
 * @author S??bastien Binda
 */
@TestPropertySource(
        properties = {"spring.jpa.properties.hibernate.default_schema=feature_deletion", "regards.amqp.enabled=true",
        "regards.feature.max.bulk.size=10"},
        locations = {"classpath:regards_perf.properties", "classpath:batch.properties", "classpath:metrics.properties",})
@ActiveProfiles(value = { "testAmqp", "noscheduler", "noFemHandler" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FeatureDeletionIT extends AbstractFeatureMultitenantServiceTest {

    @Captor
    private ArgumentCaptor<List<NotificationRequestEvent>> recordsCaptor;

    private boolean isToNotify;

    @Override
    public void doInit() {
        // check if notifications are required
        this.isToNotify = initDefaultNotificationSettings();
    }

    /**
     * Nominal test case of deletion create feature then send delete request
     * we will test that the {@link FeatureDeletionRequest}
     * are deleted and all FeatureEntity are deleted too
     * because they have not files
     *
     * @throws InterruptedException
     */
    @Test
    public void testDeletionWithoutFiles() throws InterruptedException {
        String deletionOwner = "deleter";
        // mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisher).publish(Mockito.any(NotificationRequestEvent.class));
        long featureNumberInDatabase;
        int cpt = 0;
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(deletionOwner, false,
                                                                           properties.getMaxBulkSize(),
                                                                           this.isToNotify);

        this.featureDeletionService.registerRequests(events);

        this.featureDeletionService.scheduleRequests();
        do {
            featureNumberInDatabase = this.featureRepo.count();
            Thread.sleep(1000);
            cpt++;
        } while ((cpt < 100) && (featureNumberInDatabase != 0));

        // in that case all features hasn't be deleted
        if (cpt == 100) {
            fail("Doesn't have all features haven't be deleted");
        }

        if (this.isToNotify) {
            mockNotificationSuccess();
            // the publisher must be called 2 times one for feature creation and one for feature deletion
            Mockito.verify(publisher, Mockito.times(2)).publish(recordsCaptor.capture());
            // each call concern properties.getMaxBulkSize().intValue() features
            assertEquals(properties.getMaxBulkSize().intValue(), recordsCaptor.getAllValues().get(0).size());
            assertEquals(properties.getMaxBulkSize().intValue(), recordsCaptor.getAllValues().get(1).size());
        }
        assertEquals(0, this.featureRepo.count());
    }

    /**
     * Nominal test case of deletion create feature then send delete request
     * we will test that the {@link FeatureDeletionRequest} have their step to
     * REMOTE_STORAGE_DELETEION_REQUESTED and all FeatureEntity are still in database
     * because they have files
     * @throws InterruptedException
     */
    @Test
    public void testDeletionWithFiles() throws InterruptedException {

        int nbFeature = properties.getMaxBulkSize();
        String deletionOwner = "deleter";

        // mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisher).publish(Mockito.any(NotificationRequestEvent.class));

        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(deletionOwner, true,
                                                                           nbFeature,
                                                                           this.isToNotify);
        this.featureDeletionService.registerRequests(events);
        this.featureDeletionService.scheduleRequests();

        waitForStep(featureDeletionRequestRepo, FeatureRequestStep.REMOTE_STORAGE_DELETION_REQUESTED, nbFeature, 10_000);

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

        String deletionOwner = "deleter";
        int nbFeaturesBatch1 = properties.getMaxBulkSize();
        int nbFeaturesBatch2 = properties.getMaxBulkSize() / 2;
        int nbFeatures = nbFeaturesBatch1 + nbFeaturesBatch2;
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(deletionOwner, true,
                                                                           nbFeatures, this.isToNotify);
        this.featureDeletionService.registerRequests(events);

        this.featureDeletionService.scheduleRequests();

        waitFeature(nbFeaturesBatch2, null, 30_000);

        if (this.isToNotify) {
            // first feature batch has been successfully deleted, now let simulate notification success
            mockNotificationSuccess();
        }
        // there should remain properties.getMaxBulkSize / 2 request to be handled (scheduleRequest only schedule properties.getMaxBulkSize requests)
        List<FeatureDeletionRequest> notScheduled = this.featureDeletionRequestRepo.findAll();
        assertEquals(nbFeaturesBatch2, notScheduled.size());
        assertTrue(notScheduled.stream().allMatch(request -> PriorityLevel.NORMAL.equals(request.getPriority())));
    }

    @Test
    public void testRetrieveRequests() throws InterruptedException {
        int nbValid = properties.getMaxBulkSize();
        OffsetDateTime start = OffsetDateTime.now();
        // Register valid requests
        String deletionOwner = "deleter";
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(deletionOwner, true, nbValid, false);
        this.featureDeletionService.registerRequests(events);

        RequestsPage<FeatureRequestDTO> results = this.featureRequestService
                .findAll(FeatureRequestTypeEnum.DELETION, FeatureRequestsSelectionDTO.build(), PageRequest.of(0, 100));
        Assert.assertEquals(nbValid, results.getContent().size());
        Assert.assertEquals(nbValid, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.DELETION,
                                                     FeatureRequestsSelectionDTO.build().withState(RequestState.ERROR),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.DELETION,
                                                     FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED)
                                                             .withStart(OffsetDateTime.now().plusSeconds(5)),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.DELETION,
                                                     FeatureRequestsSelectionDTO.build().withStart(start)
                                                             .withEnd(OffsetDateTime.now().plusSeconds(5)),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(nbValid, results.getContent().size());
        Assert.assertEquals(nbValid, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.DELETION,
                                                     FeatureRequestsSelectionDTO.build().withProviderId("id1"),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(1, results.getContent().size());
        Assert.assertEquals(1, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.DELETION,
                                                     FeatureRequestsSelectionDTO.build().withProviderId("id2"),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(1, results.getContent().size());
        Assert.assertEquals(1, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());
    }

    @Test
    public void testDeleteRequests() throws InterruptedException {

        int nbValid = properties.getMaxBulkSize();
        // Register valid requests
        String deletionOwner = "deleter";
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(deletionOwner, true, nbValid, false);
        this.featureDeletionService.registerRequests(events);

        // Simulate all requests to scheduled
        this.featureDeletionService.findRequests(FeatureRequestsSelectionDTO.build(), PageRequest.of(0, nbValid*2))
                .forEach(r -> {
                    r.setStep(FeatureRequestStep.LOCAL_SCHEDULED);
                    this.featureDeletionRequestRepo.save(r);
                });

        // Try delete all requests.
        RequestHandledResponse response = this.featureDeletionService
                .deleteRequests(FeatureRequestsSelectionDTO.build());
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as all request are processing", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as all request are processing", 0,
                            response.getTotalRequested());

        response = this.featureDeletionService
                .deleteRequests(FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as selection set on GRANTED Requests", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as selection set on GRANTED Requests", 0,
                            response.getTotalRequested());

        // Simulate all requests to scheduled
        this.featureDeletionService.findRequests(FeatureRequestsSelectionDTO.build(), PageRequest.of(0, nbValid*2))
                .forEach(r -> {
                    r.setStep(FeatureRequestStep.REMOTE_STORAGE_ERROR);
                    this.featureDeletionRequestRepo.save(r);
                });

        response = this.featureDeletionService.deleteRequests(FeatureRequestsSelectionDTO.build());
        LOGGER.info(response.getMessage());
        Assert.assertEquals("invalid number of granted delete requests", nbValid, response.getTotalHandled());
        Assert.assertEquals("invalid number of granted delete requests", nbValid, response.getTotalRequested());

    }

    @Test
    public void testRetryRequests() throws InterruptedException {

        int nbValid = properties.getMaxBulkSize();
        // Register valid requests
        String deletionOwner = "deleter";
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(deletionOwner, true, nbValid, false);
        this.featureDeletionService.registerRequests(events);

        // Try delete all requests.
        RequestHandledResponse response = this.featureDeletionService
                .retryRequests(FeatureRequestsSelectionDTO.build().withState(RequestState.ERROR));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests retryed as request are not in ERROR state", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to retry as request are not in ERROR state", 0,
                            response.getTotalRequested());

        response = this.featureDeletionService
                .retryRequests(FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("invalid number of GRANTED Requests", nbValid,
                            response.getTotalHandled());
        Assert.assertEquals("invalid number GRANTED Requests", nbValid,
                            response.getTotalRequested());

    }

    @Test
    public void test1SessionNotifierWithFiles() throws InterruptedException {

        // Create and Delete One with Files
        List<FeatureDeletionRequestEvent> eventWithFiles = prepareDeletionTestData(owner, true, 1, true);
        featureDeletionService.registerRequests(eventWithFiles);
        featureDeletionService.scheduleRequests();
        TimeUnit.SECONDS.sleep(5);
        mockNotificationSuccess();
        waitRequest(featureDeletionRequestRepo, 0, 20000);

        checkOneDeletion();
    }

    @Test
    public void test1SessionNotifierWithoutFiles() throws InterruptedException {

        // Create and Delete One without files
        List<FeatureDeletionRequestEvent> eventWithoutFiles = prepareDeletionTestData(owner, false, 1, true);
        featureDeletionService.registerRequests(eventWithoutFiles);
        featureDeletionService.scheduleRequests();
        TimeUnit.SECONDS.sleep(5);
        mockNotificationSuccess();
        waitRequest(featureDeletionRequestRepo, 0, 20000);

        checkOneDeletion();
    }

    @Test
    public void test1SessionNotifierWithoutNotification() throws InterruptedException, EntityException {

        setNotificationSetting(false);

        // Create and Delete One without files
        List<FeatureDeletionRequestEvent> eventWithoutFiles = prepareDeletionTestData(owner, false, 1, false);
        featureDeletionService.registerRequests(eventWithoutFiles);
        TimeUnit.SECONDS.sleep(5);
        featureDeletionService.scheduleRequests();
        waitRequest(featureDeletionRequestRepo, 0, 20000);

        checkOneDeletion();
    }

    @Test
    public void test1SessionNotifierWithRetry() throws InterruptedException {

        createRequestWithError();

        // Retry
        featureDeletionService.retryRequests(new FeatureRequestsSelectionDTO());
        mockNotificationSuccess();
        waitRequest(featureDeletionRequestRepo, 0, 20000);

        // Compute Session step
        computeSessionStep(13);

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
        featureDeletionService.deleteRequests(new FeatureRequestsSelectionDTO());
        mockNotificationSuccess();
        waitRequest(featureDeletionRequestRepo, 0, 20000);

        // Compute Session step
        computeSessionStep(12);

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

        List<FeatureCreationRequestEvent> featureCreationRequestEvents = initFeatureCreationRequestEvent(1, false, false);
        FeatureUniformResourceName urn = FeatureUniformResourceName.build(FeatureIdentifier.FEATURE, EntityType.DATA, "tenant", UUID.randomUUID(), 1);
        featureCreationRequestEvents.get(0).getFeature().setUrn(urn);
        featureCreationService.registerRequests(featureCreationRequestEvents);

        FeatureDeletionRequestEvent featureDeletionRequestEvent = FeatureDeletionRequestEvent.build(owner, urn, PriorityLevel.NORMAL);

        featureDeletionService.registerRequests(Collections.singletonList(featureDeletionRequestEvent));
        featureDeletionService.scheduleRequests();

        Set<FeatureDeletionRequest> notScheduled = featureDeletionRequestRepo.findByStepIn(Collections.singletonList(FeatureRequestStep.LOCAL_DELAYED), OffsetDateTime.now());
        assertEquals(1, notScheduled.size());
    }

    @Test
    public void testDeletionWithCreationPendingWithoutURN() throws InterruptedException {

        prepareCreationTestData(false, 1, false, false, false);

        FeatureUniformResourceName urn = featureCreationRequestRepo.findAll().get(0).getUrn();

        FeatureDeletionRequestEvent featureDeletionRequestEvent = FeatureDeletionRequestEvent.build(owner, urn, PriorityLevel.NORMAL);

        featureDeletionService.registerRequests(Collections.singletonList(featureDeletionRequestEvent));
        featureDeletionService.scheduleRequests();

        Set<FeatureDeletionRequest> notScheduled = featureDeletionRequestRepo.findByStepIn(Collections.singletonList(FeatureRequestStep.LOCAL_DELAYED), OffsetDateTime.now());
        assertEquals(1, notScheduled.size());
    }

    private void checkOneDeletion() throws InterruptedException {
        // Compute Session step
        computeSessionStep(9);

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
        computeSessionStep(10);

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
