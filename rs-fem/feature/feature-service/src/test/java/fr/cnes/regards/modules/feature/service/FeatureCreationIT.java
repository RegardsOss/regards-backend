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

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.IUrnVersionByProvider;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.feature.service.request.IFeatureRequestService;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@TestPropertySource(properties = {"spring.jpa.properties.hibernate.default_schema=feature_version", "regards.amqp.enabled=true"},
        locations = {"classpath:regards_perf.properties", "classpath:batch.properties", "classpath:metrics.properties"})
@ActiveProfiles(value = {"testAmqp", "noscheduler", "nohandler"})
public class FeatureCreationIT extends AbstractFeatureMultitenantServiceTest {

    @SpyBean
    private IPublisher publisherSpy;

    @Captor
    private ArgumentCaptor<List<NotificationRequestEvent>> recordsCaptor;

    @Autowired
    private IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepo;
    @Autowired
    private IFeatureNotificationService featureNotificationService;
    @Autowired
    private IFeatureRequestService featureRequestService;

    /**
     * Test creation of properties.getMaxBulkSize() features Check if
     * {@link FeatureCreationRequest} and {@link FeatureEntity}are stored in
     * database then at the end of the job test if all
     * {@link FeatureCreationRequest} are deleted
     */
    @Test
    public void testFeatureCreation() throws InterruptedException {

        int maxBulkSize = properties.getMaxBulkSize();

        // mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisherSpy).publish(Mockito.any(NotificationRequestEvent.class));


        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(maxBulkSize, true);
        // clear file to test notifications without files
        events.forEach(request -> request.getFeature().getFiles().clear());
        this.featureCreationService.registerRequests(events);

        assertEquals(maxBulkSize, this.featureCreationRequestRepo.count());

        this.featureCreationService.scheduleRequests();

        int cpt = 0;
        long featureNumberInDatabase;
        do {
            featureNumberInDatabase = this.featureRepo.count();
            Thread.sleep(1000);
            cpt++;
        } while ((cpt < 100) && (featureNumberInDatabase != maxBulkSize));

        assertEquals(maxBulkSize, this.featureRepo.count());

        // in that case all features hasn't been saved
        if (cpt == 100) {
            fail("Doesn't have all features at the end of time");
        }

        if (initDefaultNotificationSettings()) {
            testNotification();
        }

        events.clear();
        // lets add one feature which is the same as the first to test versioning code
        events = super.initFeatureCreationRequestEvent(1, false);
        // clear file to test notifications without files
        events.forEach(request -> request.getFeature().getFiles().clear());
        this.featureCreationService.registerRequests(events);

        assertEquals(1, this.featureCreationRequestRepo.count());

        this.featureCreationService.scheduleRequests();

        cpt = 0;
        do {
            featureNumberInDatabase = this.featureRepo.count();
            Thread.sleep(1000);
            cpt++;
        } while ((cpt < 100) && (featureNumberInDatabase != (maxBulkSize + 1)));

        assertEquals(maxBulkSize + 1, this.featureRepo.count());

        // in that case all features hasn't been saved
        if (cpt == 100) {
            fail("Doesn't have all features at the end of time");
        }

        // id0 come from super.init
        List<IUrnVersionByProvider> urnsForId1 = featureRepo
                .findByProviderIdInOrderByVersionDesc(Lists.newArrayList("id0"));
        Assert.assertTrue(featureRepo.findByUrn(urnsForId1.get(0).getUrn()).getFeature().isLast());
        Assert.assertFalse(featureRepo.findByUrn(urnsForId1.get(1).getUrn()).getFeature().isLast());

        List<FeatureCreationRequest> requests = featureCreationRequestRepo.findAll();
        Assert.assertFalse(requests.isEmpty());
        Assert.assertTrue("All feature creation request should have urn and feature entity set to ensure proper notification processing",
                          requests.stream().allMatch(fcr -> (fcr.getUrn() != null) && (fcr.getFeatureEntity() != null)));
    }

    @Test
    public void testRetrieveRequests() throws InterruptedException {
        int nbValid = 20;
        // Register valid requests
        OffsetDateTime start = OffsetDateTime.now();
        List<FeatureCreationRequestEvent> events = initFeatureCreationRequestEvent(nbValid, true);
        this.featureCreationService.registerRequests(events);

        RequestsPage<FeatureRequestDTO> results = this.featureRequestService
                .findAll(FeatureRequestTypeEnum.CREATION, FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED), PageRequest.of(0, 100));
        Assert.assertEquals(nbValid, results.getContent().size());
        Assert.assertEquals(nbValid, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.CREATION,
                                                     FeatureRequestsSelectionDTO.build().withState(RequestState.ERROR),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.CREATION,
                                                     FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED).withProviderId(events.get(0).getFeature().getId()),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(1, results.getContent().size());
        Assert.assertEquals(1, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.CREATION,
                                                     FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED)
                                                             .withProviderId(events.get(0).getFeature().getId())
                                                             .withStart(OffsetDateTime.now().plusSeconds(5)),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.CREATION,
                                                     FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED)
                                                             .withProviderId(events.get(0).getFeature().getId()).withStart(start)
                                                             .withEnd(OffsetDateTime.now().plusSeconds(5)),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(1, results.getContent().size());
        Assert.assertEquals(1, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());
    }

    @Test
    public void testDeleteRequests() {

        int nbValid = 20;
        // Register valid requests
        List<FeatureCreationRequestEvent> events = initFeatureCreationRequestEvent(nbValid, true);
        this.featureCreationService.registerRequests(events);

        // Try delete all requests.
        RequestHandledResponse response = this.featureCreationService.deleteRequests(FeatureRequestsSelectionDTO.build());
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as requests are not in ERROR state", 0, response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as request are not in ERROR state", 0, response.getTotalRequested());

        response = this.featureCreationService.deleteRequests(FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as selection set on GRANTED Requests", 0, response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as selection set on GRANTED Requests", 0, response.getTotalRequested());
    }

    @Test
    public void testRetryRequests() {

        int nbValid = 20;
        // Register valid requests
        List<FeatureCreationRequestEvent> events = initFeatureCreationRequestEvent(nbValid, true);
        this.featureCreationService.registerRequests(events);

        // Try delete all requests.
        RequestHandledResponse response = this.featureCreationService.retryRequests(FeatureRequestsSelectionDTO.build());
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests retried as requests are not in ERROR state", 0, response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to retry as request are not in ERROR state", 0, response.getTotalRequested());

        response = this.featureCreationService.retryRequests(FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests retryed as selection set on GRANTED Requests", 0, response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to retry as selection set on GRANTED Requests", 0, response.getTotalRequested());
    }

    private void testNotification() {
        // now that feature are created, lets do logic to get to notification of the creation
        // lets check that for feature created, there is a request in step LOCAL_TO_BE_NOTIFIED
        Page<AbstractFeatureRequest> requestsToSend = abstractFeatureRequestRepo.findByStepAndRequestDateLessThanEqual(
                FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                OffsetDateTime.now(),
                PageRequest.of(0, properties.getMaxBulkSize(), Sort.by(Sort.Order.asc("priority"), Sort.Order.asc("requestDate")))
        );
        Assert.assertEquals("There should be at least max bulk size request in step LOCAL_TO_BE_NOTIFIED", properties.getMaxBulkSize().intValue(), requestsToSend.getSize());
        Assert.assertEquals("There should be only one page of request in step LOCAL_TO_BE_NOTIFIED", 1, requestsToSend.getTotalPages());
        // now that we are sure only right requests are in step LOCAL_TO_BE_NOTIFIED, lets ask them to be sent (method called by task scheduler)
        featureNotificationService.sendToNotifier();

        // lets capture events sent and check that there is properties.getMaxBulkSize NotificationActionEvent
        Mockito.verify(publisherSpy, Mockito.atLeastOnce()).publish(recordsCaptor.capture());
        // That captor also records SessionStepEvent published, for some reason, hence the need to filter
        List<List<NotificationRequestEvent>> value = recordsCaptor.getAllValues().stream()
                .filter(list -> list.get(0) instanceof NotificationRequestEvent)
                .collect(Collectors.toList());
        assertEquals(1, value.size());
        assertEquals(properties.getMaxBulkSize().intValue(), value.get(0).size());

        //simulate that notification has been handle with success
        featureNotificationService.handleNotificationSuccess(requestsToSend.toSet());
    }

    @Test
    public void testFeatureCreationWithDuplicateRequestId() throws InterruptedException {

        // mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisherSpy).publish(Mockito.any(NotificationRequestEvent.class));

        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(properties.getMaxBulkSize(), true);

        // clear file to test notifications without files and put the same request id
        events.forEach(request -> {
            request.setRequestId("1");
            request.getFeature().getFiles().clear();
        });
        this.featureCreationService.registerRequests(events);

        assertEquals(1, this.featureCreationRequestRepo.count());

        this.featureCreationService.scheduleRequests();

        int cpt = 0;
        long featureNumberInDatabase;
        do {
            featureNumberInDatabase = this.featureRepo.count();
            Thread.sleep(1000);
            cpt++;
        } while ((cpt < 100) && (featureNumberInDatabase != 1));

        // only 1 feature should be created
        assertEquals(1, this.featureRepo.count());
    }

    /**
     * Test creation of properties.getMaxBulkSize() features one will be invalid test that this
     * one will not be sored in database
     *
     */
    @Test
    public void testFeatureCreationWithInvalidFeature() throws InterruptedException {

        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(properties.getMaxBulkSize(), true);

        Feature f = events.get(0).getFeature();
        f.setEntityType(null);

        this.featureCreationService.registerRequests(events);

        assertEquals(properties.getMaxBulkSize() - 1, this.featureCreationRequestRepo.count());

        featureCreationService.scheduleRequests();

        int cpt = 0;
        long featureNumberInDatabase;
        do {
            featureNumberInDatabase = this.featureRepo.count();
            Thread.sleep(1000);
            cpt++;
        } while ((cpt < 100) && (featureNumberInDatabase != (properties.getMaxBulkSize() - 1)));

        assertEquals(properties.getMaxBulkSize() - 1, this.featureRepo.count());

        // in that case all features hasn't been saved
        if (cpt == 100) {
            fail("Doesn't have all features at the end of time");
        }
    }

    @Test
    public void testRegisterScheduleProcess() {
        List<Feature> features = new ArrayList<>();
        String model = mockModelClient("feature_model_01.xml", cps, factory, this.getDefaultTenant(), modelAttrAssocClientMock);
        for (int i = 0; i < properties.getMaxBulkSize(); i++) {
            Feature toAdd = Feature.build("id" + i, "owner", null, IGeometry.point(IGeometry.position(10.0, 20.0)), EntityType.DATA, model);
            features.add(toAdd);
            toAdd.addProperty(IProperty.buildString("data_type", "TYPE01"));
            toAdd.addProperty(IProperty.buildObject("file_characterization", IProperty.buildBoolean("valid", Boolean.TRUE)));
        }

        StorageMetadata.build("id ");
        FeatureCreationCollection collection = FeatureCreationCollection.build(
                "owner",
                FeatureCreationSessionMetadata.build("owner", "session", PriorityLevel.NORMAL, false, StorageMetadata.build("id ")),
                features);
        RequestInfo<String> infos = this.featureCreationService.registerRequests(collection);

        assertEquals(properties.getMaxBulkSize().intValue(), this.featureCreationRequestRepo.count());
        assertEquals(properties.getMaxBulkSize().intValue(), infos.getGranted().size());
        assertEquals(0, infos.getDenied().size());
    }

    @Test
    public void testRegisterScheduleProcessWithErrors() {
        List<Feature> features = new ArrayList<>();
        for (int i = 0; i < properties.getMaxBulkSize(); i++) {
            features.add(Feature.build("id" + i, "owner", null, IGeometry.point(IGeometry.position(10.0, 20.0)), null, "model"));
        }

        StorageMetadata.build("id ");
        FeatureCreationCollection collection = FeatureCreationCollection.build(
                "owner",
                FeatureCreationSessionMetadata.build("owner", "session", PriorityLevel.NORMAL, false, StorageMetadata.build("id ")),
                features);
        RequestInfo<String> infos = this.featureCreationService.registerRequests(collection);

        assertEquals(0, infos.getGranted().size());
        assertEquals(properties.getMaxBulkSize() * 2, infos.getDenied().size());
    }

    /**
     * Test priority level for feature creation we will schedule properties.getMaxBulkSize() {@link FeatureCreationRequestEvent}
     * with priority set to average plus properties.getMaxBulkSize() /2 {@link FeatureCreationRequestEvent} with {@link PriorityLevel}
     * to average
     */
    @Test
    public void testFeaturePriority() throws InterruptedException {

        List<FeatureCreationRequestEvent> events =
                initFeatureCreationRequestEvent(properties.getMaxBulkSize() + (properties.getMaxBulkSize() / 2), true);

        // we will set all priority to normal except for the (properties.getMaxBulkSize() / 2) last events
        for (int i = properties.getMaxBulkSize(); i < (properties.getMaxBulkSize() + (properties.getMaxBulkSize() / 2)); i++) {
            events.get(i).getMetadata().setPriority(PriorityLevel.HIGH);
        }

        this.featureCreationService.registerRequests(events);

        assertEquals(properties.getMaxBulkSize() + (properties.getMaxBulkSize() / 2), this.featureCreationRequestRepo.count());

        this.featureCreationService.scheduleRequests();

        // check that half of the FeatureCreationRequest with step to LOCAL_SCHEDULED
        // have their priority to HIGH and half to AVERAGE
        Page<FeatureCreationRequest> scheduled = featureCreationRequestRepo.findByStep(FeatureRequestStep.LOCAL_SCHEDULED, PageRequest.of(0, properties.getMaxBulkSize()));
        int highPriorityNumber = 0;
        int otherPriorityNumber = 0;
        for (FeatureCreationRequest request : scheduled) {
            if (request.getPriority().equals(PriorityLevel.HIGH)) {
                highPriorityNumber++;
            } else {
                otherPriorityNumber++;
            }
        }

        // half of scheduled should be with priority HIGH
        assertEquals(properties.getMaxBulkSize().intValue(), highPriorityNumber + otherPriorityNumber);
        assertEquals(highPriorityNumber, otherPriorityNumber);

        // wait for first job to be done
        int cpt = 0;
        long featureNumberInDatabase;
        do {
            featureNumberInDatabase = this.featureRepo.count();
            Thread.sleep(1000);
            cpt++;
        } while ((cpt < 100) && (featureNumberInDatabase != properties.getMaxBulkSize()));

        assertEquals(properties.getMaxBulkSize().intValue(), this.featureRepo.count());

        // in that case all features hasn't been saved
        if (cpt == 100) {
            fail("Doesn't have all features at the end of time");
        }
    }

    @Test
    public void testSessionNotifierWithNotification() throws InterruptedException {

        int requestCount = 10;
        prepareCreationTestData(true, requestCount, true, true);
        waitCreationRequestDeletion(0, 20000);

        // Compute Session step
        computeSessionStep();

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        Assertions.assertEquals(requestCount * 4, requests.size());
        checkRequests(requestCount * 3, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(requestCount, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(requestCount, property("referencingRequests"), requests);
        checkRequests(requestCount, property("referencedProducts"), requests);
        checkRequests(requestCount * 2, property("runningReferencingRequests"), requests);
        checkRequests(requestCount, inputRelated(), requests);
        checkRequests(requestCount, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(requestCount, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(3, sessionStepProperties.size());
        checkKey(requestCount, "referencingRequests", sessionStepProperties);
        checkKey(requestCount, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
    }

    @Test
    public void testSessionNotifierWithoutNotification() throws InterruptedException {

        setNotificationSetting(false);

        int requestCount = 10;
        prepareCreationTestData(true, requestCount, false, true);
        waitCreationRequestDeletion(0, 20000);

        // Compute Session step
        computeSessionStep();

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        Assertions.assertEquals(requestCount * 4, requests.size());
        checkRequests(requestCount * 3, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(requestCount, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(requestCount, property("referencingRequests"), requests);
        checkRequests(requestCount, property("referencedProducts"), requests);
        checkRequests(requestCount * 2, property("runningReferencingRequests"), requests);
        checkRequests(requestCount, inputRelated(), requests);
        checkRequests(requestCount, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(requestCount, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(3, sessionStepProperties.size());
        checkKey(requestCount, "referencingRequests", sessionStepProperties);
        checkKey(requestCount, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
    }

    @Test
    public void testSessionNotifierWithRetryOnFileError() throws InterruptedException {

        // Init requests
        int requestCount = 10;
        createRequestsWithOneFileError(requestCount);

        // Retry request in error
        featureCreationService.retryRequests(new FeatureRequestsSelectionDTO());
        featureCreationService.scheduleRequests();
        TimeUnit.SECONDS.sleep(5);
        featureRequestService.handleStorageSuccess(featureCreationRequestRepo
                                                           .findByStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED, PageRequest.of(0, 1))
                                                           .stream()
                                                           .map(AbstractFeatureRequest::getGroupId)
                                                           .collect(Collectors.toSet()));
        mockNotificationSuccess();
        // Give it some time
        waitCreationRequestDeletion(0, 20000);

        // Compute Session step
        computeSessionStep();

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        Assertions.assertEquals((requestCount * 4) + 4, requests.size());
        checkRequests((requestCount * 3) + 2, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(requestCount + 2, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(requestCount, property("referencingRequests"), requests);
        checkRequests(requestCount, property("referencedProducts"), requests);
        checkRequests((requestCount + 1) * 2, property("runningReferencingRequests"), requests);
        checkRequests(2, property("inErrorReferencingRequests"), requests);
        checkRequests(requestCount, inputRelated(), requests);
        checkRequests(requestCount, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(requestCount, sessionStep.getInputRelated());
        Assertions.assertEquals(requestCount, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(4, sessionStepProperties.size());
        checkKey(requestCount, "referencingRequests", sessionStepProperties);
        checkKey(requestCount, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(0, "inErrorReferencingRequests", sessionStepProperties);
    }

    @Test
    public void testSessionNotifierWithDeleteOnFileError() throws InterruptedException {

        // Init requests
        int requestCount = 10;
        createRequestsWithOneFileError(requestCount);

        // Delete request in error
        featureCreationService.deleteRequests(new FeatureRequestsSelectionDTO());
        waitCreationRequestDeletion(0, 20000);

        // Compute Session step
        computeSessionStep();

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        Assertions.assertEquals((requestCount * 4) + 2, requests.size());
        checkRequests(requestCount * 3, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(requestCount + 2, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(requestCount + 1, property("referencingRequests"), requests);
        checkRequests(requestCount - 1, property("referencedProducts"), requests);
        checkRequests(requestCount * 2, property("runningReferencingRequests"), requests);
        checkRequests(2, property("inErrorReferencingRequests"), requests);
        checkRequests(requestCount + 1, inputRelated(), requests);
        checkRequests(requestCount - 1, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(requestCount - 1, sessionStep.getInputRelated());
        Assertions.assertEquals(requestCount - 1, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(4, sessionStepProperties.size());
        checkKey(requestCount - 1, "referencingRequests", sessionStepProperties);
        checkKey(requestCount - 1, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(0, "inErrorReferencingRequests", sessionStepProperties);
    }

    @Test
    public void testSessionNotifierWithRetryOnNotificationError() throws InterruptedException {

        // Init requests
        createOneRequestsWithNotificationError();

        // Retry request in error
        featureCreationService.retryRequests(new FeatureRequestsSelectionDTO());
        featureCreationService.scheduleRequests();
        TimeUnit.SECONDS.sleep(5);
        mockNotificationSuccess();
        waitCreationRequestDeletion(0, 20000);

        // Compute Session step
        computeSessionStep();

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        Assertions.assertEquals(8, requests.size());
        checkRequests(5, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(3, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(1, property("referencedProducts"), requests);
        checkRequests(4, property("runningReferencingRequests"), requests);
        checkRequests(2, property("inErrorReferencingRequests"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(1, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        Assertions.assertEquals(1, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(4, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(1, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(0, "inErrorReferencingRequests", sessionStepProperties);
    }

    @Test
    public void testSessionNotifierWithDeleteOnNotificationError() throws InterruptedException {

        // Init requests
        createOneRequestsWithNotificationError();

        // Delete request in error
        featureCreationService.deleteRequests(new FeatureRequestsSelectionDTO());
        waitCreationRequestDeletion(0, 20000);

        // Compute Session step
        computeSessionStep();

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        Assertions.assertEquals(6, requests.size());
        checkRequests(3, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(3, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(2, property("referencingRequests"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(2, property("inErrorReferencingRequests"), requests);
        checkRequests(2, inputRelated(), requests);
        checkRequests(0, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(0, sessionStep.getInputRelated());
        Assertions.assertEquals(0, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(3, sessionStepProperties.size());
        checkKey(0, "referencingRequests", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(0, "inErrorReferencingRequests", sessionStepProperties);
    }

    private void createRequestsWithOneFileError(int requestCount) throws InterruptedException {

        initData(requestCount);
        mockNotificationSuccess();

        Pageable pageToRequest = PageRequest.of(0, requestCount);
        Page<FeatureCreationRequest> fcrPage = featureCreationRequestRepo.findByStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED, pageToRequest);
        List<String> requestIds = fcrPage.stream().map(AbstractFeatureRequest::getGroupId).collect(Collectors.toList());
        String errorId = requestIds.remove(0);
        RequestResultInfoDTO requestResultInfoDTO = new RequestResultInfoDTO();
        ReflectionTestUtils.setField(requestResultInfoDTO, "groupId", errorId);
        featureRequestService.handleStorageError(Sets.newSet(requestResultInfoDTO));
        featureRequestService.handleStorageSuccess(new HashSet<>(requestIds));
        mockNotificationSuccess();
        // Give it some time
        waitCreationRequestDeletion(1, 20000);
        waitForStep(featureCreationRequestRepo, FeatureRequestStep.REMOTE_STORAGE_ERROR, 1, 20);

        // Compute Session step
        computeSessionStep();

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        Assertions.assertEquals(requestCount * 4, requests.size());
        checkRequests(requestCount * 3, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(requestCount, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(requestCount, property("referencingRequests"), requests);
        checkRequests(requestCount - 1, property("referencedProducts"), requests);
        checkRequests(requestCount * 2, property("runningReferencingRequests"), requests);
        checkRequests(1, property("inErrorReferencingRequests"), requests);
        checkRequests(requestCount, inputRelated(), requests);
        checkRequests(requestCount - 1, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(requestCount, sessionStep.getInputRelated());
        Assertions.assertEquals(requestCount - 1, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(4, sessionStepProperties.size());
        checkKey(requestCount, "referencingRequests", sessionStepProperties);
        checkKey(requestCount - 1, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "inErrorReferencingRequests", sessionStepProperties);
    }

    private void createOneRequestsWithNotificationError() throws InterruptedException {

        prepareCreationTestData(false, 1, false, true);
        mockNotificationError();
        waitCreationRequestDeletion(1, 20000);
        waitForStep(featureCreationRequestRepo, FeatureRequestStep.REMOTE_NOTIFICATION_ERROR, 1, 20);

        // Compute Session step
        computeSessionStep();

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        Assertions.assertEquals(4, requests.size());
        checkRequests(3, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(1, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(1, property("inErrorReferencingRequests"), requests);
        checkRequests(1, inputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(3, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "inErrorReferencingRequests", sessionStepProperties);
    }

}
