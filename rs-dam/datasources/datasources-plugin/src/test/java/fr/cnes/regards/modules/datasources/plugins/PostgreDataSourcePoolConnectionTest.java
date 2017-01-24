/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.datasources.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.modules.datasources.plugins.plugintypes.IDBConnectionPlugin;
import fr.cnes.regards.modules.datasources.utils.PostgreDataSourcePluginTestConfiguration;
import fr.cnes.regards.plugins.utils.PluginUtils;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { PostgreDataSourcePluginTestConfiguration.class })
@ComponentScan(basePackages = { "fr.cnes.regards.modules.datasources.utils" })
public class PostgreDataSourcePoolConnectionTest {

    private static final Logger LOG = LoggerFactory.getLogger(PostgreDataSourcePoolConnectionTest.class);

    private static final String PLUGIN_CURRENT_PACKAGE = "fr.cnes.regards.modules.datasources.plugins";

    @Value("${datasource.url}")
    private String url;

    @Value("${datasource.username}")
    private String user;

    @Value("${datasource.password}")
    private String password;

    @Value("${datasource.driver}")
    private String driver;

    private IPluginConfigurationRepository pluginConfRepositoryMocked;

    private IPluginService pluginServiceMocked;

    @Before
    public void setUp() {
        // create a mock repository
        pluginConfRepositoryMocked = Mockito.mock(IPluginConfigurationRepository.class);
        pluginServiceMocked = new PluginService(pluginConfRepositoryMocked);
        pluginServiceMocked.addPluginPackage("fr.cnes.regards.modules.datasources.plugins");
    }

    @Test
    public void testPoolConnectionWithGetFirstPluginByType() throws PluginUtilsException {
        // Save a PluginConfiguration
        final Long anId = 33L;
        final PluginConfiguration aPluginConfiguration = getPostGreSqlConnectionConfiguration();
        aPluginConfiguration.setId(anId);
        Mockito.when(pluginConfRepositoryMocked.save(aPluginConfiguration)).thenReturn(aPluginConfiguration);
        pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);

        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        pluginConfs.add(aPluginConfiguration);

        Mockito.when(pluginConfRepositoryMocked
                .findByPluginIdOrderByPriorityOrderDesc(DefaultPostgreSQLConnectionPlugin.class.getCanonicalName()))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId())).thenReturn(aPluginConfiguration);

        // Get the first Plugin
        final DefaultPostgreSQLConnectionPlugin aa = pluginServiceMocked
                .getFirstPluginByType(IDBConnectionPlugin.class);

        Assert.assertNotNull(aa);
        Assert.assertTrue(aa.testConnection());

        // Get the first Plugin : the same than the previous
        final DefaultPostgreSQLConnectionPlugin bb = pluginServiceMocked
                .getFirstPluginByType(IDBConnectionPlugin.class);

        Assert.assertNotNull(bb);
        Assert.assertTrue(bb.testConnection());
        Assert.assertEquals(aa, bb);
    }

    @Test
    public void testPoolConnectionWithGetPlugin() throws PluginUtilsException {
        // Save a PluginConfiguration
        final Long anId = 33L;
        final PluginConfiguration aPluginConfiguration = getPostGreSqlConnectionConfiguration();
        aPluginConfiguration.setId(anId);
        Mockito.when(pluginConfRepositoryMocked.save(aPluginConfiguration)).thenReturn(aPluginConfiguration);
        pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);

        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        pluginConfs.add(aPluginConfiguration);

        Mockito.when(pluginConfRepositoryMocked
                .findByPluginIdOrderByPriorityOrderDesc(DefaultPostgreSQLConnectionPlugin.class.getCanonicalName()))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId())).thenReturn(aPluginConfiguration);

        // Get a Plugin for a specific configuration
        final DefaultPostgreSQLConnectionPlugin aa = pluginServiceMocked.getPlugin(anId);

        Assert.assertNotNull(aa);
        Assert.assertTrue(aa.testConnection());

        // Get a Plugin for a specific configuration
        final DefaultPostgreSQLConnectionPlugin bb = pluginServiceMocked.getPlugin(anId);

        Assert.assertNotNull(bb);
        Assert.assertTrue(bb.testConnection());
        Assert.assertEquals(aa, bb);
    }

    @After
    public void erase() {
        // repository.deleteAll();
    }

    /**
     * Define the {@link PluginConfiguration} for a {@link DefaultPostgreSQLConnectionPlugin} to connect to the
     * PostgreSql database
     * 
     * @return the {@link PluginConfiguration}
     */
    private PluginConfiguration getPostGreSqlConnectionConfiguration() {
        final List<PluginParameter> params = PluginParametersFactory.build()
                .addParameter(DefaultPostgreSQLConnectionPlugin.USER, user)
                .addParameter(DefaultPostgreSQLConnectionPlugin.PASSWORD, password)
                .addParameter(DefaultPostgreSQLConnectionPlugin.URL, url)
                .addParameter(DefaultPostgreSQLConnectionPlugin.DRIVER, driver)
                .addParameter(DefaultPostgreSQLConnectionPlugin.MAX_POOLSIZE, "3")
                .addParameter(DefaultPostgreSQLConnectionPlugin.MIN_POOLSIZE, "1").getParameters();

        try {
            return PluginUtils.getPluginConfiguration(params, DefaultPostgreSQLConnectionPlugin.class,
                                                      Arrays.asList(PLUGIN_CURRENT_PACKAGE));
        } catch (PluginUtilsException e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

}
