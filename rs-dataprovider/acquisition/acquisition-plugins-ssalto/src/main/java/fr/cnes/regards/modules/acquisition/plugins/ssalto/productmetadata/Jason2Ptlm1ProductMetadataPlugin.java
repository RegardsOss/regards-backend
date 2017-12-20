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
 * plugin specifiques au donnees jason2 GPSP10Flot Les attributs traites specifiquement sont les TIME_PERIOD ( résolu
 * comme pour les donnees Doris, et FILE_CREATION_DATE, qui ne se recupere pas de la meme maniere en fonction du nom des
 * fichiers.
 *
 * @author Christophe Mertz
 */
@Plugin(description = "Jason2Ptlm1ProductMetadataPlugin", id = "Jason2Ptlm1ProductMetadataPlugin", version = "1.0.0",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class Jason2Ptlm1ProductMetadataPlugin extends AbstractJasonPltm1ProductMetadataPlugin {

    /**
     * JASON2 project name
     */
    private static final String PROJECT_NAME = "JASON2";

    @Autowired
    private PluginsRepositoryProperties pluginsRepositoryProperties;

    @Override
    protected PluginsRepositoryProperties getPluginsRepositoryProperties() {
        return pluginsRepositoryProperties;
    }

    @Override
    protected String getProjectPrefix() {
        return "JA2";
    }

    @Override
    protected String getProjectName() {
        return PROJECT_NAME;
    }
}
