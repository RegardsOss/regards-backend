/*
 * LICENSE_PLACEHOLDER
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
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.search.domain.ServiceScope;
import fr.cnes.regards.modules.search.service.link.ILinkPluginsDatasetsService;
import fr.cnes.regards.plugins.utils.PluginUtilsRuntimeException;

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
