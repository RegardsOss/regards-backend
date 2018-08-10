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
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;

/**
 *
 * Tests for plugin instanciation with complex parameter types
 *
 * @author sbinda
 *
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
    public void init() {
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn("tenant");

        publisherMocked = Mockito.mock(IPublisher.class);
        // create a mock repository
        pluginConfRepositoryMocked = Mockito.mock(IPluginConfigurationRepository.class);
        pluginServiceMocked = new PluginService(pluginConfRepositoryMocked, publisherMocked, runtimeTenantResolver);
        PluginUtils.setup();
    }

    @Test
    public void test() throws ModuleException {
        PluginMetaData result = pluginServiceMocked.getPluginMetaDataById("complexPlugin");

        Long pPluginConfigurationId = 10L;

        PluginParametersFactory ppf = PluginParametersFactory.build();

        TestPojo pojo = new TestPojo();
        TestPojo2 pojo2 = new TestPojo2();
        pojo2.setIntValue(12);
        pojo.setPojoParam("string_value");
        pojo.setOtherPojoParam(pojo2);

        ppf.addDynamicParameter(TestPlugin.FIELD_NAME_POJO_PARAM, pojo);

        final List<PluginConfiguration> pluginConfs = new ArrayList<>();
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(result,
                "a configuration from PluginServiceUtility", ppf.getParameters(), 0);
        aPluginConfiguration.setId(pPluginConfigurationId);

        pluginConfs.add(aPluginConfiguration);

        Mockito.when(pluginConfRepositoryMocked.findByPluginIdOrderByPriorityOrderDesc("complexPlugin"))
                .thenReturn(pluginConfs);
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId()))
                .thenReturn(aPluginConfiguration);
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);

        ITestPlugin plugin = pluginServiceMocked.getPlugin(pPluginConfigurationId);

        Assert.assertEquals(plugin.getPojoParam().getPojoParam(), pojo.getPojoParam());
        Assert.assertEquals(plugin.getPojoParam().getOtherPojoParam().getIntValue(),
                            pojo.getOtherPojoParam().getIntValue());
    }

}