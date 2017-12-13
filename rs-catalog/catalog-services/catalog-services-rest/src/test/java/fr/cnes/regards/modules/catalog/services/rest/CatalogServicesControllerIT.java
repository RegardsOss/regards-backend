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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
        final PluginParameter parameter = new PluginParameter("para", "never used");
        parameter.setIsDynamic(true);
        final PluginMetaData metaData = new PluginMetaData();
        metaData.setPluginId("tata");
        metaData.setAuthor("toto");
        metaData.setDescription("titi");
        metaData.setVersion("tutu");
        metaData.getInterfaceNames().add(IService.class.getName());
        metaData.setPluginClassName(TestService.class.getName());
        conf = new PluginConfiguration(metaData, "testConf");
        conf.setParameters(Lists.newArrayList(parameter));
        pluginService.addPluginPackage(TestService.class.getPackage().getName());
        conf = pluginService.savePluginConfiguration(conf);

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addDynamicParameter(SampleServicePlugin.RESPONSE_TYPE_PARAMETER,
                                     SampleServicePlugin.RESPONSE_TYPE_JSON)
                .getParameters();
        samplePlgConf = new PluginConfiguration(
                PluginUtils.createPluginMetaData(SampleServicePlugin.class,
                                                 Arrays.asList(SampleServicePlugin.class.getPackage().getName())),
                "SampleServicePlugin", parameters);
        pluginService.savePluginConfiguration(samplePlgConf);

        linkService.updateLink(DATA_SET_NAME,
                               new LinkPluginsDatasets(DATA_SET_NAME, Sets.newHashSet(conf, samplePlgConf)));
    }

    @Test
    public void testRetrieveServicesQuery() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$").isArray());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$").isNotEmpty());
        requestBuilderCustomizer.customizeRequestParam().param("dataset_id", DATA_SET_NAME)
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
        requestBuilderCustomizer.customizeRequestParam().param("dataset_id", DATA_SET_NAME)
                .param("service_scope", ServiceScope.ONE.toString());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        performDefaultGet(CatalogServicesController.PATH_SERVICES, requestBuilderCustomizer,
                          "there should not be any error");
    }

    @Test
    public void retrieveServices_shouldHaveMeta() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + "[0].content.pluginId",
                                                                               Matchers.is("tata")));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                .jsonPath(JSON_PATH_ROOT + "[0].content.applicationModes", Matchers.containsInAnyOrder("MANY", "ONE")));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                .jsonPath(JSON_PATH_ROOT + "[0].content.entityTypes", Matchers.contains("DATA")));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + "[1].content.pluginId",
                                                                               Matchers.is("aSampleServicePlugin")));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                .jsonPath(JSON_PATH_ROOT + "[1].content.applicationModes", Matchers.containsInAnyOrder("MANY", "ONE")));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                .jsonPath(JSON_PATH_ROOT + "[1].content.entityTypes", Matchers.contains(EntityType.DATA.toString())));
        requestBuilderCustomizer.customizeRequestParam().param("dataset_id", DATA_SET_NAME)
                .param("service_scope", ServiceScope.ONE.toString());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        performDefaultGet(CatalogServicesController.PATH_SERVICES, requestBuilderCustomizer,
                          "There should be plugin configurations augmented with meta data");
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_ARC_030")
    @Requirement("REGARDS_DSL_DAM_ARC_010")
    @Purpose("System has a joinpoint \"Service\" that allows to apply treatment on a dataset, or one of its subset. Those treatments are applied to informations contained into the catalog. A plugin \"Service\" can have as parameters: parameters defined at configuration by an administrator, parameters dynamicly defined at each request, parameters to select objects from a dataset.")
    public void testApplyService() {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put("q", "truc");
        dynamicParameters.put("para", TestService.EXPECTED_VALUE);

        ServicePluginParameters parameters = new ServicePluginParameters("ENTITY_ID", null, null, null,
                dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$").isArray());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$").isNotEmpty());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        performDefaultPost(CatalogServicesController.PATH_SERVICES + CatalogServicesController.PATH_SERVICE_NAME,
                           parameters, requestBuilderCustomizer, "there should not be any error", conf.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_310")
    @Purpose("System allows to set a dynamic plugin parameter in the HTPP request")
    public void testApplyServiceSetSpecificParamValue() {
        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put("q", "truc");
        dynamicParameters.put("para", "HelloWorld");
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$").isEmpty());

        ServicePluginParameters parameters = new ServicePluginParameters("ENTITY_ID", null, null, null,
                dynamicParameters);

        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        performDefaultPost(CatalogServicesController.PATH_SERVICES + CatalogServicesController.PATH_SERVICE_NAME,
                           parameters, requestBuilderCustomizer, "there should not be any error", conf.getId());
    }

    @Test
    public void testSampleServiceWithJsonResponse() {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put(SampleServicePlugin.RESPONSE_TYPE_PARAMETER, SampleServicePlugin.RESPONSE_TYPE_JSON);

        ServicePluginParameters parameters = new ServicePluginParameters("ENTITY_ID", null, null, null,
                dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$.value").value("ENTITY_ID"));
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        performDefaultPost(CatalogServicesController.PATH_SERVICES + CatalogServicesController.PATH_SERVICE_NAME,
                           parameters, requestBuilderCustomizer, "there should not be any error",
                           samplePlgConf.getId());
    }

    @Test
    public void testSampleServiceWithXmlResponse() {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put(SampleServicePlugin.RESPONSE_TYPE_PARAMETER, SampleServicePlugin.RESPONSE_TYPE_XML);

        ServicePluginParameters parameters = new ServicePluginParameters("ENTITY_ID", null, null, null,
                dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_XML));
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        performDefaultPost(CatalogServicesController.PATH_SERVICES + CatalogServicesController.PATH_SERVICE_NAME,
                           parameters, requestBuilderCustomizer, "there should not be any error",
                           samplePlgConf.getId());
    }

    @Test
    public void testSampleServiceWithImageResponse() {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put(SampleServicePlugin.RESPONSE_TYPE_PARAMETER, SampleServicePlugin.RESPONSE_TYPE_IMG);

        ServicePluginParameters parameters = new ServicePluginParameters("ENTITY_ID", null, null, null,
                dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().contentType(MediaType.IMAGE_PNG));
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        performDefaultPost(CatalogServicesController.PATH_SERVICES + CatalogServicesController.PATH_SERVICE_NAME,
                           parameters, requestBuilderCustomizer, "there should not be any error",
                           samplePlgConf.getId());
    }

    @Test
    public void testSampleServiceWithUnkownResponse() {

        HashMap<String, String> dynamicParameters = new HashMap<>();
        dynamicParameters.put(SampleServicePlugin.RESPONSE_TYPE_PARAMETER, SampleServicePlugin.RESPONSE_TYPE_OTHER);

        ServicePluginParameters parameters = new ServicePluginParameters("ENTITY_ID", null, null, null,
                dynamicParameters);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_OCTET_STREAM));
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        performDefaultPost(CatalogServicesController.PATH_SERVICES + CatalogServicesController.PATH_SERVICE_NAME,
                           parameters, requestBuilderCustomizer, "there should not be any error",
                           samplePlgConf.getId());
    }

    protected Map<String, List<String>> getHeadersToApply() {
        Map<String, List<String>> headers = Maps.newHashMap();
        headers.put(HttpConstants.CONTENT_TYPE, Lists.newArrayList("application/json"));
        headers.put(HttpConstants.ACCEPT,
                    Lists.newArrayList(MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.TEXT_PLAIN_VALUE));

        return headers;
    }

}
