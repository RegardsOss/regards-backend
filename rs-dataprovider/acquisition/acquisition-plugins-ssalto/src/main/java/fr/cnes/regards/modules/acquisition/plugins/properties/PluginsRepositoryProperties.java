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
package fr.cnes.regards.modules.acquisition.plugins.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Plugin Ssalto repository configuration <b>pluginsRepository.properties</b>.<br>
 * The properties are :<br>
 * <li>regards.acquisition.ssalto.plugin-conf-files-dir : path to the Ssalto plugin configuration file
 * <li>regards.acquisition.ssalto.plugin-translation-files-dir : path the translation properties file
 * 
 * @author Christophe Mertz
 *
 */
@ConfigurationProperties(prefix = "regards.acquisition.ssalto")
public class PluginsRepositoryProperties {

    private String pluginConfFilesDir;

    private String pluginTranslationFilesDir;

    public String getPluginConfFilesDir() {
        return pluginConfFilesDir;
    }

    public void setPluginConfFilesDir(String pluginConfFilesDir) {
        this.pluginConfFilesDir = pluginConfFilesDir;
    }

    public String getPluginTranslationFilesDir() {
        return pluginTranslationFilesDir;
    }

    public void setPluginTranslationFilesDir(String pluginTranslationFilesDir) {
        this.pluginTranslationFilesDir = pluginTranslationFilesDir;
    }

}
