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

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;

/**
 * Metadata caculation's plugin for SPOT5 products.
 * 
 * @author Christophe Mertz
 *
 */
@Plugin(description = "Metadata caculation's plugin for SPOT5 products", id = "Spot5ProductMetadataPlugin",
        version = "1.0.0", author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class Spot5ProductMetadataPlugin extends AbstractProductMetadataPlugin {

    /**
     * SPOT5 project name
     */
    private static final String PROJECT_NAME = "SPOT5";

    protected String getProjectName() {
        return PROJECT_NAME;
    }
}
