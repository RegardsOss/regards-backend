/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestParamBuilder;
import fr.cnes.regards.modules.configuration.dao.IUIPluginConfigurationRepository;
import fr.cnes.regards.modules.configuration.dao.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.configuration.domain.UIPluginConfiguration;
import fr.cnes.regards.modules.configuration.domain.UIPluginDefinition;
import fr.cnes.regards.modules.configuration.domain.UIPluginTypesEnum;

/**
 *
 * Class UIPluginConfigurationControllerIT
 *
 * IT Tests for REST Controller
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class UIPluginConfigurationControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(UIPluginConfigurationControllerIT.class);

    @Autowired
    private IUIPluginDefinitionRepository pluginDefRepository;

    @Autowired
    private IUIPluginConfigurationRepository repository;

    private UIPluginDefinition plugin;

    private UIPluginConfiguration pluginConf;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    private UIPluginDefinition createPlugin(final UIPluginTypesEnum pType) {
        final UIPluginDefinition plugin = new UIPluginDefinition();
        plugin.setName("PluginTest");
        plugin.setType(pType);
        plugin.setSourcePath("plugins/test/bundle.js");
        return plugin;
    }

    private UIPluginConfiguration createPluginConf(final UIPluginDefinition pPluginDef, final Boolean pIsActive,
            final Boolean pIsLinked) {
        final UIPluginConfiguration conf = new UIPluginConfiguration();
        conf.setActive(pIsActive);
        conf.setLinkedToAllEntities(pIsLinked);
        conf.setPluginDefinition(pPluginDef);
        conf.setConf("{}");
        return conf;
    }

    @Before
    public void init() {

        // Create plugin definitions
        plugin = createPlugin(UIPluginTypesEnum.CRITERIA);
        final UIPluginDefinition plugin2 = createPlugin(UIPluginTypesEnum.SERVICE);
        pluginDefRepository.save(plugin);
        pluginDefRepository.save(plugin2);

        // Create plugin Configurations
        pluginConf = repository.save(createPluginConf(plugin, true, false));
        repository.save(createPluginConf(plugin, true, true));
        repository.save(createPluginConf(plugin, false, true));
        repository.save(createPluginConf(plugin2, false, false));
    }

    /**
     *
     * Test endpoint to retrieve UIPluginConfigurations with parameters isActive or/and isLinkedToAllEntities
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveConfigurations() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT, Matchers.hasSize(4)));
        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT, expectations,
                          "Error getting all plugins");

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT, Matchers.hasSize(2)));
        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT, expectations,
                          "Error getting all active plugins", RequestParamBuilder.build().param("isActive", "true"));

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT, Matchers.hasSize(2)));
        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT, expectations,
                          "Error getting all linked plugins",
                          RequestParamBuilder.build().param("isLinkedToAllEntities", "true"));

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT, Matchers.hasSize(1)));

        final RequestParamBuilder builder = RequestParamBuilder.build().param("isActive", "true");
        builder.param("isLinkedToAllEntities", "true");
        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT, expectations,
                          "Error getting all active and linked plugins", builder);

    }

    /**
     *
     * Test endpoint to retrieve all UIPluginConfiguration associated to a ginve UIPluginDefinition
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveConfigurationByPluginDefinition() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT, Matchers.hasSize(3)));
        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                + UIPluginConfigurationController.REQUEST_PLUGIN_DEFINITION, expectations, "Error getting all plugins",
                          plugin.getId());
    }

    /**
     *
     * Test endpoint to update a UIPluginConfiguration
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void updatePluginConfiguration() {

        final UIPluginConfiguration conf = createPluginConf(plugin, true, true);
        conf.setId(pluginConf.getId());

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".id")
                .value(Matchers.equalTo(conf.getId().intValue())));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".active")
                .value(Matchers.equalTo(conf.getActive().booleanValue())));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".linkedToAllEntities")
                .value(Matchers.equalTo(conf.getLinkedToAllEntities().booleanValue())));
        performDefaultPut(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATION, conf, expectations,
                          "Error getting all plugins", conf.getId());

    }

    /**
     *
     * Test endpoint to create a new UIPluginConfiguration
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void createPluginConfiguration() {

        final UIPluginConfiguration conf = createPluginConf(plugin, true, true);
        conf.setConf("{\"param\":\"value\"}");

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".active")
                .value(Matchers.equalTo(conf.getActive().booleanValue())));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".linkedToAllEntities")
                .value(Matchers.equalTo(conf.getLinkedToAllEntities().booleanValue())));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".conf")
                .value(Matchers.equalTo(conf.getConf())));
        performDefaultPost(UIPluginConfigurationController.REQUEST_MAPPING_ROOT, conf, expectations,
                           "Error getting all plugins");

    }

    /**
     *
     * Test endpoint to delete a UIPluginConfiguration
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void deletePluginConfiguration() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultDelete(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATION, expectations,
                             "Error getting all plugins", plugin.getId());
    }

    /**
     *
     * Test endpoint to check that bad JSON format are refused.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void badPluginConfiguration() {

        final UIPluginConfiguration conf = createPluginConf(plugin, true, true);
        conf.setConf("{invalidJson");
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().is(422));
        performDefaultPost(UIPluginConfigurationController.REQUEST_MAPPING_ROOT, conf, expectations,
                           "Error getting all plugins");

    }

}
