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

import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;

import static org.junit.Assert.assertNotEquals;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.service.request.FeatureStorageListener;
import fr.cnes.regards.modules.storage.client.RequestInfo;

/**
 * Test feature mutation based on null property values.
 *
 * @author Marc SORDI
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=feature_duplication",
                "regards.amqp.enabled=true" },
        locations = { "classpath:batch.properties", "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class DuplicatedFeatureIT extends AbstractFeatureMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicatedFeatureIT.class);

    @Autowired
    private FeatureStorageListener listener;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IFeatureNotificationService featureNotificationService;

    @Autowired
    private IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepo;

    private boolean isToNotify;

    @Override
    public void doInit() {
        this.isToNotify = initDefaultNotificationSettings();
    }
    @Test
    public void testDuplicatedFeatureCreationWithOverride() throws InterruptedException {
        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(1, true, false);
        events.get(0).getFeature().setId("id");
        publisher.publish(events);
        waitRequest(this.featureCreationRequestRepo, 1, 30000);
        this.featureCreationService.scheduleRequests();
        waitRequest(this.featureRepo, 1, 30000);

        // mock storage response to indicate creation success
        FeatureEntity featureInDatabase = this.featureRepo.findAll().get(0);
        FeatureCreationRequest fcr = this.featureCreationRequestRepo.findAll().get(0);
        RequestInfo info = RequestInfo.build();
        fcr.setGroupId(info.getGroupId());
        fcr = this.featureCreationRequestRepo.save(fcr);
        this.listener.onStoreSuccess(Sets.newHashSet(info));

        //end creation process
        if(this.isToNotify) {
            mockNotificationSuccess();
        }

        // publish a duplicated feature creation
        publisher.publish(events);
        waitRequest(this.featureCreationRequestRepo, 1, 30000);
        this.featureCreationService.scheduleRequests();
        waitRequest(this.featureRepo, 2, 30000);

        // mock storage response to indicate creation success
        fcr = this.featureCreationRequestRepo.findAll().get(0);
        fcr.setGroupId(info.getGroupId());
        fcr = this.featureCreationRequestRepo.save(fcr);
        this.listener.onStoreSuccess(Sets.newHashSet(info));

        //end creation process
        if(this.isToNotify) {
            mockNotificationSuccess();
        }

        // that must publish a FeatureDeletionRequestEvent
        waitRequest(this.featureDeletionRequestRepo, 1, 30000);
        this.featureDeletionService.scheduleRequests();

        // mock the deletion success for storage
        FeatureDeletionRequest fdr = this.featureDeletionRequestRepo.findAll().get(0);
        fdr.setGroupId(info.getGroupId());
        fdr = this.featureDeletionRequestRepo.save(fdr);
        this.listener.onDeletionSuccess(Sets.newHashSet(info));

        //end deletion process
        if(this.isToNotify) {
            mockNotificationSuccess();
        }

        // it must remain only 1 FeatureEntity in database
        waitRequest(this.featureRepo, 1, 30000);
        // it mustn't be the created one of the fist feature creation
        assertNotEquals(featureInDatabase.getId(), this.featureRepo.findAll().get(0).getId());

    }

    @Test
    public void testDuplicatedFeatureCreationWithOverrideCaseNoFiles() throws InterruptedException {
        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(1, true,false);
        events.get(0).getFeature().setId("id");
        events.get(0).getFeature().setFiles(new ArrayList<>());
        publisher.publish(events);
        waitRequest(this.featureCreationRequestRepo, 1, 30000);

        this.featureCreationService.scheduleRequests();

        waitRequest(this.featureRepo, 1, 30000);

        // mock storage response to indicate creation succed
        FeatureEntity featureInDatabase = this.featureRepo.findAll().get(0);

        // end creation process
        if(this.isToNotify) {
            mockNotificationSuccess();
        }

        // publish a duplicated feature creation
        publisher.publish(events);
        waitRequest(this.featureCreationRequestRepo, 1, 30000);
        this.featureCreationService.scheduleRequests();
        waitRequest(this.featureRepo, 2, 30000);

        // that must publish a FeatureDeletionRequestEvent
        waitRequest(this.featureDeletionRequestRepo, 1, 30000);
        this.featureDeletionService.scheduleRequests();

        // end creation process
        if(this.isToNotify) {
            mockNotificationSuccess();
        }

        waitRequest(this.featureRepo, 1, 30000);
        // it mustn't be the created one of the fist feature creation
        assertNotEquals(featureInDatabase.getId(), this.featureRepo.findAll().get(0).getId());

    }

    @Test
    public void testDuplicatedFeatureCreationWithoutOverride() throws InterruptedException {
        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(1, true,false);
        events.get(0).getMetadata().setOverride(false);

        events.get(0).getFeature().setId("id");
        publisher.publish(events);
        waitRequest(this.featureCreationRequestRepo, 1, 30000);
        this.featureCreationService.scheduleRequests();
        waitRequest(this.featureRepo, 1, 30000);

        // mock storage response to indicate creation succed
        FeatureCreationRequest fcr = this.featureCreationRequestRepo.findAll().get(0);
        RequestInfo info = RequestInfo.build();
        fcr.setGroupId(info.getGroupId());
        fcr = this.featureCreationRequestRepo.save(fcr);
        this.listener.onStoreSuccess(Sets.newHashSet(info));

        // end creation process
        if(this.isToNotify) {
            mockNotificationSuccess();
        }

        // publish a duplicated feature creation
        publisher.publish(events);
        waitRequest(this.featureCreationRequestRepo, 1, 30000);
        this.featureCreationService.scheduleRequests();
        waitRequest(this.featureRepo, 2, 30000);

        // mock storage response to indicate creation succed
        fcr = this.featureCreationRequestRepo.findAll().get(0);
        fcr.setGroupId(info.getGroupId());
        fcr = this.featureCreationRequestRepo.save(fcr);
        this.listener.onStoreSuccess(Sets.newHashSet(info));

        // end creation process
        if(this.isToNotify) {
            mockNotificationSuccess();
        }

        // it must remain the 2 FeatureEntity in database
        waitRequest(this.featureRepo, 2, 30000);

    }

    @Test
    public void testDuplicatedFeatureCreationWithoutOverrideCaseNoFiles() throws InterruptedException {
        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(1, true,false);
        events.get(0).getMetadata().setOverride(false);

        events.get(0).getFeature().setId("id");
        publisher.publish(events);
        waitRequest(this.featureCreationRequestRepo, 1, 30000);
        this.featureCreationService.scheduleRequests();
        waitRequest(this.featureRepo, 1, 30000);

        // mock storage response to indicate creation succed
        FeatureCreationRequest fcr = this.featureCreationRequestRepo.findAll().get(0);
        RequestInfo info = RequestInfo.build();
        fcr.setGroupId(info.getGroupId());
        fcr = this.featureCreationRequestRepo.save(fcr);
        this.listener.onStoreSuccess(Sets.newHashSet(info));

        // end creation process
        if(this.isToNotify) {
            mockNotificationSuccess();
        }

        // publish a duplicated feature creation
        publisher.publish(events);
        waitRequest(this.featureCreationRequestRepo, 1, 30000);
        this.featureCreationService.scheduleRequests();

        // it must remain the 2 FeatureEntity in database
        waitRequest(this.featureRepo, 2, 30000);

        // end creation process
        if(this.isToNotify) {
            mockNotificationSuccess();
        }
    }
}
