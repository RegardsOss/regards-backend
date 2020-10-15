/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
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
                                                                           properties.getMaxBulkSize());

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

        if(initNotificationSettings()) {
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
                                                                           properties.getMaxBulkSize());

        this.featureDeletionService.registerRequests(events);

        this.featureDeletionService.scheduleRequests();
        do {
            featureNumberInDatabase = this.featureDeletionRequestRepo.count();
            Thread.sleep(100);
            cpt++;
        } while ((cpt < 100)
                && ((featureNumberInDatabase != properties.getMaxBulkSize().intValue()) || !this.featureDeletionRequestRepo
                        .findAll().stream().allMatch(request -> FeatureRequestStep.REMOTE_STORAGE_DELETION_REQUESTED
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
        // the publisher must not be called
        Mockito.verify(publisherSpy, Mockito.times(0)).publish(recordsCaptor.capture());

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
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(deletionOwner, false,
                                                                           properties.getMaxBulkSize()
                                                                                   + (properties.getMaxBulkSize() / 2));

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
        // first feature batch has been successfully deleted, now let simulate notification success
        mockNotificationSuccess();

        // there should remain properties.getMaxBulkSize / 2 request to be handled (scheduleRequest only schedule properties.getMaxBulkSize requests)
        List<FeatureDeletionRequest> notScheduled = this.featureDeletionRequestRepo.findAll();

        assertEquals(properties.getMaxBulkSize() / 2, notScheduled.size());
        assertTrue(notScheduled.stream().allMatch(request -> PriorityLevel.NORMAL.equals(request.getPriority())));
    }
}
