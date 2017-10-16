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
package fr.cnes.regards.modules.search.service;

import java.util.HashSet;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.search.domain.ServiceScope;
import fr.cnes.regards.modules.search.service.link.ILinkPluginsDatasetsService;

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
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setPluginClassName("FakePlugin");
        PLUGIN_CONFIGURATIONS_WRONG_PLUGIN.add(new PluginConfiguration(pluginMetaData, "First configuration"));
    };

    /**
     * Initialize a set of {@link PluginConfiguration}
     */
    private static final Set<PluginConfiguration> PLUGIN_CONFIGURATIONS = new HashSet<>();
    static {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setPluginClassName("fr.cnes.regards.modules.search.service.utils.TestService");
        PLUGIN_CONFIGURATIONS.add(new PluginConfiguration(pluginMetaData, "First configuration"));
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
     * {@link fr.cnes.regards.modules.search.service.ServiceManager#retrieveServices(java.lang.Long, fr.cnes.regards.modules.search.domain.ServiceScope)}.
     *
     * @throws EntityNotFoundException
     */
    @SuppressWarnings("unchecked")
    @Test(expected = EntityNotFoundException.class)
    public final void testRetrieveServices_shouldThrowEntityNotFoundException() throws EntityNotFoundException {
        // Prepare test
        Mockito.when(linkPluginsDatasetsService.retrieveLink(Mockito.anyString()))
                .thenThrow(EntityNotFoundException.class);

        // Trigger exception
        serviceManager.retrieveServices("test", ServiceScope.ONE);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.search.service.ServiceManager#retrieveServices(java.lang.Long, fr.cnes.regards.modules.search.domain.ServiceScope)}.
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
        serviceManager.retrieveServices("test", ServiceScope.ONE);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.search.service.ServiceManager#retrieveServices(java.lang.Long, fr.cnes.regards.modules.search.domain.ServiceScope)}.
     *
     * @throws EntityNotFoundException
     */
    @Test
    public final void testRetrieveServices() throws EntityNotFoundException {
        // Prepare test
        final LinkPluginsDatasets linkPluginsDatasets = new LinkPluginsDatasets("test", PLUGIN_CONFIGURATIONS);
        Mockito.when(linkPluginsDatasetsService.retrieveLink(Mockito.anyString())).thenReturn(linkPluginsDatasets);

        // Call tested method
        final Set<PluginConfiguration> retrieveServices = serviceManager.retrieveServices("test", ServiceScope.MANY);

        // Check
        Assert.assertThat(retrieveServices, Matchers.is(Matchers.emptyCollectionOf(PluginConfiguration.class)));
    }

}
