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
package fr.cnes.regards.modules.ingest.service.aip;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMatchMode;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntityLight;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.ingest.service.flow.IngestRequestFlowHandler;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test the searching of AIP entities
 *
 * @author Stephane Cortine
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=sip_search",
                                   "spring.jpa.show-sql=false",
                                   "regards.amqp.enabled=true",
                                   "regards.ingest.maxBulkSize=100",
                                   "eureka.client.enabled=false",
                                   "regards.ingest.aip.delete.bulk.delay=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock" })
public class AIPServiceSearchIT extends IngestMultitenantServiceIT {

    private static final List<String> CATEGORIES_0 = Lists.newArrayList("CATEGORY");

    private static final List<String> CATEGORIES_1 = Lists.newArrayList("CATEGORY1");

    private static final List<String> CATEGORIES_2 = Lists.newArrayList("CATEGORY", "CATEGORY2");

    private static final List<String> TAG_0 = Lists.newArrayList("toto", "tata");

    private static final List<String> TAG_1 = Lists.newArrayList("toto", "tutu");

    private static final List<String> TAG_2 = Lists.newArrayList("antonio", "farra's");

    private static final String STORAGE_0 = "fake";

    private static final String STORAGE_1 = "AWS";

    private static final String STORAGE_2 = "Azure";

    private static final String SESSION_OWNER_0 = "NASA";

    private static final String SESSION_OWNER_1 = "CNES";

    public static final String SESSION_0 = OffsetDateTime.now().toString();

    public static final String SESSION_1 = OffsetDateTime.now().minusDays(4).toString();

    @Autowired
    private IAIPService aipService;

    @Autowired
    private StorageClientMock storageClient;

    @Autowired
    private IngestRequestFlowHandler ingestRequestFlowHandler;

    @Before
    public void storeAIPS() {
        storageClient.setBehavior(true, true);
        int nbSIP = 7;
        List<IngestRequestFlowItem> sipEvents = new ArrayList<>();
        sipEvents.add(createSipEvent("AIPServiceSearchIT 1",
                                     TAG_0,
                                     STORAGE_0,
                                     SESSION_0,
                                     SESSION_OWNER_0,
                                     CATEGORIES_0));
        sipEvents.add(createSipEvent("AIPServiceSearchIT 2",
                                     TAG_0,
                                     STORAGE_0,
                                     SESSION_0,
                                     SESSION_OWNER_1,
                                     CATEGORIES_1));
        sipEvents.add(createSipEvent("AIPServiceSearchIT 3",
                                     TAG_1,
                                     STORAGE_1,
                                     SESSION_0,
                                     SESSION_OWNER_0,
                                     CATEGORIES_0));
        sipEvents.add(createSipEvent("AIPServiceSearchIT 4",
                                     TAG_1,
                                     STORAGE_1,
                                     SESSION_1,
                                     SESSION_OWNER_1,
                                     CATEGORIES_1));
        sipEvents.add(createSipEvent("AIPServiceSearchIT 5",
                                     TAG_1,
                                     STORAGE_2,
                                     SESSION_1,
                                     SESSION_OWNER_1,
                                     CATEGORIES_2));
        sipEvents.add(createSipEvent("AIPServiceSearchIT 6",
                                     TAG_0,
                                     STORAGE_2,
                                     SESSION_1,
                                     SESSION_OWNER_0,
                                     CATEGORIES_0));
        sipEvents.add(createSipEvent("AIPServiceSearchIT 7",
                                     TAG_2,
                                     STORAGE_0,
                                     SESSION_1,
                                     SESSION_OWNER_0,
                                     CATEGORIES_0));

        ingestRequestFlowHandler.handleBatch(sipEvents);
        waitSipCount(nbSIP);
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000, SIPState.STORED);
    }

    @Test
    public void test_search_AIP_with_tags_storages() {
        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withTagsIncluded(TAG_0)
                                                                              .withStoragesIncluded(List.of(STORAGE_0));
        // When
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));

        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(2, AIPEntityResults.getTotalElements());
        Assert.assertEquals(2, AIPEntityLightResults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_categories() {
        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withCategoriesIncluded(CATEGORIES_0);
        // When
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));

        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(5, AIPEntityResults.getTotalElements());
        Assert.assertEquals(5, AIPEntityLightResults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_categroies_storages() {
        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withCategoriesIncluded(CATEGORIES_0)
                                                                              .withStoragesIncluded(List.of(STORAGE_1));
        // When
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(1, AIPEntityResults.getTotalElements());
        Assert.assertEquals(1, AIPEntityLightResults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_storages() {
        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withStoragesIncluded(Arrays.asList(
            STORAGE_1,
            STORAGE_2));
        // When
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(4, AIPEntityResults.getTotalElements());
        Assert.assertEquals(4, AIPEntityLightResults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_session_owner() {
        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withSessionOwner(SESSION_OWNER_1);
        // When
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(3, AIPEntityResults.getTotalElements());
        Assert.assertEquals(3, AIPEntityLightResults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_session_owner_session() {
        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withSessionOwner(SESSION_OWNER_0)
                                                                              .withSession(SESSION_1);
        // When
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(2, AIPEntityResults.getTotalElements());
        Assert.assertEquals(2, AIPEntityLightResults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_last_update_from_50_days() {
        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withLastUpdateAfter(OffsetDateTime.now()
                                                                                                                 .plusDays(
                                                                                                                     50));
        // When
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(0, AIPEntityResults.getTotalElements());
        Assert.assertEquals(0, AIPEntityLightResults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_last_update_from_5_min_to_5_days() {
        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withLastUpdateAfter(OffsetDateTime.now()
                                                                                                                 .minusHours(
                                                                                                                     5))
                                                                              .withLastUpdateBefore(OffsetDateTime.now()
                                                                                                                  .plusDays(
                                                                                                                      5));
        // When
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));

        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(7, AIPEntityResults.getTotalElements());
        Assert.assertEquals(7, AIPEntityLightResults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_tags() {
        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withTagsIncluded(List.of("toto"));
        // When
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(6, AIPEntityResults.getTotalElements());
        Assert.assertEquals(6, AIPEntityLightResults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_tags_0() {
        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withTagsIncluded(TAG_0);
        // When
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));
        //Then
        Assert.assertEquals(6, AIPEntityResults.getTotalElements());
        Assert.assertEquals(6, AIPEntityLightResults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_state_stored() {
        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withStatesIncluded(List.of(AIPState.STORED));
        // When
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(7, AIPEntityResults.getTotalElements());
        Assert.assertEquals(7, AIPEntityLightResults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_provider_id_like() {

        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withProviderIdsIncludedLike(List.of(
            "PServiceS"), ValuesRestrictionMatchMode.CONTAINS, true);
        SearchAIPsParameters SearchAIPsParametersStarts = new SearchAIPsParameters().withProviderIdsIncludedLike(List.of(
            "AIP"), ValuesRestrictionMatchMode.STARTS_WITH, false);
        SearchAIPsParameters SearchAIPsParametersEnds = new SearchAIPsParameters().withProviderIdsIncludedLike(List.of(
            "1"), ValuesRestrictionMatchMode.ENDS_WITH, false);

        // When
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));
        Page<AIPEntity> AIPEntityResultsStarts = aipService.findByFilters(SearchAIPsParametersStarts,
                                                                          PageRequest.of(0, 100));
        Page<AIPEntity> AIPEntityResultsEnds = aipService.findByFilters(SearchAIPsParametersEnds,
                                                                        PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(7, AIPEntityLightResults.getTotalElements());
        Assert.assertEquals(7, AIPEntityResults.getTotalElements());
        Assert.assertEquals(7, AIPEntityResultsStarts.getTotalElements());
        Assert.assertEquals(1, AIPEntityResultsEnds.getTotalElements());

    }

    @Test
    public void test_search_AIPS_provider_id_like_no_result() {
        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withProviderIdsIncludedLike(List.of(
            "%toto%"), ValuesRestrictionMatchMode.CONTAINS, true);

        // When
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(0, AIPEntityLightResults.getTotalElements());
        Assert.assertEquals(0, AIPEntityResults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_provider_id_not_like() {

        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withProviderIdsExcludedLike(List.of(
            "AIPServiceSearchIT"), ValuesRestrictionMatchMode.CONTAINS, true);

        // When
        Page<AIPEntityLight> AIPEntityLightResults = aipService.findLightByFilters(SearchAIPsParameters,
                                                                                   PageRequest.of(0, 100));
        Page<AIPEntity> AIPEntityResults = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(0, AIPEntityLightResults.getTotalElements());
        Assert.assertEquals(0, AIPEntityResults.getTotalElements());

    }

    @Test
    public void test_search_AIPS_all_criterias() {
        // Given
        SearchAIPsParameters SearchAIPsParameters = new SearchAIPsParameters().withLastUpdateAfter(OffsetDateTime.now()
                                                                                                                 .minusHours(
                                                                                                                     5))
                                                                              .withLastUpdateBefore(OffsetDateTime.now()
                                                                                                                  .plusDays(
                                                                                                                      5))
                                                                              .withTagsIncluded(TAG_1)
                                                                              .withStoragesIncluded(List.of(STORAGE_2))
                                                                              .withCategoriesIncluded(CATEGORIES_2)
                                                                              .withAipIpType(List.of(EntityType.DATA))
                                                                              .withStatesIncluded(List.of(AIPState.STORED))
                                                                              .withSessionOwner(SESSION_OWNER_1)
                                                                              .withSession(SESSION_1);

        // When
        Page<AIPEntity> resultsAIPEntity = aipService.findByFilters(SearchAIPsParameters, PageRequest.of(0, 100));

        Page<AIPEntityLight> results = aipService.findLightByFilters(SearchAIPsParameters, PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(1, results.getTotalElements());
        Assert.assertEquals(1, resultsAIPEntity.getTotalElements());
    }

}
