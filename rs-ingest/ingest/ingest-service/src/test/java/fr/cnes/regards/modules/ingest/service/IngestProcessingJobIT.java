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
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.chain.ProcessingChainTestErrorSimulator;
import fr.cnes.regards.modules.ingest.service.flow.StorageResponseFlowHandler;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.ingest.service.plugin.*;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.FileLocationDTO;
import fr.cnes.regards.modules.storage.domain.dto.FileReferenceDTO;
import fr.cnes.regards.modules.storage.domain.dto.FileReferenceMetaInfoDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test class to verify {@link IngestProcessingJob}.
 *
 * @author Sébastien Binda
 */
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=ingestjob", "eureka.client.enabled=false",
        "regards.ingest.aip.delete.bulk.delay=100" })
public class IngestProcessingJobIT extends IngestMultitenantServiceIT {

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

    @Autowired
    private IIngestRequestRepository ingestRequestRepo;

    @SpyBean
    private IStorageClient storageClient;

    @Autowired
    private StorageResponseFlowHandler storageResponseHandler;

    @Override
    public void doInit() throws ModuleException {
        initFullProcessingChain();
        Mockito.clearInvocations(ingestRequestService);
        Mockito.clearInvocations(storageClient);
    }

    private void initFullProcessingChain() throws ModuleException {
        PluginConfiguration preProcessingPlugin = new PluginConfiguration("preProcessingPlugin",
                                                                          PreprocessingTestPlugin.class.getAnnotation(
                                                                              Plugin.class).id());
        pluginService.savePluginConfiguration(preProcessingPlugin);

        PluginConfiguration validationPlugin = new PluginConfiguration("validationPlugin",
                                                                       ValidationTestPlugin.class.getAnnotation(Plugin.class)
                                                                                                 .id());
        pluginService.savePluginConfiguration(validationPlugin);

        PluginConfiguration generationPlugin = new PluginConfiguration("generationPlugin",
                                                                       AIPGenerationTestPlugin.class.getAnnotation(
                                                                           Plugin.class).id());
        pluginService.savePluginConfiguration(generationPlugin);

        PluginConfiguration aipStorageMetadataPlugin = new PluginConfiguration("aipStorageMetadataPlugin",
                                                                               AIPStorageMetadataTestPlugin.class.getAnnotation(
                                                                                   Plugin.class).id());
        pluginService.savePluginConfiguration(aipStorageMetadataPlugin);

        PluginConfiguration taggingPlugin = new PluginConfiguration("taggingPlugin",
                                                                    AIPTaggingTestPlugin.class.getAnnotation(Plugin.class)
                                                                                              .id());
        pluginService.savePluginConfiguration(taggingPlugin);

        PluginConfiguration postProcessingPlugin = new PluginConfiguration("postProcessingPlugin",
                                                                           PostProcessingTestPlugin.class.getAnnotation(
                                                                               Plugin.class).id());
        pluginService.savePluginConfiguration(postProcessingPlugin);

        IngestProcessingChain fullChain = new IngestProcessingChain(PROCESSING_CHAIN_TEST,
                                                                    "Full test Ingestion processing chain",
                                                                    validationPlugin,
                                                                    generationPlugin);
        fullChain.setPreProcessingPlugin(preProcessingPlugin);
        fullChain.setGenerationPlugin(generationPlugin);
        fullChain.setTagPlugin(taggingPlugin);
        fullChain.setAipStorageMetadataPlugin(aipStorageMetadataPlugin);
        fullChain.setPostProcessingPlugin(postProcessingPlugin);
        processingChainRepository.save(fullChain);
    }

    @Requirements({ @Requirement("REGARDS_DSL_ING_PRO_160"), @Requirement("REGARDS_DSL_STO_AIP_010") })
    @Purpose("Test default process chain to ingest a new SIP provided by value and ask for files storage")
    @Test
    public void testDefaultProcessingChain() {
        // Init a SIP in database with state CREATED and managed with default chain
        SIPCollection sips = SIPCollection.build(IngestMetadataDto.build(SESSION_OWNER,
                                                                         SESSION,
                                                                         IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                                         CATEGORIES,
                                                                         null,
                                                                         STORAGE_METADATA));

        Path filePath = Paths.get("data1.fits");
        String checksum = "sdsdfm1211vd";
        SIP sip = SIP.build(EntityType.DATA, SIP_DEFAULT_CHAIN_ID_TEST);
        sip.withDataObject(DataType.RAWDATA, filePath, checksum);
        sip.withSyntax("FITS(FlexibleImageTransport)",
                       "http://www.iana.org/assignments/media-types/application/fits",
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
        Assert.assertEquals(SESSION_OWNER, resultSip.getSessionOwner());
        Assert.assertEquals(SESSION, resultSip.getSession());

        // Check that files storage has been requested
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<FileStorageRequestDTO>> storageArgs = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(storageClient, Mockito.times(1)).store(storageArgs.capture());
        Assert.assertTrue("File storage url is not vali in storage request",
                          storageArgs.getValue()
                                     .stream()
                                     .anyMatch(req -> req.getOriginUrl()
                                                         .equals(OAISDataObjectLocation.build(filePath).getUrl())));
        Assert.assertTrue("File storage is not valid in storage request",
                          storageArgs.getValue()
                                     .stream()
                                     .anyMatch(req -> req.getStorage().equals(STORAGE_METADATA.getPluginBusinessId())));

        // Check status of IngestRequest
        List<IngestRequest> reqs = ingestRequestRepo.findByProviderId(resultSip.getProviderId());
        Assert.assertEquals(1, reqs.size());
        IngestRequest req = reqs.get(0);
        Assert.assertEquals(1, req.getAips().size());
        Assert.assertEquals(IngestRequestStep.REMOTE_STORAGE_REQUESTED, req.getStep());
        Assert.assertEquals(InternalRequestState.TO_SCHEDULE, req.getState());
        AIPEntity aip = req.getAips().get(0);

        String remoteStepGroupId = req.getRemoteStepGroupIds().get(0);
        RequestResultInfoDTO success = RequestResultInfoDTO.build(remoteStepGroupId,
                                                                  checksum,
                                                                  STORAGE_METADATA.getPluginBusinessId(),
                                                                  null,
                                                                  Sets.newHashSet(aip.getAipId()),
                                                                  FileReferenceDTO.build(OffsetDateTime.now(),
                                                                                         FileReferenceMetaInfoDTO.build(
                                                                                             checksum,
                                                                                             "MD5",
                                                                                             "file.name",
                                                                                             10L,
                                                                                             null,
                                                                                             null,
                                                                                             MediaType.APPLICATION_JSON,
                                                                                             "type"),
                                                                                         FileLocationDTO.build(
                                                                                             STORAGE_METADATA.getPluginBusinessId(),
                                                                                             "file:///test/file.name"),
                                                                                         Sets.newHashSet(aip.getAipId())),
                                                                  null);
        RequestInfo ri = RequestInfo.build(remoteStepGroupId, Sets.newHashSet(success), Sets.newHashSet());
        Set<RequestInfo> requestInfos = Sets.newHashSet(ri);
        // Simulate error response from storage
        storageResponseHandler.onStoreSuccess(requestInfos);

        // Check status of IngestRequest
        if (initDefaultNotificationSettings()) {
            mockNotificationSuccess(RequestTypeConstant.INGEST_VALUE);
        }
        reqs = ingestRequestRepo.findByProviderId(resultSip.getProviderId());
        Assert.assertEquals("Request should be deleted as requests is done success", 0, reqs.size());
    }

    @Requirements({ @Requirement("REGARDS_DSL_ING_PRO_160"), @Requirement("REGARDS_DSL_STO_AIP_010") })
    @Purpose("Test default process chain to ingest a new SIP provided by value and ask for files storage")
    @Test
    public void testDefaultProcessingChainWithError() {
        // Init a SIP in database with state CREATED and managed with default chain
        SIPCollection sips = SIPCollection.build(IngestMetadataDto.build(SESSION_OWNER,
                                                                         SESSION,
                                                                         IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                                         CATEGORIES,
                                                                         null,
                                                                         STORAGE_METADATA));

        Path filePath = Paths.get("data1.fits");
        String checksum = "sdsdfm1211vd";
        SIP sip = SIP.build(EntityType.DATA, SIP_DEFAULT_CHAIN_ID_TEST);
        sip.withDataObject(DataType.RAWDATA, filePath, checksum);
        sip.withSyntax("FITS(FlexibleImageTransport)",
                       "http://www.iana.org/assignments/media-types/application/fits",
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
        Assert.assertEquals(SESSION_OWNER, resultSip.getSessionOwner());
        Assert.assertEquals(SESSION, resultSip.getSession());

        // Check that files storage has been requested
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<FileStorageRequestDTO>> storageArgs = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(storageClient, Mockito.times(1)).store(storageArgs.capture());
        Assert.assertTrue("File storage url is not vali in storage request",
                          storageArgs.getValue()
                                     .stream()
                                     .anyMatch(req -> req.getOriginUrl()
                                                         .equals(OAISDataObjectLocation.build(filePath).getUrl())));
        Assert.assertTrue("File storage is not valid in storage request",
                          storageArgs.getValue()
                                     .stream()
                                     .anyMatch(req -> req.getStorage().equals(STORAGE_METADATA.getPluginBusinessId())));

        // Check status of IngestRequest
        List<IngestRequest> reqs = ingestRequestRepo.findByProviderId(resultSip.getProviderId());
        Assert.assertEquals(1, reqs.size());
        IngestRequest req = reqs.get(0);
        Assert.assertEquals(1, req.getAips().size());
        Assert.assertEquals(IngestRequestStep.REMOTE_STORAGE_REQUESTED, req.getStep());
        Assert.assertEquals(InternalRequestState.TO_SCHEDULE, req.getState());
        AIPEntity aip = req.getAips().get(0);

        String remoteStepGroupId = req.getRemoteStepGroupIds().get(0);
        RequestResultInfoDTO error = RequestResultInfoDTO.build(remoteStepGroupId,
                                                                checksum,
                                                                STORAGE_METADATA.getPluginBusinessId(),
                                                                null,
                                                                Sets.newHashSet(aip.getAipId()),
                                                                new FileReferenceDTO(),
                                                                "simulated error");
        RequestInfo ri = RequestInfo.build(remoteStepGroupId, Sets.newHashSet(), Sets.newHashSet(error));
        Set<RequestInfo> requestInfos = Sets.newHashSet(ri);
        // Simulate error response from storage
        storageResponseHandler.onStoreError(requestInfos);

        // Check status of IngestRequest
        reqs = ingestRequestRepo.findByProviderId(resultSip.getProviderId());
        Assert.assertEquals(1, reqs.size());
        req = reqs.get(0);
        Assert.assertEquals(1, req.getAips().size());
        Assert.assertEquals(IngestRequestStep.REMOTE_STORAGE_ERROR, req.getStep());
        Assert.assertEquals(InternalRequestState.ERROR, req.getState());
    }

    @Requirement("REGARDS_DSL_ING_PRO_160")
    @Requirement("REGARDS_DSL_ING_PRO_170")
    @Requirement("REGARDS_DSL_ING_PRO_180")
    @Requirement("REGARDS_DSL_ING_PRO_300")
    @Requirement("REGARDS_DSL_ING_PRO_400")
    @Purpose("Test fully configured process chain to ingest a new SIP provided by value")
    @Test
    public void testProcessingChain() {

        SIPCollection sips = SIPCollection.build(IngestMetadataDto.build(SESSION_OWNER,
                                                                         SESSION,
                                                                         PROCESSING_CHAIN_TEST,
                                                                         CATEGORIES,
                                                                         null,
                                                                         STORAGE_METADATA));

        SIP sip = SIP.build(EntityType.DATA, SIP_ID_TEST);
        sip.withDataObject(DataType.RAWDATA, Paths.get("data2.fits"), "sdsdfm1211vd");
        sip.withSyntax("FITS(FlexibleImageTransport)",
                       "http://www.iana.org/assignments/media-types/application/fits",
                       MediaType.valueOf("application/fits"));
        sip.registerContentInformation();
        sips.add(sip);

        // Simulate an error on each step
        simulateProcessingError(sips, PreprocessingTestPlugin.class);
        simulateProcessingError(sips, ValidationTestPlugin.class);
        simulateProcessingError(sips, AIPGenerationTestPlugin.class);
        simulateProcessingError(sips, AIPStorageMetadataTestPlugin.class);
        simulateProcessingError(sips, AIPTaggingTestPlugin.class);

        // Simulate a full process without error
        stepErrorSimulator.setSimulateErrorForStep(null);

        // Ingest
        ingestService.handleIngestRequests(IngestService.sipToFlow(sips));
        ingestServiceTest.waitForIngestion(1, FIVE_SECONDS);

        SIPEntity resultSip = sipRepository.findTopByProviderIdOrderByCreationDateDesc(SIP_ID_TEST);
        Assert.assertNotNull(resultSip);
        Assert.assertEquals(SIPState.INGESTED, resultSip.getState());
        Assert.assertEquals(SESSION_OWNER, resultSip.getSessionOwner());
        Assert.assertEquals(SESSION, resultSip.getSession());

        Set<AIPEntity> resultAips = aipRepository.findBySipSipId(resultSip.getSipId());
        Assert.assertNotNull(resultAips);
        Assert.assertTrue(resultAips.size() == 1);
        AIPEntity resultAip = resultAips.stream().findFirst().get();
        Assert.assertNotNull(resultAip);
        Assert.assertEquals(AIPState.GENERATED, resultAip.getState());
        Assert.assertNotNull(resultSip.getVersion());
        Assert.assertNotNull(resultAip.getAip().getVersion());
        Assert.assertEquals(resultSip.getVersion(), resultAip.getAip().getVersion());
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

        Mockito.verify(ingestRequestService, Mockito.times(1))
               .handleIngestJobFailed(ingestRequestCaptor.capture(),
                                      sipCaptor.capture(),
                                      ArgumentCaptor.forClass(String.class).capture());
        Mockito.clearInvocations(ingestRequestService);
        IngestRequest request = ingestRequestCaptor.getValue();
        Assert.assertNotNull(request);
        Assert.assertEquals(InternalRequestState.ERROR, request.getState());
        Assert.assertTrue(!request.getErrors().isEmpty());

        Assert.assertNotNull(sipCaptor.getValue());

        // No sip ingested
        ingestServiceTest.waitForIngestion(0, FIVE_SECONDS);
    }

    @Purpose("Test fully configured process chain to ingest a new SIP provided by reference")
    @Test
    public void testProcessingChainByRef() {

        // Init a SIP with reference in database with state CREATED
        SIPCollection sips = SIPCollection.build(IngestMetadataDto.build(SESSION_OWNER,
                                                                         SESSION,
                                                                         PROCESSING_CHAIN_TEST,
                                                                         CATEGORIES,
                                                                         null,
                                                                         STORAGE_METADATA));

        SIP sip = SIP.buildReference(EntityType.DATA,
                                     SIP_REF_ID_TEST,
                                     Paths.get("src/test/resources/file_ref.xml"),
                                     "1e2d4ab665784e43243b9b07724cd483");
        sips.add(sip);

        // Ingest
        ingestService.handleIngestRequests(IngestService.sipToFlow(sips));
        ingestServiceTest.waitForIngestion(1, FIVE_SECONDS);

        SIPEntity resultSip = sipRepository.findTopByProviderIdOrderByCreationDateDesc(SIP_REF_ID_TEST);
        Assert.assertNotNull(resultSip);
        Assert.assertEquals("Has no files are associated to the SIP, the SIP is immediatly at STORED state",
                            SIPState.STORED,
                            resultSip.getState());
        Assert.assertEquals(SESSION_OWNER, resultSip.getSessionOwner());
        Assert.assertEquals(SESSION, resultSip.getSession());
    }
}
