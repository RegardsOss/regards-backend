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
package fr.cnes.regards.framework.modules.plugins.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.geojson.PluginConfigurationFieldDescriptors;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.plugins.ISamplePlugin;
import fr.cnes.regards.framework.plugins.SamplePlugin;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;

/**
 * Test class for REST endpoints to manage plugin entities.
 *
 * @author Christophe Mertz
 * @author Sébastien Binda
 */
@TestPropertySource(locations = { "classpath:application-test.properties" })
@MultitenantTransactional
public class PluginControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginControllerIT.class);

    private static final String VERSION = "12345-6789-11";

    private static final String PLUGIN_ID = "aSamplePlugin";

    private static final String AUTHOR = "CS-SI-DEV";

    private static final String LABEL = "a plugin configuration for the test";

    private static final String TRUE = "true";

    private static final String FALSE = "false";

    /**
     * Generated token for tests
     */
    private static String token = "";

    private List<PluginParameter> pluginParameters;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Before
    public void init() {
        tenantResolver.forceTenant(DEFAULT_TENANT);

        pluginParameters = PluginParametersFactory.build()
                .addDynamicParameter("param31", "value31", Arrays.asList("red", "green", "blue"))
                .addParameter("param32", "value32").addParameter("isActive", "true").getParameters();

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

        token = generateToken(DEFAULT_USER_EMAIL, DEFAULT_ROLE);
    }

    @Test
    public void getAllPlugins() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_STAR, Matchers.hasSize(3)));

        performGet(PluginController.PLUGINS, token, requestBuilderCustomizer, "unable to load all plugins");
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_500")
    @Purpose("The system allows to list all the plugins of a specific plugin's type")
    public void getPluginOneType() {
        final String pluginType = ISamplePlugin.class.getCanonicalName();

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_STAR, Matchers.hasSize(2)));
        requestBuilderCustomizer.customizeRequestParam().param("pluginType", pluginType);

        performGet(PluginController.PLUGINS, token, requestBuilderCustomizer,
                   String.format("unable to load plugins of type <%s>", ISamplePlugin.class.getCanonicalName()),
                   pluginType);
    }

    @Test
    public void getPluginOneUnknownType() {
        final String pluginType = "hello";

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isUnprocessableEntity());
        requestBuilderCustomizer.customizeRequestParam().param("pluginType", pluginType);

        performGet(PluginController.PLUGINS, token, requestBuilderCustomizer,
                   String.format("unable to load plugins of type <%s>", ISamplePlugin.class.getCanonicalName()),
                   pluginType);
    }

    @Test
    public void getOnePlugin() {
        final String pluginId = pluginService.getPlugins().get(0).getPluginId();

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isOk());

        performGet(PluginController.PLUGINS_PLUGINID, token, requestBuilderCustomizer,
                   String.format("unable to load plugin id <%s>", pluginId), pluginId);
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_400")
    @Purpose("The system allows to list all the plugin's type of a microservice")
    public void getAllPluginTypes() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                .jsonPath(JSON_PATH_STAR, Matchers.hasSize(pluginService.getPluginTypes().size())));

        performGet(PluginController.PLUGIN_TYPES, token, requestBuilderCustomizer, "unable to load all plugin types");
    }

    @Test
    public void getPluginConfigurationsForOnePluginId() throws ModuleException, MalformedURLException {
        // Add a PluginConfiguration with the PluginService
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        // Get all the PluginConfiguration with the a specific ID
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        // expectations.add(MockMvcResultMatchers.jsonPath("$..content.pluginId", Matchers.hasToString(PLUGIN_ID)));
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$..content.active", Matchers.hasToString("[true]")));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$..content.parameters[0].dynamic",
                                                                               Matchers.hasToString("[true]")));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$..content.parameters[1].dynamic",
                                                                               Matchers.hasToString("[false]")));

        performGet(PluginController.PLUGINS_PLUGINID_CONFIGS, token, requestBuilderCustomizer,
                   "unable to load all plugin configuration of a specific plugin id",
                   aPluginConfiguration.getPluginId());
    }

    @Test
    public void getPluginConfiguration() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        // Get the added PluginConfiguration
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$..content.active", Matchers.hasToString("[true]")));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$..content.parameters[0].dynamic",
                                                                               Matchers.hasToString("[true]")));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$..content.parameters[1].dynamic",
                                                                               Matchers.hasToString("[false]")));

        performGet(PluginController.PLUGINS_CONFIGID, token, requestBuilderCustomizer,
                   "unable to load a plugin configuration", aPluginConfiguration.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_210")
    @Purpose("When a HTTP request GET is successed, the HTTP return code is 200")
    public void getAllPluginConfiguration() throws ModuleException, MalformedURLException {

        createPluginConfiguration(LABEL);
        createPluginConfiguration(LABEL + " - second");

        // Get the added PluginConfiguration
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.[0].content.active", Matchers.hasToString(TRUE)));
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.[1].content.active", Matchers.hasToString(TRUE)));
        // FIXME fix plugin pollution!!!!!!!
        //        expectations
        //                .add(MockMvcResultMatchers.jsonPath("$.[0].content.parameters[0].dynamic", Matchers.hasToString(TRUE)));

        performGet(PluginController.PLUGINS_CONFIGS, token, requestBuilderCustomizer,
                   "unable to load all plugin configuration");
    }

    @Test
    public void getAllPluginConfigurationForOneSpecificType() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        // Get the added PluginConfiguration
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                .jsonPath("$.[0].content.pluginId", Matchers.hasToString(aPluginConfiguration.getPluginId())));
        requestBuilderCustomizer.customizeRequestParam().param("pluginType", ISamplePlugin.class.getCanonicalName());

        performGet(PluginController.PLUGINS_CONFIGS, token, requestBuilderCustomizer,
                   "unable to load all plugin configuration");
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_220")
    @Purpose("When a HTTP request GET an unknown entity, the HTTP return code is 404")
    public void getAllPluginConfigurationByTypeError() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        // Get the added PluginConfiguration
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isNotFound());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        requestBuilderCustomizer.customizeRequestParam().param("pluginType", "HelloWorld");

        performGet(PluginController.PLUGINS_CONFIGS, token, requestBuilderCustomizer,
                   "unable to load all plugin configuration", aPluginConfiguration.getPluginId());
    }

    @Test
    public void getPluginConfigurationError() {
        // Get an unknown PluginConfiguration
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isNotFound());

        performGet(PluginController.PLUGINS_PLUGINID_CONFIGID, token, requestBuilderCustomizer,
                   "unable to load a plugin configuration", "PLUGIN_ID_FAKE", 157L);
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_220")
    @Purpose("When a HTTP request GET an unknown plugin configuration, the HTTP return code is 404")
    public void getPluginConfigurationErrorWithoutPluginId() {
        // Get an unknown PluginConfiguration
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isNotFound());

        performGet(PluginController.PLUGINS_CONFIGID, token, requestBuilderCustomizer,
                   "unable to load a plugin configuration", 156L);
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_210")
    @Purpose("When a HTTP request PUT is successed, the HTTP return code is 200")
    public void updatePluginConfiguration() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                .jsonPath("$.content.pluginId", Matchers.hasToString(aPluginConfiguration.getPluginId())));
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.content.version", Matchers.hasToString(VERSION)));
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.content.active", Matchers.hasToString(TRUE)));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.parameters[0].dynamic",
                                                                               Matchers.hasToString(TRUE)));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.parameters[1].dynamic",
                                                                               Matchers.hasToString(FALSE)));

        documentPluginConfRequestBody(requestBuilderCustomizer, true);

        // Update the added PluginConfiguration
        performPut(PluginController.PLUGINS_PLUGINID_CONFIGID, token, aPluginConfiguration, requestBuilderCustomizer,
                   "unable to update a plugin configuration", aPluginConfiguration.getPluginId(),
                   aPluginConfiguration.getId());
    }

    @Test
    public void updatePluginConfigurationErrorId() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isNotFound());

        // Update the added PluginConfiguration
        performPut(PluginController.PLUGINS_PLUGINID_CONFIGID, token, aPluginConfiguration, requestBuilderCustomizer,
                   "unable to update a plugin configuration", aPluginConfiguration.getPluginId(), 9989L);
    }

    @Test
    public void updatePluginConfigurationErrorPluginId() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL + "tttttt");
        aPluginConfiguration.setPluginId("hello-toulouse");

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isNotFound());

        // Update the added PluginConfiguration
        performPut(PluginController.PLUGINS_PLUGINID_CONFIGID, token, aPluginConfiguration, requestBuilderCustomizer,
                   "unable to update a plugin configuration", aPluginConfiguration.getPluginId(), 9999L);
    }

    @Test
    public void updateUnknownPluginConfigurationError() {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                pluginParameters, 0);
        aPluginConfiguration.setId(133L);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isNotFound());

        // Update the added PluginConfiguration
        performPut(PluginController.PLUGINS_PLUGINID_CONFIGID, token, aPluginConfiguration, requestBuilderCustomizer,
                   "unable to update a plugin configuration", aPluginConfiguration.getPluginId(),
                   aPluginConfiguration.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_210")
    @Purpose("When a HTTP request POST is successed, the HTTP return code is 201")
    public void savePluginConfiguration() throws MalformedURLException {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                pluginParameters, 0);
        aPluginConfiguration.setIconUrl(new URL("http://google.fr/svg/logo.svg"));

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isCreated());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                .jsonPath("$.content.pluginId", Matchers.hasToString(aPluginConfiguration.getPluginId())));
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.content.version", Matchers.hasToString(VERSION)));
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.content.active", Matchers.hasToString(TRUE)));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.parameters[0].dynamic",
                                                                               Matchers.hasToString(TRUE)));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.parameters[1].dynamic",
                                                                               Matchers.hasToString(FALSE)));

        documentPluginConfRequestBody(requestBuilderCustomizer, false);

        performPost(PluginController.PLUGINS_PLUGINID_CONFIGS, token, aPluginConfiguration, requestBuilderCustomizer,
                    "unable to save a plugin configuration", aPluginConfiguration.getPluginId());
    }

    private void documentPluginConfRequestBody(RequestBuilderCustomizer requestBuilderCustomizer, boolean update) {
        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();

        PluginConfigurationFieldDescriptors pluginConfDescriptors = new PluginConfigurationFieldDescriptors();
        lfd.addAll(pluginConfDescriptors.build(update));

        requestBuilderCustomizer.addDocumentationSnippet(PayloadDocumentation
                .relaxedRequestFields(Attributes
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE)
                                .value("Plugin configuration")), lfd.toArray(new FieldDescriptor[lfd.size()])));
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_230")
    @Purpose("If a HTTP request POST is unsupported or mal-formatted, the HTTP return code is 400")
    public void savePluginConfigurationErrorConfNull() {

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isBadRequest());

        performPost(PluginController.PLUGINS_PLUGINID_CONFIGS, token, null, requestBuilderCustomizer,
                    "unable to save a plugin configuration", "badPluginId");

    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_210")
    @Purpose("When a HTTP request DELETE is successed, the HTTP return code is 204")
    public void deletePluginConfiguration() throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isNoContent());

        performDelete(PluginController.PLUGINS_PLUGINID_CONFIGID, token, requestBuilderCustomizer,
                      "unable to delete a plugin configuration", aPluginConfiguration.getPluginId(),
                      aPluginConfiguration.getId());
    }

    private PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setPluginClassName(SamplePlugin.class.getCanonicalName());
        pluginMetaData.setInterfaceNames(Sets.newHashSet(ISamplePlugin.class.getCanonicalName()));
        pluginMetaData.setPluginId(PLUGIN_ID);
        pluginMetaData.setAuthor(AUTHOR);
        pluginMetaData.setVersion(VERSION);

        return pluginMetaData;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    // Add a PluginConfiguration with the PluginService
    private PluginConfiguration createPluginConfiguration(String label) throws ModuleException, MalformedURLException {
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), label,
                pluginParameters, 0);
        aPluginConfiguration.setIconUrl(new URL("http://google.fr/svg/logo.svg"));
        return pluginService.savePluginConfiguration(aPluginConfiguration);
    }

}
