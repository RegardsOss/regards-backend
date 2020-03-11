/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileInfoRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.chain.StorageMetadataProvider;
import fr.cnes.regards.modules.acquisition.plugins.Arcad3IsoprobeDensiteProductPlugin;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;
import fr.cnes.regards.modules.acquisition.service.session.SessionProductPropertyEnum;
import fr.cnes.regards.modules.sessionmanager.client.SessionNotificationPublisher;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;

/**
 * Test {@link AcquisitionProcessingService} for {@link Product} workflow
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acq_cdpp_product" })
public class CdppProductAcquisitionServiceTest extends AbstractMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(CdppProductAcquisitionServiceTest.class);

    private static final Path SRC_BASE_PATH = Paths.get("src", "test", "resources", "data", "plugins", "cdpp");

    private static final Path SRC_DATA_PATH = SRC_BASE_PATH.resolve("data");

    private static final Path SRC_BROWSE_PATH = SRC_BASE_PATH.resolve("browse");

    private static final Path TARGET_BASE_PATH = Paths.get("target", "cdpp");

    private static final Path TARGET_DATA_PATH = TARGET_BASE_PATH.resolve("data");

    private static final Path TARGET_BROWSE_PATH = TARGET_BASE_PATH.resolve("browse");

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IAcquisitionFileRepository acqFileRepository;

    @Autowired
    private IProductService productService;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IAcquisitionFileInfoRepository fileInfoRepository;

    @Autowired
    private IAcquisitionProcessingChainRepository acquisitionProcessingChainRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IPluginConfigurationRepository pluginConfRepository;

    @Autowired
    private SessionNotificationPublisher notificationClient;

    @Before
    public void before() throws ModuleException, IOException {
        acqFileRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        fileInfoRepository.deleteAllInBatch();
        acquisitionProcessingChainRepository.deleteAllInBatch();
        pluginConfRepository.deleteAllInBatch();

        // Prepare data repositories
        if (Files.exists(TARGET_DATA_PATH)) {
            FileUtils.forceDelete(TARGET_DATA_PATH.toFile());
        }
        Files.createDirectories(TARGET_DATA_PATH);
        if (Files.exists(TARGET_BROWSE_PATH)) {
            FileUtils.forceDelete(TARGET_BROWSE_PATH.toFile());
        }
        Files.createDirectories(TARGET_BROWSE_PATH);

        notificationClient.clearCache();
    }

    @SuppressWarnings("deprecation")
    public AcquisitionProcessingChain createProcessingChain() throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("CDPPChain");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");
        processingChain.setPeriodicity("0 * * * * *");
        processingChain.setCategories(Sets.newLinkedHashSet());

        // RAW DATA file infos
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("RAWDATA");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(GlobDiskScanning.FIELD_DIRS,
                                        PluginParameterTransformer.toJson(Arrays.asList(TARGET_DATA_PATH.toString()))));

        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(parameters, GlobDiskScanning.class);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin RAWDATA" + Math.random());
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // QUICKLOOK SD file infos
        fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("QUICKLOOK_SD");
        fileInfo.setMimeType(MediaType.IMAGE_PNG);
        fileInfo.setDataType(DataType.QUICKLOOK_SD);

        parameters = IPluginParam.set(
                                      IPluginParam.build(GlobDiskScanning.FIELD_DIRS,
                                                         PluginParameterTransformer
                                                                 .toJson(Arrays.asList(TARGET_BROWSE_PATH.toString()))),
                                      IPluginParam.build(GlobDiskScanning.FIELD_GLOB, "*B.png"));

        scanPlugin = PluginUtils.getPluginConfiguration(parameters, GlobDiskScanning.class);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin QUICKLOOK_SD" + Math.random());
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // QUICKLOOK MD file infos
        fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("QUICKLOOK_MD");
        fileInfo.setMimeType(MediaType.IMAGE_PNG);
        fileInfo.setDataType(DataType.QUICKLOOK_MD);

        parameters = IPluginParam.set(
                                      IPluginParam.build(GlobDiskScanning.FIELD_DIRS,
                                                         PluginParameterTransformer
                                                                 .toJson(Arrays.asList(TARGET_BROWSE_PATH.toString()))),
                                      IPluginParam.build(GlobDiskScanning.FIELD_GLOB, "*C.png"));

        scanPlugin = PluginUtils.getPluginConfiguration(parameters, GlobDiskScanning.class);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin QUICKLOOK_MD" + Math.random());
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                                  DefaultFileValidation.class);
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin" + Math.random());
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginUtils
                .getPluginConfiguration(Sets.newHashSet(), Arcad3IsoprobeDensiteProductPlugin.class);
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin" + Math.random());
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                              DefaultSIPGeneration.class);
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin" + Math.random());
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        List<StorageMetadataProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataProvider.build("AWS", "/path/to/file", new HashSet<>()));
        storages.add(StorageMetadataProvider.build("HELLO", "/other/path/to/file", new HashSet<>()));
        processingChain.setStorages(storages);

        // Save processing chainJe vois q
        return processingService.createChain(processingChain);
    }

    @Test
    public void acquisitionWorkflowTest() throws ModuleException, IOException {
        // Prepare data
        FileUtils.copyDirectory(SRC_DATA_PATH.toFile(), TARGET_DATA_PATH.toFile(), false);
        FileUtils.copyDirectory(SRC_BROWSE_PATH.toFile(), TARGET_BROWSE_PATH.toFile(), false);

        AcquisitionProcessingChain processingChain = createProcessingChain();
        String session1 = "session1";
        doAcquire(processingChain, session1, true);

        // Reset last modification date
        processingChain.getFileInfos().forEach(f -> f.setLastModificationDate(null));

        String session2 = "session2";
        doAcquire(processingChain, session2, true);

        Assert.assertTrue(assertSession(processingChain.getLabel(), session1, 0L, 0L, null, 0L));
        Assert.assertTrue(assertSession(processingChain.getLabel(), session2, 6L, 0L, null, 1L));
    }

    @Test
    public void twoPhaseAcquisitionTest() throws ModuleException, IOException {
        // Prepare data (only data)
        FileUtils.copyDirectory(SRC_DATA_PATH.toFile(), TARGET_DATA_PATH.toFile(), false);

        AcquisitionProcessingChain processingChain = createProcessingChain();
        String session1 = "session1";
        doAcquire(processingChain, session1, false);

        // Prepare data (browse data)
        FileUtils.copyDirectory(SRC_BROWSE_PATH.toFile(), TARGET_BROWSE_PATH.toFile(), false);

        String session2 = "session2";
        doAcquire(processingChain, session2, false);

        Assert.assertTrue(assertSession(processingChain.getLabel(), session1, 0L, null, 0L, null));
        Assert.assertTrue(assertSession(processingChain.getLabel(), session2, 3L, 0L, null, 1L));
    }

    private boolean assertSession(String sessionOwner, String session, Long acquiredFiles, Long completed,
            Long incompleted, Long generated) {
        return notificationClient.assertCount(sessionOwner, session,
                                              SessionProductPropertyEnum.PROPERTY_GENERATED.getValue(),
                                              SessionNotificationState.OK, generated)
                && notificationClient.assertCount(sessionOwner, session,
                                                  SessionProductPropertyEnum.PROPERTY_COMPLETED.getValue(),
                                                  SessionNotificationState.OK, completed)
                && notificationClient.assertCount(sessionOwner, session,
                                                  SessionProductPropertyEnum.PROPERTY_INCOMPLETE.getValue(),
                                                  SessionNotificationState.OK, incompleted)
                && notificationClient.assertCount(sessionOwner, session,
                                                  SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED.getValue(),
                                                  SessionNotificationState.OK, acquiredFiles);
    }

    public void doAcquire(AcquisitionProcessingChain processingChain, String session, boolean assertResult)
            throws ModuleException {

        processingService.scanAndRegisterFiles(processingChain, session);

        // Check registered files
        if (assertResult) {
            for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {
                Page<AcquisitionFile> inProgressFiles = acqFileRepository
                        .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS, fileInfo,
                                                            PageRequest.of(0, 1));
                Assert.assertTrue(inProgressFiles.getTotalElements() == 1);
            }
        }

        processingService.manageRegisteredFiles(processingChain, session);
        productService.manageUpdatedProducts(processingChain);

        // Check registered files
        if (assertResult) {
            for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {
                Page<AcquisitionFile> inProgressFiles = acqFileRepository
                        .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS, fileInfo,
                                                            PageRequest.of(0, 1));
                Assert.assertTrue(inProgressFiles.getTotalElements() == 0);

                Page<AcquisitionFile> validFiles = acqFileRepository
                        .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.VALID, fileInfo, PageRequest.of(0, 1));
                Assert.assertTrue(validFiles.getTotalElements() == 0);

                Page<AcquisitionFile> acquiredFiles = acqFileRepository
                        .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.ACQUIRED, fileInfo,
                                                            PageRequest.of(0, 1));
                Assert.assertTrue(acquiredFiles.getTotalElements() == 1);
            }
        }

        // Find product to schedule
        if (assertResult) {
            long scheduled = productService
                    .countByProcessingChainAndSipStateIn(processingChain, Arrays.asList(ProductSIPState.SCHEDULED));
            Assert.assertEquals(1, scheduled);

            Assert.assertTrue(productService.existsByProcessingChainAndSipStateIn(processingChain,
                                                                                  ProductSIPState.SCHEDULED));
        }

        // Run the job synchronously
        SIPGenerationJob genJob = new SIPGenerationJob();
        beanFactory.autowireBean(genJob);

        Map<String, JobParameter> parameters = new HashMap<>();
        parameters.put(SIPGenerationJob.CHAIN_PARAMETER_ID,
                       new JobParameter(SIPGenerationJob.CHAIN_PARAMETER_ID, processingChain.getId()));
        Set<String> productNames = new HashSet<>();
        productNames.add("ISO_DENS_20330518_0533");
        parameters.put(SIPGenerationJob.PRODUCT_NAMES, new JobParameter(SIPGenerationJob.PRODUCT_NAMES, productNames));

        genJob.setParameters(parameters);
        genJob.run();

        if (assertResult) {
            Assert.assertFalse(productService.existsByProcessingChainAndSipStateIn(processingChain,
                                                                                   ProductSIPState.SCHEDULED));

            // Find product to submitted
            long submitted = productService
                    .countByProcessingChainAndSipStateIn(processingChain, Arrays.asList(ProductSIPState.SUBMITTED));
            Assert.assertEquals(1, submitted);
        }

        notificationClient.debugSession();
    }
}
