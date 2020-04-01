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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.chain.StorageMetadataProvider;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChainType;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChains;
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;
import fr.cnes.regards.modules.acquisition.service.plugin.LongLastingSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;
import fr.cnes.regards.modules.acquisition.service.session.SessionNotifier;
import fr.cnes.regards.modules.acquisition.service.session.SessionProductPropertyEnum;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;

/**
 * Launch a chain with very long plugin actions and stop it.
 *
 *
 * <b>Must not be transactional in order to run jobs</b>
 *
 * @author Marc SORDI
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=acq_start_stop", "regards.amqp.enabled=true" }
// ,locations = { "classpath:application-local.properties" }
)
@ActiveProfiles("testAmqp")
public class StartStopChainTest extends AbstractMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(StartStopChainTest.class);

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private SessionNotificationHandler notifHandler;

    @Before
    public void before() throws ModuleException {
        processingService.getFullChains().forEach(c -> {
            try {
                processingService.patchStateAndMode(c.getId(), UpdateAcquisitionProcessingChains
                        .build(false, AcquisitionProcessingChainMode.AUTO, UpdateAcquisitionProcessingChainType.ALL));
                processingService.deleteChain(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
        pluginRepo.deleteAll();
        jobInfoRepo.deleteAll();
        productRepository.deleteAll();
    }

    /**
     * Create chain with slow SIP generation plugin
     */
    private AcquisitionProcessingChain createProcessingChain(String label, Class<?> sipGenPluginClass, Path searchDir,
            Path searchDirThumbnail) throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel(label);
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");
        processingChain.setPeriodicity("0 * * * * *");
        processingChain.setCategories(Sets.newLinkedHashSet());

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(GlobDiskScanning.FIELD_DIRS,
                                        PluginParameterTransformer.toJson(Arrays.asList(searchDir.toString()))));

        PluginConfiguration scanPlugin = PluginConfiguration.build(GlobDiskScanning.class, null, parameters);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin");
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        if (searchDirThumbnail != null) {
            AcquisitionFileInfo fileInfo2 = new AcquisitionFileInfo();
            fileInfo2.setMandatory(Boolean.TRUE);
            fileInfo2.setComment("A comment 2");
            fileInfo2.setMimeType(MediaType.IMAGE_PNG);
            fileInfo2.setDataType(DataType.THUMBNAIL);

            Set<IPluginParam> parameters2 = IPluginParam.set(IPluginParam
                    .build(GlobDiskScanning.FIELD_DIRS,
                           PluginParameterTransformer.toJson(Arrays.asList(searchDirThumbnail.toString()))));

            PluginConfiguration scanPlugin2 = PluginConfiguration.build(GlobDiskScanning.class, "ScanPlugin2",
                                                                        parameters2);
            scanPlugin2.setIsActive(true);
            scanPlugin2.setLabel("Scan plugin");
            fileInfo2.setScanPlugin(scanPlugin2);

            processingChain.addFileInfo(fileInfo2);
        }

        // Validation
        PluginConfiguration validationPlugin = PluginConfiguration.build(DefaultFileValidation.class, "validPlugin",
                                                                         new HashSet<IPluginParam>());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        Set<IPluginParam> parametersProduct = IPluginParam
                .set(IPluginParam.build(DefaultProductPlugin.FIELD_REMOVE_EXT, true));

        PluginConfiguration productPlugin = PluginConfiguration.build(DefaultProductPlugin.class, "productPlugin",
                                                                      parametersProduct);
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginConfiguration.build(sipGenPluginClass, "sipGenPlugin",
                                                                     new HashSet<IPluginParam>());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        List<StorageMetadataProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataProvider.build("AWS", "/path/to/file", new HashSet<>()));
        storages.add(StorageMetadataProvider.build("HELLO", "/other/path/to/file", new HashSet<>()));
        processingChain.setStorages(storages);

        // Save processing chain
        return processingService.createChain(processingChain);
    }

    /**
     * Update chain with default generation plugin to speed up generation
     */
    private void updateProcessingChain(Long processingChainId) throws ModuleException {

        // Reload from database
        AcquisitionProcessingChain processingChain = processingService.getChain(processingChainId);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginConfiguration.build(DefaultSIPGeneration.class, "sipGenPlugin",
                                                                     new HashSet<IPluginParam>());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("Default SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        processingService.updateChain(processingChain);
    }

    @Before
    public void init() {
        simulateApplicationReadyEvent();
    }

    @Test
    public void startChainWithIncompletes() throws ModuleException, InterruptedException {
        notifHandler.clear();
        // Create a chain
        AcquisitionProcessingChain processingChain = createProcessingChain("Start Stop 1", DefaultSIPGeneration.class,
                                                                           Paths.get("src", "test", "resources",
                                                                                     "startstop", "fake")
                                                                                   .toAbsolutePath(),
                                                                           Paths.get("src", "test", "resources",
                                                                                     "startstop", "images")
                                                                                   .toAbsolutePath());
        // Start chain
        processingService.startManualChain(processingChain.getId(), Optional.of(UUID.randomUUID().toString()), false);

        // Check all products are registered
        long productCount;
        int loops = 100;
        do {
            Thread.sleep(1_000);
            loops--;
            productCount = productRepository.count();
        } while ((productCount < 100) && (loops != 0));

        if (loops == 0) {
            Assert.fail();
        }

        LOGGER.info("-----> All {} products are registered !", productCount);

        // Check that no running jobs remain
        loops = 100;
        long runningAcquisitionJobs = 0;
        do {
            Thread.sleep(1_000);
            loops--;
            runningAcquisitionJobs = jobInfoService.retrieveJobsCount(ProductAcquisitionJob.class.getName(),
                                                                      JobStatus.RUNNING);
        } while (((runningAcquisitionJobs != 0) && (loops != 0)));

        if (loops == 0) {
            Assert.assertEquals(0, runningAcquisitionJobs);
        }

        LOGGER.info("-----> Number of running ProductAcquisitionJob jobs {} ", runningAcquisitionJobs);

        // At the end, 95 product must be valid / 5 incomplete
        loops = 100;
        long validProducts;
        do {
            Thread.sleep(1_000);
            loops--;
            validProducts = productRepository
                    .countByProcessingChainAndSipStateIn(processingChain, Arrays.asList(ProductSIPState.SUBMITTED));
        } while ((validProducts < 95) && (loops != 0));

        if (loops == 0) {
            Assert.assertEquals(95, validProducts);
        }

        Assert.assertEquals(5, productRepository
                .countByProcessingChainAndStateIn(processingChain, Arrays.asList(ProductState.ACQUIRING)));

        // Check notification for acquired files
        // --- 195 files scanned / 100 data / 95 images
        Assert.assertEquals(195,
                            notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                          SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED.getValue(),
                                                          SessionNotificationOperator.INC));
        // Check notification for completed files
        // -- 95 products set to COMPLETED status
        Assert.assertEquals(95,
                            notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                          SessionProductPropertyEnum.PROPERTY_COMPLETED.getValue(),
                                                          SessionNotificationOperator.INC));
        // -- 95 products pass from COMPLETED to GENERATED
        Assert.assertEquals(95,
                            notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                          SessionProductPropertyEnum.PROPERTY_COMPLETED.getValue(),
                                                          SessionNotificationOperator.DEC));
        // Check notification for incomplet files
        // --- After All 5 products should be incomplets
        int inc = notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                SessionProductPropertyEnum.PROPERTY_INCOMPLETE.getValue(),
                                                SessionNotificationOperator.INC);
        int dec = notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                SessionProductPropertyEnum.PROPERTY_INCOMPLETE.getValue(),
                                                SessionNotificationOperator.DEC);
        Assert.assertEquals(5, inc - dec);

        // Check notification for generated products files
        Assert.assertEquals(95,
                            notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                          SessionProductPropertyEnum.PROPERTY_GENERATED.getValue(),
                                                          SessionNotificationOperator.INC));
        Assert.assertEquals(0,
                            notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                          SessionProductPropertyEnum.PROPERTY_GENERATED.getValue(),
                                                          SessionNotificationOperator.DEC));
    }

    @Test
    public void startAndStop() throws ModuleException, InterruptedException {
        // Create a chain
        AcquisitionProcessingChain processingChain = createProcessingChain("Start Stop 1",
                                                                           LongLastingSIPGeneration.class,
                                                                           Paths.get("src", "test", "resources",
                                                                                     "startstop", "fake")
                                                                                   .toAbsolutePath(),
                                                                           null);

        String session = UUID.randomUUID().toString();
        notifHandler.clear();
        // Start chain
        processingService.startManualChain(processingChain.getId(), Optional.of(session), false);

        // Check all products are registered
        long productCount;
        int loops = 100;
        do {
            Thread.sleep(1_000);
            loops--;
            productCount = productRepository.count();
        } while ((productCount < 100) && (loops != 0));

        if (loops == 0) {
            Assert.fail();
        }

        // Check at least one SIP generation job is working
        loops = 100;
        long runningGenerationJobs;
        do {
            Thread.sleep(1_000);
            loops--;
            runningGenerationJobs = jobInfoService.retrieveJobsCount(SIPGenerationJob.class.getName(),
                                                                     JobStatus.RUNNING);
        } while ((runningGenerationJobs < 1) && (loops != 0));

        if (loops == 0) {
            Assert.fail();
        }

        // Stop it verifying all threads are properly stopped
        processingService.stopAndCleanChain(processingChain.getId());

        // Check that no running jobs remain
        loops = 100;
        long runningAcquisitionJobs;
        do {
            Thread.sleep(1_000);
            loops--;
            runningAcquisitionJobs = jobInfoService.retrieveJobsCount(ProductAcquisitionJob.class.getName(),
                                                                      JobStatus.RUNNING);
            runningGenerationJobs = jobInfoService.retrieveJobsCount(SIPGenerationJob.class.getName(),
                                                                     JobStatus.RUNNING);
        } while (((runningAcquisitionJobs != 0) || (runningGenerationJobs != 0)) && (loops != 0));

        if (loops == 0) {
            Assert.fail();
        }

        // Waiting for stop chain thread to really stop!
        Thread.sleep(2_000);

        // Restart chain verifying all re-run properly
        updateProcessingChain(processingChain.getId());
        processingService.startManualChain(processingChain.getId(), Optional.of(session), false);

        // At the end, all product must be valid
        loops = 100;
        long validProducts;
        do {
            Thread.sleep(1_000);
            loops--;
            validProducts = productRepository
                    .countByProcessingChainAndSipStateIn(processingChain, Arrays.asList(ProductSIPState.SUBMITTED));
        } while ((validProducts < 100) && (loops != 0));

        if (loops == 0) {
            Assert.fail();
        }

        // Check notification for acquired files
        Assert.assertEquals(100,
                            notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                          SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED.getValue(),
                                                          SessionNotificationOperator.INC));
        // Check notification for generated products files
        Assert.assertEquals(100,
                            notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                          SessionProductPropertyEnum.PROPERTY_GENERATED.getValue(),
                                                          SessionNotificationOperator.INC));
        Assert.assertEquals(0,
                            notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                          SessionProductPropertyEnum.PROPERTY_GENERATED.getValue(),
                                                          SessionNotificationOperator.DEC));
        // Check notification for completed files
        Assert.assertEquals(100,
                            notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                          SessionProductPropertyEnum.PROPERTY_COMPLETED.getValue(),
                                                          SessionNotificationOperator.INC));
        Assert.assertEquals(100,
                            notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                          SessionProductPropertyEnum.PROPERTY_COMPLETED.getValue(),
                                                          SessionNotificationOperator.DEC));
        // Check notification for incomplet files
        Assert.assertEquals(0,
                            notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                          SessionProductPropertyEnum.PROPERTY_INCOMPLETE.getValue(),
                                                          SessionNotificationOperator.INC));
        Assert.assertEquals(0,
                            notifHandler.getPropertyCount(SessionNotifier.GLOBAL_SESSION_STEP.toString(),
                                                          SessionProductPropertyEnum.PROPERTY_INCOMPLETE.getValue(),
                                                          SessionNotificationOperator.DEC));
    }

}
