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
package fr.cnes.regards.modules.feature.service.request;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceTest;
import fr.cnes.regards.modules.feature.service.FeatureDeletionService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Kevin Marchois
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_listener_deletion",
        "regards.amqp.enabled=true", "spring.jpa.properties.hibernate.jdbc.batch_size=1024",
        "spring.jpa.properties.hibernate.order_inserts=true" })
@ActiveProfiles(value = { "noscheduler", "nohandler" })
public class FeatureStorageListenerDeletionIT extends AbstractFeatureMultitenantServiceTest {

    @Autowired
    private FeatureRequestService requestService;

    @Autowired
    private FeatureDeletionService featureDeletionService;

    private boolean isToNotify;

    @Override
    public void doInit() {
        this.isToNotify = initDefaultNotificationSettings();
    }
    /**
     * Test storage listener we will prepare data send {@linkFeatureDeletionReventEvent} and call handleDeletionSuccess
     * from {@link FeatureRequestService} to mock storage call back
     * Then we will test that all  {@linkFeatureDeletionRevent} are delete and all {@link FeatureEntity} too
     * @throws InterruptedException
     */
    @Test
    public void testOnDeletionSuccess() throws InterruptedException {

        prepareData();
        this.featureCreationRequestRepo.deleteAll();

        List<FeatureDeletionRequest> toDelete = this.featureDeletionRequestRepo.findAll();
        requestService.handleDeletionSuccess(toDelete.stream().map(AbstractFeatureRequest::getGroupId)
                                                     .collect(Collectors.toSet()));
        if(this.isToNotify) {
            mockNotificationSuccess();
        }

        // Feature entity and FeatureDeletionRequest must be deleted
        assertEquals(0, this.featureRepo.count());
        assertEquals(0, this.featureDeletionRequestRepo.count());

    }

    /**
     * Test storage listener we will prepare data send {@linkFeatureDeletionReventEvent} and call handleDeletionError
     * from {@link FeatureRequestService} to mock storage call back
     * Then we will test that all  {@linkFeatureDeletionRevent} and all {@link FeatureEntity} are still in database
     * and  {@linkFeatureDeletionRevent} have their {@link RequestState} to ERROR
     * @throws InterruptedException
     */
    @Test
    public void testHandlerStorageError() throws InterruptedException {
        prepareData();

        List<FeatureDeletionRequest> toDelete = this.featureDeletionRequestRepo.findAll();
        requestService.handleDeletionError(toDelete.stream().map(AbstractFeatureRequest::getGroupId)
                                                   .collect(Collectors.toSet()));

        // Feature entity and FeatureDeletionRequest must be deleted
        assertEquals(properties.getMaxBulkSize().intValue(), this.featureRepo.count());
        assertEquals(properties.getMaxBulkSize().intValue(), this.featureDeletionRequestRepo.count());

        // all FeatureDeletionRequest must have their state to ERROR
        toDelete = this.featureDeletionRequestRepo.findAll();
        assertTrue(toDelete.stream().allMatch(request -> RequestState.ERROR.equals(request.getState())));
    }

    private void prepareData() throws InterruptedException {
        String deletionOwner = "deleter";
        long deletionRequestWaitingForStorageResponseCount;
        int cpt = 0;
        List<FeatureDeletionRequestEvent> events = prepareDeletionTestData(deletionOwner,
                                                                           true,
                                                                           properties.getMaxBulkSize(), this.isToNotify);

        this.featureDeletionService.registerRequests(events);

        this.featureDeletionService.scheduleRequests();

        do {
            deletionRequestWaitingForStorageResponseCount = this.featureDeletionRequestRepo
                    .findByStep(FeatureRequestStep.REMOTE_STORAGE_DELETION_REQUESTED, OffsetDateTime.now()).size();
            Thread.sleep(100);
            cpt++;
        } while ((cpt < 100) && deletionRequestWaitingForStorageResponseCount != properties.getMaxBulkSize());
        // in that case all features hasn't be deleted
        if (cpt == 100) {
            fail("Some FeatureDeletionRequest have been deleted");
        }

        assertEquals(properties.getMaxBulkSize().intValue(), this.featureRepo.count());

    }
}
