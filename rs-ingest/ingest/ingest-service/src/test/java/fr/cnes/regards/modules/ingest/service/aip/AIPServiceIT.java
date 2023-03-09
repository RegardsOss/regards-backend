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
import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMatchMode;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=sipflow",
                                   "spring.jpa.show-sql=false",
                                   "regards.amqp.enabled=true",
                                   "spring.task.scheduling.pool.size=4",
                                   "regards.ingest.maxBulkSize=100",
                                   "eureka.client.enabled=false",
                                   "regards.ingest.aip.delete.bulk.delay=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock" })
public class AIPServiceIT extends IngestMultitenantServiceIT {

    private static final List<String> CATEGORIES_0 = Lists.newArrayList("CATEGORY");

    private static final List<String> CATEGORIES_1 = Lists.newArrayList("CATEGORY1");

    private static final List<String> CATEGORIES_2 = Lists.newArrayList("CATEGORY", "CATEGORY2");

    public static final String TEST_TAG = "toto";

    private static final List<String> TAG_0 = Lists.newArrayList(TEST_TAG, "tata");

    private static final List<String> TAG_1 = Lists.newArrayList(TEST_TAG, "tutu");

    private static final List<String> TAG_2 = Lists.newArrayList("antonio", "farra's");

    private static final String STORAGE_0 = "fake";

    private static final String STORAGE_1 = "AWS";

    private static final String STORAGE_2 = "Azure";

    private static final String SESSION_OWNER_0 = "NASA";

    private static final String SESSION_OWNER_1 = "CNES";

    public static final String SESSION_0 = OffsetDateTime.now().toString();

    public static final String SESSION_1 = OffsetDateTime.now().minusDays(4).toString();

    public static final String TEST_ORIGIN_URN = "testOriginUrn";

    public static final String ORIGIN_URN_ADDITIONAL_INFORMATION = "originUrn";

    @Autowired
    private IAIPService aipService;

    @Autowired
    private StorageClientMock storageClient;

    @Autowired
    private Gson gson;

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_110")
    @Requirement("REGARDS_DSL_STO_AIP_130")
    @Purpose("Check that a AIP file is downloadable")
    public void testDownloadAIPFile() throws ModuleException, IOException, NoSuchAlgorithmException {
        storageClient.setBehavior(true, true);

        publishSIPEvent(create("provider 1", TAG_0), STORAGE_0, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        ingestServiceTest.waitForIngestion(1, 20000);

        Page<AIPEntity> results = aipService.findByFilters(new SearchAIPsParameters(), PageRequest.of(0, 100));

        Assert.assertEquals(1, results.getTotalElements());

        AIPEntity aip = results.getContent().get(0);

        MockHttpServletResponse response = new MockHttpServletResponse();

        aipService.downloadAIP(aip.getAipIdUrn(), response);
        response.getOutputStream().flush();

        OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(new File("target/aip.json")),
                                                            StandardCharsets.UTF_8);

        String aipFileContent = response.getContentAsString();
        fstream.write(aipFileContent);
        fstream.flush();
        fstream.close();

        String cs = aipService.calculateChecksum(aip.getAip());
        String calculatedCs = ChecksumUtils.computeHexChecksum(new FileInputStream(new File("target/aip.json")), "MD5");

        Assert.assertNotNull(cs);
        Assert.assertNotNull(calculatedCs);
        if (!cs.equals(calculatedCs)) {
            LOGGER.error("The real AIP looks like {}", gson.toJsonTree(aip.getAip()).toString());
            LOGGER.error("The AIP downloaded looks like {}", gson.toJsonTree(response.getContentAsString()).toString());
        }
        Assert.assertEquals("This test failed as the AIP has not the right checksum", cs, calculatedCs);

    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_110"),
                    @Requirement("REGARDS_DSL_STO_AIP_115"),
                    @Requirement("REGARDS_DSL_STO_AIP_120"),
                    @Requirement("REGARDS_DSL_STO_AIP_560") })
    @Purpose("Check that ingested AIPs are retrievable")
    public void testSearchAIPEntity() throws InterruptedException {
        // Given
        storageClient.setBehavior(true, true);
        long nbSIP = 7;
        publishSIPEvent(create("provider 1", TAG_0), STORAGE_0, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("provider 2", TAG_0), STORAGE_0, SESSION_0, SESSION_OWNER_1, CATEGORIES_1);
        publishSIPEvent(create("provider 3", TAG_1), STORAGE_1, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("provider 4", TAG_1), STORAGE_1, SESSION_1, SESSION_OWNER_1, CATEGORIES_1);
        publishSIPEvent(create("provider 5", TAG_1), STORAGE_2, SESSION_1, SESSION_OWNER_1, CATEGORIES_2);
        publishSIPEvent(create("provider 6", TAG_0), STORAGE_2, SESSION_1, SESSION_OWNER_0, CATEGORIES_0);
        SIP sip = create("provider 7", TAG_2);
        sip.withAdditionalProvenanceInformation(ORIGIN_URN_ADDITIONAL_INFORMATION, TEST_ORIGIN_URN);
        publishSIPEvent(sip, STORAGE_0, SESSION_1, SESSION_OWNER_0, CATEGORIES_0);
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000, SIPState.STORED);

        // When
        Page<AIPEntity> results = aipService.findByFilters(new SearchAIPsParameters().withTagsIncluded(TAG_0)
                                                                                     .withStoragesIncluded(List.of(
                                                                                         STORAGE_0)),
                                                           PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(2, results.getTotalElements());

        // When
        results = aipService.findByFilters(new SearchAIPsParameters().withCategoriesIncluded(CATEGORIES_0)
                                                                     .withStoragesIncluded(List.of(STORAGE_1)),
                                           PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(1, results.getTotalElements());

        // When
        results = aipService.findByFilters(new SearchAIPsParameters().withCategoriesIncluded(CATEGORIES_0),
                                           PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(5, results.getTotalElements());

        // When
        results = aipService.findByFilters(new SearchAIPsParameters().withStoragesIncluded(List.of(STORAGE_1,
                                                                                                   STORAGE_2)),
                                           PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(4, results.getTotalElements());

        // When
        results = aipService.findByFilters(new SearchAIPsParameters().withSessionOwner(SESSION_OWNER_1),
                                           PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(3, results.getTotalElements());

        // When
        results = aipService.findByFilters(new SearchAIPsParameters().withSessionOwner(SESSION_OWNER_0)
                                                                     .withSession(SESSION_1), PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(2, results.getTotalElements());

        // When
        results = aipService.findByFilters(new SearchAIPsParameters().withLastUpdateAfter(OffsetDateTime.now()
                                                                                                        .plusDays(50)),
                                           PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(0, results.getTotalElements());

        // When
        results = aipService.findByFilters(new SearchAIPsParameters().withLastUpdateAfter(OffsetDateTime.now()
                                                                                                        .minusHours(5))
                                                                     .withLastUpdateBefore(OffsetDateTime.now()
                                                                                                         .plusDays(5)),
                                           PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(7, results.getTotalElements());

        // When
        results = aipService.findByFilters(new SearchAIPsParameters().withTagsIncluded(List.of(TEST_TAG)),
                                           PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(6, results.getTotalElements());

        // When
        results = aipService.findByFilters(new SearchAIPsParameters().withTagsIncluded(TAG_0), PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(6, results.getTotalElements());

        // When
        results = aipService.findByFilters(new SearchAIPsParameters().withStatesIncluded(List.of(AIPState.STORED)),
                                           PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(7, results.getTotalElements());

        // When
        results = aipService.findByFilters(new SearchAIPsParameters().withStatesIncluded(List.of(AIPState.STORED))
                                                                     .withLastUpdateAfter(OffsetDateTime.now()
                                                                                                        .minusHours(5))
                                                                     .withLastUpdateBefore(OffsetDateTime.now()
                                                                                                         .plusDays(5))
                                                                     .withTagsIncluded(TAG_1)
                                                                     .withSessionOwner(SESSION_OWNER_1)
                                                                     .withSession(SESSION_1)
                                                                     .withStoragesIncluded(List.of(STORAGE_2))
                                                                     .withCategoriesIncluded(CATEGORIES_2)
                                                                     .withAipIpType(List.of(EntityType.DATA)),
                                           PageRequest.of(0, 100));
        // Then
        Assert.assertEquals(1, results.getTotalElements());

        //When
        results = aipService.findByFilters(new SearchAIPsParameters().withOriginUrn(TEST_ORIGIN_URN),
                                           PageRequest.of(0, 100));

        // Then
        Assert.assertEquals(1, results.getTotalElements());
    }

    @Test
    public void testOtherSearchEndpoints() throws InterruptedException {
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

        Page<AIPEntity> allAips = aipService.findByFilters(new SearchAIPsParameters(), PageRequest.of(0, 100));
        Set<String> aipIds = allAips.stream().map(AIPEntity::getAipId).collect(Collectors.toSet());

        SearchAIPsParameters filters = new SearchAIPsParameters().withStatesIncluded(List.of(AIPState.STORED))
                                                                 .withTagsIncluded(TAG_0);
        List<String> results = aipService.findTags(filters);
        Assert.assertEquals(3, results.size());
        // Tests categories
        results = aipService.findCategories(filters);
        Assert.assertEquals(3, results.size());
        // Tests storages
        results = aipService.findStorages(filters);
        Assert.assertEquals(3, results.size());

        // Full test (with almost all attributes)
        filters = filters.withProviderIdsIncluded(List.of("provider 1", "provider"))
                         .withLastUpdateAfter(OffsetDateTime.now().minusHours(5))
                         .withLastUpdateBefore(OffsetDateTime.now().plusDays(6))
                         .withAipIdsIncluded(aipIds)
                         .withCategoriesIncluded(Sets.newHashSet(CATEGORIES_0))
                         .withStoragesIncluded(Sets.newLinkedHashSet(STORAGE_0))
                         .withSession(SESSION_0)
                         .withSessionOwner(SESSION_OWNER_0);

        allAips = aipService.findByFilters(filters, PageRequest.of(0, 100));
        // Test tags
        results = aipService.findTags(filters);
        Assert.assertEquals(2, results.size());
        // Tests categories
        results = aipService.findCategories(filters);
        Assert.assertEquals(1, results.size());
        // Tests storages
        results = aipService.findStorages(filters);
        Assert.assertEquals(1, results.size());

        // Test the aipIdsExcluded
        filters.withAipIdsExcluded(aipIds);
        results = aipService.findTags(filters);
        Assert.assertEquals(0, results.size());

        // Test ignoreCase
        filters = new SearchAIPsParameters().withProviderIdsIncludedLike(List.of("ProVider 1"),
                                                                         ValuesRestrictionMatchMode.CONTAINS,
                                                                         true);
        results = aipService.findTags(filters);
        Assert.assertEquals(2, results.size());

        filters = new SearchAIPsParameters().withAipIdsExcluded(List.of())
                                            .withProviderIdsIncludedLike(List.of("ProVider 1"),
                                                                         ValuesRestrictionMatchMode.CONTAINS,
                                                                         false);
        results = aipService.findTags(filters);
        Assert.assertEquals(0, results.size());

        // Test with session
        filters = new SearchAIPsParameters().withSession(SESSION_0)
                                            .withSessionOwner(SESSION_OWNER_0)
                                            .withStoragesIncluded(List.of(STORAGE_0, STORAGE_1, STORAGE_2))
                                            .withProviderIdsIncludedLike(List.of("provider"),
                                                                         ValuesRestrictionMatchMode.STARTS_WITH,
                                                                         false);
        results = aipService.findTags(filters);
        Assert.assertEquals(3, results.size());
        // Tests categories
        results = aipService.findCategories(filters);
        Assert.assertEquals(1, results.size());
        // Tests storages
        results = aipService.findStorages(filters);
        Assert.assertEquals(2, results.size());
    }

}
