/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMonitor;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;

/**
 * Test {@link AcquisitionProcessingService} for {@link Product} workflow
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acq_product" })
@MultitenantTransactional
public class ProductAcquisitionServiceTest extends AbstractMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductAcquisitionServiceTest.class);

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IAcquisitionFileRepository acqFileRepository;

    @Autowired
    private IProductService productService;

    @SuppressWarnings("unused")
    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IAcquisitionFileService fileService;

    public AcquisitionProcessingChain createProcessingChain(Path searchDir) throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("Product");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(GlobDiskScanning.FIELD_DIRS, Arrays.asList(searchDir.toString())).getParameters();

        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(parameters, GlobDiskScanning.class);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin");
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginUtils.getPluginConfiguration(Lists.newArrayList(),
                                                                                  DefaultFileValidation.class);
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginUtils.getPluginConfiguration(Lists.newArrayList(),
                                                                               DefaultProductPlugin.class);
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginUtils.getPluginConfiguration(Lists.newArrayList(),
                                                                              DefaultSIPGeneration.class);
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        // Save processing chain
        return processingService.createChain(processingChain);
    }

    @Test
    public void acquisitionWorkflowTest() throws ModuleException {

        AcquisitionProcessingChain processingChain = createProcessingChain(Paths.get("src", "test", "resources", "data",
                                                                                     "plugins", "scan"));
        AcquisitionFileInfo fileInfo = processingChain.getFileInfos().get(0);

        processingService.scanAndRegisterFiles(processingChain);

        // Check registered files
        Page<AcquisitionFile> inProgressFiles = acqFileRepository
                .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS, fileInfo, new PageRequest(0, 1));
        Assert.assertTrue(inProgressFiles.getTotalElements() == 4);

        processingService.manageRegisteredFiles(processingChain);

        // Check registered files
        inProgressFiles = acqFileRepository.findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS,
                                                                               processingChain.getFileInfos().get(0),
                                                                               new PageRequest(0, 1));
        Assert.assertTrue(inProgressFiles.getTotalElements() == 0);

        Page<AcquisitionFile> validFiles = acqFileRepository
                .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.VALID, fileInfo, new PageRequest(0, 1));
        Assert.assertTrue(validFiles.getTotalElements() == 0);

        Page<AcquisitionFile> acquiredFiles = acqFileRepository
                .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.ACQUIRED, fileInfo, new PageRequest(0, 1));
        Assert.assertTrue(acquiredFiles.getTotalElements() == 4);

        // Find product to schedule
        long scheduled = productService.countByProcessingChainAndSipStateIn(processingChain,
                                                                            Arrays.asList(ProductSIPState.SCHEDULED));
        Assert.assertTrue(scheduled == 4);

        //        // Test job algo synchronously
        //        for (Product product : products) {
        //
        //            SIPGenerationJob genJob = new SIPGenerationJob();
        //            beanFactory.autowireBean(genJob);
        //
        //            Map<String, JobParameter> parameters = new HashMap<>();
        //            parameters.put(SIPGenerationJob.CHAIN_PARAMETER_ID,
        //                           new JobParameter(SIPGenerationJob.CHAIN_PARAMETER_ID, processingChain.getId()));
        //            parameters.put(SIPGenerationJob.PRODUCT_ID, new JobParameter(SIPGenerationJob.PRODUCT_ID, product.getId()));
        //
        //            genJob.setParameters(parameters);
        //            genJob.run();
        //        }

        Assert.assertTrue(fileService.countByChain(processingChain) == 4);
        Assert.assertTrue(fileService.countByChainAndStateIn(processingChain,
                                                             Arrays.asList(AcquisitionFileState.ACQUIRED)) == 4);
        Assert.assertTrue(fileService.countByChainAndStateIn(processingChain,
                                                             Arrays.asList(AcquisitionFileState.ERROR)) == 0);

        Page<AcquisitionProcessingChainMonitor> monitor = processingService
                .buildAcquisitionProcessingChainSummaries(null, null, null, new PageRequest(0, 10));
        Assert.assertTrue(!monitor.getContent().isEmpty());
        Assert.assertTrue(monitor.getContent().get(0).getNbFileErrors() == 0);
        Assert.assertTrue(monitor.getContent().get(0).getNbFiles() == 4);
        Assert.assertTrue(monitor.getContent().get(0).getNbFilesInProgress() == 0);
        Assert.assertTrue(monitor.getContent().get(0).getNbProducts() == 4);
        Assert.assertTrue(monitor.getContent().get(0).getNbProductErrors() == 0);
        // Assert.assertTrue(monitor.getContent().get(0).getNbProductsInProgress() == 0);
    }
    //
    //    @Test
    //    public void testScan() throws ModuleException {
    //        runtimeTenantResolver.forceTenant(getDefaultTenant());
    //
    //        AcquisitionProcessingChain processingChain = createProcessingChain(Paths
    //                .get("/home/msordi/git/rs-e2e/data/cdpp/dataobjects/DA_TC_ARC_ISO_DENSITE/results/data2"));
    //
    //        long startTime = System.currentTimeMillis();
    //        processingService.scanAndRegisterFiles(processingChain);
    //
    //        LOGGER.info("Scan action took {} milliseconds", System.currentTimeMillis() - startTime);
    //
    //        processingService.manageRegisteredFiles(processingChain);
    //
    //    }
}
