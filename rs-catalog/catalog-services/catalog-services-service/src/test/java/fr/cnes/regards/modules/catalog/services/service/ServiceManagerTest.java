/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.catalog.services.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.catalog.services.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.catalog.services.service.link.ILinkPluginsDatasetsService;

/**
 * Unit test for {@link ServiceManager}
 *
 * @author Xavier-Alexandre Brochard
 */
public class ServiceManagerTest {

    /**
     * Initialize a set of {@link PluginConfiguration} where the plugin class name is wrong on purpose
     */
    private static final Set<PluginConfiguration> PLUGIN_CONFIGURATIONS_WRONG_PLUGIN = new HashSet<>();
    static {
        //This plugin being non-existent, lets create PluginMetadata by hand and put some random values.
        final PluginMetaData fakePluginMeta = new PluginMetaData();
        fakePluginMeta.setPluginClassName("FakePlugin");
        PluginConfiguration fakePluginConf = new PluginConfiguration("First configuration", "FakePluginId");
        fakePluginConf.setMetaData(fakePluginMeta);
        PLUGIN_CONFIGURATIONS_WRONG_PLUGIN.add(fakePluginConf);
    };

    /**
     * Initialize a set of {@link PluginConfiguration}
     */
    private static final Set<PluginConfiguration> PLUGIN_CONFIGURATIONS = new HashSet<>();
    static {
        // Thanks to mockito, we have to put plugin metadata ourselves and call PluginUtils.setup
        PluginUtils.setup();
        PluginMetaData exampleOneManyMeta = PluginUtils.getPlugins().get(ExampleOneManyPlugin.class.getAnnotation(Plugin.class).id());
        PluginConfiguration exampleOneManyConf = new PluginConfiguration("First configuration", exampleOneManyMeta.getPluginId());
        exampleOneManyConf.setMetaData(exampleOneManyMeta);
        PLUGIN_CONFIGURATIONS.add(exampleOneManyConf);
        PluginMetaData exampleManyMeta = PluginUtils.getPlugins().get(ExampleManyPlugin.class.getAnnotation(Plugin.class).id());
        PluginConfiguration exampleManyConf = new PluginConfiguration("Second configuration", exampleManyMeta.getPluginId());
        exampleManyConf.setMetaData(exampleManyMeta);
        PLUGIN_CONFIGURATIONS.add(exampleManyConf);
        PluginMetaData exampleOneMeta = PluginUtils.getPlugins().get(ExampleOnePlugin.class.getAnnotation(Plugin.class).id());
        PluginConfiguration exampleOneConf = new PluginConfiguration("Third configuration", exampleOneMeta.getPluginId());
        exampleOneConf.setMetaData(exampleOneMeta);
        PLUGIN_CONFIGURATIONS.add(exampleOneConf);
    };

    /**
     * Class under test
     */
    private ServiceManager serviceManager;

    /**
     * The service managing plugins
     */
    private IPluginService pluginService;

    /**
     * Service linking plugins with datasets
     */
    private ILinkPluginsDatasetsService linkPluginsDatasetsService;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        pluginService = Mockito.mock(IPluginService.class);
        linkPluginsDatasetsService = Mockito.mock(ILinkPluginsDatasetsService.class);
        serviceManager = new ServiceManager(pluginService, linkPluginsDatasetsService);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.catalog.services.service.ServiceManager#retrieveServices(List, List)}.
     *
     * @throws EntityNotFoundException
     */
    @Test(expected = PluginUtilsRuntimeException.class)
    public final void testRetrieveServices_shouldThrowPluginUtilsRuntimeException() throws EntityNotFoundException {
        // Prepare test
        final LinkPluginsDatasets linkPluginsDatasets = new LinkPluginsDatasets("test",
                PLUGIN_CONFIGURATIONS_WRONG_PLUGIN);
        Mockito.when(linkPluginsDatasetsService.retrieveLink(Mockito.anyString())).thenReturn(linkPluginsDatasets);

        // Trigger exception
        serviceManager.retrieveServices(Arrays.asList("test"), Arrays.asList(ServiceScope.ONE));
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.catalog.services.service.ServiceManager#retrieveServices(List, List)}.
     */
    @Test
    public final void testRetrieveServices() {
        String datasetId = "aSampleServicePlugin";
        // Prepare test
        final LinkPluginsDatasets linkPluginsDatasets = new LinkPluginsDatasets(datasetId, PLUGIN_CONFIGURATIONS);
        Mockito.when(linkPluginsDatasetsService.retrieveLink(Mockito.anyString())).thenReturn(linkPluginsDatasets);

        // Call tested method
        List<PluginConfigurationDto> pluginConfigurationDtos = serviceManager
                .retrieveServices(Arrays.asList(datasetId), Arrays.asList(ServiceScope.ONE));

        // Define expected
        Set<ServiceScope> expectedApplicationModes = Sets.newHashSet(ServiceScope.ONE);
        Set<EntityType> expectedEntityTypes = Sets.newHashSet(EntityType.DATA);

        // Check
        Assert.assertThat(pluginConfigurationDtos, Matchers.hasSize(2));
        Assert.assertThat(pluginConfigurationDtos, Matchers
                .hasItem(Matchers.hasProperty("applicationModes", Matchers.equalTo(expectedApplicationModes))));
        Assert.assertThat(pluginConfigurationDtos,
                          Matchers.hasItem(Matchers.hasProperty("entityTypes", Matchers.equalTo(expectedEntityTypes))));

        // Call tested method
        pluginConfigurationDtos = serviceManager.retrieveServices(Arrays.asList(datasetId),
                                                                  Arrays.asList(ServiceScope.MANY));

        // Define expected
        expectedApplicationModes = Sets.newHashSet(ServiceScope.MANY);
        expectedEntityTypes = Sets.newHashSet(EntityType.DATA);

        // Check
        Assert.assertThat(pluginConfigurationDtos, Matchers.hasSize(2));
        Assert.assertThat(pluginConfigurationDtos, Matchers
                .hasItem(Matchers.hasProperty("applicationModes", Matchers.equalTo(expectedApplicationModes))));
        Assert.assertThat(pluginConfigurationDtos,
                          Matchers.hasItem(Matchers.hasProperty("entityTypes", Matchers.equalTo(expectedEntityTypes))));

        // Call tested method
        pluginConfigurationDtos = serviceManager.retrieveServices(Arrays.asList(datasetId),
                                                                  Arrays.asList(ServiceScope.MANY, ServiceScope.ONE));

        // Define expected
        expectedApplicationModes = Sets.newHashSet(ServiceScope.ONE, ServiceScope.MANY);
        expectedEntityTypes = Sets.newHashSet(EntityType.DATA);

        // Check
        Assert.assertThat(pluginConfigurationDtos, Matchers.hasSize(1));
        Assert.assertThat(pluginConfigurationDtos, Matchers
                .hasItem(Matchers.hasProperty("applicationModes", Matchers.equalTo(expectedApplicationModes))));
        Assert.assertThat(pluginConfigurationDtos,
                          Matchers.hasItem(Matchers.hasProperty("entityTypes", Matchers.equalTo(expectedEntityTypes))));
    }

}
