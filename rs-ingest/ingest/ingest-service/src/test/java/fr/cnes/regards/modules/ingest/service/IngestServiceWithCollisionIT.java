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
package fr.cnes.regards.modules.ingest.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.service.aip.scheduler.IngestRequestSchedulerService;
import fr.cnes.regards.modules.ingest.service.plugin.AIPPostProcessTestPlugin;
import fr.cnes.regards.modules.ingest.service.request.IngestRequestService;
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

import java.nio.file.Paths;

/**
 * @author Marc Sordi
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest",
                                   "eureka.client.enabled=false",
                                   "regards.ingest.aip.delete.bulk.delay=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "noscheduler" })
public class IngestServiceWithCollisionIT extends IngestMultitenantServiceIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestServiceWithCollisionIT.class);

    @Autowired
    private IIngestService ingestService;

    @SpyBean
    private IngestRequestService ingestRequestService;

    @Autowired
    private IngestRequestSchedulerService ingestRequestSchedulerService;

    private final static String SESSION_OWNER = "sessionOwner";

    private final static String SESSION = "session";

    @Override
    public void doInit() throws ModuleException {
        // Creates a test chain with default post processing plugin
        createChainWithPostProcess(CHAIN_PP_LABEL, AIPPostProcessTestPlugin.class);
        Mockito.clearInvocations(ingestRequestService);
    }

    private void ingestSIP(String providerId, String checksum) throws EntityInvalidException {
        SIPCollection sips = SIPCollection.build(IngestMetadataDto.build(SESSION_OWNER,
                                                                         SESSION,
                                                                         null,
                                                                         IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                                         Sets.newHashSet("CAT"),
                                                                         null,
                                                                         StorageMetadata.build("disk")));

        sips.add(SIP.build(EntityType.DATA, providerId)
                    .withDataObject(DataType.RAWDATA, Paths.get("sip1.xml"), checksum)
                    .withSyntax(MediaType.APPLICATION_XML)
                    .registerContentInformation());

        // First ingestion with synchronous service
        ingestService.handleSIPCollection(sips);
    }

    /**
     * Check if service properly store SIP and prevent to store a SIP twice
     *
     * @throws ModuleException if error occurs!
     */
    @Requirement("REGARDS_DSL_ING_PRO_240")
    @Requirement("REGARDS_DSL_ING_PRO_250")
    @Requirement("REGARDS_DSL_ING_PRO_710")
    @Purpose("Store SIP checksum and prevent from submitting twice")
    @Test
    public void ingest_with_collision() throws ModuleException {

        // Ingest SIP
        String providerId = "SIP_001";
        String checksum = "zaasfsdfsdlfkmsldgfml12df";
        ingestSIP(providerId, checksum);
        ingestRequestSchedulerService.scheduleRequests();
        ingestServiceTest.waitForIngestion(1, TEN_SECONDS, getDefaultTenant());

        SIPEntity entity = sipRepository.findTopByProviderIdOrderByCreationDateDesc(providerId);
        Assert.assertNotNull(entity);
        Assert.assertEquals(providerId, entity.getProviderId());
        Assert.assertEquals(1, (int) entity.getVersion());
        Assert.assertEquals(SIPState.INGESTED, entity.getState());

        // Re-ingest same SIP
        ingestSIP(providerId, checksum);
        ingestRequestSchedulerService.scheduleRequests();
        ingestServiceTest.waitDuring(FIVE_SECONDS);

        // Detect error
        ArgumentCaptor<IngestRequest> argumentCaptor = ArgumentCaptor.forClass(IngestRequest.class);
        Mockito.verify(ingestRequestService, Mockito.times(1))
               .handleIngestJobFailed(argumentCaptor.capture(),
                                      ArgumentCaptor.forClass(SIPEntity.class).capture(),
                                      ArgumentCaptor.forClass(String.class).capture());
        IngestRequest request = argumentCaptor.getValue();
        Assert.assertNotNull(request);
        Assert.assertEquals(InternalRequestState.ERROR, request.getState());
        Assert.assertEquals(IngestErrorType.INITIAL_SIP_ALREADY_EXISTS, request.getErrorType());

        // Check repository
        ingestServiceTest.waitForIngestion(1, TWO_SECONDS, getDefaultTenant());
    }
}
