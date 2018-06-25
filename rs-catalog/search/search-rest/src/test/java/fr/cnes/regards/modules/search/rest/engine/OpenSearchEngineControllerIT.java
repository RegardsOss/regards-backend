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

import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.net.HttpHeaders;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.entities.domain.Dataset;
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
    public void searchDataAtom() throws XPathExpressionException {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_ATOM_XML));
        customizer.customizeRequestParam().param("page", "1");
        customizer.customizeRequestParam().param("size", "3");
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/itemsPerPage").string(Matchers.is("3")));
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/totalResults").string(Matchers.is("9")));
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/startIndex").string(Matchers.is("4")));
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
    public void searchAllDataJson() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "100");
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.properties.totalResults", Matchers.equalTo(9)));
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataJsonWithParams() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "100");
        customizer.customizeRequestParam().param("properties.planet", "Mercury");
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.properties.totalResults", Matchers.equalTo(1)));
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.features.length()", Matchers.equalTo(1)));
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.features[0].properties.planet",
                                                                 Matchers.equalTo("Mercury")));
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataAtomWithGeoBboxParams() throws XPathExpressionException {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_ATOM_XML));
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "100");
        customizer.customizeRequestParam().param("box", "15.0,15.0,20.0,20.0");
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/itemsPerPage").string(Matchers.is("100")));
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/totalResults").string(Matchers.is("1")));
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/startIndex").string(Matchers.is("1")));
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/entry[1]/title").string(Matchers.is("Mercury")));
        // Validate time extension
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/entry[1]/ValidTime/TimePeriod/beginPosition")
                .exists());
        customizer
                .addExpectation(MockMvcResultMatchers.xpath("feed/entry[1]/ValidTime/TimePeriod/endPosition").exists());
        // Validate geo extension
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/entry[1]/where/Polygon").exists());
        // Validate media extension
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/entry[1]/group/content[1]/category")
                .string(Matchers.is("QUICKLOOK")));
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/entry[1]/group/content[2]/category")
                .string(Matchers.is("THUMBNAIL")));
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataJsonWithGeoBboxParams() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "100");
        customizer.customizeRequestParam().param("box", "15.0,15.0,20.0,20.0");
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.properties.totalResults", Matchers.equalTo(1)));
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataJsonWithGeoCircleParams() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "100");
        customizer.customizeRequestParam().param("lon", "20.0");
        customizer.customizeRequestParam().param("lat", "20.0");
        customizer.customizeRequestParam().param("radius", "5.0");
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.properties.totalResults", Matchers.equalTo(1)));
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
    public void getDatasetDescription() {

        // Retrieve dataset URN
        Dataset solarSystem = getAstroObject(SOLAR_SYSTEM);
        Assert.assertNotNull(solarSystem);

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_XML));
        performDefaultGet(SearchEngineController.TYPE_MAPPING
                + SearchEngineController.SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA, customizer,
                          "open search description error", ENGINE_TYPE, solarSystem.getIpId().toString(),
                          OpenSearchEngine.EXTRA_DESCRIPTION);
    }

    @Test
    public void searchCollections() throws XPathExpressionException {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.customizeHeaders().add(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML_VALUE);
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeRequestParam().param("q", "properties." + STAR + ":Sun");
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/totalResults").string(Matchers.is("1")));
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_COLLECTIONS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchAllAtom() throws XPathExpressionException {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.customizeHeaders().add(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML_VALUE);
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "100");
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/totalResults").string(Matchers.is("13")));
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_ALL_MAPPING, customizer,
                          "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchAllDataAtom() throws XPathExpressionException {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.customizeHeaders().add(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML_VALUE);
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "100");
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/totalResults").string(Matchers.is("9")));
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchFomDatasetAtom() throws XPathExpressionException {

        // Retrieve dataset URN
        Dataset solarSystem = getAstroObject(SOLAR_SYSTEM);
        Assert.assertNotNull(solarSystem);

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.customizeHeaders().add(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML_VALUE);
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "100");
        customizer.addExpectation(MockMvcResultMatchers.xpath("feed/totalResults").string(Matchers.is("8")));
        performDefaultGet(SearchEngineController.TYPE_MAPPING
                + SearchEngineController.SEARCH_DATASET_DATAOBJECTS_MAPPING, customizer, "Search all error",
                          ENGINE_TYPE, solarSystem.getIpId().toString());
    }

    @Test
    public void searchAllGeojson() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.customizeHeaders().add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "100");
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.properties.totalResults", Matchers.equalTo(13)));
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_ALL_MAPPING, customizer,
                          "Search all error", ENGINE_TYPE);
    }

}
