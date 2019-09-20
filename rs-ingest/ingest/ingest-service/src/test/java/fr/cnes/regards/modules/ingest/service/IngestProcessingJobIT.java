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

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.chain.ProcessingChainTestErrorSimulator;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.ingest.service.plugin.AIPGenerationTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.AIPTaggingTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.PostProcessingTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.PreprocessingTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.ValidationTestPlugin;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;

/**
 * Test class to verify {@link IngestProcessingJob}.
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingestjob" })
public class IngestProcessingJobIT extends IngestMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestProcessingJobIT.class);

    private static final String SIP_ID_TEST = "SIP_001";

    private static final String SIP_DEFAULT_CHAIN_ID_TEST = "SIP_002";

    private static final String SIP_REF_ID_TEST = "SIP_003";

    private static final String PROCESSING_CHAIN_TEST = "fullProcessingChain";

    private static final String SESSION_OWNER = "sessionOwner";

    private static final String SESSION = "session";

    private static final StorageMetadata STORAGE_METADATA = StorageMetadata.build("disk");

    private static final HashSet<String> CATEGORIES = Sets.newHashSet("cat 1");

    @Autowired
    private IIngestProcessingChainRepository processingChainRepository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private ProcessingChainTestErrorSimulator stepErrorSimulator;

    @SpyBean
    private IIngestRequestService ingestRequestService;

    @Override
    public void doInit() throws ModuleException {

        simulateApplicationReadyEvent();
        // Re-set tenant because above simulation clear it!
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        initFullProcessingChain();

        Mockito.clearInvocations(ingestRequestService);
    }

    private void initFullProcessingChain() throws ModuleException {
        PluginMetaData preProcessingPluginMeta = PluginUtils.createPluginMetaData(PreprocessingTestPlugin.class);
        PluginConfiguration preProcessingPlugin = new PluginConfiguration(preProcessingPluginMeta,
                "preProcessingPlugin");
        pluginService.savePluginConfiguration(preProcessingPlugin);

        PluginMetaData validationPluginMeta = PluginUtils.createPluginMetaData(ValidationTestPlugin.class);
        PluginConfiguration validationPlugin = new PluginConfiguration(validationPluginMeta, "validationPlugin");
        pluginService.savePluginConfiguration(validationPlugin);

        PluginMetaData generationPluginMeta = PluginUtils.createPluginMetaData(AIPGenerationTestPlugin.class);
        PluginConfiguration generationPlugin = new PluginConfiguration(generationPluginMeta, "generationPlugin");
        pluginService.savePluginConfiguration(generationPlugin);

        PluginMetaData taggingPluginMeta = PluginUtils.createPluginMetaData(AIPTaggingTestPlugin.class);
        PluginConfiguration taggingPlugin = new PluginConfiguration(taggingPluginMeta, "taggingPlugin");
        pluginService.savePluginConfiguration(taggingPlugin);

        PluginMetaData postProcessingMeta = PluginUtils.createPluginMetaData(PostProcessingTestPlugin.class);
        PluginConfiguration postProcessingPlugin = new PluginConfiguration(postProcessingMeta, "postProcessingPlugin");
        pluginService.savePluginConfiguration(postProcessingPlugin);

        IngestProcessingChain fullChain = new IngestProcessingChain(PROCESSING_CHAIN_TEST,
                "Full test Ingestion processing chain", validationPlugin, generationPlugin);
        fullChain.setPreProcessingPlugin(preProcessingPlugin);
        fullChain.setGenerationPlugin(generationPlugin);
        fullChain.setTagPlugin(taggingPlugin);
        fullChain.setPostProcessingPlugin(postProcessingPlugin);
        processingChainRepository.save(fullChain);
    }

    @Requirement("REGARDS_DSL_ING_PRO_160")
    @Purpose("Test default process chain to ingest a new SIP provided by value")
    @Test
    public void testDefaultProcessingChain() {
        // Init a SIP in database with state CREATED and managed with default chain
        SIPCollection sips = SIPCollection.build(IngestMetadataDto
                .build(SESSION_OWNER, SESSION, IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                        CATEGORIES, STORAGE_METADATA));

        SIP sip = SIP.build(EntityType.DATA, SIP_DEFAULT_CHAIN_ID_TEST);
        sip.withDataObject(DataType.RAWDATA, Paths.get("data1.fits"), "sdsdfm1211vd");
        sip.withSyntax("FITS(FlexibleImageTransport)", "http://www.iana.org/assignments/media-types/application/fits",
                       MediaType.valueOf("application/fits"));
        sip.registerContentInformation();
        sips.add(sip);

        // Ingest
        Collection<IngestRequestFlowItem> items = IngestService.sipToFlow(sips);
        ingestService.handleIngestRequests(items);
        ingestServiceTest.waitForIngestion(1, FIVE_SECONDS);

        SIPEntity resultSip = sipRepository.findTopByProviderIdOrderByCreationDateDesc(SIP_DEFAULT_CHAIN_ID_TEST);
        Assert.assertNotNull(resultSip);
        Assert.assertEquals(SIPState.INGESTED, resultSip.getState());
        Assert.assertEquals(IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                            resultSip.getIngestMetadata().getIngestChain());
    }

    @Requirement("REGARDS_DSL_ING_PRO_160")
    @Requirement("REGARDS_DSL_ING_PRO_170")
    @Requirement("REGARDS_DSL_ING_PRO_180")
    @Requirement("REGARDS_DSL_ING_PRO_300")
    @Requirement("REGARDS_DSL_ING_PRO_400")
    @Purpose("Test fully configured process chain to ingest a new SIP provided by value")
    @Test
    public void testProcessingChain() {

        SIPCollection sips = SIPCollection
                .build(IngestMetadataDto.build(SESSION_OWNER, SESSION, PROCESSING_CHAIN_TEST, CATEGORIES, STORAGE_METADATA));

        SIP sip = SIP.build(EntityType.DATA, SIP_ID_TEST);
        sip.withDataObject(DataType.RAWDATA, Paths.get("data2.fits"), "sdsdfm1211vd");
        sip.withSyntax("FITS(FlexibleImageTransport)", "http://www.iana.org/assignments/media-types/application/fits",
                       MediaType.valueOf("application/fits"));
        sip.registerContentInformation();
        sips.add(sip);

        // Simulate an error on each step
        simulateProcessingError(sips, PreprocessingTestPlugin.class);
        simulateProcessingError(sips, ValidationTestPlugin.class);
        simulateProcessingError(sips, AIPGenerationTestPlugin.class);
        simulateProcessingError(sips, AIPTaggingTestPlugin.class);
        simulateProcessingError(sips, PostProcessingTestPlugin.class);

        // Simulate a full process without error
        stepErrorSimulator.setSimulateErrorForStep(null);

        // Ingest
        ingestService.handleIngestRequests(IngestService.sipToFlow(sips));
        ingestServiceTest.waitForIngestion(1, FIVE_SECONDS);

        SIPEntity resultSip = sipRepository.findTopByProviderIdOrderByCreationDateDesc(SIP_ID_TEST);
        Assert.assertNotNull(resultSip);
        Assert.assertEquals(SIPState.INGESTED, resultSip.getState());
        Assert.assertEquals(PROCESSING_CHAIN_TEST, resultSip.getIngestMetadata().getIngestChain());

        Set<AIPEntity> resultAips = aipRepository.findBySipSipId(resultSip.getSipId());
        Assert.assertNotNull(resultAips);
        Assert.assertTrue(resultAips.size() == 1);
        AIPEntity resultAip = resultAips.stream().findFirst().get();
        Assert.assertNotNull(resultAip);
        Assert.assertEquals(AIPState.GENERATED, resultAip.getState());
    }

    private void simulateProcessingError(SIPCollection sips, Class<?> errorClass) {
        // Simulate an error during PreprocessingStep
        stepErrorSimulator.setSimulateErrorForStep(errorClass);

        // Ingest
        ingestService.handleIngestRequests(IngestService.sipToFlow(sips));
        ingestServiceTest.waitDuring(TWO_SECONDS);

        // Detect request error and no SIP or AIP is persisted
        ArgumentCaptor<IngestRequest> ingestRequestCaptor = ArgumentCaptor.forClass(IngestRequest.class);
        ArgumentCaptor<SIPEntity> sipCaptor = ArgumentCaptor.forClass(SIPEntity.class);

        Mockito.verify(ingestRequestService, Mockito.times(1)).handleRequestError(ingestRequestCaptor.capture(),
                                                                                  sipCaptor.capture());
        Mockito.clearInvocations(ingestRequestService);
        IngestRequest request = ingestRequestCaptor.getValue();
        Assert.assertNotNull(request);
        Assert.assertEquals(RequestState.ERROR, request.getState());
        Assert.assertTrue(!request.getErrors().isEmpty());

        Assert.assertNotNull(sipCaptor.getValue());

        // Not sip ingested
        ingestServiceTest.waitForIngestion(0, FIVE_SECONDS);
    }

    @Purpose("Test fully configured process chain to ingest a new SIP provided by reference")
    @Test
    public void testProcessingChainByRef() {

        // Init a SIP with reference in database with state CREATED
        SIPCollection sips = SIPCollection
                .build(IngestMetadataDto.build(SESSION_OWNER, SESSION, PROCESSING_CHAIN_TEST, CATEGORIES, STORAGE_METADATA));

        SIP sip = SIP.buildReference(EntityType.DATA, SIP_REF_ID_TEST, Paths.get("src/test/resources/file_ref.xml"),
                                     "1e2d4ab665784e43243b9b07724cd483");
        sips.add(sip);

        // Ingest
        ingestService.handleIngestRequests(IngestService.sipToFlow(sips));
        ingestServiceTest.waitForIngestion(1, FIVE_SECONDS);

        SIPEntity resultSip = sipRepository.findTopByProviderIdOrderByCreationDateDesc(SIP_REF_ID_TEST);
        Assert.assertNotNull(resultSip);
        Assert.assertEquals(SIPState.INGESTED, resultSip.getState());
        Assert.assertEquals(PROCESSING_CHAIN_TEST, resultSip.getIngestMetadata().getIngestChain());
    }
}
