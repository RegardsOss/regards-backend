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
package fr.cnes.regards.modules.acquisition.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.chain.StorageMetadataProvider;
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;
import fr.cnes.regards.modules.acquisition.service.plugin.LongLastingSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;

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
        properties = { "spring.jpa.properties.hibernate.default_schema=acq_start_stop", "regards.amqp.enabled=true" })
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

    @Before
    public void before() {
        pluginRepo.deleteAll();
        jobInfoRepo.deleteAll();
    }

    /**
     * Create chain with slow SIP generation plugin
     */
    private AcquisitionProcessingChain createProcessingChain(String label, Path searchDir) throws ModuleException {

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

        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(parameters, GlobDiskScanning.class);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin");
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                                  DefaultFileValidation.class);
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                               DefaultProductPlugin.class);
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                              LongLastingSIPGeneration.class);
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
        PluginConfiguration sipGenPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                              DefaultSIPGeneration.class);
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
    public void startAndStop() throws ModuleException, InterruptedException {
        // Create a chain
        AcquisitionProcessingChain processingChain = createProcessingChain("Start Stop 1", Paths
                .get("src", "test", "resources", "startstop", "fake").toAbsolutePath());

        // Start chain
        processingService.startManualChain(processingChain.getId(), Optional.empty(), false);

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
        processingService.startManualChain(processingChain.getId(), Optional.empty(), false);

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
    }

}
