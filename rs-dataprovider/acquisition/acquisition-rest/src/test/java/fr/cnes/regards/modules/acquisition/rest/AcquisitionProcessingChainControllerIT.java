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
package fr.cnes.regards.modules.acquisition.rest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;

import fr.cnes.regards.framework.microservice.rest.MicroserviceConfigurationController;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;

/**
 * Test acquisition chain workflow. This test cannot be done in a transaction due to transient entity!
 *
 * @author Marc Sordi
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acquisition_it" })
public class AcquisitionProcessingChainControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    public static AcquisitionProcessingChain getNewChain(String labelPrefix) {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("Processing chain 1");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        fileInfo.setDataType(DataType.RAWDATA);

        List<PluginParameter> param = PluginParametersFactory.build()
                .addParameter(GlobDiskScanning.FIELD_DIRS, new ArrayList<>()).getParameters();
        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(param, GlobDiskScanning.class,
                                                                            Lists.newArrayList());
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel(labelPrefix + " : " + "Scan plugin");
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), DefaultFileValidation.class, Lists.newArrayList());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel(labelPrefix + " : " + "Validation plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), DefaultProductPlugin.class, Lists.newArrayList());
        productPlugin.setIsActive(true);
        productPlugin.setLabel(labelPrefix + " : " + "Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), DefaultSIPGeneration.class, Lists.newArrayList());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel(labelPrefix + " : " + "SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        return processingChain;
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_020")
    @Requirement("REGARDS_DSL_ING_PRO_030")
    @Purpose("Create and update a manual acquisition chain")
    public void createAndUpdateChain() throws ModuleException {

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        AcquisitionProcessingChain chain = getNewChain("First");

        // Create the chain
        ResultActions result = performDefaultPost(AcquisitionProcessingChainController.TYPE_PATH, chain, customizer,
                                                  "Chain should be created!");

        // Update chain
        String resultAsString = payload(result);
        Integer chainId = JsonPath.read(resultAsString, "$.content.id");

        // Load it
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        AcquisitionProcessingChain loadedChain = processingService.getChain(chainId.longValue());
        Assert.assertNotNull("Chain must exist", loadedChain);

        // Update scan plugin
        PluginConfiguration scanPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), GlobDiskScanning.class, Lists.newArrayList());
        scanPlugin.setIsActive(true);
        String label = "Scan plugin update";
        scanPlugin.setLabel(label);
        loadedChain.getFileInfos().get(0).setScanPlugin(scanPlugin);

        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultPut(AcquisitionProcessingChainController.TYPE_PATH
                + AcquisitionProcessingChainController.CHAIN_PATH, loadedChain, customizer, "Chain should be updated",
                          loadedChain.getId());

        // Load new scan plugin configuration
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        loadedChain = processingService.getChain(chainId.longValue());
        Assert.assertEquals(label, loadedChain.getFileInfos().get(0).getScanPlugin().getLabel());

        // Delete active chain
        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isForbidden());
        performDefaultDelete(AcquisitionProcessingChainController.TYPE_PATH
                + AcquisitionProcessingChainController.CHAIN_PATH, customizer, "Chain should be removed",
                             chainId.longValue());

        // Change to inactive
        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        loadedChain.setActive(Boolean.FALSE);
        performDefaultPut(AcquisitionProcessingChainController.TYPE_PATH
                + AcquisitionProcessingChainController.CHAIN_PATH, loadedChain, customizer, "Chain should be updated",
                          loadedChain.getId());

        // Delete inactive chain
        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNoContent());
        performDefaultDelete(AcquisitionProcessingChainController.TYPE_PATH
                + AcquisitionProcessingChainController.CHAIN_PATH, customizer, "Chain should be removed",
                             chainId.longValue());
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_020")
    @Requirement("REGARDS_DSL_ING_PRO_030")
    @Purpose("Create an automatic acquisition chain without a periodicity")
    public void createAutomaticChainWithoutPeriodicity() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isUnprocessableEntity());

        AcquisitionProcessingChain chain = getNewChain("AutoError");
        chain.setMode(AcquisitionProcessingChainMode.AUTO);

        // Create the chain
        performDefaultPost(AcquisitionProcessingChainController.TYPE_PATH, chain, customizer,
                           "Chain should be created!");
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_020")
    @Requirement("REGARDS_DSL_ING_PRO_030")
    @Purpose("Create an automatic acquisition chain with a periodicity")
    public void createAutomaticChain() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        AcquisitionProcessingChain chain = getNewChain("Auto10s");
        chain.setMode(AcquisitionProcessingChainMode.AUTO);
        chain.setPeriodicity(10L);

        // Create the chain
        performDefaultPost(AcquisitionProcessingChainController.TYPE_PATH, chain, customizer,
                           "Chain should be created!");
    }

    @Ignore("Development test")
    @Test
    public void createFromContract01() {
        String processingChain = readJsonContract("createChain01.json");

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        // Create the chain
        performDefaultPost(AcquisitionProcessingChainController.TYPE_PATH, processingChain, customizer,
                           "Chain should be created!");
    }

    @Test
    public void exportConfiguration() {
        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());

        performDefaultGet(MicroserviceConfigurationController.TYPE_MAPPING, requestBuilderCustomizer,
                          "Should export configuration");
    }

    @Test
    public void importConfiguration() {
        Path filePath = Paths.get("src", "test", "resources", "acquisition-configuration2.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        performDefaultFileUpload(MicroserviceConfigurationController.TYPE_MAPPING, filePath, requestBuilderCustomizer,
                                 "Should be able to import configuration");
    }

    @Test
    public void importExport() {
        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());

        performDefaultGet(MicroserviceConfigurationController.TYPE_MAPPING
                + MicroserviceConfigurationController.ENABLED_MAPPING, requestBuilderCustomizer, "Shoulb be enabled");
    }
}
