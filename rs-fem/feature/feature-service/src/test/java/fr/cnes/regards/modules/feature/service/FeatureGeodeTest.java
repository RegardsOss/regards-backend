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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_geode_rc" })
@ActiveProfiles(value = { "noscheduler", "noFemHandler" })
public class FeatureGeodeTest extends AbstractFeatureMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureGeodeTest.class);

    @Autowired
    private Gson gson;

    @Override
    @Before
    public void before() throws Exception {
        super.before();
        mockModelClient("model_geode_V1.0.0.xml");
    }

    @Test
    public void parseGeodeRequest() throws UnsupportedEncodingException, IOException {
        try (JsonReader reader = new JsonReader(new InputStreamReader(
                Files.newInputStream(Paths.get("src", "test", "resources", "GEODE", "2321-feature-request.json")),
                "UTF-8"))) {
            FeatureCreationRequestEvent request = gson.fromJson(reader, FeatureCreationRequestEvent.class);
            LOGGER.debug(request.toString());
        }
    }
}
