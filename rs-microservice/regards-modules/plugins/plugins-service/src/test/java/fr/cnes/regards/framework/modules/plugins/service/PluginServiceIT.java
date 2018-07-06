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

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;

/**
 * IT test class for IPluginService
 * @author Sébastien Binda
 */
@MultitenantTransactional
@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=plugin_test_db")
public class PluginServiceIT extends AbstractMultitenantServiceTest {

    @Autowired
    private IPluginService pluginService;

    @Test
    public void testPluginsWithOnlyOneConfActivable() throws ModuleException {
        String label1 = "conf1";
        String label2 = "conf2";
        pluginService.savePluginConfiguration(
                                              new PluginConfiguration(
                                                      PluginUtils.createPluginMetaData(UniqueConfActivePluginImpl.class,
                                                                                       UniqueConfActivePluginImpl.class
                                                                                               .getPackage().getName()),
                                                      label1, Lists.newArrayList(), 0));

        Assert.assertTrue(pluginService.getPluginConfigurationByLabel(label1).isActive());

        pluginService.savePluginConfiguration(
                                              new PluginConfiguration(
                                                      PluginUtils.createPluginMetaData(UniqueConfActivePluginImpl.class,
                                                                                       UniqueConfActivePluginImpl.class
                                                                                               .getPackage().getName()),
                                                      label2, Lists.newArrayList(), 0));

        PluginConfiguration conf1 = pluginService.getPluginConfigurationByLabel(label1);
        Assert.assertTrue(pluginService.getPluginConfigurationByLabel(label2).isActive());
        Assert.assertFalse(conf1.isActive());

        conf1.setIsActive(true);
        pluginService.updatePluginConfiguration(conf1);

        Assert.assertTrue(pluginService.getPluginConfigurationByLabel(label1).isActive());
        Assert.assertFalse(pluginService.getPluginConfigurationByLabel(label2).isActive());
    }

    @Test
    public void testPluginsWithMultipleConfActivable() throws ModuleException {
        String label1 = "conf1";
        String label2 = "conf2";
        pluginService.savePluginConfiguration(
                                              new PluginConfiguration(
                                                      PluginUtils.createPluginMetaData(TestPlugin.class,
                                                                                       TestPlugin.class.getPackage()
                                                                                               .getName()),
                                                      label1, Lists.newArrayList(), 0));

        Assert.assertTrue(pluginService.getPluginConfigurationByLabel(label1).isActive());

        pluginService.savePluginConfiguration(
                                              new PluginConfiguration(
                                                      PluginUtils.createPluginMetaData(TestPlugin.class,
                                                                                       TestPlugin.class.getPackage()
                                                                                               .getName()),
                                                      label2, Lists.newArrayList(), 0));

        PluginConfiguration conf1 = pluginService.getPluginConfigurationByLabel(label1);
        Assert.assertTrue(pluginService.getPluginConfigurationByLabel(label2).isActive());
        Assert.assertTrue(conf1.isActive());

        conf1.setIsActive(false);
        pluginService.updatePluginConfiguration(conf1);

        Assert.assertFalse(pluginService.getPluginConfigurationByLabel(label1).isActive());
        Assert.assertTrue(pluginService.getPluginConfigurationByLabel(label2).isActive());

        conf1.setIsActive(true);
        pluginService.updatePluginConfiguration(conf1);

        Assert.assertTrue(pluginService.getPluginConfigurationByLabel(label1).isActive());
        Assert.assertTrue(pluginService.getPluginConfigurationByLabel(label2).isActive());
    }

}
