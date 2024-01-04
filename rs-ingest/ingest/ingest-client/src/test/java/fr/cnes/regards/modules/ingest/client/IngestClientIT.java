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
package fr.cnes.regards.modules.ingest.client;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.chain.IngestProcessingChainService;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import fr.cnes.regards.modules.test.IngestServiceIT;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Test asychronous ingestion client
 *
 * @author Marc SORDI
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingestclient",
                                   "regards.amqp.enabled=true",
                                   "regards.aips.save-metadata.bulk.delay=100" })
@ContextConfiguration(classes = { IngestClientIT.IngestConfiguration.class })
@ActiveProfiles(value = { "default", "test", "testAmqp", "StorageClientMock" }, inheritProfiles = false)
public class IngestClientIT extends AbstractRegardsWebIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestClientIT.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IIngestClient ingestClient;

    @SpyBean
    private TestIngestClientListener listener;

    @Autowired
    private StorageClientMock storageClientMock;

    @Autowired
    private IngestServiceIT ingestServiceTest;

    @Autowired
    private IngestProcessingChainService procCahinService;

    @Before
    public void doInit() throws Exception {
        // Re-set tenant because above simulation clear it!
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        storageClientMock.setBehavior(true, true);
        ingestServiceTest.init(getDefaultTenant());
        procCahinService.initDefaultServiceConfiguration();
        listener.clear();
    }

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules" })
    static class IngestConfiguration {

    }

    @Test
    public void ingest() throws IngestClientException, InterruptedException {

        String providerId = "sipFromClient";
        Mockito.clearInvocations(listener);
        Mockito.verify(listener, Mockito.times(0)).onGranted(Mockito.anyCollection());
        RequestInfo clientInfo = ingestClient.ingest(IngestMetadataDto.build("sessionOwner",
                                                                             "session",
                                                                             null,
                                                                             IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                                             Sets.newHashSet("cat 1"),
                                                                             null,
                                                                             StorageMetadata.build("disk")),
                                                     create(providerId));
        ingestServiceTest.waitForIngestion(1, 15_000, SIPState.STORED, getDefaultTenant());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> (long) listener.getSuccess().size() >= 1);

        Assert.assertTrue("Missing granted request response",
                          listener.getGranted()
                                  .stream()
                                  .anyMatch(r -> r.getRequestId().equals(clientInfo.getRequestId())));
        Assert.assertTrue("Missing success request response",
                          listener.getSuccess()
                                  .stream()
                                  .anyMatch(r -> r.getRequestId().equals(clientInfo.getRequestId())));
    }

    private SIP create(String providerId) {

        String fileName = String.format("file-%s.dat", providerId);
        SIP sip = SIP.build(EntityType.DATA, providerId);
        sip.withDataObject(DataType.RAWDATA,
                           Paths.get("src", "main", "test", "resources", "data", fileName),
                           "MD5",
                           UUID.randomUUID().toString());
        sip.withSyntax(MediaType.APPLICATION_JSON_UTF8);
        sip.registerContentInformation();

        // Add creation event
        sip.withEvent(String.format("SIP %s generated", providerId));

        return sip;
    }

    @After
    public void doAfter() {
        ingestServiceTest.init(getDefaultTenant());
        listener.clear();
    }
}
