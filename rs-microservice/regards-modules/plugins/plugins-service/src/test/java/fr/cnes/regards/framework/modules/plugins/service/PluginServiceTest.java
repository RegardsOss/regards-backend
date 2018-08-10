/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.framework.modules.plugins.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.IComplexInterfacePlugin;
import fr.cnes.regards.framework.modules.plugins.ISamplePlugin;
import fr.cnes.regards.framework.modules.plugins.SamplePlugin;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.event.BroadcastPluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginServiceAction;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;

/**
 * Unit testing of {@link PluginService}.
 *
 * @author Christophe Mertz
 * @author Sébastien Binda
 */
public class PluginServiceTest extends PluginServiceUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginServiceTest.class);

    private IPluginConfigurationRepository pluginConfRepositoryMocked;

    private IPluginService pluginServiceMocked;

    private IPublisher publisherMocked;

    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * This method is run before all tests
     */
    @Before
    public void init() {
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn("tenant");

        publisherMocked = Mockito.mock(IPublisher.class);
        // create a mock repository
        pluginConfRepositoryMocked = Mockito.mock(IPluginConfigurationRepository.class);
        pluginServiceMocked = new PluginService(pluginConfRepositoryMocked, publisherMocked, runtimeTenantResolver);
        PluginUtils.setup(Arrays.asList("fr.cnes.regards.plugins", "fr.cnes.regards.framework.plugins",
                                        "fr.cnes.regards.framework.modules.plugins"));
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_200")
    @Purpose("Load all plugin's metada.")
    public void getAllPlugins() {
        final List<PluginMetaData> metadaDatas = pluginServiceMocked.getPlugins();

        Assert.assertNotNull(metadaDatas);
        Assert.assertFalse(metadaDatas.isEmpty());

        LOGGER.debug("List all plugins :");
        metadaDatas.forEach(p -> LOGGER.debug(p.getPluginId()));
    }

    @Test
    public void getAllPluginTypes() {
        Set<String> types = pluginServiceMocked.getPluginTypes();

        Assert.assertNotNull(types);
        Assert.assertFalse(types.isEmpty());

        LOGGER.debug("List all plugin types :");
        types.forEach(p -> LOGGER.debug(p));
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_200")
    @Purpose("Load all plugin's metada for a specific plugin type identified by a Class.")
    public void getPluginOneType() {
        final List<PluginMetaData> plugins = pluginServiceMocked.getPluginsByType(IComplexInterfacePlugin.class);

        Assert.assertNotNull(plugins);
        Assert.assertFalse(plugins.isEmpty());

        LOGGER.debug("List all plugins of type <IComplexInterfacePlugin.class> :");
        plugins.forEach(p -> LOGGER.debug(p.getPluginId()));
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_200")
    @Purpose("Load all plugin's metada for a specific plugin type identified by a class name.")
    public void getPluginTypesByString() {
        final String aClass = "fr.cnes.regards.framework.modules.plugins.IComplexInterfacePlugin";
        List<PluginMetaData> plugins = null;

        try {
            plugins = pluginServiceMocked.getPluginsByType(Class.forName(aClass));
        } catch (final ClassNotFoundException e) {
            Assert.fail();
        }

        Assert.assertNotNull(plugins);
        Assert.assertFalse(plugins.isEmpty());

        LOGGER.debug(String.format("List all plugins of type <%s> :", aClass));
        plugins.forEach(p -> LOGGER.debug(p.getPluginId()));
    }

    /**
     * Get a {@link PluginConfiguration}.
     *
     * @throws ModuleException
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_100")
    @Purpose("Get a plugin configuration identified by an identifier.")
    public void getAPluginConfiguration() {
        try {
            final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
            aPluginConfiguration.setId(AN_ID);
            Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId()))
                    .thenReturn(aPluginConfiguration);
            Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);

            PluginConfiguration aConf;
            aConf = pluginServiceMocked.getPluginConfiguration(aPluginConfiguration.getId());
            Assert.assertEquals(aConf.getLabel(), aPluginConfiguration.getLabel());
        } catch (ModuleException e) {
            Assert.fail();
        }
    }

    /**
     * Delete a {@link PluginConfiguration}.
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_100")
    @Purpose("Delete a plugin configuration identified by an identifier")
    public void deleteAPluginConfiguration() {
        try {
            final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
            aPluginConfiguration.setId(AN_ID);
            Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId()))
                    .thenReturn(aPluginConfiguration);
            pluginServiceMocked.deletePluginConfiguration(aPluginConfiguration.getId());
            Mockito.verify(pluginConfRepositoryMocked).delete(aPluginConfiguration.getId());
            Mockito.verify(publisherMocked).publish(new BroadcastPluginConfEvent(aPluginConfiguration.getId(),
                    PluginServiceAction.DELETE, aPluginConfiguration.getInterfaceNames()));

        } catch (final ModuleException e) {
            Assert.fail();
        }
    }

    /**
     * Save a {@link PluginConfiguration}.
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_100")
    @Requirement("REGARDS_DSL_CMP_PLG_300")
    @Purpose("Create a new plugin configuration")
    public void saveAPluginConfiguration() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        final PluginConfiguration aPluginConfigurationWithId = new PluginConfiguration(aPluginConfiguration);
        aPluginConfigurationWithId.setId(AN_ID);
        try {
            Mockito.when(pluginConfRepositoryMocked.save(aPluginConfiguration)).thenReturn(aPluginConfigurationWithId);

            final PluginConfiguration savedPluginConfiguration = pluginServiceMocked
                    .savePluginConfiguration(aPluginConfiguration);

            Assert.assertEquals(aPluginConfiguration.getLabel(), savedPluginConfiguration.getLabel());
            Assert.assertEquals(aPluginConfiguration.getPluginId(), savedPluginConfiguration.getPluginId());
            Assert.assertEquals(aPluginConfiguration.isActive(), savedPluginConfiguration.isActive());
            Assert.assertEquals(aPluginConfiguration.getParameters().size(),
                                savedPluginConfiguration.getParameters().size());

            Mockito.verify(publisherMocked).publish(new BroadcastPluginConfEvent(aPluginConfigurationWithId.getId(),
                    PluginServiceAction.CREATE, aPluginConfigurationWithId.getInterfaceNames()));
        } catch (final ModuleException e) {
            Assert.fail();
        }
    }

    @Test(expected = ModuleException.class)
    public void saveTwoPluginConfigurationWithSameLabel() throws ModuleException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        final PluginConfiguration aPluginConfigurationWithId = new PluginConfiguration(aPluginConfiguration);
        aPluginConfigurationWithId.setId(AN_ID);

        final PluginConfiguration theSamePluginConfiguration = getPluginConfigurationWithParameters();

        // save a first plugin configuration
        Mockito.when(pluginConfRepositoryMocked.save(aPluginConfiguration)).thenReturn(aPluginConfigurationWithId);
        try {
            pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);
        } catch (ModuleException e) {
            Assert.fail();
        }

        // save a second plugin configuration with the same label
        Mockito.when(pluginConfRepositoryMocked.findOneByLabel(theSamePluginConfiguration.getLabel()))
                .thenReturn(aPluginConfigurationWithId);
        pluginServiceMocked.savePluginConfiguration(theSamePluginConfiguration);

        // An exception is throw, otherwise the test is failed
        Assert.fail();
    }

    /**
     * Update a {@link PluginConfiguration}.
     *
     * @throws ModuleException
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_100")
    @Requirement("REGARDS_DSL_CMP_PLG_100")
    @Purpose("Update a plugin configuration identified by an identifier")
    public void updateAPluginConfiguration() throws ModuleException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        Mockito.when(pluginConfRepositoryMocked.save(aPluginConfiguration)).thenReturn(aPluginConfiguration);

        final PluginConfiguration updatedConf = pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);
        Assert.assertEquals(updatedConf.getLabel(), aPluginConfiguration.getLabel());
        Assert.assertEquals(updatedConf.getPluginId(), aPluginConfiguration.getPluginId());
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_100")
    @Requirement("REGARDS_DSL_CMP_PLG_100")
    @Purpose("Update a plugin configuration identified by an identifier by deactivating it")
    public void deactivateAPluginConfiguration() throws ModuleException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        PluginConfiguration toBeUpdated = new PluginConfiguration(aPluginConfiguration);
        toBeUpdated.setIsActive(false);
        Mockito.when(pluginConfRepositoryMocked.save(toBeUpdated)).thenReturn(toBeUpdated);

        final PluginConfiguration updatedConf = pluginServiceMocked.updatePluginConfiguration(toBeUpdated);
        Assert.assertEquals(updatedConf.getLabel(), aPluginConfiguration.getLabel());
        Assert.assertEquals(updatedConf.getPluginId(), aPluginConfiguration.getPluginId());
        Mockito.verify(publisherMocked).publish(new BroadcastPluginConfEvent(aPluginConfiguration.getId(),
                PluginServiceAction.DISABLE, aPluginConfiguration.getInterfaceNames()));
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_100")
    @Requirement("REGARDS_DSL_CMP_PLG_100")
    @Purpose("Update a plugin configuration identified by an identifier by activating it")
    public void activateAPluginConfiguration() throws ModuleException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);
        aPluginConfiguration.setIsActive(false);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        PluginConfiguration toBeUpdated = new PluginConfiguration(aPluginConfiguration);
        toBeUpdated.setIsActive(true);
        Mockito.when(pluginConfRepositoryMocked.save(toBeUpdated)).thenReturn(toBeUpdated);

        final PluginConfiguration updatedConf = pluginServiceMocked.updatePluginConfiguration(toBeUpdated);
        Assert.assertEquals(updatedConf.getLabel(), aPluginConfiguration.getLabel());
        Assert.assertEquals(updatedConf.getPluginId(), aPluginConfiguration.getPluginId());
        Mockito.verify(publisherMocked).publish(new BroadcastPluginConfEvent(aPluginConfiguration.getId(),
                PluginServiceAction.ACTIVATE, aPluginConfiguration.getInterfaceNames()));
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_200")
    @Purpose("Load a plugin's metada for a specific plugin type identified by a plugin identifier.")
    public void getPluginMetaDataById() {
        final PluginMetaData pluginMetaData = pluginServiceMocked.getPluginMetaDataById("aSamplePlugin");
        Assert.assertNotNull(pluginMetaData);
        Assert.assertNotNull(pluginMetaData.getAuthor());
        Assert.assertNotNull(pluginMetaData.getVersion());
        Assert.assertNotNull(pluginMetaData.getDescription());
    }

    @Test
    public void getPluginConfigurationsByTypeWithClass() {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        pluginConfs.add(getPluginConfigurationWithParameters());
        pluginConfs.add(getPluginConfigurationWithDynamicParameter());
        Mockito.when(pluginConfRepositoryMocked.findAll()).thenReturn(pluginConfs);
        final List<PluginConfiguration> results = pluginServiceMocked
                .getPluginConfigurationsByType(ISamplePlugin.class);

        Assert.assertNotNull(results);
        Assert.assertEquals(pluginConfs.size(), results.size());
    }

    @Test
    public void getPluginConfigurationsByTypeWithPluginId() {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        pluginConfs.add(getPluginConfigurationWithParameters());
        pluginConfs.add(getPluginConfigurationWithDynamicParameter());
        Mockito.when(pluginConfRepositoryMocked.findByPluginIdOrderByPriorityOrderDesc(PLUGIN_PARAMETER_ID))
                .thenReturn(pluginConfs);
        final List<PluginConfiguration> results = pluginServiceMocked.getPluginConfigurations(PLUGIN_PARAMETER_ID);

        Assert.assertNotNull(results);
        Assert.assertEquals(pluginConfs.size(), results.size());
    }

    @Test
    public void getAllPluginConfigurations() {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        pluginConfs.add(getPluginConfigurationWithParameters());
        pluginConfs.add(getPluginConfigurationWithDynamicParameter());
        Mockito.when(pluginConfRepositoryMocked.findAll()).thenReturn(pluginConfs);
        final List<PluginConfiguration> results = pluginServiceMocked.getAllPluginConfigurations();

        Assert.assertNotNull(results);
        Assert.assertEquals(pluginConfs.size(), results.size());
    }

    /**
     * Get the first plugin of a specific type
     *
     * @throws ModuleException throw if an error occurs
     * @throws ModuleException
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_120")
    @Purpose("Load a plugin from a specific type with a configuration and execute a method.")
    public void getFirstPluginInstanceByType() throws ModuleException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithDynamicParameter());

        Mockito.when(pluginConfRepositoryMocked.findAll()).thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);

        Assert.assertNotNull(aSamplePlugin);

        final int result = aSamplePlugin.add(QUATRE, CINQ);
        LOGGER.debug(RESULT + result);
        Assert.assertTrue(result > 0);
        Assert.assertTrue(aSamplePlugin.echo(HELLO).contains(HELLO));
    }

    @Test
    public void getAPluginInstance() throws ModuleException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithDynamicParameter());

        Mockito.when(pluginConfRepositoryMocked.findByPluginIdOrderByPriorityOrderDesc(PLUGIN_PARAMETER_ID))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getPlugin(aPluginConfiguration.getId());

        Assert.assertNotNull(aSamplePlugin);

        final int result = aSamplePlugin.add(QUATRE, CINQ);
        LOGGER.debug(RESULT + result);
        Assert.assertTrue(result > 0);
        Assert.assertTrue(aSamplePlugin.echo(HELLO).contains(HELLO));
    }

    /**
     * Get twice a specific Plugin with the same PluginConfiguration
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    @Purpose("Load twice a plugin with the same configuration.")
    public void getExistingFirstPluginInstanceByType() throws ModuleException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithDynamicParameter());

        Mockito.when(pluginConfRepositoryMocked.findAll()).thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);
        Assert.assertNotNull(aSamplePlugin);

        final SamplePlugin bSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);
        Assert.assertNotNull(bSamplePlugin);

        Assert.assertEquals(aSamplePlugin.add(5, 3), bSamplePlugin.add(5, 3));
        Assert.assertEquals(aSamplePlugin.echo(GREEN), bSamplePlugin.echo(GREEN));

        Assert.assertEquals(aSamplePlugin, bSamplePlugin);
    }

    /**
     * Get twice a specific Plugin with the same PluginConfiguration with a dynamic parameter
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    @Purpose("Load a plugin twice from a specific type with a configuration.")
    public void getExistingFirstPluginInstanceByTypeWithDynamicParameter() throws ModuleException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setId(AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithParameters());

        Mockito.when(pluginConfRepositoryMocked.findAll()).thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);

        // the argument for the dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                .addDynamicParameter(SamplePlugin.FIELD_NAME_SUFFIX, BLUE).getParameters().get(0);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class,
                                                                                    aDynamicPlgParam);
        Assert.assertNotNull(aSamplePlugin);

        final SamplePlugin bSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class,
                                                                                    aDynamicPlgParam);
        Assert.assertNotNull(bSamplePlugin);
        Assert.assertNotEquals(aSamplePlugin, bSamplePlugin);
    }

    /**
     * Get twice a specific Plugin with the same PluginConfiguration with a dynamic parameter the second time
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    @Purpose("Load a plugin twice from a specific type with a configuration.")
    public void getExistingFirstPluginInstanceByTypeWithDynamicParameter2() throws ModuleException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setId(AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithParameters());

        Mockito.when(pluginConfRepositoryMocked.findAll()).thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);

        // the argument for the dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                .addDynamicParameter(SamplePlugin.FIELD_NAME_SUFFIX, BLUE).getParameters().get(0);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);
        Assert.assertNotNull(aSamplePlugin);

        final SamplePlugin bSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class,
                                                                                    aDynamicPlgParam);
        Assert.assertNotNull(bSamplePlugin);
        Assert.assertNotEquals(aSamplePlugin, bSamplePlugin);
    }

    /**
     * Get the first plugin of a specific type with a specific parameter
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_120")
    @Requirement("REGARDS_DSL_CMP_PLG_120")
    @Requirement("REGARDS_DSL_CMP_PLG_300")
    @Requirement("REGARDS_DSL_CMP_PLG_320")
    @Requirement("REGARDS_DSL_CMP_PLG_340")
    @Purpose("Load a plugin with a dynamic parameter from a specific type with a configuration and execute a method.")
    public void getFirstPluginInstanceByTypeWithADynamicParameter() throws ModuleException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setId(AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithDynamicParameter());

        Mockito.when(pluginConfRepositoryMocked.findAll()).thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);

        // the argument for the dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                .addDynamicParameter(SamplePlugin.FIELD_NAME_COEF, -1).getParameters().get(0);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class,
                                                                                    aDynamicPlgParam);

        Assert.assertNotNull(aSamplePlugin);

        final int result = aSamplePlugin.add(QUATRE, CINQ);
        LOGGER.debug(RESULT + result);

        Assert.assertTrue(result < 0);
        Assert.assertTrue(aSamplePlugin.echo(HELLO).contains(HELLO));
    }

    /**
     * Get the first plugin of a specific type with a dynamic parameter. Used the default value for the dynamic
     * parameter.
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_120")
    @Requirement("REGARDS_DSL_CMP_PLG_300")
    @Requirement("REGARDS_DSL_CMP_PLG_340")
    @Purpose("Load a plugin with a dynamic parameter with a list of value from a specific type with a configuration and execute a method.")
    public void getFirstPluginInstanceByTypeWithADynamicParameterWithAListOfValue() throws ModuleException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setId(AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithParameters());

        Mockito.when(pluginConfRepositoryMocked.findAll()).thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);

        Assert.assertNotNull(aSamplePlugin);
        Assert.assertTrue(aSamplePlugin.echo(HELLO).contains(RED));
    }

    /**
     * Get the first plugin of a specific type with a dynamic parameter. Set a value for the dynamic parameter.
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_120")
    @Requirement("REGARDS_DSL_CMP_PLG_300")
    @Requirement("REGARDS_DSL_CMP_PLG_340")
    @Purpose("Load a plugin with a dynamic parameter with a list of value from a specific type with a configuration and set a parameter value and execute a method.")
    public void getFirstPluginInstanceByTypeWithADynamicParameterWithAListOfValueAndSetAValue() throws ModuleException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setId(AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithParameters());

        Mockito.when(pluginConfRepositoryMocked.findAll()).thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);

        // the argument for the dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                .addDynamicParameter(SamplePlugin.FIELD_NAME_SUFFIX, BLUE).getParameters().get(0);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class,
                                                                                    aDynamicPlgParam);

        Assert.assertNotNull(aSamplePlugin);
        Assert.assertTrue(aSamplePlugin.echo(HELLO).contains(BLUE));
    }

    /**
     * Try to get a plugin of a specific type with a dynamic parameter BUT a bad version.
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test(expected = CannotInstanciatePluginException.class)
    public void getAPluginInstanceWithBadVersionConfiguration() throws ModuleException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setVersion(BLUE);
        aPluginConfiguration.setId(AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(getPluginConfigurationWithParameters());

        Mockito.when(pluginConfRepositoryMocked.findAll()).thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);

        pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);
    }

    /**
     * Get the first plugin with the configuration the most priority.
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test
    public void getFirstPluginInstanceTheMostPrioritary() throws ModuleException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();

        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        // this conf is the most priority
        aPluginConfiguration.setPriorityOrder(1);
        aPluginConfiguration.setId(AN_ID);

        final PluginConfiguration bPluginConfiguration = getPluginConfigurationWithParameters();
        bPluginConfiguration.setPriorityOrder(2);
        bPluginConfiguration.setId(1 + AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(bPluginConfiguration);

        Mockito.when(pluginConfRepositoryMocked.findAll()).thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);

        final SamplePlugin aSamplePlugin = pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);

        Assert.assertNotNull(aSamplePlugin);
        Assert.assertTrue(aSamplePlugin.echo(HELLO).contains(RED));
    }

    @Test
    public void checkPluginName() throws EntityInvalidException {
        String className = "fr.cnes.regards.framework.modules.plugins.SamplePlugin";
        PluginMetaData metaData = pluginServiceMocked.checkPluginClassName(ISamplePlugin.class, className);
        Assert.assertNotNull(metaData);
        Assert.assertEquals(className, metaData.getPluginClassName());
    }

    @Test(expected = EntityInvalidException.class)
    public void checkPluginNameFailed() throws EntityInvalidException {
        String className = "fr.cnes.regards.framework.plugins.SmplePlugin";
        pluginServiceMocked.checkPluginClassName(ISamplePlugin.class, className);
        Assert.fail();
    }

}
