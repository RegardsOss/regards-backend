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
package fr.cnes.regards.modules.acquisition.rest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
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
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
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

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_020")
    @Requirement("REGARDS_DSL_ING_PRO_030")
    @Purpose("Create a manual acquisition chain")
    public void createChain() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        AcquisitionProcessingChain chain = AcquisitionTestUtils.getNewChain("post");

        customizer.addDocumentationSnippet(PayloadDocumentation
                .relaxedRequestFields(Attributes.attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE)
                        .value("Acquisition processing chain")), documentAcquisitionProcessingChain()));

        // Create the chain
        performDefaultPost(AcquisitionProcessingChainController.TYPE_PATH, chain, customizer,
                           "Chain should be created!");
    }

    private List<FieldDescriptor> documentAcquisitionProcessingChain() {

        ConstrainedFields constrainedFields = new ConstrainedFields(AcquisitionProcessingChain.class);
        List<FieldDescriptor> fields = new ArrayList<>();

        fields.add(constrainedFields.withPath("label", "Label"));
        fields.add(constrainedFields.withPath("active", "Activation status"));

        StringJoiner joiner = new StringJoiner(", ");
        for (AcquisitionProcessingChainMode mode : AcquisitionProcessingChainMode.values()) {
            joiner.add(mode.name());
        }
        fields.add(constrainedFields.withPath("mode", "mode", "Mode", "Allowed values : " + joiner.toString()));

        fields.add(constrainedFields.withPath("session", "Ingest session name for SIP submission").optional()
                .type("String"));
        fields.add(constrainedFields.withPath("ingestChain", "Ingest chain name for SIP submission"));
        fields.add(constrainedFields.withPath("locked", "locked", "Internal chain processing lock", "NA").optional()
                .type("Boolean"));
        fields.add(constrainedFields.withPath("periodicity", "Automatic chain activation periodicity in second")
                .optional().type("Long"));

        fields.add(constrainedFields.withPath("fileInfos[]", "Arrays of file information / TODO"));
        fields.addAll(documentFileInfo("fileInfos[]"));

        fields.add(constrainedFields.withPath("validationPluginConf", "Validation plugin configuration / TODO"));
        fields.add(constrainedFields.withPath("productPluginConf", "Product plugin configuration / TODO"));
        fields.add(constrainedFields.withPath("generateSipPluginConf", "Generate SIP plugin configuration / TODO"));
        fields.add(constrainedFields
                .withPath("postProcessSipPluginConf", "Optional SIP post processing plugin configuration / TODO")
                .optional().type("Object"));
        return fields;
    }

    private List<FieldDescriptor> documentFileInfo(String basePath) {
        ConstrainedFields constrainedFields = new ConstrainedFields(AcquisitionFileInfo.class);
        List<FieldDescriptor> fields = new ArrayList<>();

        String prefix = basePath == null ? "" : basePath + ".";
        fields.add(constrainedFields.withPath(prefix + "mandatory", "mandatory",
                                              "True if the product must contain this file"));
        fields.add(constrainedFields.withPath(prefix + "scanPlugin", "scanPlugin", "Scan plugin configuration / TODO"));
        fields.add(constrainedFields
                .withPath(prefix + "lastModificationDate", "lastModificationDate",
                          "Most recent last modification ISO 8601 date of all scanned files")
                .optional().type("String"));
        fields.add(constrainedFields.withPath(prefix + "mimeType", "mimeType", "File MIME type"));

        StringJoiner joiner = new StringJoiner(", ");
        for (DataType mode : DataType.values()) {
            joiner.add(mode.name());
        }
        fields.add(constrainedFields.withPath(prefix + "dataType", "dataType", "REGARDS data type",
                                              "Allowed values : " + joiner.toString()));

        fields.add(constrainedFields.withPath(prefix + "comment", "comment", "REGARDS data type").optional()
                .type("String"));
        return fields;
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_020")
    @Requirement("REGARDS_DSL_ING_PRO_030")
    @Purpose("Get all acquisition chain")
    public void getAllChains() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        AcquisitionProcessingChain chain = AcquisitionTestUtils.getNewChain("one");
        performDefaultPost(AcquisitionProcessingChainController.TYPE_PATH, chain, customizer,
                           "Chain should be created!");

        chain = AcquisitionTestUtils.getNewChain("two");
        performDefaultPost(AcquisitionProcessingChainController.TYPE_PATH, chain, customizer,
                           "Chain should be created!");

        // Retrieve chains
        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());

        performDefaultGet(AcquisitionProcessingChainController.TYPE_PATH, customizer, "Chains should be retrieved");
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_020")
    @Requirement("REGARDS_DSL_ING_PRO_030")
    @Purpose("Get an acquisition chain")
    public void getOneChain() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        AcquisitionProcessingChain chain = AcquisitionTestUtils.getNewChain("first");
        ResultActions result = performDefaultPost(AcquisitionProcessingChainController.TYPE_PATH, chain, customizer,
                                                  "Chain should be created!");

        // Update chain
        String resultAsString = payload(result);
        Integer chainId = JsonPath.read(resultAsString, "$.content.id");

        // Retrieve chains
        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        // Document path parameter
        customizer.addDocumentationSnippet(RequestDocumentation.pathParameters(RequestDocumentation
                .parameterWithName(AcquisitionProcessingChainController.CHAIN_PATH_PARAM)
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("Acquisition chain identifier")));
        performDefaultGet(AcquisitionProcessingChainController.TYPE_PATH
                + AcquisitionProcessingChainController.CHAIN_PATH, customizer, "Chain should be retrieved", chainId);
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_020")
    @Requirement("REGARDS_DSL_ING_PRO_030")
    @Purpose("Create and update a manual acquisition chain")
    public void updateChain() throws ModuleException {

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        AcquisitionProcessingChain chain = AcquisitionTestUtils.getNewChain("update");

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
        List<PluginParameter> params = PluginParametersFactory.build()
                .addParameter(GlobDiskScanning.FIELD_DIRS, new ArrayList<>()).getParameters();
        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(params, GlobDiskScanning.class,
                                                                            Lists.newArrayList());
        scanPlugin.setIsActive(true);
        String label = "Scan plugin update";
        scanPlugin.setLabel(label);
        loadedChain.getFileInfos().get(0).setScanPlugin(scanPlugin);

        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        // Document path parameter
        customizer.addDocumentationSnippet(RequestDocumentation.pathParameters(RequestDocumentation
                .parameterWithName(AcquisitionProcessingChainController.CHAIN_PATH_PARAM)
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(Long.class.getName()))
                .description("Acquisition chain identifier")));

        performDefaultPut(AcquisitionProcessingChainController.TYPE_PATH
                + AcquisitionProcessingChainController.CHAIN_PATH, loadedChain, customizer, "Chain should be updated",
                          loadedChain.getId());

        // Load new scan plugin configuration
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        loadedChain = processingService.getChain(chainId.longValue());
        Assert.assertEquals(label, loadedChain.getFileInfos().get(0).getScanPlugin().getLabel());
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_020")
    @Requirement("REGARDS_DSL_ING_PRO_030")
    @Purpose("Delete a inactive manual acquisition chain")
    public void deleteChain() throws ModuleException {

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        AcquisitionProcessingChain chain = AcquisitionTestUtils.getNewChain("delete");

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
        List<PluginParameter> params = PluginParametersFactory.build()
                .addParameter(GlobDiskScanning.FIELD_DIRS, new ArrayList<>()).getParameters();
        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(params, GlobDiskScanning.class,
                                                                            Lists.newArrayList());
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

        // Document path parameter
        customizer.addDocumentationSnippet(RequestDocumentation.pathParameters(RequestDocumentation
                .parameterWithName(AcquisitionProcessingChainController.CHAIN_PATH_PARAM)
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(Long.class.getName()))
                .description("Acquisition chain identifier to update").attributes(Attributes
                        .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Chain must be disabled."))));

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

        AcquisitionProcessingChain chain = AcquisitionTestUtils.getNewChain("AutoError");
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

        AcquisitionProcessingChain chain = AcquisitionTestUtils.getNewChain("Auto10s");
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
