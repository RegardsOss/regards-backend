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
package fr.cnes.regards.modules.search.rest.engine;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.search.domain.ComplexSearchRequest;
import fr.cnes.regards.modules.search.domain.SearchRequest;
import fr.cnes.regards.modules.search.rest.ComplexSearchController;
import fr.cnes.regards.modules.search.service.engine.plugin.legacy.LegacySearchEngine;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.OpenSearchEngine;

@TestPropertySource(locations = { "classpath:test.properties" }, properties = { "regards.tenant=complex_search",
        "spring.jpa.properties.hibernate.default_schema=complex_search" })
@MultitenantTransactional
public class ComplexSeachControllerIT extends AbstractEngineIT {

    private SearchRequest createSearchRequest(String engineType, String datasetUrn, OffsetDateTime date,
            List<String> excludeIds) {
        return new SearchRequest(engineType, datasetUrn, new LinkedMultiValueMap<>(), Lists.newArrayList(), excludeIds,
                date);
    }

    private SearchRequest createSearchRequest(String engineType, String datasetUrn, String paramKey,
            String paramValue) {
        return createSearchRequest(engineType, datasetUrn, paramKey, paramValue, OffsetDateTime.now(), null);
    }

    private SearchRequest createSearchRequest(String engineType, String datasetUrn, String paramKey, String paramValue,
            OffsetDateTime date, List<String> excludeIds) {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.add(paramKey, paramValue);
        return new SearchRequest(engineType, datasetUrn, searchParameters, excludeIds, Lists.newArrayList(), date);
    }

    @Test
    public void searchAll() {
        ComplexSearchRequest request = new ComplexSearchRequest(Lists.newArrayList(DataType.values()));
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        // Should be 2 for the legacy request on planet type
        customizer.expect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", Matchers.equalTo(11)));
        performDefaultPost(ComplexSearchController.TYPE_MAPPING, request, customizer, "Search all error");
    }

    @Test
    public void searchAtttributes() {
        SearchRequest request = createSearchRequest(LegacySearchEngine.PLUGIN_ID, astroObjects.get(SOLAR_SYSTEM)
                .getIpId().toString(), "q", String.format("%s:%s", PLANET_TYPE, protect(PLANET_TYPE_GAS_GIANT)));
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        // Should be 8 attributes associated to model planet result of the dataobject search
        customizer.expect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(8)));
        performDefaultPost(ComplexSearchController.TYPE_MAPPING + ComplexSearchController.SEARCH_DATAOBJECTS_ATTRIBUTES,
                           request, customizer, "Search all error");
    }

    @Test
    public void searchWithSingleEngine() {
        ComplexSearchRequest request = new ComplexSearchRequest(Lists.newArrayList(DataType.values()));
        request.getRequests()
                .add(createSearchRequest(LegacySearchEngine.PLUGIN_ID,
                                         astroObjects.get(SOLAR_SYSTEM).getIpId().toString(), "q",
                                         String.format("%s:%s", PLANET_TYPE, protect(PLANET_TYPE_GAS_GIANT))));
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        // Should be 2 for the legacy request on planet type
        customizer.expect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", Matchers.equalTo(2)));
        performDefaultPost(ComplexSearchController.TYPE_MAPPING, request, customizer, "Search all error");
    }

    @Test
    public void searchWithSingleEngineAndUnknownDataset() {
        ComplexSearchRequest request = new ComplexSearchRequest(Lists.newArrayList(DataType.values()));
        request.getRequests()
                .add(createSearchRequest(LegacySearchEngine.PLUGIN_ID,
                                         "URN:AIP:" + EntityType.DATASET.toString() + ":PROJECT:" + UUID.randomUUID()
                                                 + ":V2",
                                         "q", String.format("%s:%s", PLANET_TYPE, protect(PLANET_TYPE_GAS_GIANT))));
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        // No entity matching the given dataset
        customizer.expect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", Matchers.equalTo(0)));
        performDefaultPost(ComplexSearchController.TYPE_MAPPING, request, customizer, "Search all error");
    }

    @Test
    public void searchWithSingleEngineAndOldDate() {
        ComplexSearchRequest request = new ComplexSearchRequest(Lists.newArrayList(DataType.values()));
        request.getRequests()
                .add(createSearchRequest(LegacySearchEngine.PLUGIN_ID,
                                         astroObjects.get(SOLAR_SYSTEM).getIpId().toString(), "q",
                                         String.format("%s:%s", PLANET_TYPE, protect(PLANET_TYPE_GAS_GIANT)),
                                         OffsetDateTime.now().minusDays(20), null));
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", Matchers.equalTo(0)));
        performDefaultPost(ComplexSearchController.TYPE_MAPPING, request, customizer, "Search all error");
    }

    @Test
    public void searchWithMultipleEngines() {
        ComplexSearchRequest request = new ComplexSearchRequest(Lists.newArrayList(DataType.values()));
        request.getRequests()
                .add(createSearchRequest(LegacySearchEngine.PLUGIN_ID,
                                         astroObjects.get(SOLAR_SYSTEM).getIpId().toString(), "q",
                                         String.format("%s:%s", PLANET_TYPE, protect(PLANET_TYPE_GAS_GIANT))));
        request.getRequests().add(createSearchRequest(OpenSearchEngine.ENGINE_ID, null, PLANET, MERCURY));
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        // Should be 2 for the legacy request on planet type
        // Should be 1 for the open search request on planet name
        customizer.expect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", Matchers.equalTo(3)));
        performDefaultPost(ComplexSearchController.TYPE_MAPPING, request, customizer, "Search all error");
    }

    @Test
    public void searchWithMultipleEnginesAndExcludeIds() {
        ComplexSearchRequest request = new ComplexSearchRequest(Lists.newArrayList(DataType.values()));
        request.getRequests()
                .add(createSearchRequest(LegacySearchEngine.PLUGIN_ID, null, "q",
                                         String.format("%s:%s", PLANET_TYPE, protect(PLANET_TYPE_GAS_GIANT)),
                                         OffsetDateTime.now(),
                                         Lists.newArrayList(astroObjects.get(JUPITER).getIpId().toString())));
        request.getRequests()
                .add(createSearchRequest(OpenSearchEngine.ENGINE_ID,
                                         astroObjects.get(SOLAR_SYSTEM).getIpId().toString(), PLANET, MERCURY));
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        // Should be 1 for the legacy request (2) on planet type (-1) on exluded ipId of jupiter.
        // Should be 1 for the open search request on planet name
        customizer.expect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", Matchers.equalTo(2)));
        performDefaultPost(ComplexSearchController.TYPE_MAPPING, request, customizer, "Search all error");
    }

    @Test
    public void searchWithSingleEngineAndExcludeIds() {
        ComplexSearchRequest request = new ComplexSearchRequest(Lists.newArrayList(DataType.values()));
        request.getRequests()
                .add(createSearchRequest(LegacySearchEngine.PLUGIN_ID, null, OffsetDateTime.now(),
                                         Lists.newArrayList(astroObjects.get(JUPITER).getIpId().toString())));
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        // Should be 9 for the legacy all request (-1) for excluded id of jupiter
        customizer.expect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", Matchers.equalTo(10)));
        performDefaultPost(ComplexSearchController.TYPE_MAPPING, request, customizer, "Search all error");
    }

    @Test
    public void computeDatasetSummary() {
        ComplexSearchRequest request = new ComplexSearchRequest(Lists.newArrayList(DataType.values()));
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        // Should be 2 for the legacy request on planet type
        customizer.expect(MockMvcResultMatchers.jsonPath("$.documentsCount", Matchers.equalTo(11)));
        customizer.expect(MockMvcResultMatchers.jsonPath("$.filesCount", Matchers.equalTo(1)));
        customizer.expect(MockMvcResultMatchers.jsonPath("$.filesSize", Matchers.equalTo(10)));
        performDefaultPost(ComplexSearchController.TYPE_MAPPING + ComplexSearchController.SUMMARY_MAPPING, request,
                           customizer, "Search all error");
    }
}
