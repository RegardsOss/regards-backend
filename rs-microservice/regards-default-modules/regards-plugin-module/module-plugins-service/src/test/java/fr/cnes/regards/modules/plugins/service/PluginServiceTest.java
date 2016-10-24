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
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.plugins.dao.stubs.PluginDataUtility;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.plugins.IComplexInterfacePlugin;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 *
 * Unit testing of {@link PluginService}.
 *
 * @author cmertz
 */
@Service
public class PluginServiceTest extends PluginDataUtility {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginServiceTest.class);

    private static final Long AN_ID = new Long(33);

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
        final List<PluginMetaData> types = pluginServiceMocked.getPluginsByType(IComplexInterfacePlugin.class);

        Assert.assertNotNull(types);
        Assert.assertFalse(types.isEmpty());

        LOGGER.debug("List all plugins of type <IComplexInterfacePlugin.class> :");
        types.forEach(p -> LOGGER.debug(p.getPluginId()));
    }

    /**
     * Get a {@link PluginConfiguration}
     */
    @Test
    public void getAPluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
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
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        Mockito.when(pluginConfRepositoryMocked.findOne(null)).thenReturn(null);

        pluginServiceMocked.getPluginConfiguration(aPluginConfiguration.getId());
    }

    /**
     * Delete a {@link PluginConfiguration}
     */
    @Test
    public void deleteAPluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
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
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
        Mockito.doThrow(EmptyResultDataAccessException.class).when(pluginConfRepositoryMocked)
                .delete(aPluginConfiguration.getId());
        pluginServiceMocked.deletePluginConfiguration(aPluginConfiguration.getId());
    }

    /**
     * Delete a {@link PluginConfiguration}
     */
    @Test
    public void saveAPluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        final PluginConfiguration aPluginConfigurationWithId = getPluginConfigurationWithParameters();
        aPluginConfigurationWithId.setId(AN_ID);
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
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
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
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
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
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setVersion(null);
        pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);
        Assert.fail();
    }

    /**
     * Update a {@link PluginConfiguration}
     */
    @Test
    public void updateAPluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
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
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
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
    public void getPluginConfigurationsByType() {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        pluginConfs.add(getPluginConfigurationWithParameters());
        pluginConfs.add(getPluginConfigurationWithDynamicParameter());
        Mockito.when(pluginConfRepositoryMocked.findByPluginIdOrderByPriorityOrderDesc(("aParameterPlugin")))
                .thenReturn(pluginConfs);
        final List<PluginConfiguration> results = pluginServiceMocked
                .getPluginConfigurationsByType(IComplexInterfacePlugin.class);

        Assert.assertNotNull(results);
        Assert.assertEquals(pluginConfs.size(), results.size());
    }

}
