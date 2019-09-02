/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=sipflow",
        "spring.jpa.show-sql=true",
        "regards.amqp.enabled=true", "regards.scheduler.pool.size=4", "regards.ingest.maxBulkSize=100" })
@ActiveProfiles("testAmqp")
public class AIPServiceIT extends IngestMultitenantServiceTest {


    private static final Logger LOGGER = LoggerFactory.getLogger(AIPServiceIT.class);

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

    @Override
    public void doInit() {
        simulateApplicationReadyEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @Override
    protected void doAfter() throws Exception {
        // WARNING : clean context manually because Spring doesn't do it between tests
        subscriber.unsubscribeFrom(IngestRequestFlowItem.class);
    }

    @Test
    public void testSearchAIPEntity() throws InterruptedException {
        long nbSIP = 7;
        publishSIPEvent(create("provider 1", CATEGORIES_0, TAG_0), STORAGE_0, SESSION_0, SESSION_OWNER_0);
        publishSIPEvent(create("provider 2", CATEGORIES_1, TAG_0), STORAGE_0, SESSION_0, SESSION_OWNER_1);
        publishSIPEvent(create("provider 3", CATEGORIES_0, TAG_1), STORAGE_1, SESSION_0, SESSION_OWNER_0);
        publishSIPEvent(create("provider 4", CATEGORIES_1, TAG_1), STORAGE_1, SESSION_1, SESSION_OWNER_1);
        publishSIPEvent(create("provider 5", CATEGORIES_2, TAG_1), STORAGE_2, SESSION_1, SESSION_OWNER_1);
        publishSIPEvent(create("provider 6", CATEGORIES_0, TAG_0), STORAGE_2, SESSION_1, SESSION_OWNER_0);
        publishSIPEvent(create("provider 7", CATEGORIES_0, TAG_2), STORAGE_0, SESSION_1, SESSION_OWNER_0);
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 1000);

        Page<AIPEntity> results = aipService.search(null, null, null, TAG_0, null,
                null, null, Lists.newArrayList(STORAGE_0), null, PageRequest.of(0, 100));
        Assert.assertEquals(2, results.getTotalElements());


        results = aipService.search(null, null, null, TAG_1, null,
                null, null, Lists.newArrayList(STORAGE_1), CATEGORIES_0, PageRequest.of(0, 100));
        Assert.assertEquals(1, results.getTotalElements());


        results = aipService.search(null, null, null, null, null,
                null, null, null, CATEGORIES_0, PageRequest.of(0, 100));
        Assert.assertEquals(5, results.getTotalElements());


        results = aipService.search(null, null, null, null, null,
                null, "provider%", Lists.newArrayList(STORAGE_1, STORAGE_2), null, PageRequest.of(0, 100));
        Assert.assertEquals(4, results.getTotalElements());


        results = aipService.search(null, null, null, null, SESSION_OWNER_1,
                null, null, null, null, PageRequest.of(0, 100));
        Assert.assertEquals(3, results.getTotalElements());

        results = aipService.search(null, null, null, null, SESSION_OWNER_0,
                SESSION_1, null, null, null, PageRequest.of(0, 100));
        Assert.assertEquals(2, results.getTotalElements());


        results = aipService.search(null, OffsetDateTime.now().plusDays(50), null, null, null,
                null, null, null, null, PageRequest.of(0, 100));
        Assert.assertEquals(0, results.getTotalElements());

        results = aipService.search(null, OffsetDateTime.now().minusHours(5), OffsetDateTime.now().plusDays(5), null, null,
                null, null, null, null, PageRequest.of(0, 100));
        Assert.assertEquals(7, results.getTotalElements());


        results = aipService.search(null, null, null, Lists.newArrayList("toto"), null,
                null, null, null, null, PageRequest.of(0, 100));
        Assert.assertEquals(6, results.getTotalElements());

        results = aipService.search(null, null, null, TAG_0, null,
                null, null, null, null, PageRequest.of(0, 100));
        Assert.assertEquals(3, results.getTotalElements());


        results = aipService.search(AIPState.CREATED, null, null, null, null,
                null, null, null, null, PageRequest.of(0, 100));
        Assert.assertEquals(7, results.getTotalElements());


        results = aipService.search(AIPState.CREATED, OffsetDateTime.now().minusHours(5), OffsetDateTime.now().plusDays(5), TAG_1, SESSION_OWNER_1,
                SESSION_1, null, Lists.newArrayList(STORAGE_2), CATEGORIES_2, PageRequest.of(0, 100));
        Assert.assertEquals(1, results.getTotalElements());
    }

}
