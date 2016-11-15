/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.dao.EmptyResultDataAccessException;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.plugins.IComplexInterfacePlugin;
import fr.cnes.regards.plugins.INotInterfacePlugin;
import fr.cnes.regards.plugins.SamplePlugin;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 *
 * Unit testing of {@link PluginService}.
 *
 * @author Christophe Mertz
 */
public class PluginServiceFailedTest extends PluginServiceUtility {

    /**
     * 
     */
    private IPluginConfigurationRepository pluginConfRepositoryMocked;

    /**
     * 
     */
    private IPluginService pluginServiceMocked;

    /**
     * This method is run before all tests
     */
    @Before
    public void init() {
        // create a mock repository
        pluginConfRepositoryMocked = Mockito.mock(IPluginConfigurationRepository.class);
        pluginServiceMocked = new PluginService(pluginConfRepositoryMocked);
    }

    /**
     * Get an unsaved {@link PluginConfiguration}.
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void getAPluginConfigurationUnknown() throws PluginUtilsException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        Mockito.when(pluginConfRepositoryMocked.findOne(null)).thenReturn(null);

        pluginServiceMocked.getPluginConfiguration(aPluginConfiguration.getId());
    }

    /**
     * Delete an unsaved {@link PluginConfiguration}.
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void deleteAPluginConfigurationUnknown() throws PluginUtilsException {
        final Long aPluginId = 56789L;
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginId)).thenReturn(false);
        Mockito.doThrow(EmptyResultDataAccessException.class).when(pluginConfRepositoryMocked).delete(aPluginId);
        pluginServiceMocked.deletePluginConfiguration(aPluginId);
    }

    /**
     * Save a null {@link PluginConfiguration}.
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void saveANullPluginConfiguration() throws PluginUtilsException {
        pluginServiceMocked.savePluginConfiguration(null);
        Assert.fail();
    }

    /**
     * Save a {@link PluginConfiguration} without pluginId attribute.
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void saveAPluginConfigurationWithoutPluginId() throws PluginUtilsException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setPluginId(null);
        pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);
        Assert.fail();
    }

    /**
     * Save a {@link PluginConfiguration} without priorityOrder attribute.
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void saveAPluginConfigurationWithoutPriorityOrder() throws PluginUtilsException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setPriorityOrder(null);
        pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);
        Assert.fail();
    }

    /**
     * Save a {@link PluginConfiguration} without priorityOrder attribute.
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void saveAPluginConfigurationWithoutVersion() throws PluginUtilsException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setVersion(null);
        pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);
        Assert.fail();
    }

    /**
     * Save a {@link PluginConfiguration} without parameters.
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    public void saveAPluginConfigurationWithoutParameters() throws PluginUtilsException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithoutParameters();
        final PluginConfiguration savedPluginConfiguration = pluginServiceMocked
                .savePluginConfiguration(aPluginConfiguration);
        Assert.assertNotNull(savedPluginConfiguration);
        Assert.assertEquals(aPluginConfiguration.getLabel(), savedPluginConfiguration.getLabel());
        Assert.assertEquals(aPluginConfiguration.getPluginId(), savedPluginConfiguration.getPluginId());
        Assert.assertEquals(aPluginConfiguration.isActive(), savedPluginConfiguration.isActive());
        Assert.assertEquals(aPluginConfiguration.getParameters().size(),
                            savedPluginConfiguration.getParameters().size());
    }

    /**
     * Update an unsaved {@link PluginConfiguration}
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void updateAPluginConfigurationUnknown() throws PluginUtilsException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        final Long aPluginId = 999L;
        aPluginConfiguration.setId(aPluginId);
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(false);

        pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);
        Assert.fail();
    }

    @Test
    public void getPluginMetaDataByIdUnknown() {
        final PluginMetaData pluginMetaData = pluginServiceMocked.getPluginMetaDataById("hello world");
        Assert.assertNull(pluginMetaData);
    }

    @Test(expected = PluginUtilsException.class)
    public void getFirstPluginByTypeNullPluginConf() throws PluginUtilsException {
        pluginServiceMocked.getFirstPluginByType(INotInterfacePlugin.class);
        Assert.fail();
    }

    /**
     * Get the first plugin of a specific type with a dynamic parameter. Used the default value for the dynamic
     * parameter.
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test
    public void getAPluginWithBadVersionConfiguration() throws PluginUtilsException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setVersion(BLUE);
        aPluginConfiguration.setId(AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithParameters());

        Mockito.when(pluginConfRepositoryMocked.findByPluginIdOrderByPriorityOrderDesc(PLUGIN_PARAMETER_ID))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId())).thenReturn(aPluginConfiguration);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(IComplexInterfacePlugin.class);

        Assert.assertNotNull(aSamplePlugin);
        Assert.assertTrue(aSamplePlugin.echo(HELLO).contains(RED));
    }

    /**
     * Error to get a plugin with a configuration that is not the most priority.
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void getFirstPluginTheMostPrioritaryError() throws PluginUtilsException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();

        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setPriorityOrder(2);
        aPluginConfiguration.setId(AN_ID);

        final PluginConfiguration bPluginConfiguration = getPluginConfigurationWithParameters();
        // this conf is the most priority
        bPluginConfiguration.setPriorityOrder(1);
        aPluginConfiguration.setId(1 + AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(bPluginConfiguration);

        Mockito.when(pluginConfRepositoryMocked.findByPluginIdOrderByPriorityOrderDesc(PLUGIN_PARAMETER_ID))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findOne(bPluginConfiguration.getId())).thenReturn(null);

        pluginServiceMocked.getFirstPluginByType(IComplexInterfacePlugin.class);

        Assert.fail();
    }

    /**
     * Error to get a plugin with a configuration that is not active.
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    @Requirement("REGARDS_DSL_CMP_PLG_100")
    @Purpose("Unable to load a plugin with a no active configuration")
    public void getPluginNotActiveConfiguration() throws PluginUtilsException {

        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithoutParameters();
        aPluginConfiguration.setIsActive(Boolean.FALSE);
        aPluginConfiguration.setId(AN_ID);

        Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId())).thenReturn(aPluginConfiguration);

        pluginServiceMocked.getPlugin(AN_ID);

        Assert.fail();
    }

}
