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
import fr.cnes.regards.modules.configuration.dao.IPluginRepository;
import fr.cnes.regards.modules.configuration.domain.Plugin;

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
public class PluginControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PluginControllerIT.class);

    @Autowired
    private IPluginRepository repository;

    private Plugin plugin;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    private Plugin createPlugin() {
        final Plugin plugin = new Plugin();
        plugin.setName("PluginTest");
        plugin.setType("criteria");
        plugin.setSourcePath("plugins/test/bundle.js");
        return plugin;
    }

    @Before
    public void init() {
        plugin = createPlugin();
        final Plugin plugin2 = createPlugin();
        final Plugin plugin3 = createPlugin();
        repository.save(plugin);
        repository.save(plugin2);
        repository.save(plugin3);
    }

    /**
     *
     * Test retrieve all themes
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testGetAllPlugins() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(3)));
        performDefaultGet("/plugins", expectations, "Error getting all plugins");
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
        performDefaultGet("/plugins/{pluginId}", expectations, "Error getting one plugin", plugin.getId());
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
        performDefaultDelete("/plugins/{pluginId}", expectations, "Error deleting one theme", plugin.getId());

        expectations.clear();
        expectations.add(status().isNotFound());
        performDefaultGet("/plugins/{pluginId}", expectations, "Error retrieving plugin", plugin.getId());
    }

    /**
     * Test to save a new theme
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testSavePlugin() {
        final Plugin plugin = createPlugin();
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultPost("/plugins", plugin, expectations, "Error saving new plugin");

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(4)));
        performDefaultGet("/plugins", expectations, "Error getting all plugins");
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
        performDefaultPut("/plugins/{pluginId}", plugin, expectations, "Error saving new theme", plugin.getId());
    }

}
