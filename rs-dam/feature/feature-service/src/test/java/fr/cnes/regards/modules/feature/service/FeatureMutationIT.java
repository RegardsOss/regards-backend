/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureMetadata;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.model.dto.properties.IProperty;

/**
 * Test feature mutation based on null property values.
 *
 * @author Marc SORDI
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_mutation",
        "regards.amqp.enabled=true", "spring.jpa.properties.hibernate.jdbc.batch_size=1024",
        "spring.jpa.properties.hibernate.order_inserts=true" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class FeatureMutationIT extends AbstractFeatureMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureMutationIT.class);

    private static final Integer NB_FEATURES = 1;

    @Autowired
    private IFeatureCreationService featureCreationService;

    @Autowired
    private IFeatureUpdateService featureUpdateService;

    @Test
    public void createAndUpdateTest() {

        // Register creation requests
        FeatureMetadata metadata = FeatureMetadata.build("sessionOwner", "session", Lists.emptyList());
        String modelName = mockModelClient("feature_mutation_model.xml");
        List<FeatureCreationRequestEvent> events = new ArrayList<>();

        for (int i = 1; i <= NB_FEATURES; i++) {
            Feature feature = Feature.build(String.format("F%05d", i), null, IGeometry.unlocated(), EntityType.DATA,
                                            modelName);
            feature.addProperty(IProperty.buildString("data_type", "TYPE01"));
            feature.addProperty(IProperty.buildObject("file_characterization",
                                                      IProperty.buildBoolean("valid", Boolean.TRUE)));
            events.add(FeatureCreationRequestEvent.build(metadata, feature));
        }
        featureCreationService.registerRequests(events);

        // Schedule job
        featureCreationService.scheduleRequests();

        // Wait for feature creation
        waitFeature(NB_FEATURES, 10_000);
        // FIXME update request
    }
}
