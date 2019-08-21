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

import java.nio.file.Paths;
import java.time.OffsetDateTime;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIPBuilder;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;

/**
 * Test SIP flow handling
 *
 * @author Marc SORDI
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=sipflow",
        "regards.amqp.enabled=true", "regards.scheduler.pool.size=4", "regards.ingest.maxBulkSize=100" })
//@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=sipflow",
//        "regards.amqp.enabled=true", "regards.scheduler.pool.size=4",
//        "regards.jpa.multitenant.tenants[0].tenant=PROJECT",
//        "regards.jpa.multitenant.tenants[0].url=jdbc:postgresql://localhost:5432/rs_testdb_msordi",
//        "regards.jpa.multitenant.tenants[0].userName=azertyuiop123456789",
//        "regards.jpa.multitenant.tenants[0].password=azertyuiop123456789", "spring.rabbitmq.addresses=localhost:5672",
//        "regards.amqp.management.host=localhost", "regards.amqp.management.port=16672" })
@ActiveProfiles("testAmqp")
public class IngestPerformanceIT extends IngestMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestPerformanceIT.class);

    @Autowired
    private IPublisher publisher;

    @Override
    @Before
    public void doInit() {
        simulateApplicationReadyEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @Test
    public void generateAndPublish() throws InterruptedException {

        long start = System.currentTimeMillis();
        long existingItems = 0;
        long maxloops = 1000;
        for (long i = 0; i < maxloops; i++) {
            SIP sip = create("provider" + i);
            // Create event
            IngestMetadataDto mtd = IngestMetadataDto.build("source", OffsetDateTime.now().toString(),
                                                            IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                            StorageMetadata.build("fake", null));
            IngestRequestFlowItem flowItem = IngestRequestFlowItem.build(mtd, sip);
            publisher.publish(flowItem);
        }

        // Wait
        long countSip;
        do {
            countSip = sipRepository.count();
            LOGGER.debug("{} SIP(s) created in database", countSip);
            if (countSip >= maxloops + existingItems) {
                break;
            }
            Thread.sleep(1000);
        } while (true);

        LOGGER.info("END TEST : {} SIP(s) INGESTED in {} ms", maxloops + existingItems,
                    System.currentTimeMillis() - start);
    }

    private SIP create(String providerId) {
        // Init the builder
        SIPBuilder sipBuilder = new SIPBuilder(providerId);

        sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA,
                                                                Paths.get("src", "main", "test", "resources", "data",
                                                                          "cdpp_collection.json"),
                                                                "MD5", "azertyuiopqsdfmlmld");
        sipBuilder.getContentInformationBuilder().setSyntax(MediaType.APPLICATION_JSON_UTF8);
        sipBuilder.addContentInformation();

        // Add creation event
        sipBuilder.addEvent(String.format("SIP %s generated", providerId));

        return sipBuilder.build();
    }

}
