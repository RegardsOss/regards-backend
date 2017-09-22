/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginInterfaceUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsTestConstants;
import fr.cnes.regards.framework.utils.plugins.bean.PluginUtilsBean;

/**
 * Unit testing of {@link PluginInterfaceUtils}.
 *
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
    private PluginUtilsBean pluginUtilsBean;

    @Autowired
    private ISampleBeanService sampleBeanService;

    @Test
    public void getBean() {
        Assert.assertNotNull(pluginUtilsBean);
        Assert.assertNotNull(sampleBeanService);

        final ISampleBeanService asBeanService = pluginUtilsBean.getBeanFactory().getBean((ISampleBeanService.class));
        Assert.assertNotNull(asBeanService);
    }

    /**
     * Load a plugins
     */
    @Test
    public void loadPlugin() {
        SampleBeanFactoryPlugin samplePlugin = null;
        Assert.assertNotNull(pluginUtilsBean);
        Assert.assertNotNull(sampleBeanService);

        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SampleBeanFactoryPlugin.SUFFIXE, "chris_test_1").getParameters();
        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SampleBeanFactoryPlugin.class, pluginUtilsBean,
                                             Arrays.asList("fr.cnes.regards.plugins.utils.bean"), new HashMap<>());

        Assert.assertNotNull(samplePlugin);
        final String toulouse = "Toulouse";
        Assert.assertTrue(samplePlugin.echo(toulouse).contains(toulouse));
        LOGGER.info(samplePlugin.echo(toulouse));
    }

}
