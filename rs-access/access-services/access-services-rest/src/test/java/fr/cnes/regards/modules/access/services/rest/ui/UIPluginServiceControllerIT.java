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
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestParamBuilder;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.access.services.dao.ui.ILinkUIPluginsDatasetsRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginConfigurationRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.access.services.domain.ui.LinkUIPluginsDatasets;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.models.domain.EntityType;

/**
 *
 * Class UIPluginServiceControllerIT
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class UIPluginServiceControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(UIPluginServiceControllerIT.class);

    @Autowired
    private IUIPluginDefinitionRepository pluginDefRepository;

    @Autowired
    private ILinkUIPluginsDatasetsRepository linkRepo;

    @Autowired
    private IUIPluginConfigurationRepository repository;

    private UIPluginDefinition plugin;

    private UIPluginConfiguration pluginConf;

    private UIPluginConfiguration pluginConf2;

    private UIPluginConfiguration pluginConf3;

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
        pluginConf = repository.save(createPluginConf(plugin2, true, false));
        pluginConf2 = repository.save(createPluginConf(plugin2, true, false));
        repository.save(createPluginConf(plugin2, false, true));
        pluginConf3 = repository.save(createPluginConf(plugin2, false, false));
        // Add an active service plugin conf linked to all datasets
        repository.save(createPluginConf(plugin2, true, true));
        // Add an active plugin conf linked to all datasets but not a service type
        repository.save(createPluginConf(plugin, true, true));
        // Add an inactive service plugin conf linked to all datasets
        repository.save(createPluginConf(plugin2, false, true));

        // Link plugins to datasets
        final LinkUIPluginsDatasets link = new LinkUIPluginsDatasets();
        link.setDatasetId("firstOne");
        final List<UIPluginConfiguration> confs = new ArrayList<>();
        confs.add(pluginConf);
        link.setServices(confs);
        linkRepo.save(link);

        final LinkUIPluginsDatasets link2 = new LinkUIPluginsDatasets();
        link2.setDatasetId("second");
        final List<UIPluginConfiguration> confs2 = new ArrayList<>();
        confs2.add(pluginConf);
        confs2.add(pluginConf2);
        link2.setServices(confs2);
        linkRepo.save(link2);

        final LinkUIPluginsDatasets link3 = new LinkUIPluginsDatasets();
        link3.setDatasetId("third");
        final List<UIPluginConfiguration> confs3 = new ArrayList<>();
        confs3.add(pluginConf);
        confs3.add(pluginConf2);
        confs3.add(pluginConf3);
        link3.setServices(confs3);
        linkRepo.save(link3);
    }

    /**
     * One active plugin associated to second dataset
     * One service plugin associated to all dataset
     */
    @Test
    @Requirement("REGARDS_DSL_ACC_ADM_1530")
    @Purpose("Check retrieve dataset associated UIPluginServices")
    public void retrieveDatasetLinkedPlugins_1() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);

        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT, Matchers.hasSize(2)));
        RequestParamBuilder builder = RequestParamBuilder.build().param("dataset_id", "firstOne");
        performDefaultGet(UIPluginServiceController.REQUEST_MAPPING_ROOT, expectations,
                          "Error getting dataset linked UIPluginConfiguration", builder);
    }

    /**
     * Two active plugin associated to second dataset
     * One service plugin associated to all dataset
     */
    @Test
    @Requirement("REGARDS_DSL_ACC_ADM_1530")
    @Purpose("Check retrieve dataset associated UIPluginServices")
    public void retrieveDatasetLinkedPlugins_2() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);

        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT, Matchers.hasSize(3)));
        RequestParamBuilder builder = RequestParamBuilder.build().param("dataset_id", "second");
        performDefaultGet(UIPluginServiceController.REQUEST_MAPPING_ROOT, expectations,
                          "Error getting dataset linked UIPluginConfiguration", builder);

    }

    /**
     * Only Two active plugin associated to third dataset
     * One service plugin associated to all dataset
     */
    @Test
    @Requirement("REGARDS_DSL_ACC_ADM_1530")
    @Purpose("Check retrieve dataset associated UIPluginServices")
    public void retrieveDatasetLinkedPlugins_3() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);

        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT, Matchers.hasSize(3)));
        RequestParamBuilder builder = RequestParamBuilder.build().param("dataset_id", "third");
        performDefaultGet(UIPluginServiceController.REQUEST_MAPPING_ROOT, expectations,
                          "Error getting dataset linked UIPluginConfiguration", builder);

    }

    /**
     * One service plugin associated to all dataset
     */
    @Test
    @Requirement("REGARDS_DSL_ACC_ADM_1530")
    @Purpose("Check retrieve dataset associated UIPluginServices")
    public void retrieveDatasetLinkedPlugins_4() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);

        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT, Matchers.hasSize(1)));
        RequestParamBuilder builder = RequestParamBuilder.build().param("dataset_id", "unknown");
        performDefaultGet(UIPluginServiceController.REQUEST_MAPPING_ROOT, expectations,
                          "Error getting dataset linked UIPluginConfiguration", builder);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
