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
package fr.cnes.regards.microservices.dataprovider;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.microservice.rest.ModuleManagerController;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileInfoRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.chain.ScanDirectoryInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.StorageMetadataProvider;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@ActiveProfiles("disableDataProviderTask")
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=dataprovider_module_manager_it",
        "regards.jobs.completion.update.rate.ms=3600000" })
public class DataproviderModuleManagerControllerIT extends AbstractRegardsIT {

    @Autowired
    private Validator validator;

    @Autowired
    private IAcquisitionProcessingService acquisitionProcessingService;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IAcquisitionFileInfoRepository fileInfoRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IAcquisitionProcessingChainRepository acqChainRepository;

    @Autowired
    private IAcquisitionFileRepository acquisitionFileRepository;

    @Before
    public void beforeEachTest() throws ModuleException {
        afterEachTest();
        long startCreation = System.currentTimeMillis();
        getLogger().info("STARTING TO CREATE ACQUISITION CHAINS");
        // lets create multiple AcquisitionChains
        for (int i = 0; i < 500; i++) {
            createFullAcquisitionChain(i);
            getLogger().info("ACQUISITION CHAINS N°{} CREATED", i);
        }
        getLogger().info("ENDED ACQUISITION CHAINS CREATION IN {}ms", System.currentTimeMillis() - startCreation);
    }

    @After
    public void afterEachTest() throws ModuleException {
        //clean acquisitionChains
        getLogger().info("START CLEAN UP");
        acquisitionFileRepository.deleteAll();
        productRepository.deleteAll();
        fileInfoRepository.deleteAll();
        jobInfoRepository.deleteAll();
        acqChainRepository.deleteAll();
        getLogger().info("END CLEAN UP");
    }

    @Test
    @Purpose("This tests should help to diagnostic issues with too slow exports")
    public void testExport() throws ModuleException {
        // now lets export them
        RequestBuilderCustomizer requestCustomizerBuilder = customizer().expectStatus(HttpStatus.OK);
        long startExport = System.currentTimeMillis();
        performDefaultGet(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                          requestCustomizerBuilder,
                          "There has been some error while dataprovider configuration export");
        getLogger().info("EXPORT DONE in  {} ms", System.currentTimeMillis() - startExport);
    }

    private void createFullAcquisitionChain(int i) throws ModuleException {
        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("Processing chain " + i);
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
        fileInfo.setScanDirInfo(Sets.newHashSet(new ScanDirectoryInfo(Paths.get("path" + i + "/fake"), null)));

        PluginConfiguration scanPlugin = PluginConfiguration.build(GlobDiskScanning.class, null,
                                                                   new HashSet<IPluginParam>());
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin " + i);
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginConfiguration.build(DefaultFileValidation.class, null,
                                                                         new HashSet<IPluginParam>());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin " + i);
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginConfiguration.build(DefaultProductPlugin.class, null,
                                                                      new HashSet<IPluginParam>());
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin " + i);
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginConfiguration.build(DefaultSIPGeneration.class, null,
                                                                     new HashSet<IPluginParam>());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin " + i);
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required
        List<StorageMetadataProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataProvider.build("AWS", "/path/to/file", new HashSet<>()));
        storages.add(StorageMetadataProvider.build("HELLO", "/other/path/to/file", new HashSet<>()));
        processingChain.setStorages(storages);

        // Validate
        Errors errors = new MapBindingResult(new HashMap<>(), "apc");
        validator.validate(processingChain, errors);
        if (errors.hasErrors()) {
            errors.getAllErrors().forEach(error -> getLogger().error(error.getDefaultMessage()));
            Assert.fail("Acquisition processing chain should be valid");
        }
        acquisitionProcessingService.createChain(processingChain);
    }

}
