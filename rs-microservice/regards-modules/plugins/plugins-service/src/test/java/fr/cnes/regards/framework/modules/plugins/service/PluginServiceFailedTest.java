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

package fr.cnes.regards.framework.modules.plugins.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.encryption.BlowfishEncryptionService;
import fr.cnes.regards.framework.encryption.configuration.CipherProperties;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.IComplexInterfacePlugin;
import fr.cnes.regards.framework.modules.plugins.INotInterfacePlugin;
import fr.cnes.regards.framework.modules.plugins.ISamplePlugin;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Unit testing of {@link PluginService}.
 *
 * @author Christophe Mertz
 * @author Sébastien Binda
 */
public class PluginServiceFailedTest extends PluginServiceUtility {

    private IPluginConfigurationRepository pluginConfRepositoryMocked;

    private IPluginService pluginServiceMocked;

    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * This method is run before all tests
     */
    @Before
    public void init() throws InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn("tenant");

        // create a mock repository
        pluginConfRepositoryMocked = Mockito.mock(IPluginConfigurationRepository.class);
        BlowfishEncryptionService blowfishEncryptionService = new BlowfishEncryptionService();
        blowfishEncryptionService.init(new CipherProperties(Paths.get("src", "test", "resources", "testKey"),
                                                            "12345678"));
        pluginServiceMocked = new PluginService(pluginConfRepositoryMocked,
                                                Mockito.mock(IPublisher.class),
                                                runtimeTenantResolver,
                                                blowfishEncryptionService,
                                                null);
        PluginUtils.setup();
    }

    /**
     * Get an unsaved {@link PluginConfiguration}.
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test(expected = ModuleException.class)
    public void getAPluginConfigurationUnknown() throws ModuleException {
        String fakeBusinessId = "fakebid";
        Mockito.when(pluginConfRepositoryMocked.findCompleteByBusinessId(fakeBusinessId)).thenReturn(null);

        PluginConfiguration plg = pluginServiceMocked.getPluginConfiguration(fakeBusinessId);
        Assert.assertNull(plg);
    }

    /**
     * Delete an unsaved {@link PluginConfiguration}.
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test(expected = ModuleException.class)
    public void deleteAPluginConfigurationUnknown() throws ModuleException {
        String fakeBusinessId = "fakebid";
        Mockito.when(pluginConfRepositoryMocked.findCompleteByBusinessId(fakeBusinessId)).thenReturn(null);
        pluginServiceMocked.deletePluginConfiguration(fakeBusinessId);
        Assert.fail("There must be an exception thrown");
    }

    /**
     * Save a null {@link PluginConfiguration}.
     */
    @Test(expected = EntityInvalidException.class)
    public void saveANullPluginConfiguration()
        throws EntityInvalidException, EncryptionException, EntityNotFoundException {
        pluginServiceMocked.savePluginConfiguration(null);
        Assert.fail();
    }

    /**
     * Save a {@link PluginConfiguration} without priorityOrder attribute.
     */
    @Test(expected = EntityInvalidException.class)
    public void saveAPluginConfigurationWithoutPriorityOrder()
        throws EntityInvalidException, EncryptionException, EntityNotFoundException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setPriorityOrder(null);
        pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);
        Assert.fail();
    }

    /**
     * Save a {@link PluginConfiguration} without priorityOrder attribute.
     */
    @Test(expected = EntityInvalidException.class)
    public void saveAPluginConfigurationWithoutVersion()
        throws EntityInvalidException, EncryptionException, EntityNotFoundException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        aPluginConfiguration.setVersion("bad");
        pluginServiceMocked.savePluginConfiguration(aPluginConfiguration);
        Assert.fail();
    }

    /**
     * Save a {@link PluginConfiguration} without parameters.
     *
     * @throws ModuleException throw if an error occurs
     */
    public void saveAPluginConfigurationWithoutParameters() throws ModuleException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithoutParameters();
        final PluginConfiguration savedPluginConfiguration = pluginServiceMocked.savePluginConfiguration(
            aPluginConfiguration);
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
     * @throws ModuleException throw if an error occurs
     */
    @Test(expected = ModuleException.class)
    public void updateAPluginConfigurationUnknown() throws ModuleException {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithParameters();
        final Long aPluginId = 999L;
        aPluginConfiguration.setId(aPluginId);
        Mockito.when(pluginConfRepositoryMocked.existsById(aPluginConfiguration.getId())).thenReturn(false);

        pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);
        Assert.fail();
    }

    @Test
    public void getPluginMetaDataByIdUnknown() {
        final PluginMetaData pluginMetaData = pluginServiceMocked.getPluginMetaDataById("hello world");
        Assert.assertNull(pluginMetaData);
    }

    @Test(expected = ModuleException.class)
    public void getFirstPluginByTypeNullPluginConf() throws ModuleException, NotAvailablePluginConfigurationException {
        pluginServiceMocked.getFirstPluginByType(INotInterfacePlugin.class);
        Assert.fail();
    }

    /**
     * Get the first plugin of a specific type with a dynamic parameter. Used the default value for the dynamic
     * parameter.
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test(expected = CannotInstanciatePluginException.class)
    public void getAPluginWithBadVersionConfiguration()
        throws ModuleException, NotAvailablePluginConfigurationException {
        // Given
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        PluginConfiguration pluginConfiguration0 = getPluginConfigurationWithDynamicParameter();
        pluginConfiguration0.setVersion(BLUE);
        pluginConfiguration0.setId(AN_ID);
        pluginConfigurations.add(pluginConfiguration0);

        PluginConfiguration pluginConfiguration1 = getPluginConfigurationWithParameters();
        pluginConfiguration1.setPriorityOrder(10);
        pluginConfigurations.add(pluginConfiguration1);

        Mockito.when(pluginConfRepositoryMocked.findAll()).thenReturn(pluginConfigurations);
        Mockito.when(pluginConfRepositoryMocked.existsById(pluginConfiguration0.getId())).thenReturn(true);
        Mockito.when(pluginConfRepositoryMocked.findCompleteByBusinessId(pluginConfiguration0.getBusinessId()))
               .thenReturn(pluginConfiguration0);
        // When
        pluginServiceMocked.getFirstPluginByType(ISamplePlugin.class);
    }

    /**
     * Error to get a plugin with a configuration that is not the most priority.
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test(expected = ModuleException.class)
    public void getFirstPluginTheMostPrioritaryError()
        throws ModuleException, NotAvailablePluginConfigurationException {
        final List<PluginConfiguration> pluginConfs = new ArrayList<>();

        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setPriorityOrder(2);
        aPluginConfiguration.setId(AN_ID);

        final PluginConfiguration bPluginConfiguration = getPluginConfigurationWithParameters();
        // this conf is the most priority
        bPluginConfiguration.setPriorityOrder(1);
        bPluginConfiguration.setId(1 + AN_ID);

        pluginConfs.add(aPluginConfiguration);
        pluginConfs.add(bPluginConfiguration);

        Mockito.when(pluginConfRepositoryMocked.findByPluginIdOrderByPriorityOrderDesc(PLUGIN_PARAMETER_ID))
               .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findById(bPluginConfiguration.getId())).thenReturn(Optional.empty());

        pluginServiceMocked.getFirstPluginByType(IComplexInterfacePlugin.class);

        Assert.fail();
    }

    /**
     * Error to get a plugin with a configuration that is not active.
     *
     * @throws ModuleException throw if an error occurs
     */
    @Test(expected = NotAvailablePluginConfigurationException.class)
    @Requirement("REGARDS_DSL_CMP_PLG_100")
    @Purpose("Unable to load a plugin with a no active configuration")
    public void getPluginNotActiveConfiguration() throws ModuleException, NotAvailablePluginConfigurationException {

        //need to directly call PluginUtils.getPlugins.get(pluginId) to set metadata because of mockito
        PluginMetaData metaData = PluginUtils.getPluginMetadata(A_SAMPLE_PLUGIN_PLUGIN_ID);
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithoutParameters();
        aPluginConfiguration.setIsActive(Boolean.FALSE);
        aPluginConfiguration.setId(AN_ID);
        aPluginConfiguration.setMetaDataAndPluginId(metaData);
        aPluginConfiguration.setVersion(metaData.getVersion());

        Mockito.when(pluginConfRepositoryMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(aPluginConfiguration);

        pluginServiceMocked.getPlugin(aPluginConfiguration.getBusinessId());

        Assert.fail();
    }

}
