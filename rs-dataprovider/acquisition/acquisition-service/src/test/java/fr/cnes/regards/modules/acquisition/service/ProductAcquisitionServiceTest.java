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
package fr.cnes.regards.modules.acquisition.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultDiskScanning;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;

/**
 * Test {@link AcquisitionProcessingService} for {@link Product} workflow
 *
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acq_product", "jwt.secret=123456789",
        "regards.workspace=target/workspace" })
@ContextConfiguration(classes = { ProductAcquisitionServiceTest.AcquisitionConfiguration.class })
@MultitenantTransactional
public class ProductAcquisitionServiceTest extends AbstractDaoTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductAcquisitionServiceTest.class);

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IAcquisitionFileRepository acqFileRepository;

    @Autowired
    private IProductService productService;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules" })
    static class AcquisitionConfiguration {
    }

    public AcquisitionProcessingChain createProcessingChain() throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("Product");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");
        processingChain.setDatasetIpId("DATASET_IP_ID");

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        fileInfo.setDataType(DataType.RAWDATA);

        // Search directory
        Path searchDir = Paths.get("src", "test", "resources", "data", "plugins", "scan");

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(DefaultDiskScanning.FIELD_DIRS, Arrays.asList(searchDir.toString())).getParameters();

        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(parameters, DefaultDiskScanning.class,
                                                                            Lists.newArrayList());
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin");
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), DefaultFileValidation.class, Lists.newArrayList());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), DefaultProductPlugin.class, Lists.newArrayList());
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), DefaultSIPGeneration.class, Lists.newArrayList());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        pluginService.addPluginPackage(IScanPlugin.class.getPackage().getName());
        pluginService.addPluginPackage(DefaultDiskScanning.class.getPackage().getName());

        // Save processing chain
        return processingService.createChain(processingChain);
    }

    @Test
    public void acquisitionWorkflowTest() throws ModuleException {

        AcquisitionProcessingChain processingChain = createProcessingChain();
        AcquisitionFileInfo fileInfo = processingChain.getFileInfos().get(0);

        processingService.scanAndRegisterFiles(processingChain);

        // Check registered files
        List<AcquisitionFile> inProgressFiles = acqFileRepository
                .findByStateAndFileInfo(AcquisitionFileState.IN_PROGRESS, fileInfo);
        Assert.assertTrue(inProgressFiles.size() == 4);

        processingService.validateFiles(processingChain);

        // Check registered files
        inProgressFiles = acqFileRepository.findByStateAndFileInfo(AcquisitionFileState.IN_PROGRESS,
                                                                   processingChain.getFileInfos().get(0));
        Assert.assertTrue(inProgressFiles.size() == 0);
        List<AcquisitionFile> validFiles = acqFileRepository.findByStateAndFileInfo(AcquisitionFileState.VALID,
                                                                                    fileInfo);
        Assert.assertTrue(validFiles.size() == 4);

        processingService.buildProducts(processingChain);

        // Check registered files
        inProgressFiles = acqFileRepository.findByStateAndFileInfo(AcquisitionFileState.IN_PROGRESS,
                                                                   processingChain.getFileInfos().get(0));
        Assert.assertTrue(inProgressFiles.size() == 0);
        validFiles = acqFileRepository.findByStateAndFileInfo(AcquisitionFileState.VALID, fileInfo);
        Assert.assertTrue(validFiles.size() == 0);
        List<AcquisitionFile> acquiredFiles = acqFileRepository.findByStateAndFileInfo(AcquisitionFileState.ACQUIRED,
                                                                                       fileInfo);
        Assert.assertTrue(acquiredFiles.size() == 4);

        // Find product to schedule
        Set<Product> products = productService.findChainProductsToSchedule(processingChain);
        Assert.assertTrue(products.size() == 4);

        // Test job algo synchronously
        for (Product product : products) {
            SIPGenerationJob genJob = new SIPGenerationJob();
            beanFactory.autowireBean(genJob);

            Map<String, JobParameter> parameters = new HashMap<>();
            parameters.put(SIPGenerationJob.CHAIN_PARAMETER_ID,
                           new JobParameter(SIPGenerationJob.CHAIN_PARAMETER_ID, processingChain.getId()));
            parameters.put(SIPGenerationJob.PRODUCT_ID, new JobParameter(SIPGenerationJob.PRODUCT_ID, product.getId()));

            genJob.setParameters(parameters);
            genJob.run();
        }
    }
}
