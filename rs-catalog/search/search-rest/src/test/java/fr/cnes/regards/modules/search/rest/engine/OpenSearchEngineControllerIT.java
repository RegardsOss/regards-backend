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
package fr.cnes.regards.modules.search.rest.engine;

import java.util.Arrays;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.net.HttpHeaders;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.search.rest.SearchEngineController;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchEngine;

/**
 * Search engine tests
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(locations = { "classpath:test.properties" },
        properties = { "regards.tenant=opensearch", "spring.jpa.properties.hibernate.default_schema=opensearch" })
@MultitenantTransactional
public class OpenSearchEngineControllerIT extends AbstractEngineIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchEngineControllerIT.class);

    private static final String ENGINE_TYPE = "opensearch";

    @Test
    public void searchDataAtom() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_ATOM_XML));
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "10");
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataAtomWithParams() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_ATOM_XML));
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "10");
        customizer.customizeRequestParam().param("properties.planet", "Mercury");
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataJson() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "10");
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataJsonWithParams() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "10");
        customizer.customizeRequestParam().param("properties.planet", "Mercury");
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void getDescription() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_XML));
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_DATAOBJECTS_MAPPING_EXTRA,
                          customizer, "open search description error", ENGINE_TYPE, OpenSearchEngine.EXTRA_DESCRIPTION);
    }

    @Test
    public void searchCollections() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.customizeHeaders().add(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML_VALUE);
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeRequestParam().param("q", "properties." + STAR + ":Sun");
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_COLLECTIONS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

}
