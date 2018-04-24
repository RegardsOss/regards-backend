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
package fr.cnes.regards.modules.access.services.rest.ui;

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

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestParamBuilder;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;

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
        if (UIPluginTypesEnum.SERVICE.equals(pType)) {
            plugin.setApplicationModes(Sets.newHashSet(ServiceScope.ONE, ServiceScope.MANY));
            plugin.setEntityTypes(Sets.newHashSet(EntityType.COLLECTION, EntityType.DATA));
        }
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
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(10)));
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
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(9)));
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
        // 7 default plugins + 4 created during this test
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(11)));
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
