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
package fr.cnes.regards.modules.feature.rest;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;

/**
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature",
        "regards.amqp.enabled=true", "spring.jpa.properties.hibernate.jdbc.batch_size=1024",
        "spring.jpa.properties.hibernate.order_inserts=true" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
@ContextConfiguration(classes = { AbstractMultitenantServiceTest.ScanningConfiguration.class })
public class FeatureEntityControllerIT extends AbstractFeatureIT {

    @Test
    public void getFeatures() throws Exception {
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());

        createFeatures("feature_1_", 10, "source1", "session1");
        createFeatures("feature_2_", 10, "source1", "session2");
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$.content")
                .expectToHaveSize("$.content", 20);
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.addParameter("model", "FEATURE01");
        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT, requestBuilderCustomizer,
                          "Error retrieving features");

        requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$.content")
                .expectToHaveSize("$.content", 10);
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.addParameter("session", "session2");
        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT, requestBuilderCustomizer,
                          "Error retrieving features");

        requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$.content")
                .expectToHaveSize("$.content", 1);
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.addParameter("source", "source1");
        requestBuilderCustomizer.addParameter("model", "FEATURE01");
        requestBuilderCustomizer.addParameter("providerId", "feature_1_5");
        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT, requestBuilderCustomizer,
                          "Error retrieving features");

        requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$.content")
                .expectToHaveSize("$.content", 0);
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.addParameter("source", "source1");
        requestBuilderCustomizer.addParameter("session", "session2");
        requestBuilderCustomizer.addParameter("model", "FEATURE01");
        requestBuilderCustomizer.addParameter("providerId", "feature_1_5");
        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT, requestBuilderCustomizer,
                          "Error retrieving features");
    }

}
