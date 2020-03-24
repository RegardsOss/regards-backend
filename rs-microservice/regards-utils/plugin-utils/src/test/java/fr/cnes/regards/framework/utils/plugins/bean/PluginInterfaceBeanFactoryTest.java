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
package fr.cnes.regards.framework.utils.plugins.bean;

import java.util.HashMap;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.basic.PluginUtilsTestConstants;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;

/**
 * @author Christophe Mertz
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestConfiguration.class })
public final class PluginInterfaceBeanFactoryTest extends PluginUtilsTestConstants {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginInterfaceBeanFactoryTest.class);

    @Autowired
    private ISampleBeanService sampleBeanService;

    /**
     * Load a plugins
     * @throws NotAvailablePluginConfigurationException
     */
    @Test
    public void loadPlugin() throws NotAvailablePluginConfigurationException {
        SampleBeanFactoryPlugin samplePlugin = null;
        Assert.assertNotNull(sampleBeanService);

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(SampleBeanFactoryPlugin.FIELD_NAME_SUFFIX, "chris_test_1"));

        PluginUtils.setup(SampleBeanFactoryPlugin.class.getPackage().getName());
        samplePlugin = PluginUtils.getPlugin(PluginConfiguration.build(SampleBeanFactoryPlugin.class, "", parameters),
                                             new HashMap<>());

        Assert.assertNotNull(samplePlugin);
        final String toulouse = "Toulouse";
        Assert.assertTrue(samplePlugin.echo(toulouse).contains(toulouse));
        LOGGER.info(samplePlugin.echo(toulouse));
    }

}
