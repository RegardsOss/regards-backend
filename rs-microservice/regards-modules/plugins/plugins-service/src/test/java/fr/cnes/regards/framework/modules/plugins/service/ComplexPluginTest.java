/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for plugin instanciation with complex parameter types
 *
 * @author sbinda
 */
public class ComplexPluginTest extends PluginServiceUtility {


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
                                                                                                            PluginParameterTransformer.toJson(pojo))
                                                                                                        .dynamic()),
                                                                           0,
                                                                           pluginMetaData.getPluginId());

        aPluginConfiguration.setId(pPluginConfigurationId);
        aPluginConfiguration.setVersion(pluginMetaData.getVersion());

        pluginConfs.add(aPluginConfiguration);

        Mockito.when(pluginDaoServiceMocked.findByPluginIdOrderByPriorityOrderDesc("complexPlugin"))
               .thenReturn(pluginConfs);
        Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
               .thenReturn(aPluginConfiguration);
        Mockito.when(pluginDaoServiceMocked.existsByBusinessId(aPluginConfiguration.getBusinessId())).thenReturn(true);

        ITestPlugin plugin = pluginServiceMocked.getPlugin(aPluginConfiguration.getBusinessId());

        Assert.assertEquals(plugin.getPojoParam().getPojoParam(), pojo.getPojoParam());
        Assert.assertEquals(plugin.getPojoParam().getOtherPojoParam().getIntValue(),
                            pojo.getOtherPojoParam().getIntValue());
    }

}