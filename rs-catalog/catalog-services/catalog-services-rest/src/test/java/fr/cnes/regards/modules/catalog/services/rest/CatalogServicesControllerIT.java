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
package fr.cnes.regards.modules.catalog.services.rest;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.accessrights.client.ILicenseClient;
import fr.cnes.regards.modules.catalog.services.dao.ILinkPluginsDatasetsRepository;
import fr.cnes.regards.modules.catalog.services.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.catalog.services.domain.ServicePluginParameters;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;
import fr.cnes.regards.modules.catalog.services.plugins.SampleServicePlugin;
import fr.cnes.regards.modules.catalog.services.service.link.ILinkPluginsDatasetsService;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import fr.cnes.regards.modules.search.dto.SearchRequest;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@AutoConfigureMockMvc(printOnlyOnFailure = true)
@TestPropertySource(locations = "classpath:test.properties")
@ContextConfiguration(classes = { CatalogServicesITConfiguration.class })
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

    @Autowired
    private ILinkPluginsDatasetsRepository linkDsRepo;

    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @MockBean
    private ILicenseClient licenseClient;

    @MockBean
    private IStorageRestClient storageRestClient;

    private PluginConfiguration conf;

    private PluginConfiguration samplePlgConf;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    private void clearDb() {
        linkDsRepo.deleteAll();
        pluginConfRepo.deleteAll();
    }

    @Before
    public void init() throws ModuleException {
        tenantResolver.forceTenant(getDefaultTenant());
        this.clearDb();
        LOG.info("--------------------> Initialization <-------------------------------------");
        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build("para", "never used").dynamic());
        final PluginMetaData metaData = new PluginMetaData();
        metaData.setPluginId("tata");
        metaData.setAuthor("toto");
        metaData.setDescription("titi");
        metaData.setVersion("1.0.0");
        metaData.getInterfaceNames().add(IService.class.getName());
        metaData.setPluginClassName(TestService.class.getName());
        // Create two plugin services linked to the same dataset
        // 1. first one
        if (!pluginService.findPluginConfigurationByLabel("testConf").isPresent()) {
            conf = new PluginConfiguration("testConf", parameters, metaData.getPluginId());
            conf = pluginService.savePluginConfiguration(conf);
        } else {
            LOG.warn("----------------------------------> Conf already exists for initialization");
        }
        // 2. second one
        if (!pluginService.findPluginConfigurationByLabel(PLUGIN_CONF_LABEL_1).isPresent()) {
            parameters = IPluginParam.set(IPluginParam.build(SampleServicePlugin.RESPONSE_TYPE_PARAMETER,
                                                             SampleServicePlugin.RESPONSE_TYPE_JSON).dynamic());
            samplePlgConf = new PluginConfiguration(PLUGIN_CONF_LABEL_1,
                                                    parameters,
                                                    SampleServicePlugin.class.getAnnotation(Plugin.class).id());
            pluginService.savePluginConfiguration(samplePlgConf);
        } else {
            LOG.warn("----------------------------------> Conf already exists for initialization {}",
                     PLUGIN_CONF_LABEL_1);
        }

        // Create a plugin service linked to an other dataset
        final PluginMetaData metaData2 = new PluginMetaData();
        metaData2.setPluginId("otherPluginMetaId");
        metaData2.setAuthor("otherPluginMetaAuthor");
        metaData2.setDescription("otherPlugin");
        metaData2.setVersion("1.0");
        metaData2.getInterfaceNames().add(IService.class.getName());
        metaData2.setPluginClassName(TestService.class.getName());

        parameters = IPluginParam.set(IPluginParam.build(SampleServicePlugin.RESPONSE_TYPE_PARAMETER,
                                                         SampleServicePlugin.RESPONSE_TYPE_JSON).dynamic());
        PluginConfiguration samplePlgConf2 = new PluginConfiguration(PLUGIN_CONF_LABEL_2,
                                                                     parameters,
                                                                     SampleServicePlugin.class.getAnnotation(Plugin.class)
                                                                                              .id());
        pluginService.savePluginConfiguration(samplePlgConf2);

        linkService.updateLink(DATA_SET_NAME,
                               new LinkPluginsDatasets(DATA_SET_NAME, Sets.newHashSet(conf, samplePlgConf)));

        linkService.updateLink(DATA_SET_NAME_2,
                               new LinkPluginsDatasets(DATA_SET_NAME_2,
                                                       Sets.newHashSet(samplePlgConf2, samplePlgConf)));
        LOG.info("--------------------> Initialization Done <-------------------------------------");
    }

    @Test
    public void testRetrieveServicesQuery() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$").isArray());
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$").isNotEmpty());
        requestBuilderCustomizer.addParameter(CatalogServicesController.DATASET_IDS_QUERY_PARAM, DATA_SET_NAME)
                                .addParameter("service_scope", ServiceScope.MANY.toString());
        requestBuilderCustomizer.addHeaders(getHeadersToApply());
        performDefaultGet(CatalogServicesController.PATH_SERVICES,
                          requestBuilderCustomizer,
                          "there should not be any error");
    }

    @Test
    public void testRetrieveServicesOne() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$").isArray());
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$").isNotEmpty());
        requestBuilderCustomizer.addParameter(CatalogServicesController.DATASET_IDS_QUERY_PARAM, DATA_SET_NAME)
                                .addParameter("service_scope", ServiceScope.ONE.toString());
        requestBuilderCustomizer.addHeaders(getHeadersToApply());
        performDefaultGet(CatalogServicesController.PATH_SERVICES,
                          requestBuilderCustomizer,
                          "there should not be any error");
    }

    @Test
    public void retrieveServices_shouldHaveMeta() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());

        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + "[0].content.label",
                                                                       Matchers.is(PLUGIN_CONF_LABEL_1)));
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + "[0].content.pluginId",
                                                                       Matchers.is(SampleServicePlugin.PLUGIN_ID)));
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + "[0].content.applicationModes",
                                                                       Matchers.containsInAnyOrder("MANY", "ONE")));
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + "[0].content.entityTypes",
                                                                       Matchers.contains(EntityType.DATA.toString())));
        requestBuilderCustomizer.addParameter(CatalogServicesController.DATASET_IDS_QUERY_PARAM,
                                              DATA_SET_NAME_2 + "," + DATA_SET_NAME)
                                .addParameter("service_scope", ServiceScope.ONE.toString());
        requestBuilderCustomizer.addHeaders(getHeadersToApply());
        performDefaultGet(CatalogServicesController.PATH_SERVICES,
                          requestBuilderCustomizer,
                          "There should be plugin configurations augmented with meta data");
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_ARC_030")
    @Requirement("REGARDS_DSL_DAM_ARC_010")
    @Purpose(
        "System has a joinpoint \"Service\" that allows to apply treatment on a dataset, or one of its subset. Those treatments are applied to informations contained into the catalog. A plugin \"Service\" can have as parameters: parameters defined at configuration by an administrator, parameters dynamicly defined at each request, parameters to select objects from a dataset.")
    public void testApplyService() throws IOException {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put("q", "truc");
        dynamicParameters.put("para", TestService.EXPECTED_VALUE);

        ServicePluginParameters parameters = new ServicePluginParameters(EntityType.DATA,
                                                                         new SearchRequest(SearchEngineMappings.LEGACY_PLUGIN_ID,
                                                                                           null,
                                                                                           null,
                                                                                           Sets.newHashSet("ENTITY_ID"),
                                                                                           null,
                                                                                           null),
                                                                         dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.addHeaders(getHeadersToApply());
        ResultActions resultActions = performDefaultPost(CatalogServicesController.PATH_SERVICES
                                                         + CatalogServicesController.PATH_SERVICE_NAME,
                                                         parameters,
                                                         requestBuilderCustomizer,
                                                         "there should not be any error",
                                                         conf.getBusinessId());
        validateTestPluginResponse(resultActions, new File("src/test/resources/result.json"));
    }

    private void validateTestPluginResponse(ResultActions resultActions, File expectedFileResult) throws IOException {
        File resultFile = File.createTempFile("result.json", "");

        // resultFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(resultFile)) {
            // Wait for availability
            resultActions.andReturn().getAsyncResult();
            MockHttpServletResponse response = resultActions.andReturn().getResponse();
            InputStream is = new ByteArrayInputStream(response.getContentAsByteArray());
            ByteStreams.copy(is, fos);
            fos.flush();
            is.close();
        }
        System.out.printf("--------- %s", resultFile.getAbsolutePath());
        logFileContent(resultFile);
        System.out.println("---------");
        logFileContent(expectedFileResult);
        System.out.println("---------");
        Assert.assertTrue("Request result is not valid", Files.equal(resultFile, expectedFileResult));
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
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        ServicePluginParameters parameters = new ServicePluginParameters(EntityType.DATA,
                                                                         new SearchRequest(SearchEngineMappings.LEGACY_PLUGIN_ID,
                                                                                           null,
                                                                                           null,
                                                                                           Sets.newHashSet("ENTITY_ID"),
                                                                                           null,
                                                                                           null),
                                                                         dynamicParameters);
        requestBuilderCustomizer.addHeaders(getHeadersToApply());
        ResultActions resultActions = performDefaultPost(CatalogServicesController.PATH_SERVICES
                                                         + CatalogServicesController.PATH_SERVICE_NAME,
                                                         parameters,
                                                         requestBuilderCustomizer,
                                                         "there should not be any error",
                                                         conf.getBusinessId());
        validateTestPluginResponse(resultActions, new File("src/test/resources/result_empty.json"));
    }

    @Test
    public void testSampleServiceWithJsonResponse() throws IOException {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put(SampleServicePlugin.RESPONSE_TYPE_PARAMETER, SampleServicePlugin.RESPONSE_TYPE_JSON);

        ServicePluginParameters parameters = new ServicePluginParameters(EntityType.DATA,
                                                                         new SearchRequest(SearchEngineMappings.LEGACY_PLUGIN_ID,
                                                                                           null,
                                                                                           null,
                                                                                           Sets.newHashSet("ENTITY_ID"),
                                                                                           null,
                                                                                           null),
                                                                         dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.expect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
        requestBuilderCustomizer.addHeaders(getHeadersToApply());
        ResultActions resultActions = performDefaultPost(CatalogServicesController.PATH_SERVICES
                                                         + CatalogServicesController.PATH_SERVICE_NAME,
                                                         parameters,
                                                         requestBuilderCustomizer,
                                                         "there should not be any error",
                                                         samplePlgConf.getBusinessId());
        validateTestPluginResponse(resultActions, new File("src/test/resources/samplePluginResult.json"));
    }

    @Test
    public void testSampleServiceWithXmlResponse() throws IOException {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put(SampleServicePlugin.RESPONSE_TYPE_PARAMETER, SampleServicePlugin.RESPONSE_TYPE_XML);

        ServicePluginParameters parameters = new ServicePluginParameters(EntityType.DATA,
                                                                         new SearchRequest(SearchEngineMappings.LEGACY_PLUGIN_ID,
                                                                                           null,
                                                                                           null,
                                                                                           Sets.newHashSet("ENTITY_ID"),
                                                                                           null,
                                                                                           null),
                                                                         dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk()
                                                                        .expect(MockMvcResultMatchers.content()
                                                                                                     .contentType(
                                                                                                         MediaType.APPLICATION_XML));
        requestBuilderCustomizer.addHeaders(getHeadersToApply());
        ResultActions resultActions = performDefaultPost(CatalogServicesController.PATH_SERVICES
                                                         + CatalogServicesController.PATH_SERVICE_NAME,
                                                         parameters,
                                                         requestBuilderCustomizer,
                                                         "there should not be any error",
                                                         samplePlgConf.getBusinessId());
        validateTestPluginResponse(resultActions, new File("src/test/resources/samplePluginResult.xml"));
    }

    @Test
    public void testSampleServiceWithImageResponse() throws IOException {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put(SampleServicePlugin.RESPONSE_TYPE_PARAMETER, SampleServicePlugin.RESPONSE_TYPE_IMG);

        ServicePluginParameters parameters = new ServicePluginParameters(EntityType.DATA,
                                                                         new SearchRequest(SearchEngineMappings.LEGACY_PLUGIN_ID,
                                                                                           null,
                                                                                           null,
                                                                                           Sets.newHashSet("ENTITY_ID"),
                                                                                           null,
                                                                                           null),
                                                                         dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk()
                                                                        .expect(MockMvcResultMatchers.content()
                                                                                                     .contentType(
                                                                                                         MediaType.IMAGE_PNG));
        requestBuilderCustomizer.addHeaders(getHeadersToApply());
        ResultActions resultActions = performDefaultPost(CatalogServicesController.PATH_SERVICES
                                                         + CatalogServicesController.PATH_SERVICE_NAME,
                                                         parameters,
                                                         requestBuilderCustomizer,
                                                         "there should not be any error",
                                                         samplePlgConf.getBusinessId());
        validateTestPluginResponse(resultActions, new File("src/test/resources/LogoCnes.png"));
    }

    @Test
    public void testSampleServiceWithUnkownResponse() throws IOException {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put(SampleServicePlugin.RESPONSE_TYPE_PARAMETER, SampleServicePlugin.RESPONSE_TYPE_OTHER);

        ServicePluginParameters parameters = new ServicePluginParameters(EntityType.DATA,
                                                                         new SearchRequest(SearchEngineMappings.LEGACY_PLUGIN_ID,
                                                                                           null,
                                                                                           null,
                                                                                           Sets.newHashSet("ENTITY_ID"),
                                                                                           null,
                                                                                           null),
                                                                         dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk()
                                                                        .expect(MockMvcResultMatchers.content()
                                                                                                     .contentType(
                                                                                                         MediaType.APPLICATION_OCTET_STREAM));
        requestBuilderCustomizer.addHeaders(getHeadersToApply());
        ResultActions resultActions = performDefaultPost(CatalogServicesController.PATH_SERVICES
                                                         + CatalogServicesController.PATH_SERVICE_NAME,
                                                         parameters,
                                                         requestBuilderCustomizer,
                                                         "there should not be any error",
                                                         samplePlgConf.getBusinessId());
        validateTestPluginResponse(resultActions, new File("src/test/resources/result.other"));
    }

    protected Map<String, List<String>> getHeadersToApply() {
        Map<String, List<String>> headers = Maps.newHashMap();
        headers.put(HttpConstants.CONTENT_TYPE, Lists.newArrayList("application/json"));
        headers.put(HttpConstants.ACCEPT,
                    Lists.newArrayList(MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE));

        return headers;
    }

}
