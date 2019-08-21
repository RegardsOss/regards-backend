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
package fr.cnes.regards.modules.ingest.service;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIPBuilder;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;

/**
 * @author Marc Sordi
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest" })
public class IngestServiceIT extends IngestMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestServiceIT.class);

    @Autowired
    private IIngestService ingestService;

    @SpyBean
    private IIngestRequestService ingestRequestService;

    private final static String SESSION_OWNER = "sessionOwner";

    private final static String SESSION = "session";

    @Override
    public void doInit() {
        simulateApplicationReadyEvent();
        // Re-set tenant because above simulation clear it!
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        Mockito.clearInvocations(ingestRequestService);
    }

    private void ingestSIP(String providerId, String checksum) throws EntityInvalidException {
        SIPCollection sips = SIPCollection
                .build(IngestMetadataDto.build(SESSION_OWNER, SESSION, IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                               StorageMetadata.build("disk", null)));
        SIPBuilder builder = new SIPBuilder(providerId);
        sips.add(builder.buildReference(Paths.get("sip1.xml"), checksum));

        // First ingestion with synchronous service
        ingestService.handleSIPCollection(sips);
    }

    /**
     * Check if service properly store SIP and prevent to store a SIP twice
     * @throws ModuleException if error occurs!
     */
    @Requirement("REGARDS_DSL_ING_PRO_240")
    @Requirement("REGARDS_DSL_ING_PRO_250")
    @Requirement("REGARDS_DSL_ING_PRO_710")
    @Purpose("Store SIP checksum and prevent from submitting twice")
    @Test
    public void ingestWithCollision() throws ModuleException {

        // Ingest SIP
        String providerId = "SIP_001";
        String checksum = "zaasfsdfsdlfkmsldgfml12df";
        ingestSIP(providerId, checksum);
        waitForIngestion(1, TEN_SECONDS);

        SIPEntity entity = sipRepository.findTopByProviderIdOrderByIngestDateDesc(providerId);
        Assert.assertNotNull(entity);
        Assert.assertTrue(providerId.equals(entity.getProviderId()));
        Assert.assertTrue(entity.getVersion() == 1);
        Assert.assertTrue(SIPState.INGESTED.equals(entity.getState()));

        // Re-ingest same SIP
        ingestSIP(providerId, checksum);
        waitDuring(FIVE_SECONDS);

        // Detect error
        ArgumentCaptor<IngestRequest> argumentCaptor = ArgumentCaptor.forClass(IngestRequest.class);
        Mockito.verify(ingestRequestService, Mockito.times(1)).handleRequestError(argumentCaptor.capture(),
                ArgumentCaptor.forClass(SIPEntity.class).capture());
        IngestRequest request = argumentCaptor.getValue();
        Assert.assertNotNull(request);
        Assert.assertTrue(RequestState.ERROR.equals(request.getState()));

        // Check repository
        waitForIngestion(1, TWO_SECONDS);
    }

    /**
     * Check if service properly manage SIP versions
     * @throws ModuleException if error occurs!
     * @throws IOException if error occurs!
     * @throws NoSuchAlgorithmException if error occurs!
     */
    @Requirement("REGARDS_DSL_ING_PRO_200")
    @Requirement("REGARDS_DSL_ING_PRO_220")
    @Purpose("Manage SIP versionning")
    @Test
    public void ingestMultipleVersions() throws ModuleException, NoSuchAlgorithmException, IOException {

        // Ingest SIP
        String providerId = "SIP_002";
        ingestSIP(providerId, "zaasfsdfsdlfkmsldgfml12df");
        waitForIngestion(1, TEN_SECONDS);

        // Ingest next SIP version
        ingestSIP(providerId, "yaasfsdfsdlfkmsldgfml12df");
        waitForIngestion(2, TEN_SECONDS);

        // Check no request remains
        List<IngestRequest> requests = ingestRequestRepository.findAll();
        Assert.assertTrue(requests.isEmpty());

        // Check two versions of the SIP is persisted
        Collection<SIPEntity> sips = sipRepository.findAllByProviderIdOrderByVersionAsc(providerId);
        Assert.assertTrue(sips.size() == 2);

        List<SIPEntity> list = new ArrayList<>(sips);

        SIPEntity first = list.get(0);
        Assert.assertTrue(providerId.equals(first.getProviderId()));
        Assert.assertTrue(first.getVersion() == 1);
        Assert.assertTrue(SIPState.INGESTED.equals(first.getState()));

        SIPEntity second = list.get(1);
        Assert.assertTrue(providerId.equals(second.getProviderId()));
        Assert.assertTrue(second.getVersion() == 2);
        Assert.assertTrue(SIPState.INGESTED.equals(second.getState()));
    }
}
