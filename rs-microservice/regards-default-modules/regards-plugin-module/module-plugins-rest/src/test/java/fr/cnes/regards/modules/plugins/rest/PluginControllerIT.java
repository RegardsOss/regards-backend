/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
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

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.modules.plugins.service.IPluginService;
import fr.cnes.regards.plugins.IComplexInterfacePlugin;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 * 
 * @author Christophe Mertz
 *
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

    private static final String LABEL = "a plugin configuraion for the test";

    private static final List<PluginParameter> PARAMETERS = PluginParametersFactory.build()
            .addParameterDynamic("param31", "value31").addParameter("param32", "value32")
            .addParameter("param33", "value33").addParameter("param34", "value34").addParameter("param35", "value35")
            .getParameters();

    /**
     * 
     */
    @Autowired
    private IPluginService pluginService;

    private final String apiPlugins = "/plugins";

    private final String apiPluginsWithParamPluginType = "/plugins" + "?pluginType=";

    private final String apiPluginTypes = "/plugintypes";

    private final String apiPluginsOnePluginId = "/plugins/{pluginId}";

    private final String apiPluginsAllConfig = apiPluginsOnePluginId + "/config";

    private final String apiPluginsOneConfigId = apiPluginsAllConfig + "/{configId}";

    @Test
    public void getAllPlugins() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        performDefaultGet(apiPlugins, expectations, "unable to load all plugins");
    }

    @Test
    public void getPluginOneType() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        final String pluginType = IComplexInterfacePlugin.class.getCanonicalName();
        performDefaultGet(apiPluginsWithParamPluginType + pluginType, expectations,
                          String.format("unable to load plugins of type <%s>", pluginType));
    }

    @Test
    public void getPluginOneUnknownType() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isBadRequest());
        final String pluginType = "hello";
        performDefaultGet(apiPluginsWithParamPluginType + pluginType, expectations,
                          String.format("unable to load plugins of unknown type", pluginType));
    }

    @Test
    public void getOnePlugin() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        final String pluginId = pluginService.getPlugins().get(0).getPluginId();
        performDefaultGet(apiPluginsOnePluginId, expectations, String.format("unable to load plugin id <%s>", pluginId),
                          pluginId);
    }

    @Test
    public void getAllPluginTypes() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.*", Matchers.hasSize(pluginService.getPluginTypes().size())));
        performDefaultGet(apiPluginTypes, expectations, "unable to load all plugin types");
    }

    @Test
    @DirtiesContext
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
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        // expectations.add(MockMvcResultMatchers.jsonPath("$..content.pluginId", Matchers.hasToString(PLUGIN_ID)));
        expectations.add(MockMvcResultMatchers.jsonPath("$..content.active", Matchers.hasToString("[true]")));
        expectations.add(MockMvcResultMatchers.jsonPath("$..content.parameters[0].dynamic",
                                                        Matchers.hasToString("[true]")));
        expectations.add(MockMvcResultMatchers.jsonPath("$..content.parameters[1].dynamic",
                                                        Matchers.hasToString("[false]")));

        performDefaultGet(apiPluginsAllConfig, expectations,
                          "unable to load all plugin configuration of a specific type", PLUGIN_ID);
    }

    @Test
    @DirtiesContext
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
        final Long configId = AN_ID;
        final String pluginId = PLUGIN_ID;
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        performDefaultGet(apiPluginsOneConfigId, expectations, "unable to load a plugin configuration", pluginId,
                          configId);
    }

    @Test
    @DirtiesContext
    public void getPluginConfigurationError() {
        // Get an unknown PluginConfiguration
        final Long configId = AN_ID;
        final String pluginId = PLUGIN_ID;
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());
        performDefaultGet(apiPluginsOneConfigId, expectations, "unable to load a plugin configuration", pluginId,
                          configId);
    }

    @Test
    @DirtiesContext
    public void updatePluginConfiguration() {
        // Add a PluginConfiguration with the PluginService
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
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.pluginId", Matchers.hasToString(PLUGIN_ID)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.version", Matchers.hasToString(VERSION)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.active", Matchers.hasToString("true")));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.content.parameters[0].dynamic", Matchers.hasToString("true")));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.content.parameters[1].dynamic", Matchers.hasToString("false")));

        // Update the added PluginConfiguration
        performDefaultPut(apiPluginsOneConfigId, aPluginConfiguration, expectations,
                          "unable to update a plugin configuration", PLUGIN_ID, aPluginConfiguration.getId());
    }

    @Test
    @DirtiesContext
    public void updatePluginConfigurationErrorPluginId() {
        // Add a PluginConfiguration with the PluginService
        PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL, PARAMETERS,
                0);
        try {
            aPluginConfiguration = pluginService.savePluginConfiguration(aPluginConfiguration);
            aPluginConfiguration.setId(AN_ID);
        } catch (PluginUtilsException e) {
            Assert.fail();
        }

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());

        // Update the added PluginConfiguration
        performDefaultPut(apiPluginsOneConfigId, aPluginConfiguration, expectations,
                          "unable to update a plugin configuration", PLUGIN_ID, 9999L);
    }

    @Test
    @DirtiesContext
    public void updateUnknownPluginConfigurationError() {
        // Add a PluginConfiguration with the PluginService
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                PARAMETERS, 0);
        aPluginConfiguration.setId(AN_ID);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());

        // Update the added PluginConfiguration
        performDefaultPut(apiPluginsOneConfigId, aPluginConfiguration, expectations,
                          "unable to update a plugin configuration", PLUGIN_ID, AN_ID);
    }

    @Test
    @DirtiesContext
    public void savePluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                PARAMETERS, 0);

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

        performDefaultPost(apiPluginsAllConfig, aPluginConfiguration, expectations,
                           "unable to save a plugin configuration", PLUGIN_ID);
    }

    @Test
    @DirtiesContext
    public void savePluginConfigurationError() {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                PARAMETERS, 0);

        aPluginConfiguration.setPluginId(null);
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isBadRequest());
        performDefaultPost(apiPluginsAllConfig, aPluginConfiguration, expectations,
                           "unable to save a plugin configuration", PLUGIN_ID);
    }

    @Test
    @DirtiesContext
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
        performDefaultDelete(apiPluginsOneConfigId, expectations, "unable to delete a plugin configuration", PLUGIN_ID,
                             aPluginConfiguration.getId());
    }

    @Test
    @DirtiesContext
    public void deletePluginConfigurationError() {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(this.getPluginMetaData(), LABEL,
                PARAMETERS, 0);
        aPluginConfiguration.setId(AN_ID);
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());
        performDefaultDelete(apiPluginsOneConfigId, expectations, "unable to delete a plugin configuration", PLUGIN_ID,
                             aPluginConfiguration.getId());
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
