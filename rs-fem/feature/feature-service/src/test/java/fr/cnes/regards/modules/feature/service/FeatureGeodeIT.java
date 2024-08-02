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

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test feature mutation based on null property values.
 *
 * @author Marc SORDI
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_geode",
                                   "regards.amqp.enabled=true",
                                   "spring.task.scheduling.pool.size=2",
                                   "regards.feature.metrics.enabled=true" },
                    locations = { "classpath:regards_perf.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp" })
// Clean all context (schedulers)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
public class FeatureGeodeIT extends AbstractFeatureMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureGeodeIT.class);

    private static final Integer NB_FEATURES = 500;

    private static final String PROVIDER_ID_FORMAT = "F%05d";

    private static final Integer PUBLISH_BULK_SIZE = 100;

    @Test
    public void createAndUpdateTest() {

        // Request creations
        long creationStart = System.currentTimeMillis();
        requestCreation();
        // Wait for request handling and feature creation
        waitFeature(NB_FEATURES, null, 60_000);
        waitForStep(featureCreationRequestRepo, FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED, NB_FEATURES, 60_000);
        LOGGER.info(">>>>>>>>>>>>>>>>> {} creation requests done in {} ms",
                    NB_FEATURES,
                    System.currentTimeMillis() - creationStart);

        mockNotificationResponseSuccess();
        waitRequest(featureCreationRequestRepo, 0, 5_000);

        // Request update
        long updateStart = System.currentTimeMillis();
        OffsetDateTime requestDate = requestUpdate();
        // Wait for request handling and feature update
        waitFeature(NB_FEATURES, requestDate, 30_000);
        LOGGER.info(">>>>>>>>>>>>>>>>> {} update requests done in {} ms",
                    NB_FEATURES,
                    System.currentTimeMillis() - updateStart);

        waitForStep(featureUpdateRequestRepo, FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED, NB_FEATURES, 30_000);
        mockNotificationResponseSuccess();
        waitRequest(featureUpdateRequestRepo, 0, 5_000);

        LOGGER.info(">>>>>>>>>>>>>>>>> {} creation requests and {} update requests done in {} ms",
                    NB_FEATURES,
                    NB_FEATURES,
                    System.currentTimeMillis() - creationStart);
    }

    public void requestCreation() {
        FeatureCreationSessionMetadata metadata = FeatureCreationSessionMetadata.build("sessionOwner",
                                                                                       "session",
                                                                                       PriorityLevel.NORMAL,
                                                                                       Lists.emptyList(),
                                                                                       true,
                                                                                       false);

        long creationStart = System.currentTimeMillis();
        List<FeatureCreationRequestEvent> events = new ArrayList<>();
        int bulk = 0;
        for (int i = 1; i <= NB_FEATURES; i++) {
            bulk++;
            Feature feature = Feature.build(String.format(PROVIDER_ID_FORMAT, i),
                                            "owner",
                                            null,
                                            IGeometry.unlocated(),
                                            EntityType.DATA,
                                            geoModelName);
            GeodeProperties.addGeodeProperties(feature);
            events.add(FeatureCreationRequestEvent.build("sessionOwner", metadata, feature));

            if (bulk == PUBLISH_BULK_SIZE) {
                publish(events, "creation", i, NB_FEATURES);
                events.clear();
                bulk = 0;
            }
        }

        if (bulk > 0) {
            publish(events, "creation", NB_FEATURES, NB_FEATURES);
        }

        LOGGER.info(">>>>>>>>>>>>>>>>> {} creation requests published in {} ms",
                    NB_FEATURES,
                    System.currentTimeMillis() - creationStart);
    }

    private OffsetDateTime requestUpdate() {
        long updateStart = System.currentTimeMillis();
        OffsetDateTime requestDate = OffsetDateTime.now();
        FeatureMetadata featureMetadata = FeatureMetadata.build(PriorityLevel.NORMAL, new ArrayList<>());
        List<FeatureUpdateRequestEvent> uEvents = new ArrayList<>();
        int bulk = 0;
        for (int i = 1; i <= NB_FEATURES; i++) {
            bulk++;
            String id = String.format(PROVIDER_ID_FORMAT, i);
            Feature feature = Feature.build(id,
                                            "owner",
                                            getURN(id),
                                            IGeometry.unlocated(),
                                            EntityType.DATA,
                                            geoModelName);
            uEvents.add(FeatureUpdateRequestEvent.build("TEST", featureMetadata, feature, requestDate));

            if (bulk == PUBLISH_BULK_SIZE) {
                publish(uEvents, "update", i, NB_FEATURES);
                uEvents.clear();
                bulk = 0;
            }
        }

        if (bulk > 0) {
            publish(uEvents, "update", NB_FEATURES, NB_FEATURES);
        }

        LOGGER.info(">>>>>>>>>>>>>>>>> {} update requests published in {} ms",
                    NB_FEATURES,
                    System.currentTimeMillis() - updateStart);

        return requestDate;
    }

    private void publish(List<? extends ISubscribable> events, String type, int count, int total) {
        long creationStart = System.currentTimeMillis();
        publisher.publish(events);
        LOGGER.info(">>>>>>>>>>>>>>>>> {} {} events published in {} ms ({}/{})",
                    events.size(),
                    type,
                    System.currentTimeMillis() - creationStart,
                    count,
                    total);
    }

    private FeatureUniformResourceName getURN(String id) {
        UUID uuid = UUID.nameUUIDFromBytes(id.getBytes());
        return FeatureUniformResourceName.build(FeatureIdentifier.FEATURE,
                                                EntityType.DATA,
                                                getDefaultTenant(),
                                                uuid,
                                                1);
    }
}
