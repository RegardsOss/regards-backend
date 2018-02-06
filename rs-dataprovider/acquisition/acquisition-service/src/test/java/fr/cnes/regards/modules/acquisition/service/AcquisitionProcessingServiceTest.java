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

import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;

/**
 * Test {@link AcquisitionProcessingService} for {@link AcquisitionProcessingChain} workflow
 *
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acquisition", "jwt.secret=123456789",
        "regards.workspace=target/workspace" })
@ContextConfiguration(classes = { AcquisitionProcessingServiceTest.AcquisitionConfiguration.class })
@MultitenantTransactional
public class AcquisitionProcessingServiceTest extends AbstractDaoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionProcessingServiceTest.class);

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IAcquisitionProcessingChainRepository processingChainRepository;

    @Autowired
    private Validator validator;

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules" })
    static class AcquisitionConfiguration {
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_020")
    @Requirement("REGARDS_DSL_ING_PRO_030")
    @Purpose("Create an acquisition chain")
    public void createChain() throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("Processing chain 1");
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

        PluginConfiguration scanPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), GlobDiskScanning.class, Lists.newArrayList());
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

        // Validate
        Errors errors = new MapBindingResult(new HashMap<>(), "apc");
        validator.validate(processingChain, errors);
        if (errors.hasErrors()) {
            errors.getAllErrors().forEach(error -> LOGGER.error(error.getDefaultMessage()));
            Assert.fail("Acquisition processing chain should be valid");
        }

        // Save processing chain
        processingService.createChain(processingChain);

        // Test loading chain by mode
        List<AcquisitionProcessingChain> automaticChains = processingChainRepository.findAllBootableAutomaticChains();
        Assert.assertTrue(automaticChains.isEmpty());
        List<AcquisitionProcessingChain> manualChains = processingChainRepository
                .findByModeAndActiveTrueAndRunningFalse(AcquisitionProcessingChainMode.MANUAL);
        Assert.assertTrue(!manualChains.isEmpty() && (manualChains.size() == 1));
    }
}