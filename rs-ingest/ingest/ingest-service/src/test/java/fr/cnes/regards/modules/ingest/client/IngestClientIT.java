/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import fr.cnes.regards.modules.storagelight.client.FileRequestGroupEventHandler;
import fr.cnes.regards.modules.storagelight.client.test.StorageClientMock;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test asychronous ingestion client
 *
 * @author Marc SORDI
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=ingestclient",
                "regards.amqp.enabled=true"})
@ActiveProfiles(value={"testAmqp", "StorageClientMock"})
public class IngestClientIT extends IngestMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestClientIT.class);

    private static final List<String> CATEGORIES = Lists.newArrayList("CATEGORY");

    @Autowired
    private IIngestClient ingestClient;

    @Autowired
    private IAmqpAdmin amqpAdmin;

    @Autowired
    private IRabbitVirtualHostAdmin vhostAdmin;

    @Autowired
    private ISubscriber subscriber;

    @SpyBean
    private TestIngestClientListener listener;

    @Autowired
    private StorageClientMock storageClientMock;

    @Override
    public void doInit() throws ModuleException {
        simulateApplicationReadyEvent();
        // Re-set tenant because above simulation clear it!
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        storageClientMock.setBehavior(true, true);
        // Purge event queue
        try {
            vhostAdmin.bind(AmqpConstants.AMQP_MULTITENANT_MANAGER);
            amqpAdmin.purgeQueue(amqpAdmin.getSubscriptionQueueName(FileRequestGroupEventHandler.class,
                    Target.ONE_PER_MICROSERVICE_TYPE),
                    false);
        } finally {
            vhostAdmin.unbind();
        }

    }

    @Override
    protected void doAfter() throws Exception {
        // WARNING : clean context manually because Spring doesn't do it between tests
        subscriber.unsubscribeFrom(IngestRequestFlowItem.class);
    }

    @Test
    public void ingest() throws IngestClientException {

        String providerId = "sipFromClient";
        RequestInfo clientInfo = ingestClient.ingest(IngestMetadataDto
                .build("sessionOwner", "session", IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                        Sets.newHashSet("cat 1"),
                       StorageMetadata.build("disk", null)), create(providerId));
        ingestServiceTest.waitForIngestion(1, FIVE_SECONDS, SIPState.STORED);

        ArgumentCaptor<RequestInfo> grantedInfo = ArgumentCaptor.forClass(RequestInfo.class);
        Mockito.verify(listener, Mockito.times(1)).onGranted(grantedInfo.capture());
        Assert.assertEquals(clientInfo.getRequestId(), grantedInfo.getValue().getRequestId());

        ArgumentCaptor<RequestInfo> successInfo = ArgumentCaptor.forClass(RequestInfo.class);
        Mockito.verify(listener, Mockito.times(1)).onSuccess(successInfo.capture());
        Assert.assertEquals(clientInfo.getRequestId(), successInfo.getValue().getRequestId());
    }

    private SIP create(String providerId) {

        SIP sip = SIP.build(EntityType.DATA, providerId);
        sip.withDataObject(DataType.RAWDATA,
                           Paths.get("src", "main", "test", "resources", "data", "cdpp_collection.json"), "MD5",
                           "azertyuiopqsdfmlmld");
        sip.withSyntax(MediaType.APPLICATION_JSON_UTF8);
        sip.registerContentInformation();

        // Add creation event
        sip.withEvent(String.format("SIP %s generated", providerId));

        return sip;
    }
}
