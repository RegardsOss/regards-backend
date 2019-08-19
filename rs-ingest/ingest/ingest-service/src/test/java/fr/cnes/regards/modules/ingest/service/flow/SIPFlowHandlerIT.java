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
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.sip.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIPBuilder;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;

/**
 * Test SIP flow handling
 *
 * @author Marc SORDI
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=sipflow",
        "regards.amqp.enabled=true", "regards.scheduler.pool.size=4", "regards.ingest.job.delay:5000" })
@ActiveProfiles("testAmqp")
public class SIPFlowHandlerIT extends AbstractMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPFlowHandlerIT.class);

    @Autowired
    private IRuntimeTenantResolver threadTenantResolver;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private ISIPRepository sipRepository;

    @Before
    public void init() {
        simulateApplicationReadyEvent();
        threadTenantResolver.forceTenant(getDefaultTenant());
    }

    @Test
    public void generateAndPublish() throws InterruptedException {

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
            if (countSip >= maxloops) {
                break;
            }
            Thread.sleep(10000);
        } while (true);
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
