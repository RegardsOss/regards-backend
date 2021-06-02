/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
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
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link FeatureNotificationRequestEvent} publishing
 * @author Kevin Marchois
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=feature_notif", "regards.amqp.enabled=true" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties",
                "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FeatureNotificationServiceIT extends AbstractFeatureMultitenantServiceTest {

    @Autowired
    private IFeatureNotificationService notificationService;

    @SpyBean
    private IPublisher publisher;

    @Autowired
    private Gson gson;

    @Captor
    private ArgumentCaptor<List<NotificationRequestEvent>> recordsCaptor;

    @Test
    public void testNotification() {

        // mock the publish method to not broke other tests
        Mockito.doNothing().when(publisher).publish(Mockito.any(NotificationRequestEvent.class));

        // use it only to initialize Feature
        List<FeatureCreationRequestEvent> list = initFeatureCreationRequestEvent(2, true);
        list.get(0).getFeature().setUrn(FeatureUniformResourceName.pseudoRandomUrn(FeatureIdentifier.FEATURE,
                                                                                   EntityType.DATA, "tenant", 1));
        list.get(1).getFeature().setUrn(FeatureUniformResourceName.pseudoRandomUrn(FeatureIdentifier.FEATURE,
                                                                                   EntityType.DATA, "tenant", 1));
        FeatureEntity createdEntity = FeatureEntity.build("moi", "session", list.get(0).getFeature(), null,
                                                          list.get(0).getFeature().getModel());
        FeatureEntity updatedEntity = FeatureEntity.build("moi", "session", list.get(1).getFeature(), null,
                                                          list.get(1).getFeature().getModel());
        updatedEntity.setLastUpdate(OffsetDateTime.now().plusSeconds(1));

        createdEntity.setUrn(list.get(0).getFeature().getUrn());
        updatedEntity.setUrn(list.get(1).getFeature().getUrn());

        // to skip creation and update process
        this.featureRepo.save(createdEntity);
        this.featureRepo.save(updatedEntity);

        this.publisher
                .publish(FeatureNotificationRequestEvent.build("notifier", createdEntity.getUrn(), PriorityLevel.LOW));
        this.publisher
                .publish(FeatureNotificationRequestEvent.build("notifier", updatedEntity.getUrn(), PriorityLevel.LOW));

        this.waitRequest(notificationRequestRepo, 2, 30000);
        assertEquals(2, notificationService.sendToNotifier());
        //simulate that notification has been handle with success
        Page<AbstractFeatureRequest> requestsToSend = abstractFeatureRequestRepo
                .findByStepAndRequestDateLessThanEqual(FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED,
                                                       OffsetDateTime.now().plusDays(1),
                                                       PageRequest.of(0, 2, Sort.by(Sort.Order.asc("priority"),
                                                                                    Sort.Order.asc("requestDate"))));
        featureNotificationService.handleNotificationSuccess(requestsToSend.toSet());
        this.waitRequest(notificationRequestRepo, 0, 30000);

        Mockito.verify(publisher).publish(recordsCaptor.capture());
        // the first publish message to be intercepted must be the creation of createdEntity
        assertEquals(gson.toJson(new CreateNotificationRequestEventVisitor.NotificationActionEventMetadata(
                FeatureManagementAction.NOTIFIED)), recordsCaptor.getValue().get(0).getMetadata().toString());
        assertEquals(gson.toJson(createdEntity.getFeature()), recordsCaptor.getValue().get(0).getPayload().toString());
        // the second message is the update of updatedEntity
        assertEquals(gson.toJson(new CreateNotificationRequestEventVisitor.NotificationActionEventMetadata(
                FeatureManagementAction.NOTIFIED)), recordsCaptor.getValue().get(1).getMetadata().toString());
        assertEquals(gson.toJson(updatedEntity.getFeature()), recordsCaptor.getValue().get(1).getPayload().toString());

    }

    @Test
    public void testRetrieveRequests() throws InterruptedException {
        int nbValid = 20;
        OffsetDateTime start = OffsetDateTime.now();
        // Create features
        prepareCreationTestData(false, nbValid, true, true);
        RequestsPage<FeatureRequestDTO> results = this.featureRequestService
                .findAll(FeatureRequestTypeEnum.NOTIFICATION, FeatureRequestsSelectionDTO.build(),
                         PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        // Notify them
        List<FeatureUniformResourceName> urns = this.featureRepo.findAll().stream().map(f -> f.getUrn())
                .collect(Collectors.toList());
        this.featureNotificationService.registerRequests(prepareNotificationRequests(urns));

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.NOTIFICATION,
                                                     FeatureRequestsSelectionDTO.build(), PageRequest.of(0, 100));
        Assert.assertEquals(nbValid, results.getContent().size());
        Assert.assertEquals(nbValid, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.NOTIFICATION,
                                                     FeatureRequestsSelectionDTO.build().withState(RequestState.ERROR),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.NOTIFICATION,
                                                     FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED)
                                                             .withStart(OffsetDateTime.now().plusSeconds(5)),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.NOTIFICATION,
                                                     FeatureRequestsSelectionDTO.build().withStart(start)
                                                             .withEnd(OffsetDateTime.now().plusSeconds(5)),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(nbValid, results.getContent().size());
        Assert.assertEquals(nbValid, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());
    }

    @Test
    public void testDeleteRequests() throws InterruptedException {

        int nbValid = 20;
        // Create features
        prepareCreationTestData(false, nbValid, true, true);
        // Notify them
        List<FeatureUniformResourceName> urns = this.featureRepo.findAll().stream().map(f -> f.getUrn())
                .collect(Collectors.toList());
        Assert.assertTrue(urns.size() > 0);
        int results = this.featureNotificationService.registerRequests(prepareNotificationRequests(urns));
        Assert.assertTrue(results > 0);

        // Try delete all requests.
        RequestHandledResponse response = this.featureRequestService.delete(FeatureRequestTypeEnum.NOTIFICATION,
                                                                            FeatureRequestsSelectionDTO.build());
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as request are not in ERROR state", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as request are not in ERROR state", 0,
                            response.getTotalRequested());

        response = this.featureRequestService
                .delete(FeatureRequestTypeEnum.NOTIFICATION,
                        FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as selection set on GRANTED Requests", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as selection set on GRANTED Requests", 0,
                            response.getTotalRequested());

    }

    @Test
    public void testRetryRequests() throws InterruptedException {

        int nbValid = 20;
        // Create features
        prepareCreationTestData(false, nbValid, true, true);
        // Notify them
        List<FeatureUniformResourceName> urns = this.featureRepo.findAll().stream().map(f -> f.getUrn())
                .collect(Collectors.toList());
        Assert.assertTrue(urns.size() > 0);
        int results = this.featureNotificationService.registerRequests(prepareNotificationRequests(urns));
        Assert.assertTrue(results > 0);

        // Try delete all requests.
        RequestHandledResponse response = this.featureRequestService.retry(FeatureRequestTypeEnum.NOTIFICATION,
                                                                           FeatureRequestsSelectionDTO.build());
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests retryed as request are not in ERROR state", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to retry as request are not in ERROR state", 0,
                            response.getTotalRequested());

        response = this.featureRequestService
                .retry(FeatureRequestTypeEnum.NOTIFICATION,
                       FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests retryed as selection set on GRANTED Requests", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to retry as selection set on GRANTED Requests", 0,
                            response.getTotalRequested());

    }

    @Test
    public void testSessionNotifier() throws InterruptedException {

        prepareCreationTestData(false, 1, true, true);
        List<FeatureUniformResourceName> urn = featureRepo.findAll().stream().map(FeatureEntity::getUrn).collect(Collectors.toList());
        featureNotificationService.registerRequests(prepareNotificationRequests(urn));
        mockNotificationSuccess();
        waitRequest(notificationRequestRepo, 0, 20000);

        // Compute Session step
        computeSessionStep();

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        Assertions.assertEquals(8, requests.size());
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
        featureNotificationService.retryRequests(new FeatureRequestsSelectionDTO());
        mockNotificationSuccess();
        waitRequest(notificationRequestRepo, 0, 20000);

        // Compute Session step
        computeSessionStep();

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        Assertions.assertEquals(12, requests.size());
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
        featureNotificationService.deleteRequests(new FeatureRequestsSelectionDTO());
        waitRequest(notificationRequestRepo, 0, 20000);

        // Compute Session step
        computeSessionStep();

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        Assertions.assertEquals(10, requests.size());
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

        prepareCreationTestData(false, 1, true, true);
        List<FeatureUniformResourceName> urn = featureRepo.findAll().stream().map(FeatureEntity::getUrn).collect(Collectors.toList());
        featureNotificationService.registerRequests(prepareNotificationRequests(urn));
        mockNotificationError();
        waitForSate(notificationRequestRepo, RequestState.ERROR, 1, 20);

        // Compute Session step
        computeSessionStep();

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        Assertions.assertEquals(8, requests.size());
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
