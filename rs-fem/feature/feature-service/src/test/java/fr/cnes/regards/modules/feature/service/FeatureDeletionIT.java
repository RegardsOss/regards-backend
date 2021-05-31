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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.feature.dao.IFeatureNotificationSettingsRepository;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;

/**
 * @author Kevin Marchois
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=feature_deletion", "regards.amqp.enabled=true" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties",
                "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "nohandler" })
public class FeatureDeletionIT extends AbstractFeatureMultitenantServiceTest {

    @SpyBean
    private IPublisher publisherSpy;

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
     * @throws InterruptedException
     */
    @Test
    public void testDeletionWithoutFiles() throws InterruptedException {
        String deletionOwner = "deleter";
        // mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisherSpy).publish(Mockito.any(NotificationRequestEvent.class));
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
            Mockito.verify(publisherSpy, Mockito.times(2)).publish(recordsCaptor.capture());
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

        String deletionOwner = "deleter";

        // mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisherSpy).publish(Mockito.any(NotificationRequestEvent.class));

        long featureNumberInDatabase;
        int cpt = 0;

        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(deletionOwner, true,
                                                                           properties.getMaxBulkSize(),
                                                                           this.isToNotify);
        this.featureDeletionService.registerRequests(events);
        this.featureDeletionService.scheduleRequests();

        do {
            featureNumberInDatabase = this.featureDeletionRequestRepo.count();
            Thread.sleep(100);
            cpt++;
        } while ((cpt < 100) && ((featureNumberInDatabase != properties.getMaxBulkSize().intValue())
                || !this.featureDeletionRequestRepo.findAll().stream()
                        .allMatch(request -> FeatureRequestStep.REMOTE_STORAGE_DELETION_REQUESTED
                                .equals(request.getStep()))));
        // in that case all features hasn't be deleted
        if (cpt == 1000) {
            fail("Some FeatureDeletionRequest have been deleted");
        }

        if (!this.featureDeletionRequestRepo.findAll().stream()
                .allMatch(request -> FeatureRequestStep.REMOTE_STORAGE_DELETION_REQUESTED.equals(request.getStep()))) {
            fail("Some FeatureDeletionRequest have a wrong status");
        }

        assertEquals(properties.getMaxBulkSize().intValue(), this.featureRepo.count());
        // the publisher has been called because of storage successes (feature creation with files)
        Mockito.verify(publisherSpy, Mockito.times(1)).publish(recordsCaptor.capture());

    }

    /**
     * Test priority level for feature deletion we will schedule properties.getMaxBulkSize() {@link FeatureDeletionRequestEvent}
     * with priority set to average plus properties.getMaxBulkSize() /2 {@link FeatureDeletionRequestEvent}
     * with {@link PriorityLevel} to average
     * @throws InterruptedException
     */
    @Test
    public void testFeaturePriority() throws InterruptedException {

        String deletionOwner = "deleter";
        long featureNumberInDatabase;
        int cpt = 0;
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(deletionOwner, true,
                                                                           properties.getMaxBulkSize()
                                                                                   + (properties.getMaxBulkSize() / 2),
                                                                           this.isToNotify);
        this.featureDeletionService.registerRequests(events);

        this.featureDeletionService.scheduleRequests();

        do {
            featureNumberInDatabase = this.featureRepo.count();
            Thread.sleep(1000);
            cpt++;
        } while ((cpt < 100) && (featureNumberInDatabase != (properties.getMaxBulkSize() / 2)));

        // in that case all features hasn't been saved
        if (cpt == 100) {
            fail("Doesn't have all features at the end of time");
        }
        if (this.isToNotify) {
            // first feature batch has been successfully deleted, now let simulate notification success
            mockNotificationSuccess();
        }
        // there should remain properties.getMaxBulkSize / 2 request to be handled (scheduleRequest only schedule properties.getMaxBulkSize requests)
        List<FeatureDeletionRequest> notScheduled = this.featureDeletionRequestRepo.findAll();
        assertEquals(properties.getMaxBulkSize() / 2, notScheduled.size());
        assertTrue(notScheduled.stream().allMatch(request -> PriorityLevel.NORMAL.equals(request.getPriority())));
    }

    @Test
    public void testRetrieveRequests() throws InterruptedException {
        int nbValid = 20;
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
        Assert.assertEquals(11, results.getContent().size());
        Assert.assertEquals(11, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());

        results = this.featureRequestService.findAll(FeatureRequestTypeEnum.DELETION,
                                                     FeatureRequestsSelectionDTO.build().withProviderId("id10"),
                                                     PageRequest.of(0, 100));
        Assert.assertEquals(1, results.getContent().size());
        Assert.assertEquals(1, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());
    }

    @Test
    public void testDeleteRequests() throws InterruptedException {

        int nbValid = 20;
        // Register valid requests
        String deletionOwner = "deleter";
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(deletionOwner, true, nbValid, false);
        this.featureDeletionService.registerRequests(events);

        // Try delete all requests.
        RequestHandledResponse response = this.featureDeletionService
                .deleteRequests(FeatureRequestsSelectionDTO.build());
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as request are not in ERROR state", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as request are not in ERROR state", 0,
                            response.getTotalRequested());

        response = this.featureDeletionService
                .deleteRequests(FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests deleted as selection set on GRANTED Requests", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to delete as selection set on GRANTED Requests", 0,
                            response.getTotalRequested());

    }

    @Test
    public void testRetryRequests() throws InterruptedException {

        int nbValid = 20;
        // Register valid requests
        String deletionOwner = "deleter";
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(deletionOwner, true, nbValid, false);
        this.featureDeletionService.registerRequests(events);

        // Try delete all requests.
        RequestHandledResponse response = this.featureDeletionService
                .retryRequests(FeatureRequestsSelectionDTO.build());
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests retryed as request are not in ERROR state", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to retry as request are not in ERROR state", 0,
                            response.getTotalRequested());

        response = this.featureDeletionService
                .retryRequests(FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED));
        LOGGER.info(response.getMessage());
        Assert.assertEquals("There should be 0 requests retryed as selection set on GRANTED Requests", 0,
                            response.getTotalHandled());
        Assert.assertEquals("There should be 0 requests to retry as selection set on GRANTED Requests", 0,
                            response.getTotalRequested());

    }

    @Override
    public void doAfter() {
        notificationSettingsRepository.deleteAll();
    }
}
