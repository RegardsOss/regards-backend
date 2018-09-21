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
package fr.cnes.regards.modules.ingest.rest;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.Sets;
import com.jayway.jsonpath.JsonPath;

import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.microservice.rest.ModuleManagerController;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipGeneration;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipValidation;
import fr.cnes.regards.modules.ingest.service.plugin.FakeAIPGenerationTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.FakeValidationTestPlugin;

/**
 *
 * Test processing chain API
 *
 * @author Marc Sordi
 * @author Christophe Mertz
 *
 */
@RegardsTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest_it" })
public class IngestProcessingChainControllerIT extends AbstractRegardsTransactionalIT {

    private final static String INGEST_PROCESSING_DESCRIPTION = "The ingestion processing chain name";

    /**
     * Generated token for tests
     */
    private static String token = "";

    @Before
    public void init() {
        manageSecurity(getDefaultTenant(),
                       IngestProcessingChainController.TYPE_MAPPING + IngestProcessingChainController.NAME_PATH,
                       RequestMethod.POST, getDefaultUserEmail(), getDefaultRole());
        manageSecurity(getDefaultTenant(),
                       IngestProcessingChainController.TYPE_MAPPING + IngestProcessingChainController.NAME_PATH,
                       RequestMethod.PUT, getDefaultUserEmail(), getDefaultRole());
        manageSecurity(getDefaultTenant(),
                       IngestProcessingChainController.TYPE_MAPPING + IngestProcessingChainController.NAME_PATH,
                       RequestMethod.DELETE, getDefaultUserEmail(), getDefaultRole());

        token = generateToken(getDefaultUserEmail(), getDefaultRole());
    }

    @Test
    public void exportProcessingChain() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addDocumentationSnippet(RequestDocumentation.pathParameters(RequestDocumentation
                .parameterWithName(IngestProcessingChainController.REQUEST_PARAM_NAME)
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("Ingestion processing name")));

        ResultActions resultActions = performDefaultGet(IngestProcessingChainController.TYPE_MAPPING
                + IngestProcessingChainController.EXPORT_PATH, requestBuilderCustomizer,
                                                        "Default processing chain should be exported",
                                                        IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL);
        assertMediaType(resultActions, MediaType.APPLICATION_JSON_UTF8);
        String chain = payload(resultActions);
        Assert.assertNotNull(chain);
    }

    @Test
    public void importProcessingChain() {
        Path filePath = Paths.get("src", "test", "resources", "TestProcessingChain.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        documentFileRequestParameters(requestBuilderCustomizer);

        performDefaultFileUpload(IngestProcessingChainController.TYPE_MAPPING
                + IngestProcessingChainController.IMPORT_PATH, filePath, requestBuilderCustomizer,
                                 "Should be able to import valid test processing chain");
    }

    private void documentFileRequestParameters(RequestBuilderCustomizer requestBuilderCustomizer) {
        ParameterDescriptor paramFile = RequestDocumentation
                .parameterWithName(IngestProcessingChainController.IMPORT_PATH).optional()
                .description("A file containing an ingestion processing chain in GeoJson format")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String"));
        // Add request parameters documentation
        requestBuilderCustomizer.addDocumentationSnippet(RequestDocumentation.requestParameters(paramFile));
    }

    @Test
    public void createIngestProcessingChain() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.CONTENT_TYPE,
                                                        GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE);
        performDefaultPost(IngestProcessingChainController.TYPE_MAPPING, this.create(), requestBuilderCustomizer,
                           "Ingest processing creation error");
    }

    @Test
    public void updateIngestProcessingChain() {
        // create an IngestProcessingChain
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.CONTENT_TYPE,
                                                        GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE);
        IngestProcessingChain ingestProcessingChain = this.create();
        ResultActions resultActions = performDefaultPost(IngestProcessingChainController.TYPE_MAPPING,
                                                         ingestProcessingChain, requestBuilderCustomizer,
                                                         "Ingest processing creation error");

        // update the existing IngestProcessingChain
        ingestProcessingChain.setDescription("the updated description");
        String resultAsString = payload(resultActions);

        Integer valPluginId = JsonPath.read(resultAsString, "$.validationPlugin.id");
        ingestProcessingChain.getValidationPlugin().setId(new Long(valPluginId));
        Integer genPluginId = JsonPath.read(resultAsString, "$.generationPlugin.id");
        ingestProcessingChain.getGenerationPlugin().setId(new Long(genPluginId));

        RequestBuilderCustomizer putRequestBuilderCustomizer = getNewRequestBuilderCustomizer();
        putRequestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        putRequestBuilderCustomizer.customizeHeaders().add(HttpHeaders.CONTENT_TYPE,
                                                           GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE);
        putRequestBuilderCustomizer.addDocumentationSnippet(RequestDocumentation.pathParameters(RequestDocumentation
                .parameterWithName(IngestProcessingChainController.REQUEST_PARAM_NAME)
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description(INGEST_PROCESSING_DESCRIPTION)));

        performPut(IngestProcessingChainController.TYPE_MAPPING + IngestProcessingChainController.NAME_PATH, token,
                   ingestProcessingChain, putRequestBuilderCustomizer, "Ingest processing update error",
                   ingestProcessingChain.getName());
    }

    @Test
    public void deleteIngestProcessingChain() {
        // create an IngestProcessingChain
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.CONTENT_TYPE,
                                                        GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE);
        IngestProcessingChain ingestProcessingChain = this.create();
        performDefaultPost(IngestProcessingChainController.TYPE_MAPPING, ingestProcessingChain,
                           requestBuilderCustomizer, "Ingest processing creation error");

        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.CONTENT_TYPE,
                                                        GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE);
        requestBuilderCustomizer.addDocumentationSnippet(RequestDocumentation.pathParameters(RequestDocumentation
                .parameterWithName(IngestProcessingChainController.REQUEST_PARAM_NAME)
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description(INGEST_PROCESSING_DESCRIPTION)));

        performDelete(IngestProcessingChainController.TYPE_MAPPING + IngestProcessingChainController.NAME_PATH, token,
                      requestBuilderCustomizer, "Ingest processing delete error", ingestProcessingChain.getName());
    }

    @Test
    public void exportConfiguration() {
        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());

        performDefaultGet(ModuleManagerController.TYPE_MAPPING, requestBuilderCustomizer,
                          "Should export configuration");
    }

    @Test
    public void importConfiguration() {
        Path filePath = Paths.get("src", "test", "resources", "configuration.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isPartialContent()); // Default ingest
                                                                                                    // chain already
                                                                                                    // exists

        performDefaultFileUpload(ModuleManagerController.TYPE_MAPPING, filePath, requestBuilderCustomizer,
                                 "Should be able to import configuration");
    }

    @Test
    public void getOneIngestProcessingChain() {
        Path filePath = Paths.get("src", "test", "resources", "TestProcessingChain.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        performDefaultFileUpload(IngestProcessingChainController.TYPE_MAPPING
                + IngestProcessingChainController.IMPORT_PATH, filePath, requestBuilderCustomizer,
                                 "Should be able to import valid test processing chain");

        RequestBuilderCustomizer getCustomizer = getNewRequestBuilderCustomizer();
        getCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        getCustomizer.addDocumentationSnippet(RequestDocumentation.pathParameters(RequestDocumentation
                .parameterWithName(IngestProcessingChainController.REQUEST_PARAM_NAME)
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description(INGEST_PROCESSING_DESCRIPTION)));

        performDefaultGet(IngestProcessingChainController.TYPE_MAPPING + IngestProcessingChainController.NAME_PATH,
                          getCustomizer, "Should be able to get an ingestion processing chain", "TestProcessingChain");
    }

    private IngestProcessingChain create() {
        PluginConfiguration validationConf = new PluginConfiguration(
                getPluginMetaData("FakeValidationTestPlugin", FakeValidationTestPlugin.class.getCanonicalName(),
                                  ISipValidation.class.getCanonicalName()),
                "FakeValidationTestPlugin");
        PluginConfiguration generationConf = new PluginConfiguration(
                getPluginMetaData("FakeAIPGenerationTestPlugin", FakeAIPGenerationTestPlugin.class.getCanonicalName(),
                                  IAipGeneration.class.getCanonicalName()),
                "FakeAIPGenerationTestPlugin");
        return new IngestProcessingChain("ingestProcessingChain_test", "the ingest processing chain description",
                validationConf, generationConf);
    }

    private PluginMetaData getPluginMetaData(String pluginId, String className, String interfaceName) {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setPluginClassName(className);
        pluginMetaData.setInterfaceNames(Sets.newHashSet(interfaceName));
        pluginMetaData.setPluginId(pluginId);
        pluginMetaData.setAuthor("AUTHOR");
        pluginMetaData.setVersion("1.0.0");

        return pluginMetaData;
    }

}
