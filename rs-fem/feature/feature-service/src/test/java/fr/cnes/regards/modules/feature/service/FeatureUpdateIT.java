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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.IFeatureUpdateRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.ILightFeatureUpdateRequest;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.model.dto.properties.IProperty;

/**
 * @author kevin
 * @author S??bastien Binda
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=feature_update", "regards.amqp.enabled=true",
                "regards.feature.metrics.enabled=true" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties",
                "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "noFemHandler" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FeatureUpdateIT extends AbstractFeatureMultitenantServiceTest {

    @Autowired
    private IFeatureUpdateService featureUpdateService;

    @Autowired
    private IFeatureDeletionService featureDeletionService;

    @Autowired
    private IFeatureUpdateRequestRepository featureUpdateRequestRepository;

    @Autowired
    private Gson gson;

    @Captor
    private ArgumentCaptor<List<NotificationRequestEvent>> recordsCaptor;

    private boolean isToNotify;

    @Override
    public void doInit() {
        // initialize notification
        this.isToNotify = initDefaultNotificationSettings();
    }

    @Test
    public void testScheduleFeatureUpdateDuringDeletion() throws InterruptedException {

        // create features
        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(3, true,false);
        this.featureCreationService.registerRequests(events);

        this.featureCreationService.scheduleRequests();
        int cpt = 0;
        long featureNumberInDatabase;
        do {
            featureNumberInDatabase = this.featureRepo.count();
            Thread.sleep(1000);
            cpt++;
        } while ((cpt < 100) && (featureNumberInDatabase != 3));
        List<FeatureEntity> entities = super.featureRepo.findAll();

        // Simulate a deletion running request
        FeatureDeletionRequest req = FeatureDeletionRequest
                .build(UUID.randomUUID().toString(), "owner", OffsetDateTime.now(), RequestState.GRANTED, null,
                       FeatureRequestStep.LOCAL_SCHEDULED, PriorityLevel.NORMAL, entities.get(0).getUrn());
        this.featureDeletionRequestRepo.save(req);

        // Send a new update request on the currently deleting feature
        this.featureUpdateService
                .registerRequests(this.prepareUpdateRequests(Lists.newArrayList(entities.get(0).getUrn())));
        assertEquals("No update request sould be scheduled", 0, this.featureUpdateService.scheduleRequests());

        // Check that the update request is delayed waiting for deletion ends
        FeatureUpdateRequest uReq = super.featureUpdateRequestRepo.findAll().get(0);
        assertEquals("Update request should be on delayed step", FeatureRequestStep.LOCAL_DELAYED, uReq.getStep());

    }

    @Test
    @Purpose("Check update request on a feature with new files locations when storage error occurs")
    public void testUpdateFeatureFilesWithErrors() throws InterruptedException {
        updateFeaturesFiles(5, 2);
    }

    @Test
    @Purpose("Check update request on a feature with new files locations when storage succeed")
    public void testUpdateFeatureFiles() throws InterruptedException {
        updateFeaturesFiles(5, 0);
    }

    private void updateFeaturesFiles(int nbSuccess, int nbErrors) throws InterruptedException {
        int nbFeatures = nbSuccess+nbErrors;
        int timeout = 10_000 + (nbFeatures*100);
        // Init a feature
        List<FeatureCreationRequestEvent> events = initFeatureCreationRequestEvent(nbFeatures, true,false);
        // Remove files to create events. Files will be added with the update requests
        events.forEach(e-> e.getFeature().getFiles().clear());
        this.featureCreationService.registerRequests(events);
        this.featureCreationService.scheduleRequests();
        waitFeature(nbFeatures,null,timeout);
        mockNotificationSuccess();
        List<FeatureEntity> features = featureRepo.findAll();
        Assert.assertNotNull(features);
        Assert.assertTrue(features.size() == nbFeatures);
        Assert.assertEquals(0L, featureCreationRequestRepo.count());

        // Now create an update request on this feature to add referenced files
        List<FeatureUpdateRequestEvent> updates = prepareUpdateRequests(features.stream().map(f -> f.getUrn()).collect(
                Collectors.toList()));
        // Add one file with a store file (only url is provided in location)
        FeatureFileAttributes attributes = FeatureFileAttributes.build(DataType.RAWDATA,
                                                                       MediaType.APPLICATION_OCTET_STREAM, "fileName",
                                                                       10L, "MD5", "checksum");
        String newStorage = "somewhere";
        String newUrl = "file:///dir/file.txt";
        FeatureFileLocation location = FeatureFileLocation.build(newUrl, newStorage);
        updates.forEach(u -> u.getFeature().getFiles().add(FeatureFile.build(attributes, location)));

        // Process update request
        RequestInfo<FeatureUniformResourceName> info = featureUpdateService.registerRequests(updates);
        Assert.assertEquals(nbFeatures, info.getGranted().size());
        Assert.assertEquals(0L, info.getDenied().size());
        Thread.sleep((properties.getDelayBeforeProcessing()+1)*1000);
        Assert.assertEquals(nbFeatures, featureUpdateService.scheduleRequests());

        // As files needs to be updated, the step of the request remains REMOTE STORAGE REQUESTS still response from
        // storage is received.
        waitForStep(featureUpdateRequestRepo,FeatureRequestStep.REMOTE_STORAGE_REQUESTED,nbFeatures,timeout);

        // Simulate response from storage
        mockStorageHelper.mockFeatureUpdateStorageWithErrors(nbSuccess, nbErrors);

        // Check feature is successfully updated with new file reference
        features = featureRepo.findAll();
        FeatureEntity feature = features.get(0);
        Assert.assertNotNull(features);
        Assert.assertEquals(1L, feature.getFeature().getFiles().size());
        Assert.assertEquals(1L, feature.getFeature().getFiles().get(0).getLocations().size());
        Assert.assertEquals(newStorage, feature.getFeature().getFiles().get(0).getLocations().stream().findFirst().get().getStorage());
        Assert.assertEquals(newUrl, feature.getFeature().getFiles().get(0).getLocations().stream().findFirst().get().getUrl());
        waitForStep(featureUpdateRequestRepo,FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,nbSuccess,timeout);
        waitForStep(featureUpdateRequestRepo,FeatureRequestStep.REMOTE_STORAGE_ERROR,nbErrors,timeout);
    }

    /**
     * Test update scheduler we will create 4 {@link FeatureUpdateRequest}
     * fur1, fur2, fur3, fur4
     * fur2 is already scheduled and fur3 is on the same {@link Feature} that fur2
     * fur4 has a {@link FeatureDeletionRequest} scheduled with the same {@link Feature}
     * When we will call the scheduler it will schedule fur1 only
     * @throws InterruptedException
     */
    @Test
    public void testSchedulerSteps() throws InterruptedException {
        // create features
        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(3, true,false);
        this.featureCreationService.registerRequests(events);

        this.featureCreationService.scheduleRequests();
        int cpt = 0;
        long featureNumberInDatabase;
        do {
            featureNumberInDatabase = this.featureRepo.count();
            Thread.sleep(1000);
            cpt++;
        } while ((cpt < 100) && (featureNumberInDatabase != 3));
        List<FeatureEntity> entities = super.featureRepo.findAll();
        mockStorageHelper.mockFeatureCreationStorageSuccess();
        mockNotificationSuccess();

        // features have been created. now lets simulate a deletion of one of them
        FeatureEntity toDelete = entities.get(2);
        FeatureDeletionRequestEvent featureDeletionRequest = FeatureDeletionRequestEvent
                .build("TEST", toDelete.getUrn(), PriorityLevel.NORMAL);
        this.featureDeletionService.registerRequests(Lists.list(featureDeletionRequest));

        // simulate an update request
        FeatureEntity toUpdate = entities.get(0);
        FeatureUpdateRequest fur1 = FeatureUpdateRequest
                .build(UUID.randomUUID().toString(), "owner", OffsetDateTime.now(), RequestState.GRANTED, null,
                       toUpdate.getFeature(), PriorityLevel.NORMAL, FeatureRequestStep.LOCAL_DELAYED);

        // simulate an update request that has already been scheduled
        FeatureEntity updatingByScheduler = entities.get(1);
        FeatureUpdateRequest fur2 = FeatureUpdateRequest
                .build(UUID.randomUUID().toString(), "owner", OffsetDateTime.now(), RequestState.GRANTED, null,
                       updatingByScheduler.getFeature(), PriorityLevel.NORMAL, FeatureRequestStep.LOCAL_SCHEDULED);
        // Simulate one more update request that just arrived.
        // this update cannot be scheduled because fur2 is already scheduled and on the same feature
        FeatureUpdateRequest fur3 = FeatureUpdateRequest
                .build(UUID.randomUUID().toString(), "owner", OffsetDateTime.now(), RequestState.GRANTED, null,
                       updatingByScheduler.getFeature(), PriorityLevel.NORMAL, FeatureRequestStep.LOCAL_DELAYED);

        // simulate an update request on a feature being deleted
        FeatureUpdateRequest fur4 = FeatureUpdateRequest
                .build(UUID.randomUUID().toString(), "owner", OffsetDateTime.now(), RequestState.GRANTED, null,
                       toDelete.getFeature(), PriorityLevel.NORMAL, FeatureRequestStep.LOCAL_DELAYED);

        // bypass registration to help simulate the state we want
        fur1 = super.featureUpdateRequestRepo.save(fur1);
        fur2 = super.featureUpdateRequestRepo.save(fur2);
        fur3 = super.featureUpdateRequestRepo.save(fur3);
        fur4 = super.featureUpdateRequestRepo.save(fur4);

        // Simulate featue deletion request running
        Page<FeatureDeletionRequest> deletionRequests = featureDeletionService
                .findRequests(FeatureRequestsSelectionDTO.build(), PageRequest.of(0, 10));
        Assert.assertEquals("There should be one deletion request", 1L, deletionRequests.getTotalElements());
        FeatureDeletionRequest dr = deletionRequests.getContent().get(0);
        dr.setStep(FeatureRequestStep.LOCAL_SCHEDULED);
        featureDeletionRequestRepo.save(dr);

        // Wait minimum processing time for request to be scheduled after being delayed
        Thread.sleep(properties.getDelayBeforeProcessing() * 1100);
        // fur1 should be scheduled. fur4 cannot be scheduled as the deletion request is processing.
        assertEquals("There should be 2 update requests scheduled", 1, this.featureUpdateService.scheduleRequests());

        List<FeatureUpdateRequest> updateRequests = this.featureUpdateRequestRepo.findAll();

        // fur1 and fur2 should be scheduled
        assertEquals(2, updateRequests.stream()
                .filter(request -> request.getStep().equals(FeatureRequestStep.LOCAL_SCHEDULED)).count());
        // fur3 stay delayed cause a update on the same feature is scheduled
        assertEquals(1, updateRequests.stream()
                .filter(request -> request.getStep().equals(FeatureRequestStep.LOCAL_DELAYED)).count());

        // fur4 in error cause a deletion is scheduled on the same urn
        assertEquals(1, updateRequests.stream()
                .filter(request -> request.getStep().equals(FeatureRequestStep.LOCAL_ERROR)).count());
    }

    /**
     * Test priority level for feature update we will schedule properties.getMaxBulkSize() {@link FeatureUpdateRequestEvent}
     * with priority set to average plus properties.getMaxBulkSize() /2 {@link FeatureUpdateRequestEvent} with {@link PriorityLevel}
     * to average
     * @throws InterruptedException
     */
    @Test
    public void testFeaturePriority() throws InterruptedException {
        // create features
        int featureToCreateNumber = properties.getMaxBulkSize() + (properties.getMaxBulkSize() / 2);
        List<FeatureCreationRequestEvent> events = prepareCreationTestData(true, featureToCreateNumber, this.isToNotify,
                                                                           true, false);

        // create update requests
        List<FeatureUpdateRequestEvent> updateEvents = new ArrayList<>();
        updateEvents = events.stream()
                .map(event -> FeatureUpdateRequestEvent.build("test", event.getMetadata(), event.getFeature()))
                .collect(Collectors.toList());

        // we will set all priority to low for the (properties.getMaxBulkSize() / 2) last event
        for (int i = properties.getMaxBulkSize(); i < (properties.getMaxBulkSize()
                + (properties.getMaxBulkSize() / 2)); i++) {
            updateEvents.get(i).getMetadata().setPriority(PriorityLevel.HIGH);
        }

        updateEvents.stream().forEach(event -> {
            event.getFeature().getProperties().clear();
            event.getFeature()
                    .setUrn(FeatureUniformResourceName
                            .build(FeatureIdentifier.FEATURE, EntityType.DATA, getDefaultTenant(),
                                   UUID.nameUUIDFromBytes(event.getFeature().getId().getBytes()), 1));
            event.getFeature()
                    .addProperty(IProperty.buildObject("file_characterization",
                                                       IProperty.buildBoolean("valid", Boolean.FALSE),
                                                       IProperty.buildDate("invalidation_date", OffsetDateTime.now())));
        });
        this.featureUpdateService.registerRequests(updateEvents);

        // we wait for delay before schedule
        Thread.sleep((this.properties.getDelayBeforeProcessing() * 1000) + 1000);
        this.featureUpdateService.scheduleRequests();

        // in case notification are active, mock their successes
        if (this.isToNotify) {
            // wait until request are in state LOCAL_TO_BE_NOTIFIED
            int cpt = 0;
            while ((cpt < 10) && (featureUpdateRequestRepository
                    .findByStepAndRequestDateLessThanEqual(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                                                           OffsetDateTime.now().plusDays(1),
                                                           PageRequest.of(0, properties.getMaxBulkSize()))
                    .getSize() < (properties.getMaxBulkSize() / 2))) {
                Thread.sleep(1000);
                cpt++;
            }
            if (cpt == 10) {
                fail("Update request where not handled in less than 10_000 ms");
            }
            mockNotificationSuccess();
        }

        List<ILightFeatureUpdateRequest> scheduled = this.featureUpdateRequestRepository
                .findRequestsToSchedule(FeatureRequestStep.LOCAL_DELAYED, OffsetDateTime.now(),
                                        PageRequest.of(0, properties.getMaxBulkSize()), OffsetDateTime.now())
                .getContent();
        // half of scheduled should be with priority HIGH
        assertEquals(properties.getMaxBulkSize().intValue() / 2, scheduled.size());
        // check that remaining FeatureUpdateRequest all their their priority not to high
        assertFalse(scheduled.stream().anyMatch(request -> PriorityLevel.HIGH.equals(request.getPriority())));

    }

    @Test
    public void testRetrieveRequests() throws InterruptedException {
        int nbValid = 20;
        OffsetDateTime start = OffsetDateTime.now();
        // Create features
        prepareCreationTestData(false, nbValid, true, true, false);
        RequestsPage<FeatureRequestDTO> results = this.featureRequestService
                .findAll(FeatureRequestTypeEnum.UPDATE, FeatureRequestsSelectionDTO.build(), PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        // Notify them
        List<FeatureUniformResourceName> urns = this.featureRepo.findAll().stream().map(f -> f.getUrn())
                .collect(Collectors.toList());
        this.featureUpdateService.registerRequests(prepareUpdateRequests(urns));

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.UPDATE, FeatureRequestsSelectionDTO.build(),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(nbValid, results.getContent().size());
        Assert.assertEquals(nbValid, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.UPDATE,
                                                     FeatureRequestsSelectionDTO.build().withState(RequestState.ERROR),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.UPDATE,
                                                     FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED)
                                                             .withStart(OffsetDateTime.now().plusSeconds(5)),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getContent().size());
        Assert.assertEquals(0, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.UPDATE,
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
        prepareCreationTestData(false, nbValid, true, true, false);
        // Notify them
        List<FeatureUniformResourceName> urns = this.featureRepo.findAll().stream().map(f -> f.getUrn())
                .collect(Collectors.toList());
        RequestInfo<FeatureUniformResourceName> results = this.featureUpdateService
                .registerRequests(prepareUpdateRequests(urns));
        Assert.assertFalse(results.getGranted().isEmpty());

        // Simulate all requests to scheduled
        this.featureUpdateService.findRequests(FeatureRequestsSelectionDTO.build(), PageRequest.of(0, 1000))
                .forEach(r -> {
                    r.setStep(FeatureRequestStep.LOCAL_SCHEDULED);
                    this.featureUpdateRequestRepo.save(r);
                });

        // Try delete all requests.
        RequestHandledResponse response = this.featureUpdateService.deleteRequests(FeatureRequestsSelectionDTO.build());
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as request are not in ERROR state", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as request are not in ERROR state", 0,
                            response.getTotalRequested());

        response = this.featureUpdateService
                .deleteRequests(FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as selection set on GRANTED Requests", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as selection set on GRANTED Requests", 0,
                            response.getTotalRequested());

        // Simulate all requests to scheduled
        this.featureUpdateService.findRequests(FeatureRequestsSelectionDTO.build(), PageRequest.of(0, 1000))
                .forEach(r -> {
                    r.setStep(FeatureRequestStep.REMOTE_STORAGE_ERROR);
                    this.featureUpdateRequestRepo.save(r);
                });

        response = this.featureUpdateService.deleteRequests(FeatureRequestsSelectionDTO.build());
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 20 requests deleted", 20, response.getTotalHandled());
        Assert.assertEquals("There should be 20 requests to delete", 20, response.getTotalRequested());

    }

    @Test
    public void testRetryRequests() throws InterruptedException {

        int nbValid = 20;
        // Create features
        prepareCreationTestData(false, nbValid, true, true, false);
        // Notify them
        List<FeatureUniformResourceName> urns = this.featureRepo.findAll().stream().map(f -> f.getUrn())
                .collect(Collectors.toList());
        RequestInfo<FeatureUniformResourceName> results = this.featureUpdateService
                .registerRequests(prepareUpdateRequests(urns));
        Assert.assertFalse(results.getGranted().isEmpty());

        // Try delete all requests.
        RequestHandledResponse response = this.featureUpdateService.retryRequests(FeatureRequestsSelectionDTO.build().withState(RequestState.ERROR));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests retryed as request are not in ERROR state", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to retry as request are not in ERROR state", 0,
                            response.getTotalRequested());

        response = this.featureUpdateService
                .retryRequests(FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 20 requests retryed as selection set on GRANTED Requests", nbValid,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 20 requests to retry as selection set on GRANTED Requests", nbValid,
                            response.getTotalRequested());

    }

    @Test
    public void test1SessionNotifier() throws InterruptedException {

        int requestCount = 10;
        prepareCreationTestData(false, requestCount, true, true, false);

        // Update
        List<FeatureUniformResourceName> urns = Collections
                .singletonList(featureRepo.findAll().stream().map(FeatureEntity::getUrn).findAny().get());
        featureUpdateService.registerRequests(prepareUpdateRequests(urns));
        TimeUnit.SECONDS.sleep(5);
        featureUpdateService.scheduleRequests();
        waitForStep(featureUpdateRequestRepository, FeatureRequestStep.LOCAL_TO_BE_NOTIFIED, 1, 10_000);
        mockNotificationSuccess();

        // Check update notification valid metadata
        checkNotifications(requestCount, 1);

        waitUpdateRequestDeletion(0, 20000);

        checkOneUpdate(requestCount);
    }

    private void checkNotifications(int createNotificationExpected, int updateNotificationExpected) {
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(recordsCaptor.capture());
        AtomicInteger nbUpdate = new AtomicInteger();
        AtomicInteger nbCreate = new AtomicInteger();
        assertEquals(createNotificationExpected+updateNotificationExpected,
                     recordsCaptor.getAllValues().stream().flatMap(Collection::stream).count());
        recordsCaptor.getAllValues().stream().flatMap(Collection::stream).forEach(notification -> {
            CreateNotificationRequestEventVisitor.NotificationActionEventMetadata metadata =
                    gson.fromJson(notification.getMetadata(),
                                  CreateNotificationRequestEventVisitor.NotificationActionEventMetadata.class);
            if (metadata.getAction().equals(FeatureManagementAction.UPDATED.toString())) {
                assertEquals(gson.toJson(new CreateNotificationRequestEventVisitor.NotificationActionEventMetadata(
                        FeatureManagementAction.UPDATED,owner,session)), notification.getMetadata().toString());
                nbUpdate.getAndIncrement();
            } else if (metadata.getAction().equals(FeatureManagementAction.CREATED.toString())) {
                assertEquals(gson.toJson(new CreateNotificationRequestEventVisitor.NotificationActionEventMetadata(
                        FeatureManagementAction.CREATED,owner,session)), notification.getMetadata().toString());
                nbCreate.getAndIncrement();
            }
        });
        Assert.assertEquals(createNotificationExpected, nbCreate.get());
        Assert.assertEquals(updateNotificationExpected, nbUpdate.get());
    }

    @Test
    public void test1SessionNotifierWithoutNotification() throws InterruptedException, EntityException {

        setNotificationSetting(false);

        int requestCount = 10;
        prepareCreationTestData(false, requestCount, false, true, false);

        // Update
        List<FeatureUniformResourceName> urns = Collections
                .singletonList(featureRepo.findAll().stream().map(FeatureEntity::getUrn).findAny().get());
        featureUpdateService.registerRequests(prepareUpdateRequests(urns));
        TimeUnit.SECONDS.sleep(5);
        featureUpdateService.scheduleRequests();
        waitUpdateRequestDeletion(0, 20000);

        checkOneUpdate(requestCount);
    }

    @Test
    public void test1SessionNotifierWithRetry() throws InterruptedException {

        createOneWithError();

        featureUpdateService.retryRequests(new FeatureRequestsSelectionDTO());
        mockNotificationSuccess();
        waitRequest(featureUpdateRequestRepo, 0, 20000);

        // Compute Session step
        computeSessionStep(12);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(8, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(4, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(1, property("referencedProducts"), requests);
        checkRequests(1, property("updateRequests"), requests);
        checkRequests(4, property("runningUpdateRequests"), requests);
        checkRequests(1, property("updatedProducts"), requests);
        checkRequests(2, property("inErrorUpdateRequests"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(1, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(7, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "referencedProducts", sessionStepProperties);
        checkKey(1, "updateRequests", sessionStepProperties);
        checkKey(0, "runningUpdateRequests", sessionStepProperties);
        checkKey(0, "inErrorUpdateRequests", sessionStepProperties);
        checkKey(1, "updatedProducts", sessionStepProperties);
    }

    @Test
    public void test1SessionNotifierWithDelete() throws InterruptedException {

        createOneWithError();

        featureUpdateService.deleteRequests(new FeatureRequestsSelectionDTO());
        waitRequest(featureUpdateRequestRepo, 0, 20000);

        // Compute Session step
        computeSessionStep(11);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(7, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(4, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(1, property("referencedProducts"), requests);
        checkRequests(2, property("updateRequests"), requests);
        checkRequests(1, property("updatedProducts"), requests);
        checkRequests(2, property("runningUpdateRequests"), requests);
        checkRequests(2, property("inErrorUpdateRequests"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(1, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        Assertions.assertEquals(1, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(7, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "referencedProducts", sessionStepProperties);
        checkKey(0, "updateRequests", sessionStepProperties);
        checkKey(1, "updatedProducts", sessionStepProperties);
        checkKey(0, "runningUpdateRequests", sessionStepProperties);
        checkKey(0, "inErrorUpdateRequests", sessionStepProperties);
    }

    private void checkOneUpdate(int requestCount) throws InterruptedException {

        // Compute Session step
        computeSessionStep((requestCount + 1) * 4);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests((requestCount + 1) * 3, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(requestCount + 1, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(requestCount, property("referencingRequests"), requests);
        checkRequests(requestCount, property("referencedProducts"), requests);
        checkRequests(1, property("updateRequests"), requests);
        checkRequests(1, property("updatedProducts"), requests);
        checkRequests(requestCount * 2, property("runningReferencingRequests"), requests);
        checkRequests(2, property("runningUpdateRequests"), requests);
        checkRequests(requestCount, inputRelated(), requests);
        checkRequests(requestCount, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(requestCount, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(6, sessionStepProperties.size());
        checkKey(requestCount, "referencingRequests", sessionStepProperties);
        checkKey(requestCount, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "updateRequests", sessionStepProperties);
        checkKey(1, "updatedProducts", sessionStepProperties);
        checkKey(0, "runningUpdateRequests", sessionStepProperties);
    }

    private void createOneWithError() throws InterruptedException {
        // Create and Update One with Files, fail on notification
        prepareCreationTestData(false, 1, true, true, false);

        // Update
        List<FeatureUniformResourceName> urns = Collections
                .singletonList(featureRepo.findAll().stream().map(FeatureEntity::getUrn).findAny().get());
        featureUpdateService.registerRequests(prepareUpdateRequests(urns));
        TimeUnit.SECONDS.sleep(5);
        featureUpdateService.scheduleRequests();
        waitForStep(featureUpdateRequestRepository, FeatureRequestStep.LOCAL_TO_BE_NOTIFIED, 1, 10_000);
        mockNotificationError();
        waitForStep(featureUpdateRequestRepository, FeatureRequestStep.REMOTE_NOTIFICATION_ERROR, 1, 10_000);

        // Compute Session step
        computeSessionStep(9);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        Assertions.assertEquals(9, requests.size());
        checkRequests(7, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(2, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(1, property("referencedProducts"), requests);
        checkRequests(1, property("updateRequests"), requests);
        checkRequests(1, property("updatedProducts"), requests);
        checkRequests(2, property("runningUpdateRequests"), requests);
        checkRequests(1, property("inErrorUpdateRequests"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(1, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        Assertions.assertEquals(1, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(7, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "referencedProducts", sessionStepProperties);
        checkKey(1, "updateRequests", sessionStepProperties);
        checkKey(1, "updatedProducts", sessionStepProperties);
        checkKey(0, "runningUpdateRequests", sessionStepProperties);
        checkKey(1, "inErrorUpdateRequests", sessionStepProperties);
    }

}
