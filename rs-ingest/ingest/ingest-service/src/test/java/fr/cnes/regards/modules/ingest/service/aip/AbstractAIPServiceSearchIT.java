/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.dto.SIPState;
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
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=sip_search",
                                   "spring.jpa.show-sql=false",
                                   "regards.amqp.enabled=true",
                                   "regards.ingest.maxBulkSize=100",
                                   "eureka.client.enabled=false",
                                   "regards.ingest.aip.delete.bulk.delay=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock" })
public abstract class AbstractAIPServiceSearchIT extends IngestMultitenantServiceIT {

    protected static final List<String> CATEGORIES_0 = Lists.newArrayList("CATEGORY");

    protected static final List<String> CATEGORIES_1 = Lists.newArrayList("CATEGORY1");

    protected static final List<String> CATEGORIES_2 = Lists.newArrayList("CATEGORY", "CATEGORY2");

    protected static final List<String> TAG_0 = Lists.newArrayList("toto", "tata");

    protected static final List<String> TAG_1 = Lists.newArrayList("toto", "tutu");

    protected static final List<String> TAG_2 = Lists.newArrayList("antonio", "farra's");

    protected static final String STORAGE_0 = "fake";

    protected static final String STORAGE_1 = "AWS";

    protected static final String STORAGE_2 = "Azure";

    protected static final String SESSION_OWNER_0 = "NASA";

    protected static final String SESSION_OWNER_1 = "CNES";

    public static final String SESSION_0 = OffsetDateTime.now().toString();

    public static final String SESSION_1 = OffsetDateTime.now().minusDays(4).toString();

    @Autowired
    protected IAIPService aipService;

    @Autowired
    protected StorageClientMock storageClient;

    @Autowired
    protected IngestRequestFlowHandler ingestRequestFlowHandler;

    @Before
    public void storeAIPS() {
        storageClient.setBehavior(true, true);
        List<IngestRequestFlowItem> sipEvents = List.of(createSipEvent("AIPServiceSearchIT 1",
                                                                       TAG_0,
                                                                       STORAGE_0,
                                                                       SESSION_0,
                                                                       SESSION_OWNER_0,
                                                                       CATEGORIES_0),
                                                        createSipEvent("AIPServiceSearchIT 2",
                                                                       TAG_0,
                                                                       STORAGE_0,
                                                                       SESSION_0,
                                                                       SESSION_OWNER_1,
                                                                       CATEGORIES_1),
                                                        createSipEvent("AIPServiceSearchIT 3",
                                                                       TAG_1,
                                                                       STORAGE_1,
                                                                       SESSION_0,
                                                                       SESSION_OWNER_0,
                                                                       CATEGORIES_0),
                                                        createSipEvent("AIPServiceSearchIT 4",
                                                                       TAG_1,
                                                                       STORAGE_1,
                                                                       SESSION_1,
                                                                       SESSION_OWNER_1,
                                                                       CATEGORIES_1),
                                                        createSipEvent("AIPServiceSearchIT 5",
                                                                       TAG_1,
                                                                       STORAGE_2,
                                                                       SESSION_1,
                                                                       SESSION_OWNER_1,
                                                                       CATEGORIES_2),
                                                        createSipEvent("AIPServiceSearchIT 6",
                                                                       TAG_0,
                                                                       STORAGE_2,
                                                                       SESSION_1,
                                                                       SESSION_OWNER_0,
                                                                       CATEGORIES_0),
                                                        createSipEvent("AIPServiceSearchIT 7",
                                                                       TAG_2,
                                                                       STORAGE_0,
                                                                       SESSION_1,
                                                                       SESSION_OWNER_0,
                                                                       CATEGORIES_0));
        int nbSIP = sipEvents.size();

        ingestRequestFlowHandler.handleBatch(sipEvents);
        waitSipCount(nbSIP);
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000, SIPState.STORED, getDefaultTenant());
    }

    protected abstract <T> T findByFilters(SearchAIPsParameters searchAIPsParameters);

    @Test
    public void test_search_AIP_with_tags_storages() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withTagsIncluded(TAG_0)
                                                                              .withStoragesIncluded(List.of(STORAGE_0));
        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(2, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_with_categories() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withCategoriesIncluded(CATEGORIES_0);
        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(5, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_with_categroies_storages() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withCategoriesIncluded(CATEGORIES_0)
                                                                              .withStoragesIncluded(List.of(STORAGE_1));
        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(1, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_with_storages() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withStoragesIncluded(Arrays.asList(
            STORAGE_1,
            STORAGE_2));
        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(4, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_with_session_owner() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withSessionOwner(SESSION_OWNER_1);
        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(3, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_with_session_owner_session() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withSessionOwner(SESSION_OWNER_0)
                                                                              .withSession(SESSION_1);
        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(2, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_with_last_update_from_50_days() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withLastUpdateAfter(OffsetDateTime.now()
                                                                                                                 .plusDays(
                                                                                                                     50));
        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(0, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_with_last_update_from_5_min_to_5_days() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withLastUpdateAfter(OffsetDateTime.now()
                                                                                                                 .minusHours(
                                                                                                                     5))
                                                                              .withLastUpdateBefore(OffsetDateTime.now()
                                                                                                                  .plusDays(
                                                                                                                      5));
        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(7, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_with_tags() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withTagsIncluded(List.of("toto"));
        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(6, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_with_tags_0() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withTagsIncluded(TAG_0);
        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(6, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_with_state_stored() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withStatesIncluded(List.of(AIPState.STORED));
        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(7, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_provider_id_like_ends_with() {
        // Given
        SearchAIPsParameters searchAIPsParametersEnds = new SearchAIPsParameters().withProviderIdsIncludedLike(List.of(
            "1"), ValuesRestrictionMatchMode.ENDS_WITH, false);

        // When
        Object resultsEnds = findByFilters(searchAIPsParametersEnds);

        // Then
        Assert.assertEquals(1, getTotalElements(resultsEnds));
    }

    @Test
    public void test_search_AIPS_provider_id_like_contains() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withProviderIdsIncludedLike(List.of(
            "PServiceS"), ValuesRestrictionMatchMode.CONTAINS, true);

        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(7, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_provider_id_like_start_with() {
        // Given
        SearchAIPsParameters searchAIPsParametersStarts = new SearchAIPsParameters().withProviderIdsIncludedLike(List.of(
            "AIP"), ValuesRestrictionMatchMode.STARTS_WITH, false);

        // When
        Object resultsStarts = findByFilters(searchAIPsParametersStarts);

        // Then
        Assert.assertEquals(7, getTotalElements(resultsStarts));
    }

    @Test
    public void test_search_AIPS_provider_id_like_no_result() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withProviderIdsIncludedLike(List.of(
            "%toto%"), ValuesRestrictionMatchMode.CONTAINS, true);

        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(0, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_provider_id_not_like() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withProviderIdsExcludedLike(List.of(
            "AIPServiceSearchIT"), ValuesRestrictionMatchMode.CONTAINS, true);

        // When
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(0, getTotalElements(results));
    }

    @Test
    public void test_search_AIPS_all_criterias() {
        // Given
        SearchAIPsParameters searchAIPsParameters = new SearchAIPsParameters().withLastUpdateAfter(OffsetDateTime.now()
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
        Object results = findByFilters(searchAIPsParameters);

        // Then
        Assert.assertEquals(1, getTotalElements(results));
    }

    protected long getTotalElements(Object results) {
        if (results instanceof Page) {
            return ((Page<?>) results).getTotalElements();
        } else if (results instanceof Slice) {
            return ((Slice<?>) results).getContent().size();
        }
        return 0;
    }
}
