/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.encryption.BlowfishEncryptionService;
import fr.cnes.regards.framework.encryption.configuration.CipherProperties;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;

/**
 * Tests for plugin instanciation with complex parameter types
 * @author sbinda
 */
public class ComplexPluginTest {

    private IPluginConfigurationRepository pluginConfRepositoryMocked;

    private IPluginService pluginServiceMocked;

    private IPublisher publisherMocked;

    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * This method is run before all tests
     */
    @Before
    public void init() throws InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn("tenant");

        publisherMocked = Mockito.mock(IPublisher.class);
        // create a mock repository
        pluginConfRepositoryMocked = Mockito.mock(IPluginConfigurationRepository.class);
        BlowfishEncryptionService blowfishEncryptionService = new BlowfishEncryptionService();
        blowfishEncryptionService
                .init(new CipherProperties(Paths.get("src", "test", "resources", "testKey"), "12345678"));
        pluginServiceMocked = new PluginService(pluginConfRepositoryMocked,
                                                publisherMocked,
                                                runtimeTenantResolver,
                                                blowfishEncryptionService,
                                                null);
        PluginUtils.setup();
    }

    @Test
    public void test() throws ModuleException, NotAvailablePluginConfigurationException {
        PluginMetaData pluginMetaData = pluginServiceMocked.getPluginMetaDataById("complexPlugin");

        Long pPluginConfigurationId = 10L;

        TestPojo pojo = new TestPojo();
        TestPojo2 pojo2 = new TestPojo2();
        pojo2.setIntValue(12);
        pojo.setPojoParam("string_value");
        pojo.setOtherPojoParam(pojo2);

        List<PluginConfiguration> pluginConfs = new ArrayList<>();
        PluginConfiguration aPluginConfiguration = new PluginConfiguration("a configuration from PluginServiceUtility",
                                                                           IPluginParam.set(IPluginParam.build(
                                                                                   TestPlugin.FIELD_NAME_POJO_PARAM,
                                                                                   PluginParameterTransformer
                                                                                           .toJson(pojo)).dynamic()),
                                                                           0,
                                                                           pluginMetaData.getPluginId());

        aPluginConfiguration.setId(pPluginConfigurationId);
        aPluginConfiguration.setVersion(pluginMetaData.getVersion());

        pluginConfs.add(aPluginConfiguration);

        Mockito.when(pluginConfRepositoryMocked.findByPluginIdOrderByPriorityOrderDesc("complexPlugin"))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
                .thenReturn(aPluginConfiguration);
        Mockito.when(pluginConfRepositoryMocked.existsByBusinessId(aPluginConfiguration.getBusinessId()))
                .thenReturn(true);

        ITestPlugin plugin = pluginServiceMocked.getPlugin(aPluginConfiguration.getBusinessId());

        Assert.assertEquals(plugin.getPojoParam().getPojoParam(), pojo.getPojoParam());
        Assert.assertEquals(plugin.getPojoParam().getOtherPojoParam().getIntValue(),
                            pojo.getOtherPojoParam().getIntValue());
    }

}