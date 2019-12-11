/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.plugins.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.geojson.PluginConfigurationFieldDescriptors;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Test class for REST endpoints to manage plugin entities.
 * @author Christophe Mertz
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=plugin_basic_it",
        "regards.cipher.key-location=src/test/resources/testKey", "regards.cipher.iv=1234567812345678" })
public class PluginControllerBasicIT extends AbstractRegardsTransactionalIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginControllerBasicIT.class);

    private static final String VERSION = "12345-6789-11";

    private static final String PLUGIN_ID = "aSamplePlugin";

    @SuppressWarnings("unused")
    private static final String AUTHOR = "CS-SI-DEV";

    private static final String LABEL = "a plugin configuration for the test";

    private static final String TRUE = "true";

    /**
     * Generated token for tests
     */
    private static String token = "";

    private Set<IPluginParam> pluginParameters;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IPluginConfigurationRepository pluginConfigurationRepository;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());

        pluginParameters = IPluginParam.set(IPluginParam.build("param31", "value31").dynamic("red", "green", "blue"),
                                            IPluginParam.build("param32", "value32"),
                                            IPluginParam.build("isActive", "true"));

        manageDefaultSecurity(PluginController.PLUGINS, RequestMethod.GET);

        manageDefaultSecurity(PluginController.PLUGIN_TYPES, RequestMethod.GET);

        manageDefaultSecurity(PluginController.PLUGINS_PLUGINID, RequestMethod.GET);

        manageDefaultSecurity(PluginController.PLUGINS_PLUGINID_CONFIGS, RequestMethod.GET);
        manageDefaultSecurity(PluginController.PLUGINS_PLUGINID_CONFIGS, RequestMethod.POST);

        manageDefaultSecurity(PluginController.PLUGINS_PLUGINID_CONFIGID, RequestMethod.GET);
        manageDefaultSecurity(PluginController.PLUGINS_PLUGINID_CONFIGID, RequestMethod.PUT);
        manageDefaultSecurity(PluginController.PLUGINS_PLUGINID_CONFIGID, RequestMethod.DELETE);

        manageDefaultSecurity(PluginController.PLUGINS_CONFIGID, RequestMethod.GET);
        manageDefaultSecurity(PluginController.PLUGINS_CONFIGS, RequestMethod.GET);

        manageDefaultSecurity(PluginController.PLUGINS_CACHE, RequestMethod.DELETE);

        token = generateToken(getDefaultUserEmail(), DEFAULT_ROLE);
    }

    @After
    public void cleanUp() {
        tenantResolver.forceTenant(getDefaultTenant());
        pluginConfigurationRepository.deleteAll();
    }

    private String manageDefaultSecurity(String urlPath, RequestMethod method) {
        return manageSecurity(getDefaultTenant(), urlPath, method, getDefaultUserEmail(), getDefaultRole());
    }

    @Test
    public void getAllPlugins() {
        performGet(PluginController.PLUGINS, token, customizer().expectStatusOk().expectToHaveSize(JSON_PATH_STAR, 4),
                   "unable to load all plugins");
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_500")
    @Purpose("The system allows to list all the plugins of a specific plugin's type")
    public void getPluginOneType() {
        String pluginType = ISamplePlugin.class.getCanonicalName();
        performGet(PluginController.PLUGINS, token,
                   customizer().expectStatusOk().expectToHaveSize(JSON_PATH_STAR, 2).addParameter("pluginType",
                                                                                                  pluginType),
                   String.format("unable to load plugins of type <%s>", ISamplePlugin.class.getCanonicalName()),
                   pluginType);
    }

    @Test
    public void getPluginOneUnknownType() {
        String pluginType = "hello";
        performGet(PluginController.PLUGINS, token,
                   customizer().expect(status().isUnprocessableEntity()).addParameter("pluginType", pluginType),
                   String.format("unable to load plugins of type <%s>", ISamplePlugin.class.getCanonicalName()),
                   pluginType);
    }

    @Test
    public void getOnePlugin() {
        String pluginId = pluginService.getPlugins().get(0).getPluginId();

        performGet(PluginController.PLUGINS_PLUGINID, token, customizer().expectStatusOk(),
                   String.format("unable to load plugin id <%s>", pluginId), pluginId);
    }

    @Test
    public void getPluginMetadata() {
        String pluginId = "ParamTestPlugin";

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();

        performGet(PluginController.PLUGINS_PLUGINID, token, customizer,
                   String.format("unable to load plugin id <%s>", pluginId), pluginId);
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_400")
    @Purpose("The system allows to list all the plugin's type of a microservice")
    public void getAllPluginTypes() {
        performGet(PluginController.PLUGIN_TYPES, token,
                   customizer().expectStatusOk().expectContentType(MediaType.APPLICATION_JSON_VALUE)
                           .expectToHaveSize(JSON_PATH_STAR, pluginService.getPluginTypes().size()),
                   "unable to load all plugin types");
    }

    @Test
    public void getPluginConfigurationsForOnePluginId() throws ModuleException, MalformedURLException {
        // Add a PluginConfiguration with the PluginService
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        performGet(PluginController.PLUGINS_PLUGINID_CONFIGS, token,
                   customizer().expectStatusOk().expectContentType(MediaType.APPLICATION_JSON_VALUE)
                           .expectToHaveToString("$..content.active", "[true]")
                           .expectToHaveToString("$..content.parameters[?(@.name == 'param31')].dynamic", "[true]")
                           .expectToHaveToString("$..content.parameters[?(@.name == 'param32')].dynamic", "[false]"),
                   "unable to load all plugin configuration of a specific plugin id",
                   aPluginConfiguration.getPluginId());
    }

    @Test
    public void getPluginConfiguration() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        performGet(PluginController.PLUGINS_CONFIGID, token,
                   customizer().expectStatusOk().expectContentType(MediaType.APPLICATION_JSON_VALUE)
                           .expectToHaveToString("$..content.active", "[true]")
                           .expectToHaveToString("$..content.parameters[?(@.name == 'param31')].dynamic", "[true]")
                           .expectToHaveToString("$..content.parameters[?(@.name == 'param32')].dynamic", "[false]"),
                   "unable to load a plugin configuration", aPluginConfiguration.getBusinessId());
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_210")
    @Purpose("When a HTTP request GET is successed, the HTTP return code is 200")
    public void getAllPluginConfiguration() throws ModuleException, MalformedURLException {

        createPluginConfiguration(LABEL);
        createPluginConfiguration(LABEL + " - second");

        performDefaultGet(PluginController.PLUGINS_CONFIGS,
                          customizer().expectStatusOk().expectContentType(MediaType.APPLICATION_JSON_VALUE)
                                  .expectToHaveToString("$.[0].content.active", TRUE)
                                  .expectToHaveToString("$.[1].content.active", TRUE),
                          "unable to load all plugin configuration");
    }

    @Test
    public void getAllPluginConfigurationForOneSpecificType() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        performGet(PluginController.PLUGINS_CONFIGS, token,
                   customizer().expectStatusOk().expectContentType(MediaType.APPLICATION_JSON_VALUE)
                           .expectToHaveToString("$.[0].content.pluginId", aPluginConfiguration.getPluginId())
                           .addParameter("pluginType", ISamplePlugin.class.getCanonicalName()),
                   "unable to load all plugin configuration");
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_220")
    @Purpose("When a HTTP request GET an unknown entity, the HTTP return code is 404")
    public void getAllPluginConfigurationByTypeError() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        performGet(PluginController.PLUGINS_CONFIGS, token,
                   customizer().expectStatusNotFound().expectContentType(MediaType.APPLICATION_JSON_VALUE)
                           .addParameter("pluginType", "HelloWorld"),
                   "unable to load all plugin configuration", aPluginConfiguration.getPluginId());
    }

    @Test
    public void getPluginConfigurationError() {
        // Get an unknown PluginConfiguration
        performGet(PluginController.PLUGINS_PLUGINID_CONFIGID, token, customizer().expectStatusNotFound(),
                   "unable to load a plugin configuration", "PLUGIN_ID_FAKE", 157L);
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_220")
    @Purpose("When a HTTP request GET an unknown plugin configuration, the HTTP return code is 404")
    public void getPluginConfigurationErrorWithoutPluginId() {
        // Get an unknown PluginConfiguration
        performGet(PluginController.PLUGINS_CONFIGID, token, customizer().expectStatusNotFound(),
                   "unable to load a plugin configuration", 156L);
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_210")
    @Purpose("When a HTTP request PUT is successed, the HTTP return code is 200")
    public void updatePluginConfiguration() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                .expectContentType(MediaType.APPLICATION_JSON_VALUE)
                .expectToHaveToString("$.content.pluginId", aPluginConfiguration.getPluginId())
                .expectToHaveToString("$.content.version", VERSION).expectToHaveToString("$.content.active", TRUE)
                .expectToHaveToString("$.content.parameters[?(@.name == 'param31')].dynamic", "[true]")
                .expectToHaveToString("$.content.parameters[?(@.name == 'param32')].dynamic", "[false]")
                .document(RequestDocumentation
                        .pathParameters(RequestDocumentation.parameterWithName(PluginController.REQUEST_PARAM_PLUGIN_ID)
                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                                .description("Plugin identifier"),
                                        RequestDocumentation
                                                .parameterWithName(PluginController.REQUEST_PARAM_BUSINESS_ID)
                                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                        .value(JSON_NUMBER_TYPE))
                                                .description("Plugin configuration identifier")));

        documentPluginConfRequestBody(customizer, true);

        // Update the added PluginConfiguration
        performPut(PluginController.PLUGINS_PLUGINID_CONFIGID, token, aPluginConfiguration, customizer,
                   "unable to update a plugin configuration", aPluginConfiguration.getPluginId(),
                   aPluginConfiguration.getBusinessId());
    }

    @Test
    public void updatePluginConfigurationErrorId() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        // Update the added PluginConfiguration
        performPut(PluginController.PLUGINS_PLUGINID_CONFIGID, token, aPluginConfiguration,
                   customizer().expectStatusNotFound(), "unable to update a plugin configuration",
                   aPluginConfiguration.getPluginId(), 9989L);
    }

    @Test
    public void updatePluginConfigurationErrorPluginId() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL + "tttttt");
        aPluginConfiguration.setPluginId("hello-toulouse");

        // Update the added PluginConfiguration
        performPut(PluginController.PLUGINS_PLUGINID_CONFIGID, token, aPluginConfiguration,
                   customizer().expectStatusNotFound(), "unable to update a plugin configuration",
                   aPluginConfiguration.getPluginId(), 9999L);
    }

    @Test
    public void updateUnknownPluginConfigurationError() {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(LABEL, pluginParameters, 0, PLUGIN_ID);
        aPluginConfiguration.setId(133L);

        // Update the added PluginConfiguration
        performPut(PluginController.PLUGINS_PLUGINID_CONFIGID, token, aPluginConfiguration,
                   customizer().expectStatusNotFound(), "unable to update a plugin configuration",
                   aPluginConfiguration.getPluginId(), aPluginConfiguration.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_210")
    @Purpose("When a HTTP request POST is successed, the HTTP return code is 201")
    public void savePluginConfiguration() throws MalformedURLException {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(LABEL, pluginParameters, 0, PLUGIN_ID);
        aPluginConfiguration.setIconUrl(new URL("http://google.fr/svg/logo.svg"));

        RequestBuilderCustomizer customizer = customizer().expectStatusCreated()
                .expectContentType(MediaType.APPLICATION_JSON_VALUE)
                .expectToHaveToString("$.content.pluginId", aPluginConfiguration.getPluginId())
                .expectToHaveToString("$.content.version", VERSION).expectToHaveToString("$.content.active", TRUE)
                .expectToHaveToString("$.content.parameters[?(@.name == 'param31')].dynamic", "[true]")
                .expectToHaveToString("$.content.parameters[?(@.name == 'param32')].dynamic", "[false]");

        documentPluginConfRequestBody(customizer, false);

        performPost(PluginController.PLUGINS_PLUGINID_CONFIGS, token, aPluginConfiguration, customizer,
                    "unable to save a plugin configuration", aPluginConfiguration.getPluginId());
    }

    private void documentPluginConfRequestBody(RequestBuilderCustomizer requestBuilderCustomizer, boolean update) {
        PluginConfigurationFieldDescriptors pluginConfDescriptors = new PluginConfigurationFieldDescriptors();
        List<FieldDescriptor> lfd = new ArrayList<>(pluginConfDescriptors.build(update));

        requestBuilderCustomizer
                .document(PayloadDocumentation.relaxedRequestFields(Attributes
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE).value("Plugin configuration")),
                                                                    lfd.toArray(new FieldDescriptor[0])));
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_230")
    @Purpose("If a HTTP request POST is unsupported or mal-formatted, the HTTP return code is 400")
    public void savePluginConfigurationErrorConfNull() {
        performPost(PluginController.PLUGINS_PLUGINID_CONFIGS, token, null, customizer().expectStatusBadRequest(),
                    "unable to save a plugin configuration", "badPluginId");

    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_210")
    @Purpose("When a HTTP request DELETE is successed, the HTTP return code is 204")
    public void deletePluginConfiguration() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        performDelete(PluginController.PLUGINS_PLUGINID_CONFIGID, token, customizer().expectStatusNoContent()
                .document(RequestDocumentation
                        .pathParameters(RequestDocumentation.parameterWithName(PluginController.REQUEST_PARAM_PLUGIN_ID)
                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                                .description("Plugin identifier"),
                                        RequestDocumentation
                                                .parameterWithName(PluginController.REQUEST_PARAM_BUSINESS_ID)
                                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                        .value(JSON_STRING_TYPE))
                                                .description("Plugin configuration identifier"))),
                      "unable to delete a plugin configuration", aPluginConfiguration.getPluginId(),
                      aPluginConfiguration.getBusinessId());
    }

    @Test
    @Purpose("When a HTTP request DELETE is successed, the HTTP return code is 204")
    public void emptyPluginsCahe() throws ModuleException, MalformedURLException {
        createPluginConfiguration(LABEL);

        performDelete(PluginController.PLUGINS_CACHE, token, customizer().expectStatusNoContent(),
                      "unable to empty a cache plugin");
    }

    //    private PluginMetaData getPluginMetaData() {
    //        final PluginMetaData pluginMetaData = new PluginMetaData();
    //        pluginMetaData.setPluginClassName(SamplePlugin.class.getCanonicalName());
    //        pluginMetaData.setInterfaceNames(Sets.newHashSet(ISamplePlugin.class.getCanonicalName()));
    //        pluginMetaData.setPluginId(PLUGIN_ID);
    //        pluginMetaData.setAuthor(AUTHOR);
    //        pluginMetaData.setVersion(VERSION);
    //
    //        return pluginMetaData;
    //    }

    // Add a PluginConfiguration with the PluginService
    private PluginConfiguration createPluginConfiguration(String label) throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(label, pluginParameters, 0, PLUGIN_ID);
        aPluginConfiguration.setIconUrl(new URL("http://google.fr/svg/logo.svg"));
        PluginConfiguration savedPluginConf = pluginService.savePluginConfiguration(aPluginConfiguration);
        return savedPluginConf;
    }

}
