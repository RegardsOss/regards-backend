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
package fr.cnes.regards.modules.ingest.service.flow;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;

/**
 * Test SIP flow handling
 *
 * @author Marc SORDI
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=sipflow", "regards.amqp.enabled=true",
                "regards.scheduler.pool.size=4", "regards.ingest.maxBulkSize=100", "eureka.client.enabled=false" })
@ActiveProfiles("testAmqp")
public class IngestPerformanceIT extends IngestMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestPerformanceIT.class);

    private static final List<String> CATEGORIES = Lists.newArrayList("CATEGORY");

    @Autowired
    private ISubscriber subscriber;

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
    public void generateAndPublish() {

        long start = System.currentTimeMillis();
        long existingItems = 0;
        long maxloops = 1000;
        String session = OffsetDateTime.now().toString();
        for (long i = 0; i < maxloops; i++) {
            SIP sip = create("provider" + i, null);
            // Create event
            publishSIPEvent(sip, "fake", session, "source", CATEGORIES);
        }

        // Wait
        long countSip;
        ingestServiceTest.waitForIngestion(maxloops, maxloops * 1000);

        LOGGER.info("END TEST : {} SIP(s) INGESTED in {} ms", maxloops + existingItems,
                    System.currentTimeMillis() - start);
    }

}
