/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.plugins.ISamplePlugin;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;

/**
 *
 * Class PluginControllerIT
 *
 * Test class for REST endpoints to manage plugin entities.
 *
 * @author Christophe Mertz
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class PluginControllerIT extends AbstractRegardsIT {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginControllerIT.class);

    private static final String VERSION = "12345-6789-11";

    private static final String PLUGIN_ID = "a-plugin-id";

    private static final String AUTHOR = "CS-SI-DEV";

    private static final Long AN_ID = 050L;

    private static final String LABEL = "a plugin configuration for the test";

    /**
     * Generated token for tests
     */
    private static String token = "";

    private List<PluginParameter> pluginParameters;

    @Autowired
    private IPluginService pluginService;

    @Before
    public void init() {

        pluginParameters = PluginParametersFactory.build().addParameterDynamic("param31", "value31")
                .addParameter("param32", "value32").addParameter("param33", "value33")
                .addParameter("param34", "value34").addParameter("param35", "value35").getParameters();

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
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_STAR,
                                                        Matchers.hasSize(pluginService.getPlugins().size())));
        performGet(PluginController.PLUGINS, token, expectations, "unable to load all plugins");
    }

    @Test
    public void getPluginOneType() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        final String pluginType = ISamplePlugin.class.getCanonicalName();
        expectations.add(MockMvcResultMatchers
                .jsonPath(JSON_PATH_STAR,
                          Matchers.hasSize(pluginService.getPluginsByType(ISamplePlugin.class).size())));
        performDefaultGet(PluginController.PLUGINS + "?pluginType=" + pluginType, expectations,
                          String.format("unable to load plugins of type <%s>", pluginType));
    }

    @Test
    public void getPluginOneUnknownType() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isUnprocessableEntity());
        final String pluginType = "hello";
        performDefaultGet(PluginController.PLUGINS + "?pluginType=" + pluginType, expectations,
                          String.format("unable to load plugins of unknown type", pluginType));
    }

    @Test
    public void getOnePlugin() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        final String pluginId = pluginService.getPlugins().get(0).getPluginId();
        performDefaultGet(PluginController.PLUGINS_PLUGINID, expectations,
                          String.format("unable to load plugin id <%s>", pluginId), pluginId);
    }

    @Test
    public void getAllPluginTypes() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_STAR,
                                                        Matchers.hasSize(pluginService.getPluginTypes().size())));
        performDefaultGet(PluginController.PLUGIN_TYPES, expectations, "unable to load all plugin types");
    }

    @Test
    @DirtiesContext
    public void getPluginConfigurationsByTypeWithPluginId() {
        // Add a PluginConfiguration with the PluginService
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                pluginParameters, 0);
        try {
            aPluginConfiguration = pluginService.savePluginConfiguration(aPluginConfiguration);
            aPluginConfiguration.setId(AN_ID);
        } catch (final ModuleException e) {
            Assert.fail();
        }

        // Get all the PluginConfiguration with the a specific ID
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        // expectations.add(MockMvcResultMatchers.jsonPath("$..content.pluginId", Matchers.hasToString(PLUGIN_ID)));
        expectations.add(MockMvcResultMatchers.jsonPath("$..content.active", Matchers.hasToString("[true]")));
        expectations.add(MockMvcResultMatchers.jsonPath("$..content.parameters[0].dynamic",
                                                        Matchers.hasToString("[true]")));
        expectations.add(MockMvcResultMatchers.jsonPath("$..content.parameters[1].dynamic",
                                                        Matchers.hasToString("[false]")));

        performGet(PluginController.PLUGINS_PLUGINID_CONFIGS, token, expectations,
                   "unable to load all plugin configuration of a specific plugin id", PLUGIN_ID);
    }

    @Test
    @DirtiesContext
    public void getPluginConfiguration() {
        // Add a PluginConfiguration with the PluginService
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                pluginParameters, 0);
        try {
            aPluginConfiguration = pluginService.savePluginConfiguration(aPluginConfiguration);
            aPluginConfiguration.setId(AN_ID);
        } catch (final ModuleException e) {
            Assert.fail();
        }

        // Get the added PluginConfiguration
        final Long configId = AN_ID;
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$..content.active", Matchers.hasToString("[true]")));
        expectations.add(MockMvcResultMatchers.jsonPath("$..content.parameters[0].dynamic",
                                                        Matchers.hasToString("[true]")));
        expectations.add(MockMvcResultMatchers.jsonPath("$..content.parameters[1].dynamic",
                                                        Matchers.hasToString("[false]")));

        performGet(PluginController.PLUGINS_CONFIGID, token, expectations, "unable to load a plugin configuration",
                   configId);
    }

    @Test
    @DirtiesContext
    public void getAllPluginConfiguration() {
        // Get the added PluginConfiguration
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$.[0].content.active", Matchers.hasToString("true")));
        expectations.add(MockMvcResultMatchers.jsonPath("$.[1].content.active", Matchers.hasToString("true")));
        expectations.add(MockMvcResultMatchers.jsonPath("$.[0].content.parameters[0].dynamic",
                                                        Matchers.hasToString("true")));
        expectations.add(MockMvcResultMatchers.jsonPath("$.[1].content.parameters[0].dynamic",
                                                        Matchers.hasToString("false")));

        performGet(PluginController.PLUGINS_CONFIGS, token, expectations, "unable to load all plugin configuration");
    }

    @Test
    @DirtiesContext
    public void getPluginConfigurationError() {
        // Get an unknown PluginConfiguration
        final Long configId = AN_ID;
        final String pluginId = PLUGIN_ID;
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());
        performGet(PluginController.PLUGINS_PLUGINID_CONFIGID, token, expectations,
                   "unable to load a plugin configuration", pluginId, configId);
    }

    @Test
    @DirtiesContext
    public void getPluginConfigurationErrorWithoutPluginId() {
        // Get an unknown PluginConfiguration
        final Long configId = AN_ID;
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());
        performGet(PluginController.PLUGINS_CONFIGID, token, expectations, "unable to load a plugin configuration",
                   configId);
    }

    @Test
    @DirtiesContext
    public void updatePluginConfiguration() {
        // Add a PluginConfiguration with the PluginService
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                pluginParameters, 0);
        try {
            aPluginConfiguration = pluginService.savePluginConfiguration(aPluginConfiguration);
            aPluginConfiguration.setId(AN_ID);
        } catch (final ModuleException e) {
            Assert.fail();
        }

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.pluginId", Matchers.hasToString(PLUGIN_ID)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.version", Matchers.hasToString(VERSION)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.active", Matchers.hasToString("true")));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.content.parameters[0].dynamic", Matchers.hasToString("true")));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.content.parameters[1].dynamic", Matchers.hasToString("false")));

        // Update the added PluginConfiguration
        performPut(PluginController.PLUGINS_PLUGINID_CONFIGID, token, aPluginConfiguration, expectations,
                   "unable to update a plugin configuration", PLUGIN_ID, aPluginConfiguration.getId());
    }

    @Test
    @DirtiesContext
    public void updatePluginConfigurationErrorId() {
        // Add a PluginConfiguration with the PluginService
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                pluginParameters, 0);
        try {
            aPluginConfiguration = pluginService.savePluginConfiguration(aPluginConfiguration);
            aPluginConfiguration.setId(AN_ID);
        } catch (final ModuleException e) {
            Assert.fail();
        }

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());

        // Update the added PluginConfiguration
        performPut(PluginController.PLUGINS_PLUGINID_CONFIGID, token, aPluginConfiguration, expectations,
                   "unable to update a plugin configuration", PLUGIN_ID, 9989L);
    }

    @Test
    @DirtiesContext
    public void updatePluginConfigurationErrorPluginId() {
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                pluginParameters, 0);
        try {
            aPluginConfiguration = pluginService.savePluginConfiguration(aPluginConfiguration);
            aPluginConfiguration.setPluginId("hello-toulouse");
        } catch (final ModuleException e) {
            Assert.fail();
        }

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());

        // Update the added PluginConfiguration
        performDefaultPut(PluginController.PLUGINS_PLUGINID_CONFIGID, aPluginConfiguration, expectations,
                          "unable to update a plugin configuration", PLUGIN_ID, 9999L);
    }

    @Test
    @DirtiesContext
    public void updateUnknownPluginConfigurationError() {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                pluginParameters, 0);
        aPluginConfiguration.setId(AN_ID);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());

        // Update the added PluginConfiguration
        performDefaultPut(PluginController.PLUGINS_PLUGINID_CONFIGID, aPluginConfiguration, expectations,
                          "unable to update a plugin configuration", PLUGIN_ID, AN_ID);
    }

    @Test
    @DirtiesContext
    public void savePluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                pluginParameters, 0);
        // we use a Stub Repository, it is necessary to set the Id
        aPluginConfiguration.setId(AN_ID + 339);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isCreated());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.pluginId", Matchers.hasToString(PLUGIN_ID)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.version", Matchers.hasToString(VERSION)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.active", Matchers.hasToString("true")));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.content.parameters[0].dynamic", Matchers.hasToString("true")));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.content.parameters[1].dynamic", Matchers.hasToString("false")));

        performPost(PluginController.PLUGINS_PLUGINID_CONFIGS, token, aPluginConfiguration, expectations,
                    "unable to save a plugin configuration", PLUGIN_ID);
    }

    @Test
    @DirtiesContext
    public void savePluginConfigurationErrorConfNull() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isServiceUnavailable());
        performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS, null, expectations,
                           "unable to save a plugin configuration", PLUGIN_ID);
    }

    @Test
    @DirtiesContext
    public void deletePluginConfiguration() {
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                pluginParameters, 0);
        try {
            aPluginConfiguration = pluginService.savePluginConfiguration(aPluginConfiguration);
            aPluginConfiguration.setId(AN_ID);
        } catch (final ModuleException e) {
            Assert.fail();
        }
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNoContent());
        performDefaultDelete(PluginController.PLUGINS_PLUGINID_CONFIGID, expectations,
                             "unable to delete a plugin configuration", PLUGIN_ID, aPluginConfiguration.getId());
    }

    private PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setPluginClassName(Integer.class.getCanonicalName());
        pluginMetaData.setPluginId(PLUGIN_ID);
        pluginMetaData.setAuthor(AUTHOR);
        pluginMetaData.setVersion(VERSION);
        return pluginMetaData;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
