/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.AIPState;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.service.plugin.DefaultSingleAIPGeneration;
import fr.cnes.regards.modules.ingest.service.plugin.DefaultSipValidation;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.RejectedAip;

/**
 * Test class to check scheduled tasks to handle created SIP to be processed by processing chains.
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "regards.ingest.process.new.sips.delay:3000",
        "regards.ingest.process.new.aips.storage.delay:8000" }, locations = "classpath:test.properties")
@ContextConfiguration(classes = { TestConfiguration.class })
public class ScheduledIngestTaskTest extends AbstractRegardsServiceIT {

    @Autowired
    private IIngestProcessingChainRepository processingChainRepository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private IAipClient aipClient;

    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Value("${regards.ingest.process.new.sips.delay}")
    private String scheduledTasksDelay;

    @Value("${regards.ingest.process.new.aips.storage.delay}")
    private String scheduledAipBulkRequestDelay;

    public static final String DEFAULT_PROCESSING_CHAIN_TEST = "defaultProcessingChain";

    public static final String SIP_ID_TEST = "SIP_001";

    private final Set<String> rejectedAips = Sets.newHashSet();

    @Before
    public void init() throws ModuleException {
        tenantResolver.forceTenant(DEFAULT_TENANT);
        pluginConfRepo.deleteAll();
        aipRepository.deleteAll();
        sipRepository.deleteAll();
        jobInfoRepo.deleteAll();
        initDefaultProcessingChain();

        this.rejectedAips.clear();
        // Mock aipClient store request result
        Mockito.when(aipClient.store(Mockito.any()))
                .thenAnswer(invocation -> simulateRejectedAips((AIPCollection) invocation.getArguments()[0]));
    }

    @After
    public void cleanJobs() {
        jobInfoRepo.deleteAll();
    }

    @Requirement("REGARDS_DSL_ING_PRO_120")
    @Purpose("Manage scheduled ingestion tasks for all CREATED SIP")
    @Test
    public void scheduleIngestJobTest() throws InterruptedException, ModuleException {

        // 1. Add a new SIP with CREATED Status.
        SIPCollectionBuilder colBuilder = new SIPCollectionBuilder(DEFAULT_PROCESSING_CHAIN_TEST, "sessionId");
        SIPCollection collection = colBuilder.build();

        SIPBuilder builder = new SIPBuilder(SIP_ID_TEST);
        builder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("testing.json"),
                                                             "2323DFgfdgdfgfdgfdgdfgesd");
        builder.addContentInformation();
        collection.add(builder.build());

        Collection<SIPDto> results = ingestService.ingest(collection);
        String ipId = results.stream().findFirst().get().getIpId();
        SIPEntity sip = sipRepository.findOneByIpId(ipId).get();
        Assert.assertTrue("Error creating new SIP", SIPState.CREATED.equals(sip.getState()));

        // 2. Wait for scheduled task to be run and finished
        Thread.sleep(Integer.parseInt(scheduledTasksDelay) + 1000);

        // 3. Check that the SIP has been successully handled
        sip = sipRepository.findOneByIpId(ipId).get();
        Assert.assertEquals(SIPState.AIP_CREATED, sip.getState());

        // 4. Check that the associated AIP is generated
        Set<AIPEntity> aips = aipRepository.findBySip(sip);
        Assert.assertEquals(1, aips.size());
        AIPEntity aip = aips.iterator().next();
        Assert.assertEquals(AIPState.CREATED, aip.getState());

        // 5. Wait for AIPs bulk request is sent
        Thread.sleep(Integer.parseInt(scheduledAipBulkRequestDelay) - Integer.parseInt(scheduledTasksDelay));

        // 6. Check that AIPs has been handled by storage microservice
        Mockito.verify(aipClient, Mockito.times(1)).store(Mockito.any());
        aip = aipRepository.findOne(aip.getId());
        Assert.assertEquals(AIPState.QUEUED, aip.getState());

        sip = sipRepository.findOneByIpId(ipId).get();
        Assert.assertEquals(SIPState.AIP_CREATED, sip.getState());

        // 7. Verify that a new storage request is not sent to archival storage during the time when aip is in queued
        // state
        Mockito.reset(aipClient);
        Thread.sleep(Integer.parseInt(scheduledAipBulkRequestDelay));
        Mockito.verify(aipClient, Mockito.times(0)).store(Mockito.any());
    }

    @Requirement("REGARDS_DSL_ING_PRO_120")
    @Purpose("Manage scheduled ingestion tasks for all CREATED SIP with rejected AIPs from storage")
    @Test
    public void scheduleIngestJobTestWithStorageError() throws InterruptedException, ModuleException {

        // 1. Add a new SIP with CREATED Status.
        SIPCollectionBuilder colBuilder = new SIPCollectionBuilder(DEFAULT_PROCESSING_CHAIN_TEST, "sessionId");
        SIPCollection collection = colBuilder.build();

        SIPBuilder builder = new SIPBuilder(SIP_ID_TEST);
        builder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("test.xml"),
                                                             "sdsdfm1211vsdfdsfddsffdsd");
        builder.addContentInformation();
        collection.add(builder.build());

        Collection<SIPDto> results = ingestService.ingest(collection);
        String ipId = results.stream().findFirst().get().getIpId();
        SIPEntity sip = sipRepository.findOneByIpId(ipId).get();
        Assert.assertTrue("Error creating new SIP", SIPState.CREATED.equals(sip.getState()));

        // 2. Wait for scheduled task to be run and finished
        Thread.sleep(Integer.parseInt(scheduledTasksDelay) + 1000);

        // 3. Check that the SIP has been successully handled
        sip = sipRepository.findOneByIpId(ipId).get();
        Assert.assertEquals(SIPState.AIP_CREATED, sip.getState());

        // 4. Check that the associated AIP is generated
        Set<AIPEntity> aips = aipRepository.findBySip(sip);
        Assert.assertEquals(1, aips.size());
        AIPEntity aip = aips.iterator().next();
        Assert.assertEquals(AIPState.CREATED, aip.getState());

        // Simulate aip rejection from archival storage
        this.rejectedAips.add(aip.getIpId());

        // 5. Wait for AIPs bulk request is sent
        Thread.sleep(Integer.parseInt(scheduledAipBulkRequestDelay) - Integer.parseInt(scheduledTasksDelay));

        // 6. Check AIP Rejection from archival storage
        Mockito.verify(aipClient, Mockito.times(1)).store(Mockito.any());
        aip = aipRepository.findOne(aip.getId());
        Assert.assertEquals(AIPState.STORE_REJECTED, aip.getState());

        sip = sipRepository.findOneByIpId(ipId).get();
        Assert.assertEquals(SIPState.AIP_CREATED, sip.getState());

        // 7. Verify that a new storage request is not sent to archival storage during the time when aip is in queued
        // state
        Mockito.reset(aipClient);
        Thread.sleep(Integer.parseInt(scheduledAipBulkRequestDelay));
        Mockito.verify(aipClient, Mockito.times(0)).store(Mockito.any());

    }

    /**
     * Initialize {@link IngestProcessingChain} configuration for tests
     * @throws ModuleException
     */
    private void initDefaultProcessingChain() throws ModuleException {
        PluginMetaData defaultValidationPluginMeta = PluginUtils.createPluginMetaData(DefaultSipValidation.class);
        PluginConfiguration defaultValidationPlugin = new PluginConfiguration(defaultValidationPluginMeta,
                "defaultValidationPlugin");
        pluginService.savePluginConfiguration(defaultValidationPlugin);

        PluginMetaData defaultGenerationPluginMeta = PluginUtils.createPluginMetaData(DefaultSingleAIPGeneration.class);
        PluginConfiguration defaultGenerationPlugin = new PluginConfiguration(defaultGenerationPluginMeta,
                "defaultGenerationPlugin");
        pluginService.savePluginConfiguration(defaultGenerationPlugin);

        IngestProcessingChain defaultChain = new IngestProcessingChain(DEFAULT_PROCESSING_CHAIN_TEST,
                "Default Ingestion processing chain", defaultValidationPlugin, defaultGenerationPlugin);
        processingChainRepository.save(defaultChain);
    }

    /**
     * Simulate a response from the archival storage microservice for the store request with no AIP rejected
     * @param aipCollection
     * @return
     */
    private ResponseEntity<List<RejectedAip>> simulateRejectedAips(AIPCollection aipCollection) {
        List<RejectedAip> rejectedAips = Lists.newArrayList();
        HttpStatus status = HttpStatus.CREATED;
        if (!this.rejectedAips.isEmpty()) {
            status = HttpStatus.PARTIAL_CONTENT;
            this.rejectedAips.forEach(r -> {
                rejectedAips.add(new RejectedAip(r, Lists.newArrayList("Simulated rejected AIP")));
            });
        }
        return new ResponseEntity<>(rejectedAips, status);
    }

}
