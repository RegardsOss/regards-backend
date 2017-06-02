/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.plugins.ISamplePlugin;
import fr.cnes.regards.framework.plugins.SamplePlugin;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

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
                .addParameterDynamic("param31", "value31", Arrays.asList("red", "green", "blue"))
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
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_STAR, Matchers.hasSize(3)));
        performGet(PluginController.PLUGINS, token, expectations, "unable to load all plugins");
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_500")
    @Purpose("The system allows to list all the plugins of a specific plugin's type")
    public void getPluginOneType() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        final String pluginType = ISamplePlugin.class.getCanonicalName();
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_STAR, Matchers.hasSize(2)));
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
    @Requirement("REGARDS_DSL_CMP_PLG_400")
    @Purpose("The system allows to list all the plugin's type of a microservice")
    public void getAllPluginTypes() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_STAR,
                                                        Matchers.hasSize(pluginService.getPluginTypes().size())));
        performDefaultGet(PluginController.PLUGIN_TYPES, expectations, "unable to load all plugin types");
    }

    @Test
    public void getPluginConfigurationsForOnePluginId() throws ModuleException {
        // Add a PluginConfiguration with the PluginService
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

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
                   "unable to load all plugin configuration of a specific plugin id",
                   aPluginConfiguration.getPluginId());
    }

    @Test
    public void getPluginConfiguration() throws ModuleException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        // Get the added PluginConfiguration
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$..content.active", Matchers.hasToString("[true]")));
        expectations.add(MockMvcResultMatchers.jsonPath("$..content.parameters[0].dynamic",
                                                        Matchers.hasToString("[true]")));
        expectations.add(MockMvcResultMatchers.jsonPath("$..content.parameters[1].dynamic",
                                                        Matchers.hasToString("[false]")));

        performGet(PluginController.PLUGINS_CONFIGID, token, expectations, "unable to load a plugin configuration",
                   aPluginConfiguration.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_210")
    @Purpose("When a HTTP request GET is successed, the HTTP return code is 200")
    public void getAllPluginConfiguration() throws ModuleException {

        createPluginConfiguration(LABEL);
        createPluginConfiguration(LABEL + " - second");

        // Get the added PluginConfiguration
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$.[0].content.active", Matchers.hasToString(TRUE)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.[1].content.active", Matchers.hasToString(TRUE)));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.[0].content.parameters[0].dynamic", Matchers.hasToString(TRUE)));

        performGet(PluginController.PLUGINS_CONFIGS, token, expectations, "unable to load all plugin configuration");
    }

    @Test
    public void getAllPluginConfigurationForOneSpecificType() throws ModuleException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        // Get the added PluginConfiguration
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$.[0].content.pluginId",
                                                        Matchers.hasToString(aPluginConfiguration.getPluginId())));

        performGet(PluginController.PLUGINS_CONFIGS + "?pluginType=" + ISamplePlugin.class.getCanonicalName(), token,
                   expectations, "unable to load all plugin configuration");
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_220")
    @Purpose("When a HTTP request GET an unknown entity, the HTTP return code is 404")
    public void getAllPluginConfigurationByTypeError() throws ModuleException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        // Get the added PluginConfiguration
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));

        performGet(PluginController.PLUGINS_CONFIGS + "?pluginType=HelloWorld", token, expectations,
                   "unable to load all plugin configuration", aPluginConfiguration.getPluginId());
    }

    @Test
    public void getPluginConfigurationError() {
        // Get an unknown PluginConfiguration
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());
        performGet(PluginController.PLUGINS_PLUGINID_CONFIGID, token, expectations,
                   "unable to load a plugin configuration", "PLUGIN_ID_FAKE", 157L);
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_220")
    @Purpose("When a HTTP request GET an unknown plugin configuration, the HTTP return code is 404")
    public void getPluginConfigurationErrorWithoutPluginId() {
        // Get an unknown PluginConfiguration
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());
        performGet(PluginController.PLUGINS_CONFIGID, token, expectations, "unable to load a plugin configuration",
                   156L);
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_210")
    @Purpose("When a HTTP request PUT is successed, the HTTP return code is 200")
    public void updatePluginConfiguration() throws ModuleException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.pluginId",
                                                        Matchers.hasToString(aPluginConfiguration.getPluginId())));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.version", Matchers.hasToString(VERSION)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.active", Matchers.hasToString(TRUE)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.parameters[0].dynamic", Matchers.hasToString(TRUE)));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.content.parameters[1].dynamic", Matchers.hasToString(FALSE)));

        // Update the added PluginConfiguration
        performPut(PluginController.PLUGINS_PLUGINID_CONFIGID, token, aPluginConfiguration, expectations,
                   "unable to update a plugin configuration", aPluginConfiguration.getPluginId(),
                   aPluginConfiguration.getId());
    }

    @Test
    public void updatePluginConfigurationErrorId() throws ModuleException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());

        // Update the added PluginConfiguration
        performPut(PluginController.PLUGINS_PLUGINID_CONFIGID, token, aPluginConfiguration, expectations,
                   "unable to update a plugin configuration", aPluginConfiguration.getPluginId(), 9989L);
    }

    @Test
    public void updatePluginConfigurationErrorPluginId() throws ModuleException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL + "tttttt");
        aPluginConfiguration.setPluginId("hello-toulouse");

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());

        // Update the added PluginConfiguration
        performDefaultPut(PluginController.PLUGINS_PLUGINID_CONFIGID, aPluginConfiguration, expectations,
                          "unable to update a plugin configuration", aPluginConfiguration.getPluginId(), 9999L);
    }

    @Test
    public void updateUnknownPluginConfigurationError() {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                pluginParameters, 0);
        aPluginConfiguration.setId(133L);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());

        // Update the added PluginConfiguration
        performDefaultPut(PluginController.PLUGINS_PLUGINID_CONFIGID, aPluginConfiguration, expectations,
                          "unable to update a plugin configuration", aPluginConfiguration.getPluginId(),
                          aPluginConfiguration.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_210")
    @Purpose("When a HTTP request POST is successed, the HTTP return code is 201")
    public void savePluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                pluginParameters, 0);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isCreated());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.pluginId",
                                                        Matchers.hasToString(aPluginConfiguration.getPluginId())));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.version", Matchers.hasToString(VERSION)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.active", Matchers.hasToString(TRUE)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.parameters[0].dynamic", Matchers.hasToString(TRUE)));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.content.parameters[1].dynamic", Matchers.hasToString(FALSE)));

        performPost(PluginController.PLUGINS_PLUGINID_CONFIGS, token, aPluginConfiguration, expectations,
                    "unable to save a plugin configuration", aPluginConfiguration.getPluginId());
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_210")
    @Purpose("When a HTTP request POST is successed, the HTTP return code is 201")
    public void savePluginConfigurationMissinAParameter() {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                pluginParameters, 0);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isCreated());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.pluginId",
                                                        Matchers.hasToString(aPluginConfiguration.getPluginId())));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.version", Matchers.hasToString(VERSION)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.active", Matchers.hasToString(TRUE)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.parameters[0].dynamic", Matchers.hasToString(TRUE)));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.content.parameters[1].dynamic", Matchers.hasToString(FALSE)));

        performPost(PluginController.PLUGINS_PLUGINID_CONFIGS, token, aPluginConfiguration, expectations,
                    "unable to save a plugin configuration", aPluginConfiguration.getPluginId());
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_230")
    @Purpose("If a HTTP request POST is unsopported or mal-formatted, the HTTP return code is 400")
    public void savePluginConfigurationErrorConfNull() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isBadRequest());
        performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS, null, expectations,
                           "unable to save a plugin configuration", "badPluginId");
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_210")
    @Purpose("When a HTTP request DELETE is successed, the HTTP return code is 204")
    public void deletePluginConfiguration() throws ModuleException {
        PluginConfiguration aPluginConfiguration = createPluginConfiguration(LABEL);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNoContent());
        performDefaultDelete(PluginController.PLUGINS_PLUGINID_CONFIGID, expectations,
                             "unable to delete a plugin configuration", aPluginConfiguration.getPluginId(),
                             aPluginConfiguration.getId());
    }

    private PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setPluginClassName(SamplePlugin.class.getCanonicalName());
        // pluginMetaData.setInterfaceName(ISamplePlugin.class.getCanonicalName());
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
    private PluginConfiguration createPluginConfiguration(String label) throws ModuleException {
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), label,
                pluginParameters, 0);
        return pluginService.savePluginConfiguration(aPluginConfiguration);
    }

}
