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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;

import fr.cnes.regards.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.plugins.IComplexInterfacePlugin;
import fr.cnes.regards.plugins.INotInterfacePlugin;
import fr.cnes.regards.plugins.SamplePlugin;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 *
 * Unit testing of {@link PluginService}.
 *
 * @author cmertz
 */
public class PluginServiceTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginServiceTest.class);

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
        try {
            pluginServiceMocked = new PluginService(pluginConfRepositoryMocked);
        } catch (PluginUtilsException e) {
            LOGGER.error("Error in the init method", e);
        }
    }

    @Test
    public void getAllPlugins() {
        final List<PluginMetaData> metadaDatas = pluginServiceMocked.getPlugins();

        Assert.assertNotNull(metadaDatas);
        Assert.assertFalse(metadaDatas.isEmpty());

        LOGGER.debug("List all plugins :");
        metadaDatas.forEach(p -> LOGGER.debug(p.getPluginId()));
    }

    @Test
    public void getAllPluginTypes() {
        final List<String> types = pluginServiceMocked.getPluginTypes();

        Assert.assertNotNull(types);
        Assert.assertFalse(types.isEmpty());

        LOGGER.debug("List all plugin types :");
        types.forEach(p -> LOGGER.debug(p));
    }

    @Test
    public void getPluginTypes() {
        final List<PluginMetaData> plugins = pluginServiceMocked.getPluginsByType(IComplexInterfacePlugin.class);

        Assert.assertNotNull(plugins);
        Assert.assertFalse(plugins.isEmpty());

        LOGGER.debug("List all plugins of type <IComplexInterfacePlugin.class> :");
        plugins.forEach(p -> LOGGER.debug(p.getPluginId()));
    }

    @Test
    public void getPluginTypesByString() {
        final String aClass = "fr.cnes.regards.plugins.IComplexInterfacePlugin";
        List<PluginMetaData> plugins = null;

        try {
            plugins = pluginServiceMocked.getPluginsByType(Class.forName(aClass));
        } catch (ClassNotFoundException e) {
            Assert.fail();
        }

        Assert.assertNotNull(plugins);
        Assert.assertFalse(plugins.isEmpty());

        LOGGER.debug("List all plugins of type <IComplexInterfacePlugin.class> :");
        plugins.forEach(p -> LOGGER.debug(p.getPluginId()));
    }

    /**
     * Get a {@link PluginConfiguration}
     */
    @Test
    public void getAPluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(PluginServiceUtility.AN_ID);
        Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId())).thenReturn(aPluginConfiguration);

        try {
            final PluginConfiguration pCon = pluginServiceMocked.getPluginConfiguration(aPluginConfiguration.getId());
            Assert.assertEquals(pCon.getLabel(), aPluginConfiguration.getLabel());

        } catch (PluginUtilsException e) {
            Assert.fail();
        }
    }

    /**
     * Get an unsaved {@link PluginConfiguration}
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void getAPluginConfigurationUnknown() throws PluginUtilsException {
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        Mockito.when(pluginConfRepositoryMocked.findOne(null)).thenReturn(null);

        pluginServiceMocked.getPluginConfiguration(aPluginConfiguration.getId());
    }

    /**
     * Delete a {@link PluginConfiguration}
     */
    @Test
    public void deleteAPluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(PluginServiceUtility.AN_ID);
        try {
            pluginServiceMocked.deletePluginConfiguration(aPluginConfiguration.getId());
            Mockito.verify(pluginConfRepositoryMocked).delete(aPluginConfiguration.getId());
        } catch (PluginUtilsException e) {
            Assert.fail();
        }
    }

    /**
     * Delete an unsaved {@link PluginConfiguration}
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void deleteAPluginConfigurationUnknown() throws PluginUtilsException {
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(PluginServiceUtility.AN_ID);
        Mockito.doThrow(EmptyResultDataAccessException.class).when(pluginConfRepositoryMocked)
                .delete(aPluginConfiguration.getId());
        pluginServiceMocked.deletePluginConfiguration(aPluginConfiguration.getId());
    }

    /**
     * Delete a {@link PluginConfiguration}
     */
    @Test
    public void saveAPluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        final PluginConfiguration aPluginConfigurationWithId = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        aPluginConfigurationWithId.setId(PluginServiceUtility.AN_ID);
        try {
            Mockito.when(pluginConfRepositoryMocked.save(aPluginConfiguration)).thenReturn(aPluginConfigurationWithId);
            final PluginConfiguration savedPluginConfiguration = pluginServiceMocked
                    .savePluginConfiguration(aPluginConfiguration);
            Assert.assertEquals(aPluginConfiguration.getLabel(), savedPluginConfiguration.getLabel());
            Assert.assertEquals(aPluginConfiguration.getPluginId(), savedPluginConfiguration.getPluginId());

        } catch (PluginUtilsException e) {
            Assert.fail();
        }
    }

    /**
     * Delete a {@link PluginConfiguration} without pluginId attribute
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
     * Delete a {@link PluginConfiguration} without pluginId attribute
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void saveAPluginConfigurationWithoutPluginId() throws PluginUtilsException {
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        aPluginConfiguration.setPluginId(null);
        pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);
        Assert.fail();
    }

    /**
     * Delete a {@link PluginConfiguration} without priorityOrder attribute
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void saveAPluginConfigurationWithoutPriorityOrder() throws PluginUtilsException {
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        aPluginConfiguration.setPriorityOrder(null);
        pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);
        Assert.fail();
    }

    /**
     * Delete a {@link PluginConfiguration} without priorityOrder attribute
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void saveAPluginConfigurationWithoutVersion() throws PluginUtilsException {
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        aPluginConfiguration.setVersion(null);
        pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);
        Assert.fail();
    }

    /**
     * Update a {@link PluginConfiguration}
     */
    @Test
    public void updateAPluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(PluginServiceUtility.AN_ID);
        try {
            Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId()))
                    .thenReturn(aPluginConfiguration);
            Mockito.when(pluginConfRepositoryMocked.save(aPluginConfiguration)).thenReturn(aPluginConfiguration);

            final PluginConfiguration pCon = pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);
            Assert.assertEquals(pCon.getLabel(), aPluginConfiguration.getLabel());
            Assert.assertEquals(pCon.getPluginId(), aPluginConfiguration.getPluginId());
        } catch (PluginUtilsException e) {
            Assert.fail();
        }
    }

    /**
     * Update an unsaved {@link PluginConfiguration}
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void updateAPluginConfigurationUnknown() throws PluginUtilsException {
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(PluginServiceUtility.AN_ID);
        Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId())).thenReturn(null);

        pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);
        Assert.fail();
    }

    @Test
    public void getPluginMetaDataById() {
        final PluginMetaData pluginMetaData = pluginServiceMocked.getPluginMetaDataById("aSamplePlugin");
        Assert.assertNotNull(pluginMetaData);
    }

    @Test
    public void getPluginMetaDataByIdUnknown() {
        final PluginMetaData pluginMetaData = pluginServiceMocked.getPluginMetaDataById("hello world");
        Assert.assertNull(pluginMetaData);
    }

    @Test
    public void getPluginConfigurationsByTypeWithClass() {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        pluginConfs.add(PluginServiceUtility.getInstance().getPluginConfigurationWithParameters());
        pluginConfs.add(PluginServiceUtility.getInstance().getPluginConfigurationWithDynamicParameter());
        Mockito.when(pluginConfRepositoryMocked
                .findByPluginIdOrderByPriorityOrderDesc(PluginServiceUtility.PLUGIN_PARAMETER_ID))
                .thenReturn(pluginConfs);
        final List<PluginConfiguration> results = pluginServiceMocked
                .getPluginConfigurationsByType(IComplexInterfacePlugin.class);

        Assert.assertNotNull(results);
        Assert.assertEquals(pluginConfs.size(), results.size());
    }

    @Test
    public void getPluginConfigurationsByTypeWithPluginId() {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        pluginConfs.add(PluginServiceUtility.getInstance().getPluginConfigurationWithParameters());
        pluginConfs.add(PluginServiceUtility.getInstance().getPluginConfigurationWithDynamicParameter());
        Mockito.when(pluginConfRepositoryMocked
                .findByPluginIdOrderByPriorityOrderDesc(PluginServiceUtility.PLUGIN_PARAMETER_ID))
                .thenReturn(pluginConfs);
        final List<PluginConfiguration> results = pluginServiceMocked
                .getPluginConfigurationsByType(PluginServiceUtility.PLUGIN_PARAMETER_ID);

        Assert.assertNotNull(results);
        Assert.assertEquals(pluginConfs.size(), results.size());
    }

    @Test(expected = PluginUtilsException.class)
    public void getFirstPluginByTypeNullPluginConf() throws PluginUtilsException {
        pluginServiceMocked.getFirstPluginByType(INotInterfacePlugin.class);
        Assert.assertTrue(true);
    }

    /**
     * Get the first plugin of a specific type
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test
    public void getFirstPluginByType() throws PluginUtilsException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(PluginServiceUtility.AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(PluginServiceUtility.getInstance().getPluginConfigurationWithDynamicParameter());

        Mockito.when(pluginConfRepositoryMocked
                .findByPluginIdOrderByPriorityOrderDesc(PluginServiceUtility.PLUGIN_PARAMETER_ID))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId())).thenReturn(aPluginConfiguration);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(IComplexInterfacePlugin.class);

        Assert.assertNotNull(aSamplePlugin);

        final int result = aSamplePlugin.add(PluginServiceUtility.QUATRE, PluginServiceUtility.CINQ);
        LOGGER.debug(PluginServiceUtility.RESULT + result);
        Assert.assertTrue(result > 0);
        Assert.assertTrue(aSamplePlugin.echo(PluginServiceUtility.HELLO).contains(PluginServiceUtility.HELLO));
    }

    /**
     * Get the first plugin of a specific type with a specific parameter
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test
    public void getFirstPluginByTypeWithADynamicParameter() throws PluginUtilsException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(PluginServiceUtility.AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(PluginServiceUtility.getInstance().getPluginConfigurationWithDynamicParameter());

        Mockito.when(pluginConfRepositoryMocked
                .findByPluginIdOrderByPriorityOrderDesc(PluginServiceUtility.PLUGIN_PARAMETER_ID))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId())).thenReturn(aPluginConfiguration);

        // the argument for the dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build().addParameter(SamplePlugin.COEFF, "-1")
                .getParameters().get(0);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(IComplexInterfacePlugin.class,
                                                                                    aDynamicPlgParam);

        Assert.assertNotNull(aSamplePlugin);

        final int result = aSamplePlugin.add(PluginServiceUtility.QUATRE, PluginServiceUtility.CINQ);
        LOGGER.debug(PluginServiceUtility.RESULT + result);
        Assert.assertTrue(result < 0);

        Assert.assertTrue(aSamplePlugin.echo(PluginServiceUtility.HELLO).contains(PluginServiceUtility.HELLO));
    }

    /**
     * Get the first plugin of a specific type with a dynamic parameter. Used the default value for the dynamic
     * parameter.
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test
    public void getFirstPluginByTypeWithADynamicParameterWithAListOfValue() throws PluginUtilsException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setId(PluginServiceUtility.AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(PluginServiceUtility.getInstance().getPluginConfigurationWithParameters());

        Mockito.when(pluginConfRepositoryMocked
                .findByPluginIdOrderByPriorityOrderDesc(PluginServiceUtility.PLUGIN_PARAMETER_ID))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId())).thenReturn(aPluginConfiguration);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(IComplexInterfacePlugin.class);

        Assert.assertNotNull(aSamplePlugin);

        Assert.assertTrue(aSamplePlugin.echo(PluginServiceUtility.HELLO).contains(PluginServiceUtility.RED));
    }

    /**
     * Get the first plugin of a specific type with a dynamic parameter. Set a value for the dynamic parameter.
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test
    public void getFirstPluginByTypeWithADynamicParameterWithAListOfValueAndSetAValue() throws PluginUtilsException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setId(PluginServiceUtility.AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(PluginServiceUtility.getInstance().getPluginConfigurationWithParameters());

        Mockito.when(pluginConfRepositoryMocked
                .findByPluginIdOrderByPriorityOrderDesc(PluginServiceUtility.PLUGIN_PARAMETER_ID))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId())).thenReturn(aPluginConfiguration);

        // the argument for the dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                .addParameter(SamplePlugin.SUFFIXE, PluginServiceUtility.BLUE).getParameters().get(0);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(IComplexInterfacePlugin.class,
                                                                                    aDynamicPlgParam);

        Assert.assertNotNull(aSamplePlugin);

        Assert.assertTrue(aSamplePlugin.echo(PluginServiceUtility.HELLO).contains(PluginServiceUtility.BLUE));
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
        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setVersion(PluginServiceUtility.BLUE);
        aPluginConfiguration.setId(PluginServiceUtility.AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(PluginServiceUtility.getInstance().getPluginConfigurationWithParameters());

        Mockito.when(pluginConfRepositoryMocked
                .findByPluginIdOrderByPriorityOrderDesc(PluginServiceUtility.PLUGIN_PARAMETER_ID))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId())).thenReturn(aPluginConfiguration);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(IComplexInterfacePlugin.class);

        Assert.assertNotNull(aSamplePlugin);

        Assert.assertTrue(aSamplePlugin.echo(PluginServiceUtility.HELLO).contains(PluginServiceUtility.RED));
    }

    /**
     * Get the first plugin with the configuration the most priority.
     * 
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test
    public void getFirstPluginTheMostPrioritary() throws PluginUtilsException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();

        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithDynamicParameter();
        // this conf is the most priority
        aPluginConfiguration.setPriorityOrder(1);
        aPluginConfiguration.setId(PluginServiceUtility.AN_ID);

        final PluginConfiguration bPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        bPluginConfiguration.setPriorityOrder(10);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(bPluginConfiguration);

        Mockito.when(pluginConfRepositoryMocked
                .findByPluginIdOrderByPriorityOrderDesc(PluginServiceUtility.PLUGIN_PARAMETER_ID))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId())).thenReturn(aPluginConfiguration);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(IComplexInterfacePlugin.class);

        Assert.assertNotNull(aSamplePlugin);

        Assert.assertTrue(aSamplePlugin.echo(PluginServiceUtility.HELLO).contains(PluginServiceUtility.RED));
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

        final PluginConfiguration aPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setPriorityOrder(11);
        aPluginConfiguration.setId(PluginServiceUtility.AN_ID);

        final PluginConfiguration bPluginConfiguration = PluginServiceUtility.getInstance()
                .getPluginConfigurationWithParameters();
        // this conf is the most priority
        bPluginConfiguration.setPriorityOrder(1);
        aPluginConfiguration.setId(1 + PluginServiceUtility.AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(bPluginConfiguration);

        Mockito.when(pluginConfRepositoryMocked
                .findByPluginIdOrderByPriorityOrderDesc(PluginServiceUtility.PLUGIN_PARAMETER_ID))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findOne(bPluginConfiguration.getId())).thenReturn(null);

        pluginServiceMocked.getFirstPluginByType(IComplexInterfacePlugin.class);
    }

}
