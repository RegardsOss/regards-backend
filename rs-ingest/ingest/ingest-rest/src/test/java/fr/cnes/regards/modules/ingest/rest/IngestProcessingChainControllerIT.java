/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.rest;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.microservice.rest.MicroserviceConfigurationController;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;

/**
 *
 * Test processing chain API
 *
 * @author Marc Sordi
 *
 */
@RegardsTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest_it" })
public class IngestProcessingChainControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestProcessingChainControllerIT.class);

    @Test
    public void exportProcessingChain() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());

        ResultActions resultActions = performDefaultGet(IngestProcessingChainController.TYPE_MAPPING
                + IngestProcessingChainController.EXPORT_PATH, requestBuilderCustomizer,
                                                        "Default processing chain should be exported",
                                                        IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL);
        assertMediaType(resultActions, MediaType.APPLICATION_JSON_UTF8);
        String chain = payload(resultActions);
        LOGGER.debug(chain);
        Assert.assertNotNull(chain);
    }

    @Test
    public void importProcessingChain() {
        Path filePath = Paths.get("src", "test", "resources", "TestProcessingChain.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        performDefaultFileUpload(IngestProcessingChainController.TYPE_MAPPING
                + IngestProcessingChainController.IMPORT_PATH, filePath, requestBuilderCustomizer,
                                 "Should be able to import valid test processing chain");
    }

    @Test
    public void exportConfiguration() {
        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());

        performDefaultGet(MicroserviceConfigurationController.TYPE_MAPPING, requestBuilderCustomizer,
                          "Should export configuration");
    }

    @Test
    public void importConfiguration() {
        Path filePath = Paths.get("src", "test", "resources", "configuration.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());

        performDefaultFileUpload(MicroserviceConfigurationController.TYPE_MAPPING, filePath, requestBuilderCustomizer,
                                 "Should be able to import configuration");
    }
}
