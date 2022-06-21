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

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=feature_perf", "regards.amqp.enabled=true",
        "regards.feature.metrics.enabled=true" },
    locations = { "classpath:regards_perf.properties", "classpath:batch.properties", "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "noFemHandler" })
public class FeaturePerformanceGeodeIT extends AbstractFeatureMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturePerformanceGeodeIT.class);

    private static final Integer NB_FEATURES = 5_000;

    // Expected performance : 10_000 features/min
    private static final long DURATION = NB_FEATURES * 18;// i set 7 because someties jenkins VM sucks

    @Autowired
    private IFeatureCreationService featureService;

    /**
     * Test creation of EVENTS_NUMBER features Check if
     * {@link FeatureCreationRequest} and {@link FeatureEntity}are stored in
     * database then at the end of the job test if all
     * {@link FeatureCreationRequest} are deleted
     *
     * @throws InterruptedException
     */
    @Test
    public void createFeatures() throws InterruptedException {

        String format = "F%05d";

        // Register creation requests
        FeatureCreationSessionMetadata metadata = FeatureCreationSessionMetadata.build("sessionOwner",
                                                                                       "session",
                                                                                       PriorityLevel.NORMAL,
                                                                                       Lists.emptyList(),
                                                                                       true,
                                                                                       false);
        String modelName = mockModelClient(GeodeProperties.getGeodeModel());

        Thread.sleep(5_000);

        long start = System.currentTimeMillis();

        List<FeatureCreationRequestEvent> events = new ArrayList<>();
        int bulk = 0;
        for (int i = 1; i <= NB_FEATURES; i++) {
            bulk++;
            String id = String.format(format, i);
            Feature feature = Feature.build(id, "owner", null, IGeometry.unlocated(), EntityType.DATA, modelName);
            GeodeProperties.addGeodeProperties(feature);
            events.add(FeatureCreationRequestEvent.build("sessionOwner", metadata, feature));

            if (bulk == properties.getMaxBulkSize()) {
                saveEvents(events);
                events.clear();
                bulk = 0;
            }
        }

        if (bulk > 0) {
            saveEvents(events);
        }

        LOGGER.info(">>>>>>>>>>>>>>>>> {} requests registered in {} ms",
                    NB_FEATURES,
                    System.currentTimeMillis() - start);

        assertEquals(NB_FEATURES.longValue(), this.featureCreationRequestRepo.count());

        boolean schedule;
        do {
            schedule = featureService.scheduleRequests() > 0;
        } while (schedule);

        waitFeature(NB_FEATURES, null, 3600_000);

        long duration = System.currentTimeMillis() - start;
        LOGGER.info(">>>>>>>>>>>>>>>>> {} requests processed in {} ms", NB_FEATURES, duration);
        Assert.assertTrue(String.format("Performance not reached! (%dms/%dms)", duration, DURATION),
                          duration < DURATION);

        assertEquals(NB_FEATURES.longValue(), this.featureRepo.count());
    }

    private void saveEvents(List<FeatureCreationRequestEvent> events) {
        long start = System.currentTimeMillis();
        LOGGER.info(">>>>>>>>>>>>>>>>> Registering {} requests", events.size());
        featureService.registerRequests(events);
        LOGGER.info(">>>>>>>>>>>>>>>>> {} requests registered in {} ms",
                    events.size(),
                    System.currentTimeMillis() - start);
    }
}
