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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.repository;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.modules.acquisition.plugins.properties.PluginsRepositoryProperties;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { PluginRepositoryTestsConfiguration.class })
public class PluginsRepositoryPropertiesTest {

    @Autowired
    PluginsRepositoryProperties pluginsRepositoryProperties;

    @Test
    public void loadfileTest() {

        Assert.assertNotNull(pluginsRepositoryProperties);

        String pluginConfFilesDir = pluginsRepositoryProperties.getPluginConfFilesPath();
        Assert.assertTrue("Erreur de lecture du fichier de configuration des plugins",
                          ((pluginConfFilesDir != null) && !pluginConfFilesDir.isEmpty()));

        String pluginConfTranslationFilesDir = pluginsRepositoryProperties.getPluginTranslationFilesPath();
        Assert.assertTrue("Erreur de lecture du fichier de configuration des plugins",
                          (pluginConfTranslationFilesDir != null) && !pluginConfTranslationFilesDir.isEmpty());
        
        String pluginConfDir = pluginsRepositoryProperties.getPluginConfPath();
        Assert.assertTrue("Erreur de lecture du fichier de configuration des plugins",
                          (pluginConfDir != null) && !pluginConfDir.isEmpty());
    }

}
