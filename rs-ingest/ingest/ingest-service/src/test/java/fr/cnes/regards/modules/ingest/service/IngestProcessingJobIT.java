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
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.dto.OAISDataObjectLocationDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.filecatalog.dto.FileLocationDto;
import fr.cnes.regards.modules.filecatalog.dto.FileReferenceDto;
import fr.cnes.regards.modules.filecatalog.dto.FileReferenceMetaInfoDto;
import fr.cnes.regards.modules.filecatalog.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.filecatalog.dto.request.RequestResultInfoDto;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.dto.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.StorageDto;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.aip.scheduler.IngestRequestSchedulerService;
import fr.cnes.regards.modules.ingest.service.chain.ProcessingChainTestErrorSimulator;
import fr.cnes.regards.modules.ingest.service.flow.StorageResponseFlowHandler;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.ingest.service.plugin.*;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import org.awaitility.Awaitility;
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
import java.util.concurrent.TimeUnit;

/**
 * Test class to verify {@link IngestProcessingJob}.
 *
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingestjob",
                                   "eureka.client.enabled=false",
                                   "regards.ingest.aip.delete.bulk.delay=100",
                                   "regards.ingest.schedule.request.initial.delay=500",
                                   "regards.ingest.schedule.request.delay=500" })
public class IngestProcessingJobIT extends IngestMultitenantServiceIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestProcessingJobIT.class);

    private static final String SIP_ID_TEST = "SIP_001";

    private static final String SIP_DEFAULT_CHAIN_ID_TEST = "SIP_002";

    private static final String SIP_REF_ID_TEST = "SIP_003";

    private static final String PROCESSING_CHAIN_TEST = "fullProcessingChain";

    private static final String SESSION_OWNER = "sessionOwner";

    private static final String SESSION = "session";

    private static final StorageDto STORAGE_METADATA = new StorageDto("disk");

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

    @Autowired
    private IngestRequestSchedulerService ingestRequestSchedulerService;

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
        IngestMetadataDto metadata = new IngestMetadataDto(SESSION_OWNER,
                                                           SESSION,
                                                           null,
                                                           IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                           CATEGORIES,
                                                           null,
                                                           null,
                                                           STORAGE_METADATA);
        SIPCollection sips = SIPCollection.build(metadata);

        Path filePath = Paths.get("data1.fits");
        String checksum = "sdsdfm1211vd";
        SIPDto sip = SIPDto.build(EntityType.DATA, SIP_DEFAULT_CHAIN_ID_TEST);
        sip.withDataObject(DataType.RAWDATA, filePath, checksum);
        sip.withSyntax("FITS(FlexibleImageTransport)",
                       "http://www.iana.org/assignments/media-types/application/fits",
                       MediaType.valueOf("application/fits"));
        sip.registerContentInformation();
        sips.add(sip);

        // Ingest
        Collection<IngestRequestFlowItem> items = IngestService.sipToFlow(sips);
        ingestService.handleIngestRequests(items);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            SIPEntity resultSip = sipRepository.findTopByProviderIdOrderByCreationDateDesc(SIP_DEFAULT_CHAIN_ID_TEST);
            return resultSip != null && resultSip.getState() == SIPState.INGESTED;
        });

        SIPEntity resultSip = sipRepository.findTopByProviderIdOrderByCreationDateDesc(SIP_DEFAULT_CHAIN_ID_TEST);
        Assert.assertNotNull(resultSip);
        Assert.assertEquals(SIPState.INGESTED, resultSip.getState());
        Assert.assertEquals(SESSION_OWNER, resultSip.getSessionOwner());
        Assert.assertEquals(SESSION, resultSip.getSession());

        // Check that files storage has been requested
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<FileStorageRequestDto>> storageArgs = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(storageClient, Mockito.times(1)).store(storageArgs.capture());
        Assert.assertTrue("File storage url is not vali in storage request",
                          storageArgs.getValue()
                                     .stream()
                                     .anyMatch(req -> req.getOriginUrl()
                                                         .equals(OAISDataObjectLocationDto.build(filePath).getUrl())));
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
        Assert.assertEquals(InternalRequestState.WAITING_REMOTE_STORAGE, req.getState());
        AIPEntity aip = req.getAips().get(0);

        String remoteStepGroupId = req.getRemoteStepGroupIds().get(0);
        RequestResultInfoDto success = RequestResultInfoDto.build(remoteStepGroupId,
                                                                  checksum,
                                                                  STORAGE_METADATA.getPluginBusinessId(),
                                                                  null,
                                                                  Sets.newHashSet(aip.getAipId()),
                                                                  new FileReferenceDto(OffsetDateTime.now(),
                                                                                       new FileReferenceMetaInfoDto(
                                                                                           checksum,
                                                                                           "MD5",
                                                                                           "file.name",
                                                                                           10L,
                                                                                           null,
                                                                                           null,
                                                                                           MediaType.APPLICATION_JSON.toString(),
                                                                                           "type"),
                                                                                       new FileLocationDto(
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
        IngestMetadataDto metadata = new IngestMetadataDto(SESSION_OWNER,
                                                           SESSION,
                                                           null,
                                                           IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                           CATEGORIES,
                                                           null,
                                                           null,
                                                           STORAGE_METADATA);
        SIPCollection sips = SIPCollection.build(metadata);

        Path filePath = Paths.get("data1.fits");
        String checksum = "sdsdfm1211vd";
        SIPDto sip = SIPDto.build(EntityType.DATA, SIP_DEFAULT_CHAIN_ID_TEST);
        sip.withDataObject(DataType.RAWDATA, filePath, checksum);
        sip.withSyntax("FITS(FlexibleImageTransport)",
                       "http://www.iana.org/assignments/media-types/application/fits",
                       MediaType.valueOf("application/fits"));
        sip.registerContentInformation();
        sips.add(sip);

        // Ingest
        Collection<IngestRequestFlowItem> items = IngestService.sipToFlow(sips);
        ingestService.handleIngestRequests(items);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            SIPEntity resultSip = sipRepository.findTopByProviderIdOrderByCreationDateDesc(SIP_DEFAULT_CHAIN_ID_TEST);
            return resultSip != null && resultSip.getState() == SIPState.INGESTED;
        });

        SIPEntity resultSip = sipRepository.findTopByProviderIdOrderByCreationDateDesc(SIP_DEFAULT_CHAIN_ID_TEST);
        Assert.assertNotNull(resultSip);
        Assert.assertEquals(SIPState.INGESTED, resultSip.getState());
        Assert.assertEquals(SESSION_OWNER, resultSip.getSessionOwner());
        Assert.assertEquals(SESSION, resultSip.getSession());

        // Check that files storage has been requested
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<FileStorageRequestDto>> storageArgs = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(storageClient, Mockito.times(1)).store(storageArgs.capture());
        Assert.assertTrue("File storage url is not vali in storage request",
                          storageArgs.getValue()
                                     .stream()
                                     .anyMatch(req -> req.getOriginUrl()
                                                         .equals(OAISDataObjectLocationDto.build(filePath).getUrl())));
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
        Assert.assertEquals(InternalRequestState.WAITING_REMOTE_STORAGE, req.getState());
        AIPEntity aip = req.getAips().get(0);

        String remoteStepGroupId = req.getRemoteStepGroupIds().get(0);
        RequestResultInfoDto error = RequestResultInfoDto.build(remoteStepGroupId,
                                                                checksum,
                                                                STORAGE_METADATA.getPluginBusinessId(),
                                                                null,
                                                                Sets.newHashSet(aip.getAipId()),
                                                                new FileReferenceDto(),
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
        Assert.assertEquals(IngestErrorType.GENERATION, req.getErrorType());
    }

    @Requirement("REGARDS_DSL_ING_PRO_160")
    @Requirement("REGARDS_DSL_ING_PRO_170")
    @Requirement("REGARDS_DSL_ING_PRO_180")
    @Requirement("REGARDS_DSL_ING_PRO_300")
    @Requirement("REGARDS_DSL_ING_PRO_400")
    @Purpose("Test fully configured process chain to ingest a new SIP provided by value")
    @Test
    public void testProcessingChain() {

        IngestMetadataDto metadata = new IngestMetadataDto(SESSION_OWNER,
                                                           SESSION,
                                                           null,
                                                           PROCESSING_CHAIN_TEST,
                                                           CATEGORIES,
                                                           null,
                                                           null,
                                                           STORAGE_METADATA);
        SIPCollection sips = SIPCollection.build(metadata);

        SIPDto sip = SIPDto.build(EntityType.DATA, SIP_ID_TEST);
        sip.withDataObject(DataType.RAWDATA, Paths.get("data2.fits"), "sdsdfm1211vd");
        sip.withSyntax("FITS(FlexibleImageTransport)",
                       "http://www.iana.org/assignments/media-types/application/fits",
                       MediaType.valueOf("application/fits"));
        sip.registerContentInformation();
        sips.add(sip);

        // Simulate an error on each step
        simulateProcessingError(sips, PreprocessingTestPlugin.class);
        ingestRequestRepo.deleteAllInBatch();
        simulateProcessingError(sips, ValidationTestPlugin.class);
        ingestRequestRepo.deleteAllInBatch();
        simulateProcessingError(sips, AIPGenerationTestPlugin.class);
        ingestRequestRepo.deleteAllInBatch();
        simulateProcessingError(sips, AIPStorageMetadataTestPlugin.class);
        ingestRequestRepo.deleteAllInBatch();
        simulateProcessingError(sips, AIPTaggingTestPlugin.class);
        ingestRequestRepo.deleteAllInBatch();

        // Simulate a full process without error
        stepErrorSimulator.setSimulateErrorForStep(null);

        // Ingest
        Collection<IngestRequestFlowItem> items = IngestService.sipToFlow(sips);
        ingestService.handleIngestRequests(items);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            SIPEntity resultSip = sipRepository.findTopByProviderIdOrderByCreationDateDesc(SIP_ID_TEST);
            return resultSip != null && resultSip.getState() == SIPState.INGESTED;
        });

        SIPEntity resultSip = sipRepository.findTopByProviderIdOrderByCreationDateDesc(SIP_ID_TEST);
        Assert.assertNotNull(resultSip);
        Assert.assertEquals(SIPState.INGESTED, resultSip.getState());
        Assert.assertEquals(SESSION_OWNER, resultSip.getSessionOwner());
        Assert.assertEquals(SESSION, resultSip.getSession());

        Set<AIPEntity> resultAips = aipRepository.findBySipSipId(resultSip.getSipId());
        Assert.assertNotNull(resultAips);
        Assert.assertEquals(1, resultAips.size());
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
        Collection<IngestRequestFlowItem> items = IngestService.sipToFlow(sips);
        ingestService.handleIngestRequests(items);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return ingestRequestRepo.findByProviderIdInAndStateIn(items.stream().map(i -> i.getSip().getId()).toList(),
                                                                  List.of(InternalRequestState.ERROR)).size()
                   == items.size();
        });

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
        Assert.assertFalse(request.getErrors().isEmpty());
        Assert.assertEquals(switch (errorClass.getSimpleName()) {
            case "PreprocessingTestPlugin" -> IngestErrorType.PREPROCESSING;
            case "ValidationTestPlugin" -> IngestErrorType.VALIDATION;
            case "AIPGenerationTestPlugin" -> IngestErrorType.GENERATION;
            case "AIPStorageMetadataTestPlugin" -> IngestErrorType.METADATA;
            case "AIPTaggingTestPlugin" -> IngestErrorType.TAGGING;
            default -> null;
        }, request.getErrorType());

        Assert.assertNotNull(sipCaptor.getValue());

        // No sip ingested
        ingestServiceTest.waitForIngestion(0, FIVE_SECONDS, getDefaultTenant());
    }

    @Purpose("Test fully configured process chain to ingest a new SIP provided by reference")
    @Test
    public void testProcessingChainByRef() {

        // Init a SIP with reference in database with state CREATED
        IngestMetadataDto metadata = new IngestMetadataDto(SESSION_OWNER,
                                                           SESSION,
                                                           null,
                                                           IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                           CATEGORIES,
                                                           null,
                                                           null,
                                                           STORAGE_METADATA);
        SIPCollection sips = SIPCollection.build(metadata);

        sips.add(SIPDto.buildReference(EntityType.DATA,
                                       SIP_REF_ID_TEST,
                                       Paths.get("src/test/resources/file_ref.xml"),
                                       "1e2d4ab665784e43243b9b07724cd483"));
        // Ingest
        ingestService.handleIngestRequests(IngestService.sipToFlow(sips));
        ingestRequestSchedulerService.scheduleRequests();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            SIPEntity resultSip = sipRepository.findTopByProviderIdOrderByCreationDateDesc(SIP_REF_ID_TEST);
            return resultSip != null && resultSip.getState() == SIPState.STORED;
        });

        SIPEntity resultSip = sipRepository.findTopByProviderIdOrderByCreationDateDesc(SIP_REF_ID_TEST);
        Assert.assertNotNull(resultSip);
        Assert.assertEquals("Has no files are associated to the SIP, the SIP is immediatly at STORED state",
                            SIPState.STORED,
                            resultSip.getState());
        Assert.assertEquals(SESSION_OWNER, resultSip.getSessionOwner());
        Assert.assertEquals(SESSION, resultSip.getSession());
    }
}
