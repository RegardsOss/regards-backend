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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.PluginParamType;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.StringPluginParam;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit testing of {@link PluginService}.
 *
 * @author Christophe Mertz
 */
public class PluginServiceUpdateDynamicParameterTest extends PluginServiceUtility {

    /**
     * Update a {@link PluginConfiguration} : change the parameter's status from dynamic to not dynamic
     */
    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_330")
    @Purpose("Change the parameter's status from dynamic to not dynamic.")
    public void updateDynamicParameter() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithoutParametersToUpdate();
        aPluginConfiguration.setId(AN_ID);
        try {
            Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
                   .thenReturn(aPluginConfiguration);
            Mockito.when(pluginDaoServiceMocked.savePluginConfiguration(aPluginConfiguration))
                   .thenReturn(aPluginConfiguration);

            PluginConfiguration updatedConf = pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);

            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(IPluginParam::isDynamic).count(),
                                updatedConf.getParameters().stream().filter(IPluginParam::isDynamic).count());
            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(p -> !p.isDynamic()).count(),
                                updatedConf.getParameters().stream().filter(p -> !p.isDynamic()).count());

            aPluginConfiguration.logParams();
            Set<IPluginParam> parameters = aPluginConfiguration.getParameters();
            for (final IPluginParam p : updatedConf.getParameters()) {
                if (p.isDynamic()) {
                    if (!p.hasDynamicValues()) {
                        parameters.remove(p);
                        p.toStatic();
                        parameters.add(p);
                        break;
                    }
                }
            }

            updatedConf = pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);

            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(IPluginParam::isDynamic).count(),
                                updatedConf.getParameters().stream().filter(IPluginParam::isDynamic).count());
            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(p -> !p.isDynamic()).count(),
                                updatedConf.getParameters().stream().filter(p -> !p.isDynamic()).count());
            aPluginConfiguration.logParams();

        } catch (ModuleException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Update a {@link PluginConfiguration} : change the parameter's status from dynamic to not dynamic
     */
    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_330")
    @Purpose("Change the parameter's status from not dynamic to dynamic.")
    public void updateParameterToDynamic() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithoutParametersToUpdate();
        aPluginConfiguration.setId(AN_ID);
        try {
            Mockito.when(pluginDaoServiceMocked.findCompleteByBusinessId(aPluginConfiguration.getBusinessId()))
                   .thenReturn(aPluginConfiguration);
            Mockito.when(pluginDaoServiceMocked.savePluginConfiguration(aPluginConfiguration))
                   .thenReturn(aPluginConfiguration);

            PluginConfiguration updatedConf = pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);

            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(IPluginParam::isDynamic).count(),
                                updatedConf.getParameters().stream().filter(IPluginParam::isDynamic).count());
            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(p -> !p.isDynamic()).count(),
                                updatedConf.getParameters().stream().filter(p -> !p.isDynamic()).count());

            aPluginConfiguration.logParams();
            final Set<IPluginParam> parameters = aPluginConfiguration.getParameters();
            for (final IPluginParam p : updatedConf.getParameters()) {
                if (!p.isDynamic() && p.getType() == PluginParamType.STRING) {
                    parameters.remove(p);
                    // Update
                    StringPluginParam stringParam = (StringPluginParam) p;
                    stringParam.setValue("one");
                    stringParam.dynamic(new HashSet<>(Arrays.asList("one", "two", "three", "four", "five", "six")));
                    parameters.add(p);
                    break;
                }
            }

            updatedConf = pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);

            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(IPluginParam::isDynamic).count(),
                                updatedConf.getParameters().stream().filter(IPluginParam::isDynamic).count());
            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(p -> !p.isDynamic()).count(),
                                updatedConf.getParameters().stream().filter(p -> !p.isDynamic()).count());
            aPluginConfiguration.logParams();

        } catch (ModuleException e) {
            Assert.fail();
        }
    }

}
