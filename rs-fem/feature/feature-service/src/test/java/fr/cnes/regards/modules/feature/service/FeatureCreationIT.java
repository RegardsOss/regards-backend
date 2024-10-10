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

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.IUrnVersionByProvider;
import fr.cnes.regards.modules.feature.domain.request.*;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.request.IFeatureRequestService;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_version",
                                   "regards.amqp.enabled=true",
                                   "regards.feature.max.bulk.size=50" },
                    locations = { "classpath:regards_perf.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "noFemHandler" })
public class FeatureCreationIT extends AbstractFeatureMultitenantServiceIT {

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
    public void testFeatureCreation() {

        int maxBulkSize = properties.getMaxBulkSize();

        // mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisher).publish(Mockito.any(NotificationRequestEvent.class));

        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(maxBulkSize, true, false);
        // clear file to test notifications without files
        events.forEach(request -> request.getFeature().getFiles().clear());
        featureCreationService.registerRequests(events);

        assertEquals(maxBulkSize, this.featureCreationRequestRepo.count());

        featureCreationService.scheduleRequests();

        waitForFeatures(maxBulkSize);

        if (initDefaultNotificationSettings()) {
            testNotification();
        }

        events.clear();
        // lets add one feature which is the same as the first to test versioning code
        events = super.initFeatureCreationRequestEvent(1, false, false);
        // clear file to test notifications without files
        events.forEach(request -> request.getFeature().getFiles().clear());
        this.featureCreationService.registerRequests(events);

        assertEquals(1, this.featureCreationRequestRepo.count());

        this.featureCreationService.scheduleRequests();

        try {
            Awaitility.await().atMost(Durations.ONE_MINUTE).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return this.featureRepo.count() == maxBulkSize + 1;
            });
        } catch (ConditionTimeoutException e) {
            Assert.assertEquals("Invalid number of feature", maxBulkSize + 1, this.featureRepo.count());
        }

        // id0 come from super.init
        try {
            Awaitility.await().atMost(Durations.TEN_SECONDS).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return featureRepo.findByProviderIdInOrderByVersionDesc(Lists.newArrayList("id0")).size() == 2;
            });
        } catch (ConditionTimeoutException e) {
            Assert.assertEquals("Invalid number of version of same feature",
                                2,
                                featureRepo.findByProviderIdInOrderByVersionDesc(Lists.newArrayList("id0")).size());
        }
        List<IUrnVersionByProvider> urnsForId1 = featureRepo.findByProviderIdInOrderByVersionDesc(Lists.newArrayList(
            "id0"));
        Assert.assertTrue(featureRepo.findByUrn(urnsForId1.get(0).getUrn()).getFeature().isLast());
        Assert.assertFalse(featureRepo.findByUrn(urnsForId1.get(1).getUrn()).getFeature().isLast());

        List<FeatureCreationRequest> requests = featureCreationRequestRepo.findAll();
        Assert.assertFalse(requests.isEmpty());
        Assert.assertTrue(
            "All feature creation request should have urn and feature entity set to ensure proper notification processing",
            requests.stream().allMatch(fcr -> (fcr.getUrn() != null) && (fcr.getFeatureEntity() != null)));
    }

    @Test
    @Purpose(
        "Check that if files to store in a given feature are not all of the same store mode (reference or storage),"
        + "the request is denied")
    public void testFeatureCreationStorageModeFails() {
        // Init creation request
        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(1, true, false);
        // Add one file with a referenced file (both storage and url are provided in location)
        FeatureFileAttributes attributes = FeatureFileAttributes.build(DataType.RAWDATA,
                                                                       MediaType.APPLICATION_OCTET_STREAM,
                                                                       "fileName",
                                                                       10L,
                                                                       "MD5",
                                                                       RandomChecksumUtils.generateRandomChecksum());
        FeatureFileLocation location = FeatureFileLocation.build("file:///test/file.txt", "somewhere");
        events.get(0).getFeature().getFiles().add(FeatureFile.build(attributes, location));

        // Add one file with a store file (only url is provided in location)
        FeatureFileAttributes attributes2 = FeatureFileAttributes.build(DataType.RAWDATA,
                                                                        MediaType.APPLICATION_OCTET_STREAM,
                                                                        "fileName2",
                                                                        10L,
                                                                        "MD5",
                                                                        RandomChecksumUtils.generateRandomChecksum());
        FeatureFileLocation location2 = FeatureFileLocation.build("file:///dir/file.txt");
        events.get(0).getFeature().getFiles().add(FeatureFile.build(attributes2, location2));
        events.get(0).getMetadata().setStorages(Lists.newArrayList(StorageMetadata.build("elsewhere")));

        RequestInfo<String> info = this.featureCreationService.registerRequests(events);
        Assert.assertEquals(0L, info.getGranted().size());
        Assert.assertEquals(1L, info.getDenied().size());
    }

    @Test
    @Purpose("Check feature creation with files to store with storage microservice")
    public void testCreationRequestWithFileToStore() {
        // Init creation request
        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(1, true, false);
        // Remove files to add a custom one
        events.get(0).getFeature().getFiles().clear();
        // Add metadata to provided storage location
        events.get(0).getMetadata().setStorages(Lists.newArrayList(StorageMetadata.build("somewhere")));
        // Add one file with a store file (only url is provided in location)
        FeatureFileAttributes attributes = FeatureFileAttributes.build(DataType.RAWDATA,
                                                                       MediaType.APPLICATION_OCTET_STREAM,
                                                                       "fileName2",
                                                                       10L,
                                                                       "MD5",
                                                                       RandomChecksumUtils.generateRandomChecksum());
        FeatureFileLocation location = FeatureFileLocation.build("file://dir/file.txt");
        events.get(0).getFeature().getFiles().add(FeatureFile.build(attributes, location));

        // Submit request
        RequestInfo<String> info = this.featureCreationService.registerRequests(events);
        Assert.assertEquals(1L, info.getGranted().size());
        Assert.assertEquals(0L, info.getDenied().size());

        // schedule request
        Assert.assertEquals(1, this.featureCreationService.scheduleRequests());
        waitFeature(1L, null, 10_000);

        // Feature should be created and request set in remote storage requested step.
        Assert.assertEquals(1L, featureRepo.count());
        Assert.assertEquals(1L, featureCreationRequestRepo.count());
        FeatureEntity feature = featureRepo.findAll().get(0);
        // Check feature in db is valid and contains not stored information about file location
        Assert.assertEquals(1L, feature.getFeature().getFiles().size());
        Assert.assertEquals(1L, feature.getFeature().getFiles().get(0).getLocations().size());
        FeatureFileLocation firstFeatureFirstFileLocation = feature.getFeature()
                                                                   .getFiles()
                                                                   .get(0)
                                                                   .getLocations()
                                                                   .stream()
                                                                   .findFirst()
                                                                   .orElseThrow();
        Assert.assertNull(firstFeatureFirstFileLocation.getStorage());
        Assert.assertEquals(location.getUrl(), firstFeatureFirstFileLocation.getUrl());

        // Simulate an update request on the newly created (but not terminated) feature
        List<FeatureUpdateRequestEvent> updates = prepareUpdateRequests(Lists.newArrayList(feature.getUrn()));
        RequestInfo<FeatureUniformResourceName> updateInfo = this.featureUpdateService.registerRequests(updates);
        Assert.assertEquals(1L, updateInfo.getGranted().size());
        Assert.assertEquals(0L, updateInfo.getDenied().size());

        // Update request cannot be scheduled as a creation request is still pending
        // Wait for delay before requests can be scheduled.
        long now = System.currentTimeMillis();
        long waitTime = (properties.getDelayBeforeProcessing() + 1) * 1000L;
        Awaitility.await().until(() -> System.currentTimeMillis() - now >= waitTime);
        Assert.assertEquals(0L, this.featureUpdateService.scheduleRequests());

        // Simulate result from storage
        mockStorageHelper.mockFeatureUpdateStorageSuccess();
        feature = featureRepo.findAll().get(0);
        firstFeatureFirstFileLocation = feature.getFeature()
                                               .getFiles()
                                               .get(0)
                                               .getLocations()
                                               .stream()
                                               .findFirst()
                                               .orElseThrow();
        // Check feature file location has been updated to add the storage location and to change url
        Assert.assertEquals(1L, feature.getFeature().getFiles().size());
        Assert.assertEquals(1L, feature.getFeature().getFiles().get(0).getLocations().size());
        Assert.assertNotNull(firstFeatureFirstFileLocation.getStorage());
        Assert.assertNotEquals(location.getUrl(), firstFeatureFirstFileLocation.getUrl());

        FeatureCreationRequest request = featureCreationRequestRepo.findAll().get(0);
        Assert.assertNotNull(request);
        Assert.assertEquals(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED, request.getStep());

        // Update request cannot be scheduled as a creation request is still pending
        Assert.assertEquals(0L, this.featureUpdateService.scheduleRequests());

        // Simulate notification success to allow featureCreationRequest ends and deletion
        mockNotificationSuccess();

        // Now Update request can be scheduled
        Assert.assertEquals(1L, this.featureUpdateService.scheduleRequests());

    }

    @Test
    public void testRetrieveRequests() {
        // Given
        int nbValid = 20;
        // Register valid requests
        OffsetDateTime start = OffsetDateTime.now();
        List<FeatureCreationRequestEvent> events = initFeatureCreationRequestEvent(nbValid, true, false);
        this.featureCreationService.registerRequests(events);
        // When
        RequestsPage<FeatureRequestDTO> results = this.featureRequestService.findAll(FeatureRequestTypeEnum.CREATION,
                                                                                     new SearchFeatureRequestParameters().withStatesIncluded(
                                                                                         List.of(RequestState.GRANTED)),
                                                                                     PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(nbValid, results.getContent().size());
        Assert.assertEquals(nbValid, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());

        // When
        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.CREATION,
                                                     new SearchFeatureRequestParameters().withStatesIncluded(List.of(
                                                         RequestState.ERROR)),
                                                     PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());
        List<String> featureIds = List.of(events.get(0).getFeature().getId());

        // When
        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.CREATION,
                                                     new SearchFeatureRequestParameters().withStatesIncluded(List.of(
                                                                                             RequestState.GRANTED))
                                                                                         .withProviderIdsIncludedStrict(
                                                                                             featureIds),
                                                     PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(1, results.getContent().size());
        Assert.assertEquals(1, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());

        // When
        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.CREATION,
                                                     new SearchFeatureRequestParameters().withStatesIncluded(List.of(
                                                                                             RequestState.GRANTED))
                                                                                         .withProviderIdsIncluded(
                                                                                             featureIds)
                                                                                         .withLastUpdateAfter(
                                                                                             OffsetDateTime.now()
                                                                                                           .plusSeconds(
                                                                                                               5)),
                                                     PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());

        // When
        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.CREATION,
                                                     new SearchFeatureRequestParameters().withStatesIncluded(List.of(
                                                                                             RequestState.GRANTED))
                                                                                         .withProviderIdsIncluded(
                                                                                             featureIds)
                                                                                         .withLastUpdateBefore(
                                                                                             OffsetDateTime.now()
                                                                                                           .plusSeconds(
                                                                                                               5))
                                                                                         .withLastUpdateAfter(start),
                                                     PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(1, results.getContent().size());
        Assert.assertEquals(1, results.getTotalElements());
        Assert.assertEquals(Long.valueOf(0), results.getInfo().getNbErrors());
    }

    @Test
    public void testDeleteRequests() {

        int nbValid = 20;
        // Register valid requests
        List<FeatureCreationRequestEvent> events = initFeatureCreationRequestEvent(nbValid, true, false);
        this.featureCreationService.registerRequests(events);

        // Simulate all requests to scheduled
        this.featureCreationService.findRequests(new SearchFeatureRequestParameters(), PageRequest.of(0, 1000))
                                   .forEach(r -> {
                                       r.setStep(FeatureRequestStep.LOCAL_SCHEDULED);
                                       this.featureCreationRequestRepo.save(r);
                                   });

        // Try delete all requests.
        RequestHandledResponse response = this.featureCreationService.deleteRequests(new SearchFeatureRequestParameters());
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as request are not in ERROR state",
                            0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as request are not in ERROR state",
                            0,
                            response.getTotalRequested());

        response = this.featureCreationService.deleteRequests(new SearchFeatureRequestParameters().withStatesIncluded(
            List.of(RequestState.GRANTED)));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as selection set on GRANTED Requests",
                            0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as selection set on GRANTED Requests",
                            0,
                            response.getTotalRequested());

        // Simulate all requests to error
        this.featureCreationService.findRequests(new SearchFeatureRequestParameters(), PageRequest.of(0, 1000))
                                   .forEach(r -> {
                                       r.setStep(FeatureRequestStep.REMOTE_STORAGE_ERROR);
                                       this.featureCreationRequestRepo.save(r);
                                   });

        response = this.featureCreationService.deleteRequests(new SearchFeatureRequestParameters());
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 20 requests deleted", 20, response.getTotalHandled());
        Assert.assertEquals("There should be 20 requests to delete", 20, response.getTotalRequested());

    }

    @Test
    public void testRetryRequests() {

        int nbValid = 20;
        // Register valid requests
        List<FeatureCreationRequestEvent> events = initFeatureCreationRequestEvent(nbValid, true, false);
        this.featureCreationService.registerRequests(events);

        // Try delete all requests.
        RequestHandledResponse response = this.featureCreationService.retryRequests(new SearchFeatureRequestParameters().withStatesIncluded(
            List.of(RequestState.ERROR)));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests retryed as request are not in ERROR state",
                            0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to retry as request are not in ERROR state",
                            0,
                            response.getTotalRequested());

        response = this.featureCreationService.retryRequests(new SearchFeatureRequestParameters().withStatesIncluded(
            List.of(RequestState.GRANTED)));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 20 requests retryed as selection set on GRANTED Requests",
                            nbValid,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 20 requests to retry as selection set on GRANTED Requests",
                            nbValid,
                            response.getTotalRequested());

    }

    private void testNotification() {
        // now that feature are created, lets do logic to get to notification of the creation
        // lets check that for feature created, there is a request in step LOCAL_TO_BE_NOTIFIED
        Page<AbstractFeatureRequest> requestsToSend = abstractFeatureRequestRepo.findByStepAndRequestDateLessThanEqual(
            FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
            OffsetDateTime.now(),
            PageRequest.of(0,
                           properties.getMaxBulkSize(),
                           Sort.by(Sort.Order.asc("priority"), Sort.Order.asc("requestDate"))));
        Assert.assertEquals("There should be at least max bulk size request in step LOCAL_TO_BE_NOTIFIED",
                            properties.getMaxBulkSize().intValue(),
                            requestsToSend.getSize());
        Assert.assertEquals("There should be only one page of request in step LOCAL_TO_BE_NOTIFIED",
                            1,
                            requestsToSend.getTotalPages());
        // now that we are sure only right requests are in step LOCAL_TO_BE_NOTIFIED, lets ask them to be sent (method called by task scheduler)
        featureNotificationService.sendToNotifier();

        // lets capture events sent and check that there is properties.getMaxBulkSize NotificationActionEvent
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(recordsCaptor.capture());
        // That captor also records SessionStepEvent published, for some reason, hence the need to filter
        List<List<NotificationRequestEvent>> value = recordsCaptor.getAllValues()
                                                                  .stream()
                                                                  .filter(list -> list.get(0) instanceof NotificationRequestEvent)
                                                                  .toList();
        assertEquals(1, value.size());
        assertEquals(properties.getMaxBulkSize().intValue(), value.get(0).size());

        //simulate that notification has been handle with success
        featureNotificationService.handleNotificationSuccess(requestsToSend.toSet());
    }

    @Test
    public void testFeatureCreationWithDuplicateRequestId() {

        // mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisher).publish(Mockito.any(NotificationRequestEvent.class));

        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(properties.getMaxBulkSize(),
                                                                                         true,
                                                                                         false);

        // clear file to test notifications without files and put the same request id
        events.forEach(request -> {
            request.setRequestId("1");
            request.getFeature().getFiles().clear();
        });
        this.featureCreationService.registerRequests(events);

        assertEquals(1, this.featureCreationRequestRepo.count());

        this.featureCreationService.scheduleRequests();

        waitForFeatures(1);
    }

    /**
     * Test creation of properties.getMaxBulkSize() features one will be invalid test that this
     * one will not be sored in database
     */
    @Test
    public void testFeatureCreationWithInvalidFeature() {

        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(properties.getMaxBulkSize(),
                                                                                         true,
                                                                                         false);

        Feature f = events.get(0).getFeature();
        f.setEntityType(null);

        this.featureCreationService.registerRequests(events);

        assertEquals(properties.getMaxBulkSize() - 1, this.featureCreationRequestRepo.count());

        featureCreationService.scheduleRequests();

        waitForFeatures(properties.getMaxBulkSize() - 1);
    }

    @Test
    public void testRegisterScheduleProcess() {
        List<Feature> features = new ArrayList<>();

        for (int i = 0; i < properties.getMaxBulkSize(); i++) {
            Feature toAdd = Feature.build("id" + i,
                                          "owner",
                                          null,
                                          IGeometry.point(IGeometry.position(10.0, 20.0)),
                                          EntityType.DATA,
                                          featureModelName);
            features.add(toAdd);
            toAdd.addProperty(IProperty.buildString("data_type", "TYPE01"));
            toAdd.addProperty(IProperty.buildObject("file_characterization",
                                                    IProperty.buildBoolean("valid", Boolean.TRUE)));
        }

        StorageMetadata.build("id ");
        FeatureCreationCollection collection = FeatureCreationCollection.build("owner",
                                                                               FeatureCreationSessionMetadata.build(
                                                                                   "owner",
                                                                                   "session",
                                                                                   PriorityLevel.NORMAL,
                                                                                   false,
                                                                                   false,
                                                                                   StorageMetadata.build("id ")),
                                                                               features);
        RequestInfo<String> infos = this.featureCreationService.registerRequests(collection);

        assertEquals(properties.getMaxBulkSize().intValue(), this.featureCreationRequestRepo.count());
        assertEquals(properties.getMaxBulkSize().intValue(), infos.getGranted().size());
        assertEquals(0, infos.getDenied().size());
    }

    @Test
    public void test_schedule_request_when_other_request_with_same_providerid_is_running() {
        // Given
        int cpt = 0;
        for (FeatureRequestStep step : FeatureRequestStep.values()) {
            FeatureCreationRequest request = new FeatureCreationRequest();
            request.setFeatureEntity(null);
            request.setRequestId(UUID.randomUUID().toString());
            request.setRequestOwner("request_owner0");
            request.setState(RequestState.GRANTED);
            request.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
            request.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
            request.setStep(step);
            request.setPriority(PriorityLevel.NORMAL);
            request.setProviderId(String.format("provider_id%d", cpt));
            request.setMetadata(new FeatureCreationMetadataEntity().build("session_owner0",
                                                                          "session0",
                                                                          new ArrayList<>(),
                                                                          false));
            featureCreationRequestRepo.save(request);
            cpt++;
        }

        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(cpt + 1, true, false);
        cpt = 0;
        for (FeatureCreationRequestEvent event : events) {
            event.getFeature().setId(String.format("provider_id%d", cpt));
            cpt++;
        }

        this.featureCreationService.registerRequests(events);

        // When schedule requests
        // Then :
        //  - new request is scheduled (the additional one which does not exist with same providerId)
        //  - request created in LOCAL_DELAYED status can be schedule too. There is two requests with same providerId
        //  in LOCAL_DELAYED status, only one is scheduled.
        Assert.assertEquals(2, this.featureCreationService.scheduleRequests());

        // Then count requests
        Assert.assertEquals(1 + Arrays.stream(FeatureRequestStep.values()).count() * 2,
                            this.featureCreationRequestRepo.count());

        // Then only three requests are in LOCAL_SCHEDULED state :
        // two scheduled by the scheduler and one already in LOCAL_SCHEDULED step.
        Assert.assertEquals(3,
                            this.featureCreationRequestRepo.findByStep(FeatureRequestStep.LOCAL_SCHEDULED,
                                                                       Pageable.ofSize(10)).getTotalElements());

    }

    @Test
    @Purpose("Only one creation request can be scheduled at a time for a given provider id to avoid versioning issues")
    public void test_schedule_multiples_creation_requests_with_same_provider_id() {

        // Given 3 requests with same provider id
        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(3, true, false);
        events.forEach(e -> e.getFeature().setId("same_id"));
        this.featureCreationService.registerRequests(events);

        // When
        this.featureCreationService.scheduleRequests();

        // Then only one request is scheduled
        Assert.assertEquals(3, this.featureCreationRequestRepo.count());
        Assert.assertEquals(1,
                            this.featureCreationRequestRepo.findByStep(FeatureRequestStep.LOCAL_SCHEDULED,
                                                                       Pageable.ofSize(10)).getTotalElements());

        // Retry schedule

        // When
        this.featureCreationService.scheduleRequests();

        // Then only one request is scheduled
        Assert.assertEquals(3, this.featureCreationRequestRepo.count());
        Assert.assertEquals(1,
                            this.featureCreationRequestRepo.findByStep(FeatureRequestStep.LOCAL_SCHEDULED,
                                                                       Pageable.ofSize(10)).getTotalElements());

    }

    @Test
    public void testRegisterScheduleProcessWithErrors() {
        List<Feature> features = new ArrayList<>();
        for (int i = 0; i < properties.getMaxBulkSize(); i++) {
            features.add(Feature.build("id" + i,
                                       "owner",
                                       null,
                                       IGeometry.point(IGeometry.position(10.0, 20.0)),
                                       null,
                                       "model"));
        }

        StorageMetadata.build("id ");
        FeatureCreationCollection collection = FeatureCreationCollection.build("owner",
                                                                               FeatureCreationSessionMetadata.build(
                                                                                   "owner",
                                                                                   "session",
                                                                                   PriorityLevel.NORMAL,
                                                                                   false,
                                                                                   false,
                                                                                   StorageMetadata.build("id ")),
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
    public void testFeaturePriority() {

        List<FeatureCreationRequestEvent> events = initFeatureCreationRequestEvent(properties.getMaxBulkSize() * 2,
                                                                                   true,
                                                                                   false);
        // we will set all priority to normal except for the first bulk events
        for (int i = 0; i < properties.getMaxBulkSize(); i++) {
            events.get(i).getMetadata().setPriority(PriorityLevel.HIGH);
        }

        this.featureCreationService.registerRequests(events);

        assertEquals(properties.getMaxBulkSize() * 2, this.featureCreationRequestRepo.count());

        this.featureCreationService.scheduleRequests();
        // Retrieved scheduled requests and verify that High priority have been scheduled first.
        List<FeatureCreationRequest> scheduled = featureCreationRequestRepo.findAll()
                                                                           .stream()
                                                                           .filter(r -> r.getStep()
                                                                                        != FeatureRequestStep.LOCAL_DELAYED)
                                                                           .toList();
        Assert.assertEquals(properties.getMaxBulkSize().intValue(), scheduled.size());

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
        assertEquals(properties.getMaxBulkSize().intValue(), highPriorityNumber);
        assertEquals(0, otherPriorityNumber);

        // Now schedule second bulk
        this.featureCreationService.scheduleRequests();

        waitForFeatures(properties.getMaxBulkSize() * 2);
    }

    @Test
    public void testCreationWithURN() {
        // Given
        FeatureCreationRequestEvent featureCreationRequestEvent = createFeatureCreationRequestEvent(1, false);
        // When
        RequestInfo<String> requestInfo = featureCreationService.registerRequests(Collections.singletonList(
            featureCreationRequestEvent));
        featureCreationService.scheduleRequests();
        // Then
        assertEquals(1, requestInfo.getGranted().size());
        assertEquals(0, requestInfo.getDenied().size());
        waitForFeatures(1);
    }

    @Test
    public void testCreationWithExistingURN() {
        // Given
        FeatureCreationRequestEvent event1 = createFeatureCreationRequestEvent(1, false);
        featureCreationService.registerRequests(Collections.singletonList(event1));
        featureCreationService.scheduleRequests();
        waitForFeatures(1);
        // When
        FeatureCreationRequestEvent event2 = createFeatureCreationRequestEvent(1, false);
        event2.setRequestId("request2");
        event2.getFeature().setUrn(event1.getFeature().getUrn());
        RequestInfo<String> requestInfo = featureCreationService.registerRequests(Collections.singletonList(event2));
        featureCreationService.scheduleRequests();
        // Then
        assertEquals(0, requestInfo.getGranted().size());
        assertEquals(1, requestInfo.getDenied().size());
        waitForFeatures(1);
    }

    @Test
    public void testCreationWithExistingURNAndUpdateMode() {
        // Given
        FeatureCreationRequestEvent event1 = createFeatureCreationRequestEvent(1, false);
        featureCreationService.registerRequests(Collections.singletonList(event1));
        featureCreationService.scheduleRequests();
        waitForFeatures(1);
        mockStorageHelper.mockFeatureCreationStorageSuccess();
        mockNotificationSuccess();
        waitRequest(featureCreationRequestRepo, 0, 20_000);
        // When
        FeatureCreationRequestEvent event2 = createFeatureCreationRequestEvent(1, true);
        event2.setRequestId("request2");
        event2.getFeature().setUrn(event1.getFeature().getUrn());
        RequestInfo<String> requestInfo = featureCreationService.registerRequests(Collections.singletonList(event2));
        // Then
        assertEquals(1, requestInfo.getGranted().size());
        assertEquals(0, requestInfo.getDenied().size());
    }

    @Test
    public void testCreationWithURNAndBadVersion() {
        // Given
        FeatureCreationRequestEvent event2_1 = createFeatureCreationRequestEvent(2, false);
        event2_1.setRequestId("event2_1");
        featureCreationService.registerRequests(Collections.singletonList(event2_1));
        featureCreationService.scheduleRequests();
        waitForFeatures(1);

        // When
        FeatureCreationRequestEvent event1 = createFeatureCreationRequestEvent(1, false);
        event1.setRequestId("event1");
        RequestInfo<String> requestInfo = featureCreationService.registerRequests(Collections.singletonList(event1));
        featureCreationService.scheduleRequests();
        // Then
        assertEquals(0, requestInfo.getGranted().size());
        assertEquals(1, requestInfo.getDenied().size());
        waitForFeatures(1);

        // When
        FeatureCreationRequestEvent event2_2 = createFeatureCreationRequestEvent(2, false);
        event2_2.setRequestId("event2_2");
        requestInfo = featureCreationService.registerRequests(Collections.singletonList(event2_2));
        featureCreationService.scheduleRequests();
        // Then
        assertEquals(0, requestInfo.getGranted().size());
        assertEquals(1, requestInfo.getDenied().size());
        waitForFeatures(1);
    }

    private FeatureCreationRequestEvent createFeatureCreationRequestEvent(int version, boolean updateIfExists) {
        FeatureCreationRequestEvent featureCreationRequestEvent = initFeatureCreationRequestEvent(1,
                                                                                                  false,
                                                                                                  updateIfExists).get(0);
        Feature feature = featureCreationRequestEvent.getFeature();
        UUID uuid = UUID.nameUUIDFromBytes(feature.getId().getBytes());
        FeatureUniformResourceName urn = FeatureUniformResourceName.build(FeatureIdentifier.FEATURE,
                                                                          feature.getEntityType(),
                                                                          "ùlajzlke",
                                                                          uuid,
                                                                          version);
        feature.setUrn(urn);
        return featureCreationRequestEvent;
    }

    private void waitForFeatures(int count) {
        int timeout = count;
        // Timeout should not be less than 5 secs
        if (timeout < 5) {
            timeout = 5;
        }
        Awaitility.await().atMost(timeout, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return featureRepo.count() == count;
        });
    }

}
