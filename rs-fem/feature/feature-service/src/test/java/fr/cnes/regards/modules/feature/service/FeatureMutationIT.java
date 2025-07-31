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

import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.settings.FeatureNotificationSettingsService;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.notifier.client.INotifierClient;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test feature mutation based on null property values.
 *
 * @author Marc SORDI
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_mutation",
                                   "regards.feature.delay.before.processing=1",
                                   "regards.amqp.enabled=true" },
                    locations = { "classpath:regards_perf.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles({ "testAmqp", "noscheduler", "noFemHandler" })
public class FeatureMutationIT extends AbstractFeatureMultitenantServiceIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureMutationIT.class);

    @Autowired
    private IFeatureCreationService featureCreationService;

    @Autowired
    private IFeatureUpdateService featureUpdateService;

    @Autowired
    private FeatureConfigurationProperties conf;

    @Autowired
    private FeatureNotificationSettingsService notificationSettingsService;

    @Autowired
    private IFeatureNotificationService notificationService;

    @MockBean
    private INotifierClient notifierClient;

    @Test
    public void createAndUpdateTest() throws EntityException {

        notificationSettingsService.setActiveNotification(false);

        FeatureCreationSessionMetadata metadata = FeatureCreationSessionMetadata.build("sessionOwner",
                                                                                       "session",
                                                                                       PriorityLevel.NORMAL,
                                                                                       List.of(),
                                                                                       true,
                                                                                       false);

        // Build feature to create
        String id = String.format("F%05d", 1);
        Feature feature = Feature.build(id, "owner", null, IGeometry.unlocated(), EntityType.DATA, mutationModelName);
        feature.addProperty(IProperty.buildString("data_type", "TYPE01"));
        feature.addProperty(IProperty.buildObject("file_characterization",
                                                  IProperty.buildBoolean("valid", Boolean.FALSE),
                                                  IProperty.buildDate("invalidation_date", OffsetDateTime.now())));

        // Register creation requests
        List<FeatureCreationRequestEvent> events = new ArrayList<>();
        events.add(FeatureCreationRequestEvent.build("sessionOwner", metadata, feature));
        featureCreationService.registerRequests(events);

        // Schedule creation job
        featureCreationService.scheduleRequests();

        // Wait for feature creation
        waitFeature(1, null, 10_000);

        // Retrieve feature from database
        FeatureEntity entity = featureRepo.findTop1VersionByProviderIdOrderByVersionAsc(id);

        // Build feature to update
        Feature updated = Feature.build(id,
                                        "owner",
                                        entity.getFeature().getUrn(),
                                        IGeometry.unlocated(),
                                        EntityType.DATA,
                                        mutationModelName);
        updated.addProperty(IProperty.buildObject("file_characterization",
                                                  IProperty.buildBoolean("valid", Boolean.TRUE),
                                                  IProperty.buildDate("invalidation_date", null)));

        // Register update requests
        List<FeatureUpdateRequestEvent> updateEvents = new ArrayList<>();
        updateEvents.add(FeatureUpdateRequestEvent.build("TEST",
                                                         FeatureMetadata.build(PriorityLevel.NORMAL, new ArrayList<>()),
                                                         updated));
        featureUpdateService.registerRequests(updateEvents);

        // Schedule update job after retention delay
        try {
            Thread.sleep(conf.getDelayBeforeProcessing() * 1000);
        } catch (InterruptedException e) {
            // Nothing to do
        }
        featureUpdateService.scheduleRequests();

        // Wait for feature creation
        waitUpdateRequestDeletion(0, 10_000);
    }

    /**
     * Update a feature and verify that that notifier is notified of the event, with the list of attributes changed
     * by the update.
     */
    @Test
    public void updateTestAndNotifyChanges() throws EntityException {
        notificationSettingsService.setActiveNotification(true);

        // Build feature to create
        String id = String.format("F%05d", 2);

        Feature feature = Feature.build(id,
                                        "owner",
                                        FeatureUniformResourceName.build(FeatureIdentifier.FEATURE,
                                                                         EntityType.DATA,
                                                                         "tenant",
                                                                         UUID.randomUUID(),
                                                                         1),
                                        IGeometry.unlocated(),
                                        EntityType.DATA,
                                        mutationModelName);
        feature.addProperty(IProperty.buildString("data_type", "TYPE01"));
        feature.addProperty(IProperty.buildObject("file_characterization",
                                                  IProperty.buildBoolean("valid", Boolean.FALSE),
                                                  IProperty.buildDate("invalidation_date", OffsetDateTime.now())));

        FeatureEntity entity = FeatureEntity.build("SessionOwner", "Session", feature, null, mutationModelName);
        featureRepo.save(entity);

        // Build feature to update
        Feature updated = Feature.build(id,
                                        "owner",
                                        entity.getFeature().getUrn(),
                                        IGeometry.unlocated(),
                                        EntityType.DATA,
                                        mutationModelName);
        updated.addProperty(IProperty.buildString("label", "theLabel"));
        updated.addProperty(IProperty.buildObject("file_characterization",
                                                  IProperty.buildBoolean("valid", Boolean.TRUE)));

        // Register update requests
        List<FeatureUpdateRequestEvent> updateEvents = new ArrayList<>();
        updateEvents.add(FeatureUpdateRequestEvent.build("TEST",
                                                         FeatureMetadata.build(PriorityLevel.NORMAL, new ArrayList<>()),
                                                         updated));
        featureUpdateService.registerRequests(updateEvents);

        // Schedule update job after retention delay
        try {
            Thread.sleep(conf.getDelayBeforeProcessing() * 1000);
        } catch (InterruptedException e) {
            // Nothing to do
        }
        featureUpdateService.scheduleRequests();

        waitForStep(featureUpdateRequestRepo, FeatureRequestStep.LOCAL_TO_BE_NOTIFIED, 1, 15_000);

        notificationService.sendToNotifier();

        @SuppressWarnings("unchecked") ArgumentCaptor<List<NotificationRequestEvent>> captor = ArgumentCaptor.forClass(
            List.class);
        Mockito.verify(notifierClient).sendNotifications(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        NotificationRequestEvent event = captor.getValue().get(0);
        assertThatJson(event.getMetadata()).isObject()
                                           .node("changedAttributes")
                                           .isArray()
                                           .containsExactlyInAnyOrder("properties.label",
                                                                      "properties.file_characterization.valid");
    }
}
