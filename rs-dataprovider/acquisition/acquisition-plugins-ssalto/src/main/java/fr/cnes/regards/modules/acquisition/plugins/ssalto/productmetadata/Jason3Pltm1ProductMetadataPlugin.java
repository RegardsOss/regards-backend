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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginsRepositoryProperties;

/**
 * Metadata caculation's plugin for Jason2 Pltm1 products.
 *
 * @author Christophe Mertz
 */
@Plugin(description = "Metadata caculation's plugin for Jason2 Pltm1 products.",
        id = "Jason3Pltm1ProductMetadataPlugin", version = "1.0.0", author = "REGARDS Team", contact = "regards@c-s.fr",
        licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class Jason3Pltm1ProductMetadataPlugin extends AbstractJasonPltm1ProductMetadataPlugin {

    /**
     * JASON3 project name
     */
    private static final String PROJECT_NAME = "JASON3";

    /**
     * Plugin Ssalto repository configuration
     */
    @Autowired
    private PluginsRepositoryProperties pluginsRepositoryProperties;

    @Override
    protected PluginsRepositoryProperties getPluginsRepositoryProperties() {
        return pluginsRepositoryProperties;
    }

    @Override
    protected String getProjectPrefix() {
        return "JA3";
    }

    @Override
    protected String getProjectName() {
        return PROJECT_NAME;
    }
}
