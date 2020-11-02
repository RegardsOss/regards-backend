/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.amqp.ISubscriber;
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
import fr.cnes.regards.modules.ingest.dto.aip.SearchFacetsAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.SearchSelectionMode;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=sipflow",
        "spring.jpa.show-sql=false", "regards.amqp.enabled=true", "regards.scheduler.pool.size=4",
        "regards.ingest.maxBulkSize=100", "eureka.client.enabled=false",
        "regards.ingest.aip.delete.bulk.delay=100" }, locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock" })
public class AIPServiceIT extends IngestMultitenantServiceTest {

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
    private ISubscriber subscriber;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private StorageClientMock storageClient;

    @Override
    protected void doAfter() throws Exception {
        // WARNING : clean context manually because Spring doesn't do it between tests
        subscriber.unsubscribeFrom(IngestRequestFlowItem.class);
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_110")
    @Requirement("REGARDS_DSL_STO_AIP_130")
    @Purpose("Check that a AIP file is downloadable")
    public void testDownloadAIPFile() throws ModuleException, IOException, NoSuchAlgorithmException {
        storageClient.setBehavior(true, true);

        publishSIPEvent(create("provider 1", TAG_0), STORAGE_0, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        ingestServiceTest.waitForIngestion(1, 20000);

        Page<AIPEntity> results = aipService.findByFilters(SearchAIPsParameters.build(), PageRequest.of(0, 100));

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
        Assert.assertEquals(cs, calculatedCs);

    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_110"), @Requirement("REGARDS_DSL_STO_AIP_115"),
            @Requirement("REGARDS_DSL_STO_AIP_120"), @Requirement("REGARDS_DSL_STO_AIP_560") })
    @Purpose("Check that ingested AIPs are retrievable")
    public void testSearchAIPEntity() throws InterruptedException {
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

        Page<AIPEntity> results = aipService
                .findByFilters(SearchAIPsParameters.build().withTags(TAG_0).withStorages(STORAGE_0),
                               PageRequest.of(0, 100));
        Assert.assertEquals(2, results.getTotalElements());

        results = aipService
                .findByFilters(SearchAIPsParameters.build().withCategories(CATEGORIES_0).withStorages(STORAGE_1),
                               PageRequest.of(0, 100));
        Assert.assertEquals(1, results.getTotalElements());

        results = aipService.findByFilters(SearchAIPsParameters.build().withCategories(CATEGORIES_0),
                                           PageRequest.of(0, 100));
        Assert.assertEquals(5, results.getTotalElements());

        results = aipService.findByFilters(SearchAIPsParameters.build().withStorages(STORAGE_1, STORAGE_2),
                                           PageRequest.of(0, 100));
        Assert.assertEquals(4, results.getTotalElements());

        results = aipService.findByFilters(SearchAIPsParameters.build().withSessionOwner(SESSION_OWNER_1),
                                           PageRequest.of(0, 100));
        Assert.assertEquals(3, results.getTotalElements());

        results = aipService
                .findByFilters(SearchAIPsParameters.build().withSessionOwner(SESSION_OWNER_0).withSession(SESSION_1),
                               PageRequest.of(0, 100));
        Assert.assertEquals(2, results.getTotalElements());

        results = aipService
                .findByFilters(SearchAIPsParameters.build().withLastUpdateFrom(OffsetDateTime.now().plusDays(50)),
                               PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getTotalElements());

        results = aipService
                .findByFilters(SearchAIPsParameters.build().withLastUpdateFrom(OffsetDateTime.now().minusHours(5))
                        .withLastUpdateTo(OffsetDateTime.now().plusDays(5)), PageRequest.of(0, 100));
        Assert.assertEquals(7, results.getTotalElements());

        results = aipService.findByFilters(SearchAIPsParameters.build().withTag("toto"), PageRequest.of(0, 100));
        Assert.assertEquals(6, results.getTotalElements());

        results = aipService.findByFilters(SearchAIPsParameters.build().withTags(TAG_0), PageRequest.of(0, 100));
        Assert.assertEquals(6, results.getTotalElements());

        results = aipService.findByFilters(SearchAIPsParameters.build().withState(AIPState.STORED),
                                           PageRequest.of(0, 100));
        Assert.assertEquals(7, results.getTotalElements());

        results = aipService.findByFilters(SearchAIPsParameters.build().withState(AIPState.STORED)
                .withLastUpdateFrom(OffsetDateTime.now().minusHours(5))
                .withLastUpdateTo(OffsetDateTime.now().plusDays(5)).withTags(TAG_1).withSessionOwner(SESSION_OWNER_1)
                .withSession(SESSION_1).withStorages(STORAGE_2).withCategories(CATEGORIES_2)
                .withIpType(EntityType.DATA), PageRequest.of(0, 100));
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

        Page<AIPEntity> allAips = aipService.findByFilters(SearchAIPsParameters.build(), PageRequest.of(0, 100));
        Set<String> aipIds = allAips.stream().map(AIPEntity::getAipId).collect(Collectors.toSet());

        SearchFacetsAIPsParameters filters = SearchFacetsAIPsParameters.build().withState(AIPState.STORED)
                .withTags(TAG_0);
        List<String> results = aipService.findTags(filters);
        Assert.assertEquals(3, results.size());
        // Tests categories
        results = aipService.findCategories(filters);
        Assert.assertEquals(3, results.size());
        // Tests storages
        results = aipService.findStorages(filters);
        Assert.assertEquals(3, results.size());

        // Full test (with almost all attributes)
        filters = filters.withProviderIds("provider 1", "provider %")
                .withLastUpdateFrom(OffsetDateTime.now().minusHours(5))
                .withLastUpdateTo(OffsetDateTime.now().plusDays(6)).withAipIds(aipIds)
                .withCategories(Sets.newHashSet(CATEGORIES_0)).withStorages(Sets.newLinkedHashSet(STORAGE_0))
                .withSession(SESSION_0).withSessionOwner(SESSION_OWNER_0);
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
        filters.withSelectionMode(SearchSelectionMode.EXCLUDE);
        filters.withAipIds(aipIds);
        results = aipService.findTags(filters);
        Assert.assertEquals(0, results.size());

        // Test with session
        filters = SearchFacetsAIPsParameters.build().withSession(SESSION_0).withSessionOwner(SESSION_OWNER_0)
                .withStorages(STORAGE_0, STORAGE_1, STORAGE_2).withProviderId("provider%");
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
