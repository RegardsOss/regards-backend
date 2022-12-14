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

import fr.cnes.regards.config.FeaturePerformanceITConfig;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.FeatureMetadata;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.notifier.client.INotifierRequestListener;
import org.assertj.core.util.Lists;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test feature mutation based on null property values.
 *
 * @author Marc SORDI
 */

@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=feature_perfit", "regards.amqp.enabled=true" },
    locations = { "classpath:regards_local.properties", "classpath:batch.properties", "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
//Clean all context (schedulers)
@Ignore("warning this test might not pass according to your setup for better perf test see FeatureGeodeIT")
@ContextConfiguration(classes = { FeaturePerformanceITConfig.class })
public class FeaturePerformanceIT extends AbstractFeatureMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturePerformanceIT.class);

    private static final Integer NB_FEATURES = 10_000;

    @Autowired
    private INotifierRequestListener notifierRequestListener;

    @Test
    public void createAndUpdateTest() throws InterruptedException {

        String format = "F%05d";

        // Register creation requests
        FeatureCreationSessionMetadata metadata = FeatureCreationSessionMetadata.build("sessionOwner",
                                                                                       "session",
                                                                                       PriorityLevel.NORMAL,
                                                                                       Lists.emptyList(),
                                                                                       true,
                                                                                       false);
        String modelName = mockModelClient("feature_mutation_model.xml",
                                           this.getCps(),
                                           this.getFactory(),
                                           this.getDefaultTenant(),
                                           this.getModelAttrAssocClientMock());

        Thread.sleep(5_000);

        long creationStart = System.currentTimeMillis();
        List<FeatureCreationRequestEvent> creationRequestEvents = new ArrayList<>();
        for (int i = 1; i <= NB_FEATURES; i++) {
            Feature feature = Feature.build(String.format(format, i),
                                            "owner",
                                            null,
                                            IGeometry.unlocated(),
                                            EntityType.DATA,
                                            modelName);
            feature.addProperty(IProperty.buildString("data_type", "TYPE01"));
            feature.addProperty(IProperty.buildObject("file_characterization",
                                                      IProperty.buildBoolean("valid", Boolean.TRUE)));
            creationRequestEvents.add(FeatureCreationRequestEvent.build("sessionOwner", metadata, feature));
        }
        publisher.publish(creationRequestEvents);

        // Wait for feature creation
        waitFeature(NB_FEATURES, null, 300_000);
        LOGGER.info(">>>>>>>>>>>>>>>>> {} creation requests done in {} ms",
                    NB_FEATURES,
                    System.currentTimeMillis() - creationStart);

        // Register update requests

        long updateStart = System.currentTimeMillis();
        OffsetDateTime requestDate = OffsetDateTime.now();
        List<FeatureUpdateRequestEvent> featureUpdateRequestEvents = new ArrayList<>();
        for (int i = 1; i <= NB_FEATURES; i++) {
            String id = String.format(format, i);
            Feature feature = Feature.build(id, "owner", getURN(id), IGeometry.unlocated(), EntityType.DATA, modelName);
            feature.addProperty(IProperty.buildObject("file_characterization",
                                                      IProperty.buildBoolean("valid", Boolean.FALSE),
                                                      IProperty.buildDate("invalidation_date", OffsetDateTime.now())));
            featureUpdateRequestEvents.add(FeatureUpdateRequestEvent.build("sessionOwner",
                                                                           FeatureMetadata.build(PriorityLevel.NORMAL,
                                                                                                 new ArrayList<>()),
                                                                           feature,
                                                                           requestDate));
        }
        publisher.publish(featureUpdateRequestEvents);

        // Wait for feature update
        waitFeature(NB_FEATURES, requestDate, 300_000);
        LOGGER.info(">>>>>>>>>>>>>>>>> {} update requests done in {} ms",
                    NB_FEATURES,
                    System.currentTimeMillis() - updateStart);

        if (initDefaultNotificationSettings()) {
            mockNotificationSuccess();
        }

        LOGGER.info(">>>>>>>>>>>>>>>>> {} creation requests and {} update requests done in {} ms",
                    NB_FEATURES,
                    NB_FEATURES,
                    System.currentTimeMillis() - creationStart);
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

