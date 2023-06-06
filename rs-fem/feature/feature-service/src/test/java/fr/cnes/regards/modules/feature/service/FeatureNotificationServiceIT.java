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

import com.google.gson.Gson;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link FeatureNotificationRequestEvent} publishing
 *
 * @author Kevin Marchois
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_notif",
                                   "regards.amqp.enabled=true" },
                    locations = { "classpath:regards_perf.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FeatureNotificationServiceIT extends AbstractFeatureMultitenantServiceIT {

    @Autowired
    private IFeatureNotificationService notificationService;

    @Autowired
    private Gson gson;

    @Captor
    private ArgumentCaptor<List<NotificationRequestEvent>> recordsCaptor;

    @Test
    public void testNotification() {

        // mock the publish method to not broke other tests
        Mockito.doNothing().when(publisher).publish(Mockito.any(NotificationRequestEvent.class));

        String source = "testNotification";
        String session = UUID.randomUUID().toString();

        int numberFeature = 2;
        List<Feature> featuresNotified = doNotificationProcess(numberFeature, source, session);

        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(recordsCaptor.capture());
        // the first publish message to be intercepted must be the creation of createdEntity
        assertEquals(gson.toJson(new CreateNotificationRequestEventVisitor.NotificationActionEventMetadata(
            FeatureManagementAction.NOTIFIED,
            source,
            session)), recordsCaptor.getValue().get(0).getMetadata().toString());
        assertEquals(gson.toJson(featuresNotified.get(0)), recordsCaptor.getValue().get(0).getPayload().toString());
        // the second message is the update of updatedEntity
        assertEquals(gson.toJson(new CreateNotificationRequestEventVisitor.NotificationActionEventMetadata(
            FeatureManagementAction.NOTIFIED,
            source,
            session)), recordsCaptor.getValue().get(1).getMetadata().toString());
        assertEquals(gson.toJson(featuresNotified.get(1)), recordsCaptor.getValue().get(1).getPayload().toString());
    }

    @Test
    @Ignore("Test to manuel check perf using logs of feature notification request handling.")
    public void testNotification1000() {
        doNotificationProcess(1000, "source", "session");
    }

    private List<Feature> doNotificationProcess(int numberNotified, String source, String session) {
        // use it only to initialize Feature
        List<FeatureCreationRequestEvent> list = initFeatureCreationRequestEvent(numberNotified,
                                                                                 true,
                                                                                 false,
                                                                                 source,
                                                                                 session);
        for (FeatureCreationRequestEvent event : list) {
            event.getFeature()
                 .setUrn(FeatureUniformResourceName.pseudoRandomUrn(FeatureIdentifier.FEATURE,
                                                                    EntityType.DATA,
                                                                    "tenant",
                                                                    1));
        }
        List<FeatureNotificationRequestEvent> notifCreated = new ArrayList<>(numberNotified / 2);
        List<FeatureNotificationRequestEvent> notifUpdated = new ArrayList<>(numberNotified / 2);
        // consider half to be update entities
        List<Feature> featuresNotified = new ArrayList<>(numberNotified);
        for (int i = 0; i < numberNotified / 2; i++) {
            FeatureEntity createdEntity = FeatureEntity.build(source,
                                                              session,
                                                              list.get(i).getFeature(),
                                                              null,
                                                              list.get(i).getFeature().getModel());
            FeatureEntity updatedEntity = FeatureEntity.build(source,
                                                              session,
                                                              list.get(i + numberNotified / 2).getFeature(),
                                                              null,
                                                              list.get(i + numberNotified / 2).getFeature().getModel());
            updatedEntity.setLastUpdate(OffsetDateTime.now().plusSeconds(1));
            createdEntity.setUrn(list.get(i).getFeature().getUrn());
            updatedEntity.setUrn(list.get(i + numberNotified / 2).getFeature().getUrn());
            // to skip creation and update process
            featuresNotified.add(createdEntity.getFeature());
            featuresNotified.add(updatedEntity.getFeature());
            this.featureRepo.save(createdEntity);
            this.featureRepo.save(updatedEntity);
            notifCreated.add(FeatureNotificationRequestEvent.build("notifier",
                                                                   createdEntity.getUrn(),
                                                                   PriorityLevel.LOW,
                                                                   new HashSet<>()));
            notifUpdated.add(FeatureNotificationRequestEvent.build("notifier",
                                                                   updatedEntity.getUrn(),
                                                                   PriorityLevel.LOW,
                                                                   Set.of("recipient_businessId0",
                                                                          "recipient_businessId1")));
        }

        this.publisher.publish(notifCreated);
        this.publisher.publish(notifUpdated);

        this.waitRequest(notificationRequestRepo, numberNotified, 30000);
        assertEquals(numberNotified, notificationService.sendToNotifier());
        //simulate that notification has been handle with success
        Page<AbstractFeatureRequest> requestsToSend = abstractFeatureRequestRepo.findByStepAndRequestDateLessThanEqual(
            FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED,
            OffsetDateTime.now().plusDays(1),
            PageRequest.of(0, numberNotified, Sort.by(Sort.Order.asc("priority"), Sort.Order.asc("requestDate"))));
        featureNotificationService.handleNotificationSuccess(requestsToSend.toSet());
        this.waitRequest(notificationRequestRepo, 0, 30000);
        return featuresNotified;
    }

    @Test
    public void testRetrieveRequests() throws InterruptedException {
        int nbValid = 20;
        OffsetDateTime start = OffsetDateTime.now();
        // Create features
        prepareCreationTestData(false, nbValid, true, true, false);

        RequestsPage<FeatureRequestDTO> results = this.featureRequestService.findAll(FeatureRequestTypeEnum.NOTIFICATION,
                                                                                     new SearchFeatureRequestParameters(),
                                                                                     PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());

        // Notify them
        List<FeatureUniformResourceName> urns = this.featureRepo.findAll()
                                                                .stream()
                                                                .map(FeatureEntity::getUrn)
                                                                .collect(Collectors.toList());
        this.featureNotificationService.registerRequests(prepareNotificationRequests(urns));

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.NOTIFICATION,
                                                     new SearchFeatureRequestParameters(),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(nbValid, results.getContent().size());
        Assert.assertEquals(nbValid, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.NOTIFICATION,
                                                     new SearchFeatureRequestParameters().withStatesIncluded(List.of(
                                                         RequestState.ERROR)),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.NOTIFICATION,
                                                     new SearchFeatureRequestParameters().withStatesIncluded(List.of(
                                                                                             RequestState.GRANTED))
                                                                                         .withLastUpdateAfter(
                                                                                             OffsetDateTime.now()
                                                                                                           .plusSeconds(
                                                                                                               5)),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.NOTIFICATION,
                                                     new SearchFeatureRequestParameters().withLastUpdateBefore(
                                                                                             OffsetDateTime.now().plusSeconds(5))
                                                                                         .withLastUpdateAfter(start),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(nbValid, results.getContent().size());
        Assert.assertEquals(nbValid, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());
    }

    @Test
    public void testDeleteRequests() throws InterruptedException {

        int nbValid = 20;
        // Create features
        prepareCreationTestData(false, nbValid, true, true, false);
        // Notify them
        List<FeatureUniformResourceName> urns = this.featureRepo.findAll()
                                                                .stream()
                                                                .map(FeatureEntity::getUrn)
                                                                .collect(Collectors.toList());
        Assert.assertTrue(urns.size() > 0);
        int results = this.featureNotificationService.registerRequests(prepareNotificationRequests(urns));
        Assert.assertTrue(results > 0);

        // Try delete all requests.
        RequestHandledResponse response = this.featureRequestService.delete(FeatureRequestTypeEnum.NOTIFICATION,
                                                                            new SearchFeatureRequestParameters().withStatesIncluded(
                                                                                List.of(RequestState.ERROR)));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as request are not in ERROR state",
                            0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as request are not in ERROR state",
                            0,
                            response.getTotalRequested());

        response = this.featureRequestService.delete(FeatureRequestTypeEnum.NOTIFICATION,
                                                     new SearchFeatureRequestParameters().withStatesIncluded(List.of(
                                                         RequestState.GRANTED)));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as selection set on GRANTED Requests",
                            nbValid,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as selection set on GRANTED Requests",
                            nbValid,
                            response.getTotalRequested());

    }

    @Test
    public void testRetryRequests() throws InterruptedException {

        int nbValid = 20;
        // Create features
        prepareCreationTestData(false, nbValid, true, true, false);
        // Notify them
        List<FeatureUniformResourceName> urns = this.featureRepo.findAll()
                                                                .stream()
                                                                .map(FeatureEntity::getUrn)
                                                                .collect(Collectors.toList());
        Assert.assertTrue(urns.size() > 0);
        int results = this.featureNotificationService.registerRequests(prepareNotificationRequests(urns));
        Assert.assertTrue(results > 0);

        // Try delete all requests.
        RequestHandledResponse response = this.featureRequestService.retry(FeatureRequestTypeEnum.NOTIFICATION,
                                                                           new SearchFeatureRequestParameters().withStatesIncluded(
                                                                               List.of(RequestState.ERROR)));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests retryed as request are not in ERROR state",
                            0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to retry as request are not in ERROR state",
                            0,
                            response.getTotalRequested());

        response = this.featureRequestService.retry(FeatureRequestTypeEnum.NOTIFICATION,
                                                    new SearchFeatureRequestParameters().withStatesIncluded(List.of(
                                                        RequestState.GRANTED)));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests retryed as selection set on GRANTED Requests",
                            nbValid,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to retry as selection set on GRANTED Requests",
                            nbValid,
                            response.getTotalRequested());

    }

    @Test
    public void testSessionNotifier() throws InterruptedException {

        prepareCreationTestData(false, 1, true, true, false);
        List<FeatureUniformResourceName> urn = featureRepo.findAll()
                                                          .stream()
                                                          .map(FeatureEntity::getUrn)
                                                          .collect(Collectors.toList());
        featureNotificationService.registerRequests(prepareNotificationRequests(urn));
        mockNotificationSuccess();
        waitRequest(notificationRequestRepo, 0, 20000);

        // Compute Session step
        computeSessionStep(8, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(6, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(2, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(1, property("referencedProducts"), requests);
        checkRequests(1, property("notifyRequests"), requests);
        checkRequests(1, property("notifyProducts"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(2, property("runningNotifyRequests"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(1, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(6, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(1, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "notifyRequests", sessionStepProperties);
        checkKey(1, "notifyProducts", sessionStepProperties);
        checkKey(0, "runningNotifyRequests", sessionStepProperties);
    }

    @Test
    public void testSessionNotifierWithRetry() throws InterruptedException {

        createOneRequestWithError();

        // Retry
        featureNotificationService.retryRequests(new SearchFeatureRequestParameters());
        mockNotificationSuccess();
        waitRequest(notificationRequestRepo, 0, 20000);

        // Compute Session step
        computeSessionStep(12, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(8, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(4, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(1, property("referencedProducts"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(1, property("notifyRequests"), requests);
        checkRequests(1, property("notifyProducts"), requests);
        checkRequests(4, property("runningNotifyRequests"), requests);
        checkRequests(2, property("inErrorNotifyRequests"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(1, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(7, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(1, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "notifyRequests", sessionStepProperties);
        checkKey(1, "notifyProducts", sessionStepProperties);
        checkKey(0, "inErrorNotifyRequests", sessionStepProperties);
        checkKey(0, "runningNotifyRequests", sessionStepProperties);
    }

    @Test
    public void testSessionNotifierWithDelete() throws InterruptedException {

        createOneRequestWithError();

        // Delete
        featureNotificationService.deleteRequests(new SearchFeatureRequestParameters());
        waitRequest(notificationRequestRepo, 0, 20000);

        // Compute Session step
        computeSessionStep(10, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(6, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(4, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(1, property("referencedProducts"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(2, property("notifyRequests"), requests);
        checkRequests(2, property("runningNotifyRequests"), requests);
        checkRequests(2, property("inErrorNotifyRequests"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(1, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(6, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(1, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(0, "notifyRequests", sessionStepProperties);
        checkKey(0, "inErrorNotifyRequests", sessionStepProperties);
        checkKey(0, "runningNotifyRequests", sessionStepProperties);
    }

    private void createOneRequestWithError() throws InterruptedException {

        prepareCreationTestData(false, 1, true, true, false);
        List<FeatureUniformResourceName> urn = featureRepo.findAll()
                                                          .stream()
                                                          .map(FeatureEntity::getUrn)
                                                          .collect(Collectors.toList());
        featureNotificationService.registerRequests(prepareNotificationRequests(urn));
        mockNotificationError();
        waitForSate(notificationRequestRepo, RequestState.ERROR, 1, 20);

        // Compute Session step
        computeSessionStep(8, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(6, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(2, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(1, property("referencedProducts"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(1, property("notifyRequests"), requests);
        checkRequests(2, property("runningNotifyRequests"), requests);
        checkRequests(1, property("inErrorNotifyRequests"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(1, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(6, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(1, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "notifyRequests", sessionStepProperties);
        checkKey(1, "inErrorNotifyRequests", sessionStepProperties);
        checkKey(0, "runningNotifyRequests", sessionStepProperties);
    }

}
