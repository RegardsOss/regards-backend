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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginsRepositoryProperties;

/**
 *
 * Class Jason3Doris10ProductMetadataPlugin
 *
 * Exactly the same plugin that the JASON2 one.
 *
 * @author CS
 * @since TODO
 */
public class Jason3Doris10ProductMetadataPlugin extends AbstractJasonDoris10ProductMetadataPlugin {

    private static final String PROJECT_NAME = "JASON3";

    @Autowired
    private PluginsRepositoryProperties pluginsRepositoryProperties;

    @Override
    protected PluginsRepositoryProperties getPluginsRepositoryProperties() {
        return pluginsRepositoryProperties;
    }

    @Override
    protected String getProjectName() {
        return PROJECT_NAME;
    }

}
