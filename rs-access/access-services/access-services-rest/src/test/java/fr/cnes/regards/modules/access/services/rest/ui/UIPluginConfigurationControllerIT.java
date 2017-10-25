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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestParamBuilder;
import fr.cnes.regards.modules.access.services.dao.ui.ILinkUIPluginsDatasetsRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginConfigurationRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.access.services.domain.ui.LinkUIPluginsDatasets;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.models.domain.Model;

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

    private UIPluginConfiguration createPluginConf(final UIPluginDefinition pPluginDef, final Boolean pIsActive,
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
        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS, expectations,
                          "Error getting all plugins");

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT, Matchers.hasSize(2)));
        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS, expectations,
                          "Error getting all active plugins", RequestParamBuilder.build().param("isActive", "true"));

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT, Matchers.hasSize(2)));
        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS, expectations,
                          "Error getting all linked plugins",
                          RequestParamBuilder.build().param("isLinkedToAllEntities", "true"));

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT, Matchers.hasSize(1)));

        final RequestParamBuilder builder = RequestParamBuilder.build().param("isActive", "true");
        builder.param("isLinkedToAllEntities", "true");
        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS, expectations,
                          "Error getting all active and linked plugins", builder);

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT, Matchers.hasSize(1)));

        final RequestParamBuilder builder2 = RequestParamBuilder.build().param("type",
                                                                               UIPluginTypesEnum.SERVICE.toString());
        performDefaultGet(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS, expectations,
                          "Error getting all active and linked plugins", builder2);

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
        performDefaultPost(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS, conf, expectations,
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
                             "Error getting all plugins", pluginConf.getId());
    }

    /**
     *
     * Test endpoint to delete a UIPluginConfiguration
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void deletePluginConfiguration_shouldSucceedWhenLinkedToDataset() {
        Dataset dataset = new Dataset(new Model(), "tenant", "label");
        LinkUIPluginsDatasets link = new LinkUIPluginsDatasets(dataset.getIpId().toString(),
                Lists.newArrayList(pluginConf, pluginConf1, pluginConf2));
        linkRepository.save(link);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultDelete(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATION, expectations,
                             "Error getting all plugins", pluginConf.getId());
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
        performDefaultPost(UIPluginConfigurationController.REQUEST_MAPPING_ROOT
                + UIPluginConfigurationController.REQUEST_PLUGIN_CONFIGURATIONS, conf, expectations,
                           "Error getting all plugins");

    }

}
