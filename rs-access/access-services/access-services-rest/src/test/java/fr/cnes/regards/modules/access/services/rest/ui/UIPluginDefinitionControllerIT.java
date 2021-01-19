/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.access.services.domain.ui.UIDefaultPluginEnum;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;

/**
 * Class InstanceLayoutControllerIT
 *
 * IT Tests for REST Controller
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=access" })
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
        final UIPluginDefinition plugin = UIPluginDefinition.build("PluginTest", "plugins/test/bundle.js", pType);
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
     * Test retrieve all plugis
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testGetAllPlugins() {
        // default plugins + 3 created during this test
        performDefaultGet(UIPluginDefinitionController.REQUEST_MAPPING_ROOT,
                          customizer().expectStatusOk().expectToHaveSize("$.content",
                                                                         UIDefaultPluginEnum.values().length + 3),
                          "Error getting all plugins");
    }

    /**
     * Test retrieve all plugins by type
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testGetPluginsByType() {
        // default plugins + 2 created during this test
        performDefaultGet(UIPluginDefinitionController.REQUEST_MAPPING_ROOT,
                          customizer().expectStatusOk()
                                  .expectToHaveSize("$.content", UIDefaultPluginEnum.values().length + 2)
                                  .addParameter("type", UIPluginTypesEnum.CRITERIA.toString()),
                          "Error getting all criteria plugins");

        performDefaultGet(UIPluginDefinitionController.REQUEST_MAPPING_ROOT,
                          customizer().expectStatusOk().expectToHaveSize("$.content", 1)
                                  .addParameter("type", UIPluginTypesEnum.SERVICE.toString()),
                          "Error getting all criteria plugins");
    }

    /**
     * Test to retrieve one plugin
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testGetOnePlugin() {
        performDefaultGet(UIPluginDefinitionController.REQUEST_MAPPING_ROOT
                + UIPluginDefinitionController.REQUEST_MAPPING_PLUGIN_DEFINITION, customizer().expectStatusOk(),
                          "Error getting one plugin", plugin.getId());
    }

    /**
     * Test to delete one plugin
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testDeleteOnePlugin() {
        performDefaultDelete(UIPluginDefinitionController.REQUEST_MAPPING_ROOT
                + UIPluginDefinitionController.REQUEST_MAPPING_PLUGIN_DEFINITION, customizer().expectStatusOk(),
                             "Error deleting one theme", plugin.getId());

        performDefaultGet(UIPluginDefinitionController.REQUEST_MAPPING_ROOT
                + UIPluginDefinitionController.REQUEST_MAPPING_PLUGIN_DEFINITION, customizer().expectStatusNotFound(),
                          "Error retrieving plugin", plugin.getId());
    }

    /**
     * Test to save a new theme
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testSavePlugin() {
        final UIPluginDefinition plugin = createPlugin(UIPluginTypesEnum.SERVICE);
        performDefaultPost(UIPluginDefinitionController.REQUEST_MAPPING_ROOT, plugin, customizer().expectStatusOk(),
                           "Error saving new plugin");

        // 8 default plugins + 4 created during this test
        performDefaultGet(UIPluginDefinitionController.REQUEST_MAPPING_ROOT,
                          customizer().expectStatusOk()
                                  .expectToHaveSize("$.content", UIDefaultPluginEnum.values().length + 4)
                                  .addParameter("size", "20"),
                          "Error getting all plugins");
    }

    /**
     * Test to update a plugin
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testUpdatePlugin() {
        plugin.setSourcePath("plugins/new/bundle.js");
        performDefaultPut(UIPluginDefinitionController.REQUEST_MAPPING_ROOT
                + UIPluginDefinitionController.REQUEST_MAPPING_PLUGIN_DEFINITION, plugin, customizer().expectStatusOk(),
                          "Error saving new theme", plugin.getId());
    }

}
