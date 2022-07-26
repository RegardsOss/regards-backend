/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.engine;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineConfiguration;
import fr.cnes.regards.modules.search.rest.SearchEngineConfigurationController;
import fr.cnes.regards.modules.search.service.engine.plugin.legacy.LegacySearchEngine;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Set;
import java.util.UUID;

/**
 * {@link SearchEngineConfiguration} tests
 *
 * @author Sébastien Binda
 */
@TestPropertySource(locations = { "classpath:test.properties" },
    properties = { "regards.tenant=opensearch", "spring.jpa.properties.hibernate.default_schema=opensearch" })
@MultitenantTransactional
public class SearchEngineConfigurationIT extends AbstractEngineIT {

    @Test
    public void retrieveConfs() {
        RequestBuilderCustomizer customizer = customizer();
        customizer.expect(MockMvcResultMatchers.status().isOk());
        customizer.addParameter("page", "0");
        customizer.addParameter("size", "10");
        // 2 conf initialized in test + 1 conf initialized by default in tenant
        customizer.expectValue("$.metadata.totalElements", 3);
        performDefaultGet(SearchEngineConfigurationController.TYPE_MAPPING, customizer, "Search all error");
    }

    @Test
    public void retrieveConfByEngine() {
        RequestBuilderCustomizer customizer = customizer();
        customizer.expect(MockMvcResultMatchers.status().isOk());
        customizer.addParameter("page", "0");
        customizer.addParameter("size", "10");
        customizer.addParameter(SearchEngineConfigurationController.ENGINE_TYPE, LegacySearchEngine.PLUGIN_ID);
        customizer.expectValue("$.metadata.totalElements", 1);
        performDefaultGet(SearchEngineConfigurationController.TYPE_MAPPING, customizer, "Search by engine type error");
    }

    @Test
    public void createConf() {
        RequestBuilderCustomizer customizer = customizer();
        customizer.expect(MockMvcResultMatchers.status().isCreated());

        Set<IPluginParam> parameters = IPluginParam.set();
        PluginConfiguration pluginConf = PluginConfiguration.build(LegacySearchEngine.class, null, parameters);

        SearchEngineConfiguration conf = new SearchEngineConfiguration();
        conf.setLabel("Test create new search engine");
        conf.setConfiguration(pluginConf);
        conf.setDatasetUrn("URN:AIP:" + EntityType.DATASET.toString() + ":PROJECT:" + UUID.randomUUID() + ":V1");
        performDefaultPost(SearchEngineConfigurationController.TYPE_MAPPING,
                           conf,
                           customizer,
                           "Search by engine type error");

        // Try to create same conf. Should be error.
        customizer = customizer();
        customizer.expect(MockMvcResultMatchers.status().isUnprocessableEntity());
        performDefaultPost(SearchEngineConfigurationController.TYPE_MAPPING,
                           conf,
                           customizer,
                           "The service must not allow to create two same conf.");
    }

    @Test
    public void updateConf() {
        RequestBuilderCustomizer customizer = customizer();
        customizer.expect(MockMvcResultMatchers.status().isOk());
        openSearchEngineConf.setDatasetUrn("URN:AIP:"
                                           + EntityType.DATASET.toString()
                                           + ":PROJECT:"
                                           + UUID.randomUUID()
                                           + ":V2");
        performDefaultPut(SearchEngineConfigurationController.TYPE_MAPPING
                          + SearchEngineConfigurationController.CONF_ID_PATH,
                          openSearchEngineConf,
                          customizer,
                          "Search by engine type error",
                          openSearchEngineConf.getId());

        customizer = customizer();
        customizer.expect(MockMvcResultMatchers.status().isUnprocessableEntity());
        performDefaultPut(SearchEngineConfigurationController.TYPE_MAPPING
                          + SearchEngineConfigurationController.CONF_ID_PATH,
                          openSearchEngineConf,
                          customizer,
                          "Search by engine type error",
                          0L);
    }

    @Test
    public void deleteConf() {
        RequestBuilderCustomizer customizer = customizer();
        customizer.expect(MockMvcResultMatchers.status().isOk());
        performDefaultGet(SearchEngineConfigurationController.TYPE_MAPPING
                          + SearchEngineConfigurationController.CONF_ID_PATH,
                          customizer,
                          "Conf should exist",
                          openSearchEngineConf.getId());

        customizer = customizer();
        customizer.expect(MockMvcResultMatchers.status().isOk());
        performDefaultDelete(SearchEngineConfigurationController.TYPE_MAPPING
                             + SearchEngineConfigurationController.CONF_ID_PATH,
                             customizer,
                             "Search all error",
                             openSearchEngineConf.getId());

        customizer = customizer();
        customizer.expect(MockMvcResultMatchers.status().isNotFound());
        performDefaultGet(SearchEngineConfigurationController.TYPE_MAPPING
                          + SearchEngineConfigurationController.CONF_ID_PATH,
                          customizer,
                          "Conf should deleted",
                          openSearchEngineConf.getId());

        // Check plugin configuration always exists as another search engine is associated to
        try {
            pluginService.getPluginConfiguration(openSearchEngineConf.getConfiguration().getBusinessId());
        } catch (EntityNotFoundException e) {
            Assert.fail("Plugin Configuration should not be deleted as another SearchEngine is associated to");
        }

        customizer = customizer();
        customizer.expect(MockMvcResultMatchers.status().isOk());
        performDefaultDelete(SearchEngineConfigurationController.TYPE_MAPPING
                             + SearchEngineConfigurationController.CONF_ID_PATH,
                             customizer,
                             "Search all error",
                             openSearchEngineConfForOneDataset.getId());

        customizer = customizer();
        customizer.expect(MockMvcResultMatchers.status().isNotFound());
        performDefaultGet(SearchEngineConfigurationController.TYPE_MAPPING
                          + SearchEngineConfigurationController.CONF_ID_PATH,
                          customizer,
                          "Conf should deleted",
                          openSearchEngineConfForOneDataset.getId());

        // Check plugin configuration is also delete
        try {
            pluginService.getPluginConfiguration(openSearchEngineConfForOneDataset.getConfiguration().getBusinessId());
            Assert.fail("Plugin Configuration should be deleted as no other SearchEngine is associated to");
        } catch (EntityNotFoundException e) {
            // Nothing to do.
        }

    }

    @Override
    protected String getDefaultRole() {
        return DefaultRole.PROJECT_ADMIN.toString();
    }

}
