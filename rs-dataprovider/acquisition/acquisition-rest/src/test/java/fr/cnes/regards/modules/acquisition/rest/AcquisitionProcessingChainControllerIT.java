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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultDiskScanning;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;

/**
 * Test acquisition chain workflow
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acquisition_it" })
public class AcquisitionProcessingChainControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    private AcquisitionProcessingChain getAChain() {

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
                .getPluginConfiguration(Lists.newArrayList(), DefaultDiskScanning.class, Lists.newArrayList());
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

        return processingChain;
    }

    @Test
    public void createAndUpdateChain() throws ModuleException {

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        AcquisitionProcessingChain chain = getAChain();

        // Create the chain
        ResultActions result = performDefaultPost(AcquisitionProcessingChainController.TYPE_PATH, chain, customizer,
                                                  "Chain should be created!");

        // Update chain
        String resultAsString = payload(result);
        Integer chainId = JsonPath.read(resultAsString, "$.content.id");

        // - load it
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        AcquisitionProcessingChain loadedChain = processingService.getChain(chainId.longValue());
        Assert.assertNotNull("Chain must exist", loadedChain);

        // Update scan plugin
        PluginConfiguration scanPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), DefaultDiskScanning.class, Lists.newArrayList());
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin update");
        loadedChain.getFileInfos().get(0).setScanPlugin(scanPlugin);

        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultPut(AcquisitionProcessingChainController.TYPE_PATH
                + AcquisitionProcessingChainController.CHAIN_PATH, loadedChain, customizer, "Chain should be updated",
                          loadedChain.getId());
    }
}
