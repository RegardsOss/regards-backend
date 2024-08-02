/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.access.services.dao.ui.ILinkUIPluginsDatasetsRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginConfigurationRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.access.services.domain.ui.LinkUIPluginsDatasets;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.model.domain.Model;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Class UIPluginConfigurationControllerIT
 * <p>
 * IT Tests for REST Controller
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=access" })
public class UIPluginConfigurationControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(UIPluginConfigurationControllerIT.class);

    @Autowired
    private IUIPluginDefinitionRepository pluginDefRepository;

    @Autowired
    private IUIPluginConfigurationRepository repository;

    @Autowired
    private ILinkUIPluginsDatasetsRepository linkRepository;

    private UIPluginDefinition plugin;

    private UIPluginConfiguration pluginConf;

    private UIPluginConfiguration pluginConf1;

    private UIPluginConfiguration pluginConf2;

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

    private UIPluginConfiguration createPluginConf(final UIPluginDefinition pPluginDef,
                                                   final Boolean pIsActive,
                                                   final Boolean pIsLinked) {
        final UIPluginConfiguration conf = new UIPluginConfiguration();
        conf.setActive(pIsActive);
        conf.setLinkedToAllEntities(pIsLinked);
        conf.setPluginDefinition(pPluginDef);
        conf.setConf("{}");
        conf.setLabel("label");
        return conf;
    }

    @Before
    public void init() {

        // Create plugin definitions
        plugin = pluginDefRepository.save(createPlugin(UIPluginTypesEnum.CRITERIA));
        final UIPluginDefinition plugin2 = pluginDefRepository.save(createPlugin(UIPluginTypesEnum.SERVICE));

        // Create plugin Configurations
        pluginConf = repository.save(createPluginConf(plugin, true, false));
        pluginConf1 = repository.save(createPluginConf(plugin, true, true));
        pluginConf2 = repository.save(createPluginConf(plugin, false, true));
        repository.save(createPluginConf(plugin2, false, false));
    }

    /**
     * Test endpoint to retrieve UIPluginConfigurations with parameters isActive or/and isLinkedToAllEntities
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveConfigurations() {
        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                          + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS,
                          customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 4),
                          "Error getting all plugins");

        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                          + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS,
                          customizer().expectStatusOk()
                                      .expectToHaveSize(JSON_PATH_CONTENT, 2)
                                      .addParameter("isActive", "true"),
                          "Error getting all active plugins");

        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                          + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS,
                          customizer().expectStatusOk()
                                      .expectToHaveSize(JSON_PATH_CONTENT, 2)
                                      .addParameter("isLinkedToAllEntities", "true"),
                          "Error getting all linked plugins");

        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                          + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS,
                          customizer().expectStatusOk()
                                      .expectToHaveSize(JSON_PATH_CONTENT, 1)
                                      .addParameter("isActive", "true")
                                      .addParameter("isLinkedToAllEntities", "true"),
                          "Error getting all active and linked plugins");

        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                          + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS,
                          customizer().expectStatusOk()
                                      .expectToHaveSize(JSON_PATH_CONTENT, 1)
                                      .addParameter("type", UIPluginTypesEnum.SERVICE.toString()),
                          "Error getting all active and linked plugins");

    }

    /**
     * Test endpoint to retrieve all UIPluginConfiguration associated to a ginve UIPluginDefinition
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveConfigurationByPluginDefinition() {
        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                          + UIPluginConfigurationController.REQUEST_PLUGIN_DEFINITION,
                          customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 3),
                          "Error getting all plugins",
                          plugin.getId());
    }

    /**
     * Test endpoint to update a UIPluginConfiguration
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void updatePluginConfiguration() {

        final UIPluginConfiguration conf = createPluginConf(plugin, true, true);
        conf.setId(pluginConf.getId());

        performDefaultPut(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                          + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATION,
                          conf,
                          customizer().expectStatusOk()
                                      .expectValue(JSON_PATH_CONTENT + ".id", conf.getId().intValue())
                                      .expectValue(JSON_PATH_CONTENT + ".active", conf.getActive().booleanValue())
                                      .expectValue(JSON_PATH_CONTENT + ".linkedToAllEntities",
                                                   conf.getLinkedToAllEntities().booleanValue()),
                          "Error getting all plugins",
                          conf.getId());

    }

    /**
     * Test endpoint to create a new UIPluginConfiguration
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void createPluginConfiguration() {

        final UIPluginConfiguration conf = createPluginConf(plugin, true, true);
        conf.setConf("{\"param\":\"value\"}");

        performDefaultPost(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                           + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS,
                           conf,
                           customizer().expectStatusOk()
                                       .expectValue(JSON_PATH_CONTENT + ".active", conf.getActive().booleanValue())
                                       .expectValue(JSON_PATH_CONTENT + ".conf", conf.getConf())
                                       .expectValue(JSON_PATH_CONTENT + ".linkedToAllEntities",
                                                    conf.getLinkedToAllEntities().booleanValue()),
                           "Error getting all plugins");

    }

    /**
     * Test endpoint to delete a UIPluginConfiguration
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void deletePluginConfiguration() {
        performDefaultDelete(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                             + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATION,
                             customizer().expectStatusOk(),
                             "Error getting all plugins",
                             pluginConf.getId());
    }

    /**
     * Test endpoint to delete a UIPluginConfiguration
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void deletePluginConfiguration_shouldSucceedWhenLinkedToDataset() {
        Dataset dataset = new Dataset(new Model(), "tenant", "providerId", "label");
        LinkUIPluginsDatasets link = new LinkUIPluginsDatasets(dataset.getIpId().toString(),
                                                               Lists.newArrayList(pluginConf,
                                                                                  pluginConf1,
                                                                                  pluginConf2));
        linkRepository.save(link);

        performDefaultDelete(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                             + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATION,
                             customizer().expectStatusOk(),
                             "Error getting all plugins",
                             pluginConf.getId());
    }

    /**
     * Test endpoint to check that bad JSON format are refused.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void badPluginConfiguration() {

        final UIPluginConfiguration conf = createPluginConf(plugin, true, true);
        conf.setConf("{invalidJson");
        performDefaultPost(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                           + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS,
                           conf,
                           customizer().expect(status().is(422)),
                           "Error getting all plugins");

    }

}
