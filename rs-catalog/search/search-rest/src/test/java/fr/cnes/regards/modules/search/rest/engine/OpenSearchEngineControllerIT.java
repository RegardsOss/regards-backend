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

import java.time.format.DateTimeFormatter;
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

import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.attribute.AbstractAttribute;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchEngine;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.description.DescriptionBuilder;

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
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.headers().setAccept(Arrays.asList(MediaType.APPLICATION_ATOM_XML));
        customizer.addParameter("page", "2");
        customizer.addParameter("maxRecords", "3");
        customizer.expect(MockMvcResultMatchers.xpath("feed/itemsPerPage").string(Matchers.is("3")));
        customizer.expect(MockMvcResultMatchers.xpath("feed/totalResults").string(Matchers.is("9")));
        customizer.expect(MockMvcResultMatchers.xpath("feed/startIndex").string(Matchers.is("4")));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataAtomWithParams() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.headers().setAccept(Arrays.asList(MediaType.APPLICATION_ATOM_XML));
        customizer.addParameter("page", "1");
        customizer.addParameter("maxRecords", "10");
        customizer.addParameter("properties.planet", "Mercury");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchAllDataJson() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.headers().setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        // Test specific opensearch pagination parameters
        customizer.addParameter(DescriptionBuilder.OPENSEARCH_PAGINATION_PAGE_NAME, "1");
        customizer.addParameter(DescriptionBuilder.OPENSEARCH_PAGINATION_COUNT_NAME, "100");
        customizer.expect(MockMvcResultMatchers.jsonPath("$.properties.totalResults", Matchers.equalTo(9)));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataJsonWithParams() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.headers().setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        customizer.addParameter("page", "1");
        customizer.addParameter("maxRecords", "100");
        customizer.addParameter("planet", "Mercury");
        customizer.expect(MockMvcResultMatchers.jsonPath("$.properties.totalResults", Matchers.equalTo(1)));
        customizer.expect(MockMvcResultMatchers.jsonPath("$.features.length()", Matchers.equalTo(1)));
        customizer
                .expect(MockMvcResultMatchers.jsonPath("$.features[0].properties.planet", Matchers.equalTo("Mercury")));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataAtomWithGeoBboxParams() throws XPathExpressionException {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.headers().setAccept(Arrays.asList(MediaType.APPLICATION_ATOM_XML));
        customizer.addParameter("page", "1");
        customizer.addParameter("maxRecords", "100");
        customizer.addParameter("box", "15.0,15.0,20.0,20.0");
        customizer.expect(MockMvcResultMatchers.xpath("feed/itemsPerPage").string(Matchers.is("100")));
        customizer.expect(MockMvcResultMatchers.xpath("feed/totalResults").string(Matchers.is("1")));
        customizer.expect(MockMvcResultMatchers.xpath("feed/startIndex").string(Matchers.is("1")));
        customizer.expect(MockMvcResultMatchers.xpath("feed/entry[1]/title").string(Matchers.is("Mercury")));
        // Validate time extension
        customizer.expect(MockMvcResultMatchers.xpath("feed/entry[1]/ValidTime/TimePeriod/beginPosition").exists());
        customizer.expect(MockMvcResultMatchers.xpath("feed/entry[1]/ValidTime/TimePeriod/endPosition").exists());
        // Validate geo extension
        customizer.expect(MockMvcResultMatchers.xpath("feed/entry[1]/where/Polygon").exists());
        // Validate media extension
        customizer.expect(MockMvcResultMatchers.xpath("feed/entry[1]/group/content[1]/category")
                .string(Matchers.is("QUICKLOOK")));
        customizer.expect(MockMvcResultMatchers.xpath("feed/entry[1]/group/content[2]/category")
                .string(Matchers.is("THUMBNAIL")));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataJsonWithGeoBboxParams() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.headers().setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        customizer.addParameter("page", "1");
        customizer.addParameter("maxRecords", "100");
        customizer.addParameter("box", "15.0,15.0,20.0,20.0");
        customizer.expect(MockMvcResultMatchers.jsonPath("$.properties.totalResults", Matchers.equalTo(1)));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataJsonWithTimeParams() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.headers().setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        customizer.addParameter("page", "1");
        customizer.addParameter("maxRecords", "100");
        customizer.addParameter("TimePeriod.startDate",
                                startDateValue.plusHours(1).format(DateTimeFormatter.ISO_DATE_TIME));
        customizer.addParameter("TimePeriod.stopDate", stopDateValue.format(DateTimeFormatter.ISO_DATE_TIME));
        customizer.expect(MockMvcResultMatchers.jsonPath("$.properties.totalResults", Matchers.equalTo(0)));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);

        customizer = customizer().expectStatusOk();
        customizer.headers().setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        customizer.addParameter("page", "1");
        customizer.addParameter("maxRecords", "100");
        customizer.addParameter("TimePeriod.startDate", startDateValue.format(DateTimeFormatter.ISO_DATE_TIME));
        customizer.addParameter("TimePeriod.stopDate", stopDateValue.format(DateTimeFormatter.ISO_DATE_TIME));
        customizer.expect(MockMvcResultMatchers.jsonPath("$.properties.totalResults", Matchers.equalTo(1)));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataJsonWithGeoCircleParams() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.headers().setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        customizer.addParameter("page", "1");
        customizer.addParameter("maxRecords", "100");
        customizer.addParameter("lon", "20.0");
        customizer.addParameter("lat", "20.0");
        customizer.addParameter("radius", "5.0");
        customizer.expect(MockMvcResultMatchers.jsonPath("$.properties.totalResults", Matchers.equalTo(1)));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void getDescription() throws XPathExpressionException {

        AbstractEntity<EntityFeature> mercury = getAstroObject("Mercury");

        String atomUrl = "OpenSearchDescription/Url[@type='" + MediaType.APPLICATION_ATOM_XML_VALUE + "']";
        String geoJsonUrl = "OpenSearchDescription/Url[@type='" + GeoJsonMediaType.APPLICATION_GEOJSON_VALUE + "']";

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.headers().setAccept(Arrays.asList(MediaType.APPLICATION_XML));

        // Check metadatas
        customizer.expect(MockMvcResultMatchers.xpath("OpenSearchDescription").exists());
        customizer.expect(MockMvcResultMatchers.xpath("OpenSearchDescription/Description").exists());
        customizer.expect(MockMvcResultMatchers.xpath("OpenSearchDescription/Contact").exists());
        customizer.expect(MockMvcResultMatchers.xpath("OpenSearchDescription/Image").exists());
        customizer.expect(MockMvcResultMatchers.xpath("OpenSearchDescription/Developer").exists());
        customizer.expect(MockMvcResultMatchers.xpath("OpenSearchDescription/Attribution").exists());
        customizer.expect(MockMvcResultMatchers.xpath("OpenSearchDescription/AdultContent").exists());
        customizer.expect(MockMvcResultMatchers.xpath("OpenSearchDescription/Language").exists());
        customizer.expect(MockMvcResultMatchers.xpath("OpenSearchDescription/InputEncoding").exists());
        customizer.expect(MockMvcResultMatchers.xpath("OpenSearchDescription/OutputEncoding").exists());

        // Check urls
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl).exists());
        customizer.expect(MockMvcResultMatchers.xpath(geoJsonUrl).exists());

        // Check url parameters
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "[count(Parameter)=15]").exists());
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "/Parameter[@name='q']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "/Parameter[@name='planet']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "/Parameter[@name='planet_type']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "/Parameter[@name='diameter']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "/Parameter[@name='sun_distance']").exists());
        customizer.expect(MockMvcResultMatchers
                .xpath(atomUrl + "/Parameter[@name='startDate' and @value='{time:start}']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "/Parameter[@name='stopDate' and @value='{time:end}']")
                .exists());
        customizer.expect(MockMvcResultMatchers
                .xpath(atomUrl + "/Parameter[@name='geometry' and @value='{geo:geometry}']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "/Parameter[@name='box' and @value='{geo:box}']")
                .exists());
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "/Parameter[@name='lon' and @value='{geo:lon}']")
                .exists());
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "/Parameter[@name='lat' and @value='{geo:lat}']")
                .exists());
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "/Parameter[@name='radius' and @value='{geo:radius}']")
                .exists());
        customizer.expect(
                          MockMvcResultMatchers
                                  .xpath(atomUrl + String.format("/Parameter[@name='%s' and @value='{%s}']",
                                                                 DescriptionBuilder.OPENSEARCH_PAGINATION_PAGE_NAME,
                                                                 DescriptionBuilder.OPENSEARCH_PAGINATION_PAGE))
                                  .exists());
        customizer.expect(
                          MockMvcResultMatchers
                                  .xpath(atomUrl + String.format("/Parameter[@name='%s' and @value='{%s}']",
                                                                 DescriptionBuilder.OPENSEARCH_PAGINATION_COUNT_NAME,
                                                                 DescriptionBuilder.OPENSEARCH_PAGINATION_COUNT))
                                  .exists());

        // Check options
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "/Parameter[@name='planet' and count(Option)=9]")
                .exists());

        // Check double boundaries
        customizer.expect(MockMvcResultMatchers
                .xpath(atomUrl + "/Parameter[@name='sun_distance' and @minInclusive='5.0E7']").exists());
        customizer.expect(MockMvcResultMatchers
                .xpath(atomUrl + "/Parameter[@name='sun_distance' and @maxInclusive='4.48943598E9']").exists());

        // Check date boundaries
        AbstractAttribute<?> startDate = mercury.getProperty("TimePeriod.startDate");
        AbstractAttribute<?> stopDate = mercury.getProperty("TimePeriod.stopDate");
        Assert.assertNotNull(startDate);
        Assert.assertNotNull(stopDate);

        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + String
                .format("/Parameter[@name='startDate' and @minInclusive='%s']", startDate.getValue().toString()))
                .exists());
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl
                + String.format("/Parameter[@name='stopDate' and @maxInclusive='%s']", stopDate.getValue().toString()))
                .exists());

        customizer.addParameter("token", "public_token");

        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING_EXTRA,
                          customizer, "open search description error", ENGINE_TYPE, OpenSearchEngine.EXTRA_DESCRIPTION);
    }

    @Test
    public void getDatasetDescription() {

        // Retrieve dataset URN
        Dataset solarSystem = getAstroObject(SOLAR_SYSTEM);
        Assert.assertNotNull(solarSystem);

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.headers().setAccept(Arrays.asList(MediaType.APPLICATION_XML));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING
                + SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA, customizer,
                          "open search description error", ENGINE_TYPE, solarSystem.getIpId().toString(),
                          OpenSearchEngine.EXTRA_DESCRIPTION);
    }

    @Test
    public void searchCollections() throws XPathExpressionException {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML_VALUE);
        customizer.addParameter("q", "properties." + STAR + ":Sun");
        customizer.expect(MockMvcResultMatchers.xpath("feed/totalResults").string(Matchers.is("1")));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchAllAtom() throws XPathExpressionException {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML_VALUE);
        customizer.addParameter("page", "1");
        customizer.addParameter("maxRecords", "100");
        customizer.expect(MockMvcResultMatchers.xpath("feed/totalResults").string(Matchers.is("13")));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_ALL_MAPPING, customizer,
                          "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchAllDataAtom() throws XPathExpressionException {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML_VALUE);
        customizer.addParameter("page", "1");
        customizer.addParameter("maxRecords", "100");
        customizer.expect(MockMvcResultMatchers.xpath("feed/totalResults").string(Matchers.is("9")));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchFomDatasetAtom() throws XPathExpressionException {

        // Retrieve dataset URN
        Dataset solarSystem = getAstroObject(SOLAR_SYSTEM);
        Assert.assertNotNull(solarSystem);

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML_VALUE);
        customizer.addParameter("page", "1");
        customizer.addParameter("maxRecords", "100");
        customizer.expect(MockMvcResultMatchers.xpath("feed/totalResults").string(Matchers.is("8")));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE, solarSystem.getIpId().toString());
    }

    @Test
    public void searchAllGeojson() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        customizer.addParameter("page", "1");
        customizer.addParameter("maxRecords", "100");
        customizer.expect(MockMvcResultMatchers.jsonPath("$.properties.totalResults", Matchers.equalTo(13)));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_ALL_MAPPING, customizer,
                          "Search all error", ENGINE_TYPE);
    }

}
