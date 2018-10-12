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
package fr.cnes.regards.modules.acquisition.plugins.product;

import java.nio.file.Path;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.plugins.IProductPlugin;

/**
 * Microscope directory product name reader plugin.<br/>
 * This plugin retrieves product name from "sag_descripteur.xml" path
 * @author Olivier Rousselot
 */
@Plugin(id = "ProductFromSagDescriptorPathPlugin", version = "1.0.0-SNAPSHOT",
        description = "Retrieve product name from name of directory containing 'sag_descripteur.xml'",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class ProductFromSagDescriptorPathPlugin implements IProductPlugin {

    @Override
    public String getProductName(Path sagDescriptorFilePath) {
        String productWithVersion = sagDescriptorFilePath.getParent().getFileName().toString();
        return productWithVersion.replaceFirst("(.*)_V\\d+", "$1");
    }
}
