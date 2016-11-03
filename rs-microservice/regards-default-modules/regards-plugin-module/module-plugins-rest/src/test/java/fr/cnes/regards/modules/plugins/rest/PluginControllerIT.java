package fr.cnes.regards.modules.plugins.rest;

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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.modules.plugins.service.IPluginService;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class PluginControllerIT extends AbstractRegardsIT {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginControllerIT.class);

    static final String VERSION = "12345-6789-11";

    static final String PLUGIN_ID = "a-plugin-id";

    static final String AUTHOR = "CS-SI-DEV";

    static final Long AN_ID = 050L;

    static final String LABEL = "a plugin configuraion for the test";

    static final List<PluginParameter> PARAMETERS = PluginParametersFactory.build()
            .addParameterDynamic("param31", "value31").addParameter("param32", "value32")
            .addParameter("param33", "value33").addParameter("param34", "value34").addParameter("param35", "value35")
            .getParameters();

    /**
     * Utility service for handling JWT. Autowired by Spring.
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Method authorization service.Autowired by Spring.
     */
    @Autowired
    private MethodAuthorizationService authService;

    /**
     * The jwt string
     */
    private String jwt;

    /**
     * 
     */
    @Autowired
    private IPluginService pluginService;

    private final String apiPluginsRoot = "/plugins";

    private final String apiPluginTypes = "/plugintypes";

    private final String apiPluginsConfig = apiPluginsRoot + "/{pluginId}" + "/config";

    private final String apiPluginsConfigId = apiPluginsConfig + "/{configId}";

    /**
     * Do some setup before each test
     */
    @Before
    public void init() {
        final String tenant = "test-1";
        jwt = jwtService.generateToken(tenant, "cmz@c-s.fr", "CMZ", "ADMIN");
        authService.setAuthorities(tenant, apiPluginsRoot, RequestMethod.GET, "ADMIN");
        authService.setAuthorities(tenant, apiPluginTypes, RequestMethod.GET, "ADMIN");
        authService.setAuthorities(tenant, apiPluginsConfig, RequestMethod.GET, "ADMIN");
        authService.setAuthorities(tenant, apiPluginsConfigId, RequestMethod.GET, "ADMIN");
    }

    // @Test
    // public void getAllPluginsRest() {
    // final List<ResultMatcher> expectations = new ArrayList<>(1);
    // expectations.add(status().isOk());
    // performGet("/plugins", jwt, expectations, "unable to load all plugins");
    // }

    @Test
    public void getAllPluginTypesRest() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.*", Matchers.hasSize(pluginService.getPluginTypes().size())));
        performGet(apiPluginTypes, jwt, expectations, "unable to load all plugin types");
    }

    @Test
    public void getPluginConfigurationsByTypeWithPluginId() {
        // Add a PluginConfiguration with the PluginService
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL, PARAMETERS,
                0);
        try {
            aPluginConfiguration = pluginService.savePluginConfiguration(aPluginConfiguration);
            aPluginConfiguration.setId(AN_ID);
        } catch (PluginUtilsException e) {
            Assert.fail();
        }

        // Get all the PluginConfiguration with the a specific ID
        final List<ResultMatcher> expectations = new ArrayList<>();
        final String pluginId = PLUGIN_ID;
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$.[0].pluginId", Matchers.hasToString(pluginId)));
        performGet(apiPluginsConfig, jwt, expectations, "unable to load all plugin configuration of a specific type",
                   pluginId);
    }

    @Test
    public void getPluginConfiguration() {
        // Add a PluginConfiguration with the PluginService
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL, PARAMETERS,
                0);
        try {
            aPluginConfiguration = pluginService.savePluginConfiguration(aPluginConfiguration);
            aPluginConfiguration.setId(AN_ID);
        } catch (PluginUtilsException e) {
            Assert.fail();
        }

        // Get the added PluginConfiguration
        final String configId = AN_ID.toString();
        final String pluginId = PLUGIN_ID;
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$.id", Matchers.hasToString(configId)));
        performGet(apiPluginsConfigId, jwt, expectations, "unable to load a plugin configuration", pluginId, configId);
    }

    @Test
    public void updatePluginConfiguration() {
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL, PARAMETERS,
                0);
        try {
            aPluginConfiguration = pluginService.savePluginConfiguration(aPluginConfiguration);
            aPluginConfiguration.setId(AN_ID);
        } catch (PluginUtilsException e) {
            Assert.fail();
        }
        performPut(apiPluginsConfigId, jwt, aPluginConfiguration, getExpectations(),
                   "unable to update a plugin configuration", PLUGIN_ID, aPluginConfiguration.getId().toString());
    }

    @Test
    public void savePluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                PARAMETERS, 0);
        performPost(apiPluginsConfig, jwt, aPluginConfiguration, getExpectations(),
                    "unable to save a plugin configuration", PLUGIN_ID);
    }

    @Test
    public void deletePluginConfiguration() {
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL, PARAMETERS,
                0);
        try {
            aPluginConfiguration = pluginService.savePluginConfiguration(aPluginConfiguration);
            aPluginConfiguration.setId(AN_ID);
        } catch (PluginUtilsException e) {
            Assert.fail();
        }
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        performDelete(apiPluginsConfigId, jwt, expectations, "unable to delete a plugin configuration", PLUGIN_ID,
                      aPluginConfiguration.getId().toString());
    }

    private List<ResultMatcher> getExpectations() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$.pluginId", Matchers.hasToString(PLUGIN_ID)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.version", Matchers.hasToString(VERSION)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.parameters.[0].isDynamic", Matchers.hasToString("true")));
        expectations.add(MockMvcResultMatchers.jsonPath("$.parameters.[1].isDynamic", Matchers.hasToString("false")));
        return expectations;
    }

    private PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setClass(Integer.class);
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
