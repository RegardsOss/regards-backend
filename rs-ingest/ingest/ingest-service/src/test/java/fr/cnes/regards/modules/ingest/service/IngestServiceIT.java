/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.dto.StorageDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.service.job.IngestPostProcessingJob;
import fr.cnes.regards.modules.ingest.service.plugin.AIPPostProcessTestPlugin;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Marc Sordi
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest",
                                   "eureka.client.enabled=false",
                                   "regards.ingest.aip.delete.bulk.delay=100" },
                    locations = { "classpath:application-test.properties" })
public class IngestServiceIT extends IngestMultitenantServiceIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestServiceIT.class);

    @Autowired
    private IIngestService ingestService;

    @SpyBean
    private IIngestRequestService ingestRequestService;

    @Autowired
    private IJobInfoService jobInfoService;

    private final static String SESSION_OWNER = "sessionOwner";

    private final static String SESSION = "session";

    @Override
    public void doInit() throws ModuleException {
        // Creates a test chain with default post processing plugin
        createChainWithPostProcess(CHAIN_PP_LABEL, AIPPostProcessTestPlugin.class);
        Mockito.clearInvocations(ingestRequestService);
    }

    private void ingestSIP(String providerId, String checksum) throws EntityInvalidException {

        IngestMetadataDto metadata = new IngestMetadataDto(SESSION_OWNER,
                                                           SESSION,
                                                           null,
                                                           IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                           Sets.newHashSet("CAT"),
                                                           null,
                                                           null,
                                                           new StorageDto("disk"));
        SIPCollection sips = SIPCollection.build(metadata);

        sips.add(SIPDto.build(EntityType.DATA, providerId)
                       .withDataObject(DataType.RAWDATA, Paths.get("sip1.xml"), checksum)
                       .withSyntax(MediaType.APPLICATION_XML)
                       .registerContentInformation());

        // First ingestion with synchronous service
        ingestService.handleSIPCollection(sips);
    }

    @Test
    @Purpose("Test postprocess requests creation")
    public void ingestWithPostProcess() throws EntityInvalidException, InterruptedException {
        // Ingest SIP with no dataObject
        String providerId = "SIP_001";
        IngestMetadataDto metadata = new IngestMetadataDto(SESSION_OWNER,
                                                           SESSION,
                                                           null,
                                                           CHAIN_PP_LABEL,
                                                           Sets.newHashSet("CAT"),
                                                           null,
                                                           null,
                                                           new StorageDto("disk"));
        SIPCollection sips = SIPCollection.build(metadata);
        sips.add(SIPDto.build(EntityType.DATA, providerId));
        ingestService.handleSIPCollection(sips);
        ingestServiceTest.waitForIngestion(1, TEN_SECONDS, getDefaultTenant());

        // Check that the SIP is STORED
        SIPEntity entity = sipRepository.findTopByProviderIdOrderByCreationDateDesc(providerId);
        Assert.assertNotNull(entity);
        Assert.assertEquals(providerId, entity.getProviderId());
        Assert.assertEquals(1, (int) entity.getVersion());
        Assert.assertEquals(SIPState.STORED, entity.getState());

        // wait for postprocessing job scheduling
        Awaitility.await().atMost(Durations.TEN_SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return jobInfoService.retrieveJobsCount(IngestPostProcessingJob.class.getName(),
                                                    JobStatus.QUEUED,
                                                    JobStatus.TO_BE_RUN,
                                                    JobStatus.PENDING,
                                                    JobStatus.RUNNING,
                                                    JobStatus.SUCCEEDED).longValue() == 1L;
        });
    }

    @Test
    @Purpose("Ingest a SIP with no contentInformation to store. Only manifest should be stored.")
    public void ingestWithoutAnyDataFile() throws EntityInvalidException {
        // Ingest SIP with no dataObject
        String providerId = "SIP_001";
        IngestMetadataDto metadata = new IngestMetadataDto(SESSION_OWNER,
                                                           SESSION,
                                                           null,
                                                           IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                           Sets.newHashSet("CAT"),
                                                           null,
                                                           null,
                                                           new StorageDto("disk"));
        SIPCollection sips = SIPCollection.build(metadata);
        sips.add(SIPDto.build(EntityType.DATA, providerId));
        ingestService.handleSIPCollection(sips);
        ingestServiceTest.waitForIngestion(1, TEN_SECONDS, getDefaultTenant());

        // Check that the SIP is STORED
        SIPEntity entity = sipRepository.findTopByProviderIdOrderByCreationDateDesc(providerId);
        Assert.assertNotNull(entity);
        Assert.assertEquals(providerId, entity.getProviderId());
        Assert.assertEquals(1, (int) entity.getVersion());
        Assert.assertEquals(SIPState.STORED, entity.getState());
    }

    /**
     * Check if service properly manage SIP versions
     *
     * @throws ModuleException          if error occurs!
     * @throws IOException              if error occurs!
     * @throws NoSuchAlgorithmException if error occurs!
     */
    @Requirement("REGARDS_DSL_ING_PRO_200")
    @Requirement("REGARDS_DSL_ING_PRO_220")
    @Purpose("Manage SIP versionning")
    @Test
    public void ingestMultipleVersions() throws ModuleException, NoSuchAlgorithmException, IOException {

        // Ingest SIP
        String providerId = "SIP_002";
        ingestSIP(providerId, "a1b81e2b75c8c1ec4a2ca5bdf607bb75");
        ingestServiceTest.waitForIngestion(1, TEN_SECONDS, getDefaultTenant());

        // Ingest next SIP version
        ingestSIP(providerId, "a1b81e2b75c8c1ec4a2ca5bdf607bb76");
        ingestServiceTest.waitForIngestion(2, TEN_SECONDS, getDefaultTenant());

        // Check remove storage requested
        List<IngestRequest> requests = ingestRequestRepository.findAll();
        int nbStorageRequested = 0;
        for (IngestRequest ir : requests) {
            if (ir.getStep() == IngestRequestStep.REMOTE_STORAGE_REQUESTED) {
                nbStorageRequested = nbStorageRequested + 1;
            }
        }
        Assert.assertEquals(nbStorageRequested, requests.size());

        // Check two versions of the SIP is persisted
        Collection<SIPEntity> sips = sipRepository.findAllByProviderIdOrderByVersionAsc(providerId);
        Assert.assertEquals(2, sips.size());

        List<SIPEntity> list = new ArrayList<>(sips);

        SIPEntity first = list.get(0);
        Assert.assertEquals(providerId, first.getProviderId());
        Assert.assertEquals(1, (int) first.getVersion());
        Assert.assertEquals(SIPState.INGESTED, first.getState());

        SIPEntity second = list.get(1);
        Assert.assertEquals(providerId, second.getProviderId());
        Assert.assertEquals(2, (int) second.getVersion());
        Assert.assertEquals(SIPState.INGESTED, second.getState());
    }
}
