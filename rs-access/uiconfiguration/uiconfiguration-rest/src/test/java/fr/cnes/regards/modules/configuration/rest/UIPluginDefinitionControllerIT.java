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
import fr.cnes.regards.modules.configuration.dao.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.configuration.domain.UIPluginDefinition;
import fr.cnes.regards.modules.configuration.domain.UIPluginTypesEnum;

/**
 *
 * Class InstanceLayoutControllerIT
 *
 * IT Tests for REST Controller
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class UIPluginDefinitionControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(UIPluginDefinitionControllerIT.class);

    @Autowired
    private IUIPluginDefinitionRepository repository;

    private UIPluginDefinition plugin;

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

    @Before
    public void init() {
        plugin = createPlugin(UIPluginTypesEnum.CRITERIA);
        final UIPluginDefinition plugin2 = createPlugin(UIPluginTypesEnum.CRITERIA);
        final UIPluginDefinition plugin3 = createPlugin(UIPluginTypesEnum.SERVICE);
        repository.save(plugin);
        repository.save(plugin2);
        repository.save(plugin3);
    }

    /**
     *
     * Test retrieve all plugis
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testGetAllPlugins() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        // 6 default plugins + 3 created during this test
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(9)));
        performDefaultGet(UIPluginDefinitionController.REQUEST_MAPPING_ROOT, expectations, "Error getting all plugins");
    }

    /**
     *
     * Test retrieve all plugins by type
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testGetPluginsByType() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        // 6 default plugins + 2 created during this test
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(8)));
        performDefaultGet(UIPluginDefinitionController.REQUEST_MAPPING_ROOT, expectations,
                          "Error getting all criteria plugins",
                          RequestParamBuilder.build().param("type", UIPluginTypesEnum.CRITERIA.toString()));

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(1)));
        performDefaultGet(UIPluginDefinitionController.REQUEST_MAPPING_ROOT, expectations,
                          "Error getting all criteria plugins",
                          RequestParamBuilder.build().param("type", UIPluginTypesEnum.SERVICE.toString()));
    }

    /**
     * Test to retrieve one plugin
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testGetOnePlugin() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet(UIPluginDefinitionController.REQUEST_MAPPING_ROOT
                + UIPluginDefinitionController.REQUEST_MAPPING_PLUGIN_DEFINITION, expectations,
                          "Error getting one plugin", plugin.getId());
    }

    /**
     * Test to delete one plugin
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testDeleteOnePlugin() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultDelete(UIPluginDefinitionController.REQUEST_MAPPING_ROOT
                + UIPluginDefinitionController.REQUEST_MAPPING_PLUGIN_DEFINITION, expectations,
                             "Error deleting one theme", plugin.getId());

        expectations.clear();
        expectations.add(status().isNotFound());
        performDefaultGet(UIPluginDefinitionController.REQUEST_MAPPING_ROOT
                + UIPluginDefinitionController.REQUEST_MAPPING_PLUGIN_DEFINITION, expectations,
                          "Error retrieving plugin", plugin.getId());
    }

    /**
     * Test to save a new theme
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testSavePlugin() {
        final UIPluginDefinition plugin = createPlugin(UIPluginTypesEnum.SERVICE);
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultPost(UIPluginDefinitionController.REQUEST_MAPPING_ROOT, plugin, expectations,
                           "Error saving new plugin");

        expectations.clear();
        expectations.add(status().isOk());
        // 6 default plugins + 4 created during this test
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(10)));
        performDefaultGet(UIPluginDefinitionController.REQUEST_MAPPING_ROOT, expectations, "Error getting all plugins");
    }

    /**
     * Test to update a plugin
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testUpdatePlugin() {
        plugin.setSourcePath("plugins/new/bundle.js");
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultPut(UIPluginDefinitionController.REQUEST_MAPPING_ROOT
                + UIPluginDefinitionController.REQUEST_MAPPING_PLUGIN_DEFINITION, plugin, expectations,
                          "Error saving new theme", plugin.getId());
    }

}
