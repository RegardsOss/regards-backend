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
package fr.cnes.regards.modules.catalog.services.rest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.catalog.services.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.catalog.services.domain.ServicePluginParameters;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;
import fr.cnes.regards.modules.catalog.services.plugins.SampleServicePlugin;
import fr.cnes.regards.modules.catalog.services.service.link.ILinkPluginsDatasetsService;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@TestPropertySource(locations = "classpath:test.properties")
@ContextConfiguration(classes = { CatalogServicesITConfiguration.class })
@MultitenantTransactional
public class CatalogServicesControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogServicesControllerIT.class);

    private static final String DATA_SET_NAME = "test";

    private static final String DATA_SET_NAME_2 = "test_2";

    private static final String PLUGIN_CONF_LABEL_1 = "pluginLabel1";

    private static final String PLUGIN_CONF_LABEL_2 = "pluginLabel2";

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ILinkPluginsDatasetsService linkService;

    private PluginConfiguration conf;

    private PluginConfiguration samplePlgConf;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Before
    public void init() throws ModuleException {
        List<PluginParameter> parameters = PluginParametersFactory.build().addDynamicParameter("para", "never used")
                .getParameters();
        final PluginMetaData metaData = new PluginMetaData();
        metaData.setPluginId("tata");
        metaData.setAuthor("toto");
        metaData.setDescription("titi");
        metaData.setVersion("1.0.0");
        metaData.getInterfaceNames().add(IService.class.getName());
        metaData.setPluginClassName(TestService.class.getName());
        // Create two plugin services linked to the same dataset
        // 1. first one
        conf = new PluginConfiguration(metaData, "testConf", parameters);
        pluginService.addPluginPackage(TestService.class.getPackage().getName());
        conf = pluginService.savePluginConfiguration(conf);
        // 2. second one
        parameters = PluginParametersFactory.build().addDynamicParameter(SampleServicePlugin.RESPONSE_TYPE_PARAMETER,
                                                                         SampleServicePlugin.RESPONSE_TYPE_JSON)
                .getParameters();
        samplePlgConf = new PluginConfiguration(
                PluginUtils.createPluginMetaData(SampleServicePlugin.class,
                                                 Arrays.asList(SampleServicePlugin.class.getPackage().getName())),
                PLUGIN_CONF_LABEL_1, parameters);
        pluginService.savePluginConfiguration(samplePlgConf);

        // Create a plugin service linked to an other dataset
        final PluginMetaData metaData2 = new PluginMetaData();
        metaData2.setPluginId("otherPluginMetaId");
        metaData2.setAuthor("otherPluginMetaAuthor");
        metaData2.setDescription("otherPlugin");
        metaData2.setVersion("1.0");
        metaData2.getInterfaceNames().add(IService.class.getName());
        metaData2.setPluginClassName(TestService.class.getName());

        parameters = PluginParametersFactory.build().addDynamicParameter(SampleServicePlugin.RESPONSE_TYPE_PARAMETER,
                                                                         SampleServicePlugin.RESPONSE_TYPE_JSON)
                .getParameters();
        PluginConfiguration samplePlgConf2 = new PluginConfiguration(
                PluginUtils.createPluginMetaData(SampleServicePlugin.class,
                                                 Arrays.asList(SampleServicePlugin.class.getPackage().getName())),
                PLUGIN_CONF_LABEL_2, parameters);
        pluginService.savePluginConfiguration(samplePlgConf2);

        linkService.updateLink(DATA_SET_NAME,
                               new LinkPluginsDatasets(DATA_SET_NAME, Sets.newHashSet(conf, samplePlgConf)));

        linkService
                .updateLink(DATA_SET_NAME_2,
                            new LinkPluginsDatasets(DATA_SET_NAME_2, Sets.newHashSet(samplePlgConf2, samplePlgConf)));
    }

    @Test
    public void testRetrieveServicesQuery() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$").isArray());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$").isNotEmpty());
        requestBuilderCustomizer.customizeRequestParam()
                .param(CatalogServicesController.DATASET_IDS_QUERY_PARAM, DATA_SET_NAME)
                .param("service_scope", ServiceScope.MANY.toString());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        performDefaultGet(CatalogServicesController.PATH_SERVICES, requestBuilderCustomizer,
                          "there should not be any error");
    }

    @Test
    public void testRetrieveServicesOne() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$").isArray());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$").isNotEmpty());
        requestBuilderCustomizer.customizeRequestParam()
                .param(CatalogServicesController.DATASET_IDS_QUERY_PARAM, DATA_SET_NAME)
                .param("service_scope", ServiceScope.ONE.toString());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        performDefaultGet(CatalogServicesController.PATH_SERVICES, requestBuilderCustomizer,
                          "there should not be any error");
    }

    @Test
    public void retrieveServices_shouldHaveMeta() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        //
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + "[0].content.pluginId",
        //                                                                               Matchers.is("tata")));
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
        //                .jsonPath(JSON_PATH_ROOT + "[0].content.applicationModes", Matchers.containsInAnyOrder("MANY", "ONE")));
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
        //                .jsonPath(JSON_PATH_ROOT + "[0].content.entityTypes", Matchers.contains("DATA")));
        //
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + "[1].content.pluginId",
        //                                                                               Matchers.is("aSampleServicePlugin")));
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
        //                .jsonPath(JSON_PATH_ROOT + "[1].content.applicationModes", Matchers.containsInAnyOrder("MANY", "ONE")));
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
        //                .jsonPath(JSON_PATH_ROOT + "[1].content.entityTypes", Matchers.contains(EntityType.DATA.toString())));
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + "[1].content.label",
        //                                                                               Matchers.is(PLUGIN_CONF_LABEL_1)));
        //
        //        // Retrieve plugin services for first dataset. Should be two services as linked in init method
        //        requestBuilderCustomizer.customizeRequestParam()
        //                .param(CatalogServicesController.DATASET_IDS_QUERY_PARAM, DATA_SET_NAME)
        //                .param("service_scope", ServiceScope.ONE.toString());
        //        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        //        performDefaultGet(CatalogServicesController.PATH_SERVICES, requestBuilderCustomizer,
        //                          "There should be plugin configurations augmented with meta data");
        //
        //        // Retrieve plugin services for second dataset. Should be two services as linked in init method
        //        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        //
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + "[0].content.label",
        //                                                                               Matchers.is(PLUGIN_CONF_LABEL_2)));
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
        //                .jsonPath(JSON_PATH_ROOT + "[0].content.applicationModes", Matchers.containsInAnyOrder("MANY", "ONE")));
        //        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
        //                .jsonPath(JSON_PATH_ROOT + "[0].content.entityTypes", Matchers.contains(EntityType.DATA.toString())));
        //        requestBuilderCustomizer.customizeRequestParam()
        //                .param(CatalogServicesController.DATASET_IDS_QUERY_PARAM, DATA_SET_NAME_2)
        //                .param("service_scope", ServiceScope.ONE.toString());
        //        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        //        performDefaultGet(CatalogServicesController.PATH_SERVICES, requestBuilderCustomizer,
        //                          "There should be plugin configurations augmented with meta data");

        // Retrieve plugin services for both datasets. Should be only the common one between the two datasets.
        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());

        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + "[0].content.label",
                                                                               Matchers.is(PLUGIN_CONF_LABEL_1)));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                .jsonPath(JSON_PATH_ROOT + "[0].content.applicationModes", Matchers.containsInAnyOrder("MANY", "ONE")));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                .jsonPath(JSON_PATH_ROOT + "[0].content.entityTypes", Matchers.contains(EntityType.DATA.toString())));
        requestBuilderCustomizer.customizeRequestParam()
                .param(CatalogServicesController.DATASET_IDS_QUERY_PARAM, DATA_SET_NAME_2 + "," + DATA_SET_NAME)
                .param("service_scope", ServiceScope.ONE.toString());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        performDefaultGet(CatalogServicesController.PATH_SERVICES, requestBuilderCustomizer,
                          "There should be plugin configurations augmented with meta data");

    }

    @Test
    @Requirement("REGARDS_DSL_DAM_ARC_030")
    @Requirement("REGARDS_DSL_DAM_ARC_010")
    @Purpose("System has a joinpoint \"Service\" that allows to apply treatment on a dataset, or one of its subset. Those treatments are applied to informations contained into the catalog. A plugin \"Service\" can have as parameters: parameters defined at configuration by an administrator, parameters dynamicly defined at each request, parameters to select objects from a dataset.")
    public void testApplyService() throws IOException {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put("q", "truc");
        dynamicParameters.put("para", TestService.EXPECTED_VALUE);

        ServicePluginParameters parameters = new ServicePluginParameters("ENTITY_ID", null, null, null,
                dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        ResultActions resultActions = performDefaultPost(CatalogServicesController.PATH_SERVICES
                + CatalogServicesController.PATH_SERVICE_NAME, parameters, requestBuilderCustomizer,
                                                         "there should not be any error", conf.getId());
        validateTestPluginResponse(resultActions, new File("src/test/resources/result.json"));
    }

    private void validateTestPluginResponse(ResultActions resultActions, File expectedFileResult) throws IOException {
        File resultFile = File.createTempFile("result.json", "");

        resultFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(resultFile)) {
            // Wait for availability
            resultActions.andReturn().getAsyncResult();
            InputStream is = new ByteArrayInputStream(resultActions.andReturn().getResponse().getContentAsByteArray());
            ByteStreams.copy(is, fos);
            is.close();
        }
        logFileContent(resultFile);
        logFileContent(expectedFileResult);
        Assert.assertTrue("Request result is not valid", Files.equal(expectedFileResult, resultFile));
    }

    private void logFileContent(File file) {
        try {
            FileInputStream fstream = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            /* read log line by line */
            while ((strLine = br.readLine()) != null) {
                /* parse strLine to obtain what you want */
                System.out.println(strLine);
            }
            fstream.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_310")
    @Purpose("System allows to set a dynamic plugin parameter in the HTPP request")
    public void testApplyServiceSetSpecificParamValue() throws IOException {
        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put("q", "truc");
        dynamicParameters.put("para", "HelloWorld");
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        ServicePluginParameters parameters = new ServicePluginParameters("ENTITY_ID", null, null, null,
                dynamicParameters);
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        ResultActions resultActions = performDefaultPost(CatalogServicesController.PATH_SERVICES
                + CatalogServicesController.PATH_SERVICE_NAME, parameters, requestBuilderCustomizer,
                                                         "there should not be any error", conf.getId());
        validateTestPluginResponse(resultActions, new File("src/test/resources/result_empty.json"));
    }

    @Test
    public void testSampleServiceWithJsonResponse() throws IOException {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put(SampleServicePlugin.RESPONSE_TYPE_PARAMETER, SampleServicePlugin.RESPONSE_TYPE_JSON);

        ServicePluginParameters parameters = new ServicePluginParameters("ENTITY_ID", null, null, null,
                dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        ResultActions resultActions = performDefaultPost(CatalogServicesController.PATH_SERVICES
                + CatalogServicesController.PATH_SERVICE_NAME, parameters, requestBuilderCustomizer,
                                                         "there should not be any error", samplePlgConf.getId());
        validateTestPluginResponse(resultActions, new File("src/test/resources/samplePluginResult.json"));
    }

    @Test
    public void testSampleServiceWithXmlResponse() throws IOException {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put(SampleServicePlugin.RESPONSE_TYPE_PARAMETER, SampleServicePlugin.RESPONSE_TYPE_XML);

        ServicePluginParameters parameters = new ServicePluginParameters("ENTITY_ID", null, null, null,
                dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_XML));
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        ResultActions resultActions = performDefaultPost(CatalogServicesController.PATH_SERVICES
                + CatalogServicesController.PATH_SERVICE_NAME, parameters, requestBuilderCustomizer,
                                                         "there should not be any error", samplePlgConf.getId());
        validateTestPluginResponse(resultActions, new File("src/test/resources/samplePluginResult.xml"));
    }

    @Test
    public void testSampleServiceWithImageResponse() throws IOException {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put(SampleServicePlugin.RESPONSE_TYPE_PARAMETER, SampleServicePlugin.RESPONSE_TYPE_IMG);

        ServicePluginParameters parameters = new ServicePluginParameters("ENTITY_ID", null, null, null,
                dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().contentType(MediaType.IMAGE_PNG));
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        ResultActions resultActions = performDefaultPost(CatalogServicesController.PATH_SERVICES
                + CatalogServicesController.PATH_SERVICE_NAME, parameters, requestBuilderCustomizer,
                                                         "there should not be any error", samplePlgConf.getId());
        validateTestPluginResponse(resultActions, new File("src/test/resources/LogoCnes.png"));
    }

    @Test
    public void testSampleServiceWithUnkownResponse() throws IOException {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put(SampleServicePlugin.RESPONSE_TYPE_PARAMETER, SampleServicePlugin.RESPONSE_TYPE_OTHER);

        ServicePluginParameters parameters = new ServicePluginParameters("ENTITY_ID", null, null, null,
                dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_OCTET_STREAM));
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        ResultActions resultActions = performDefaultPost(CatalogServicesController.PATH_SERVICES
                + CatalogServicesController.PATH_SERVICE_NAME, parameters, requestBuilderCustomizer,
                                                         "there should not be any error", samplePlgConf.getId());
        validateTestPluginResponse(resultActions, new File("src/test/resources/result.other"));
    }

    protected Map<String, List<String>> getHeadersToApply() {
        Map<String, List<String>> headers = Maps.newHashMap();
        headers.put(HttpConstants.CONTENT_TYPE, Lists.newArrayList("application/json"));
        headers.put(HttpConstants.ACCEPT,
                    Lists.newArrayList(MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.TEXT_PLAIN_VALUE));

        return headers;
    }

}
