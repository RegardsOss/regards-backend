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
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntityLight;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPLightParameters;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
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
import java.util.Arrays;
import java.util.List;

/**
 * Test the searching of AIP entities
 *
 * @author Stephane Cortine
 */
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=sipflow", "spring.jpa.show-sql=false",
        "regards.amqp.enabled=true", "regards.scheduler.pool.size=4", "regards.ingest.maxBulkSize=100",
        "eureka.client.enabled=false", "regards.ingest.aip.delete.bulk.delay=100" },
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

    @Before
    public void storeAIPS() {
        storageClient.setBehavior(true, true);
        long nbSIP = 7;
        publishSIPEvent(create("provider 1", TAG_0), STORAGE_0, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("provider 2", TAG_0), STORAGE_0, SESSION_0, SESSION_OWNER_1, CATEGORIES_1);
        publishSIPEvent(create("provider 3", TAG_1), STORAGE_1, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("provider 4", TAG_1), STORAGE_1, SESSION_1, SESSION_OWNER_1, CATEGORIES_1);
        publishSIPEvent(create("provider 5", TAG_1), STORAGE_2, SESSION_1, SESSION_OWNER_1, CATEGORIES_2);
        publishSIPEvent(create("provider 6", TAG_0), STORAGE_2, SESSION_1, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("provider 7", TAG_2), STORAGE_0, SESSION_1, SESSION_OWNER_0, CATEGORIES_0);
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000, SIPState.STORED);
    }

    @Test
    public void test_search_AIP_with_tags_storages() {
        // Given
        SearchAIPLightParameters searchAIPLightParameters = new SearchAIPLightParameters().withTagsIncluded(TAG_0)
                                                                                          .withStoragesIncluded(Arrays.asList(
                                                                                              STORAGE_0));
        // When
        Page<AIPEntity> AIPEntityresults = aipService.findByFilters(SearchAIPsParameters.build()
                                                                                        .withTags(TAG_0)
                                                                                        .withStorages(STORAGE_0),
                                                                    PageRequest.of(0, 100));

        Page<AIPEntityLight> AIPEntityLightresults = aipService.findLightByFilters(searchAIPLightParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(2, AIPEntityresults.getTotalElements());
        Assert.assertEquals(2, AIPEntityLightresults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_categories() {
        // Given
        SearchAIPLightParameters searchAIPLightParameters = new SearchAIPLightParameters().withCategoriesIncluded(
            CATEGORIES_0);
        // When
        Page<AIPEntity> AIPEntityresults = aipService.findByFilters(SearchAIPsParameters.build()
                                                                                        .withCategories(CATEGORIES_0),
                                                                    PageRequest.of(0, 100));

        Page<AIPEntityLight> AIPEntityLightresults = aipService.findLightByFilters(searchAIPLightParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(5, AIPEntityresults.getTotalElements());
        Assert.assertEquals(5, AIPEntityLightresults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_categroies_storages() {
        // Given
        SearchAIPLightParameters searchAIPLightParameters = new SearchAIPLightParameters().withCategoriesIncluded(
            CATEGORIES_0).withStoragesIncluded(Arrays.asList(STORAGE_1));
        // When
        Page<AIPEntity> AIPEntityresults = aipService.findByFilters(SearchAIPsParameters.build()
                                                                                        .withCategories(CATEGORIES_0)
                                                                                        .withStorages(STORAGE_1),
                                                                    PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightresults = aipService.findLightByFilters(searchAIPLightParameters,
                                                                                   PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(1, AIPEntityresults.getTotalElements());
        Assert.assertEquals(1, AIPEntityLightresults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_storages() {
        // Given
        SearchAIPLightParameters searchAIPLightParameters = new SearchAIPLightParameters().withStoragesIncluded(Arrays.asList(
            STORAGE_1,
            STORAGE_2));
        // When
        Page<AIPEntity> AIPEntityresults = aipService.findByFilters(SearchAIPsParameters.build()
                                                                                        .withStorages(STORAGE_1,
                                                                                                      STORAGE_2),
                                                                    PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightresults = aipService.findLightByFilters(searchAIPLightParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(4, AIPEntityresults.getTotalElements());
        Assert.assertEquals(4, AIPEntityLightresults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_session_owner() {
        // Given
        SearchAIPLightParameters searchAIPLightParameters = new SearchAIPLightParameters().withSessionOwner(
            SESSION_OWNER_1);
        // When
        Page<AIPEntity> AIPEntityresults = aipService.findByFilters(SearchAIPsParameters.build()
                                                                                        .withSessionOwner(
                                                                                            SESSION_OWNER_1),
                                                                    PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightresults = aipService.findLightByFilters(searchAIPLightParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(3, AIPEntityresults.getTotalElements());
        Assert.assertEquals(3, AIPEntityLightresults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_session_owner_session() {
        // Given
        SearchAIPLightParameters searchAIPLightParameters = new SearchAIPLightParameters().withSessionOwner(
            SESSION_OWNER_0).withSession(SESSION_1);
        // When
        Page<AIPEntity> AIPEntityresults = aipService.findByFilters(SearchAIPsParameters.build()
                                                                                        .withSessionOwner(
                                                                                            SESSION_OWNER_0)
                                                                                        .withSession(SESSION_1),
                                                                    PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightresults = aipService.findLightByFilters(searchAIPLightParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(2, AIPEntityresults.getTotalElements());
        Assert.assertEquals(2, AIPEntityLightresults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_last_update_from_50_days() {
        // Given
        SearchAIPLightParameters searchAIPLightParameters = new SearchAIPLightParameters().withLastUpdateAfter(
            OffsetDateTime.now().plusDays(50));
        // When
        Page<AIPEntity> AIPEntityresults = aipService.findByFilters(SearchAIPsParameters.build()
                                                                                        .withLastUpdateFrom(
                                                                                            OffsetDateTime.now()
                                                                                                          .plusDays(50)),
                                                                    PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightresults = aipService.findLightByFilters(searchAIPLightParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(0, AIPEntityresults.getTotalElements());
        Assert.assertEquals(0, AIPEntityLightresults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_last_update_from_5_min_to_5_days() {
        // Given
        SearchAIPLightParameters searchAIPLightParameters = new SearchAIPLightParameters().withLastUpdateAfter(
            OffsetDateTime.now().minusHours(5)).withLastUpdateBefore(OffsetDateTime.now().plusDays(5));
        // When
        Page<AIPEntity> AIPEntityresults = aipService.findByFilters(SearchAIPsParameters.build()
                                                                                        .withLastUpdateFrom(
                                                                                            OffsetDateTime.now()
                                                                                                          .minusHours(5))
                                                                                        .withLastUpdateTo(OffsetDateTime.now()
                                                                                                                        .plusDays(
                                                                                                                            5)),
                                                                    PageRequest.of(0, 100));

        Page<AIPEntityLight> AIPEntityLightresults = aipService.findLightByFilters(searchAIPLightParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(7, AIPEntityresults.getTotalElements());
        Assert.assertEquals(7, AIPEntityLightresults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_tags() {
        // Given
        SearchAIPLightParameters searchAIPLightParameters = new SearchAIPLightParameters().withTagsIncluded(Arrays.asList(
            "toto"));
        // When
        Page<AIPEntity> AIPEntityresults = aipService.findByFilters(SearchAIPsParameters.build().withTag("toto"),
                                                                    PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightresults = aipService.findLightByFilters(searchAIPLightParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(6, AIPEntityresults.getTotalElements());
        Assert.assertEquals(6, AIPEntityLightresults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_tags_0() {
        // Given
        SearchAIPLightParameters searchAIPLightParameters = new SearchAIPLightParameters().withTagsIncluded(TAG_0);
        // When
        Page<AIPEntity> AIPEntityresults = aipService.findByFilters(SearchAIPsParameters.build().withTags(TAG_0),
                                                                    PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightresults = aipService.findLightByFilters(searchAIPLightParameters,
                                                                                   PageRequest.of(0, 100));
        //Then
        Assert.assertEquals(6, AIPEntityresults.getTotalElements());
        Assert.assertEquals(6, AIPEntityLightresults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_with_state_stored() {
        // Given
        SearchAIPLightParameters searchAIPLightParameters = new SearchAIPLightParameters().withStatesIncluded(Arrays.asList(
            AIPState.STORED));
        // When
        Page<AIPEntity> AIPEntityresults = aipService.findByFilters(SearchAIPsParameters.build()
                                                                                        .withState(AIPState.STORED),
                                                                    PageRequest.of(0, 100));
        Page<AIPEntityLight> AIPEntityLightresults = aipService.findLightByFilters(searchAIPLightParameters,
                                                                                   PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(7, AIPEntityresults.getTotalElements());
        Assert.assertEquals(7, AIPEntityLightresults.getTotalElements());
    }

    @Test
    public void test_search_AIPS_all_criterias() {
        // Given
        SearchAIPLightParameters searchAIPLightParameters = new SearchAIPLightParameters().withLastUpdateAfter(
                                                                                              OffsetDateTime.now().minusHours(5))
                                                                                          .withLastUpdateBefore(
                                                                                              OffsetDateTime.now()
                                                                                                            .plusDays(5))
                                                                                          .withTagsIncluded(TAG_1)
                                                                                          .withStoragesIncluded(Arrays.asList(
                                                                                              STORAGE_2))
                                                                                          .withCategoriesIncluded(
                                                                                              CATEGORIES_2)
                                                                                          .withAipIpType(Arrays.asList(
                                                                                              EntityType.DATA))
                                                                                          .withStatesIncluded(Arrays.asList(
                                                                                              AIPState.STORED))
                                                                                          .withSessionOwner(
                                                                                              SESSION_OWNER_1)
                                                                                          .withSession(SESSION_1);

        // When
        Page<AIPEntity> resultsAIPEntity = aipService.findByFilters(SearchAIPsParameters.build()
                                                                                        .withState(AIPState.STORED)
                                                                                        .withLastUpdateFrom(
                                                                                            OffsetDateTime.now()
                                                                                                          .minusHours(5))
                                                                                        .withLastUpdateTo(OffsetDateTime.now()
                                                                                                                        .plusDays(
                                                                                                                            5))
                                                                                        .withTags(TAG_1)
                                                                                        .withSessionOwner(
                                                                                            SESSION_OWNER_1)
                                                                                        .withSession(SESSION_1)
                                                                                        .withStorages(STORAGE_2)
                                                                                        .withCategories(CATEGORIES_2)
                                                                                        .withIpType(EntityType.DATA),
                                                                    PageRequest.of(0, 100));

        Page<AIPEntityLight> results = aipService.findLightByFilters(searchAIPLightParameters, PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(1, results.getTotalElements());
        Assert.assertEquals(1, resultsAIPEntity.getTotalElements());
    }

}
