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
import fr.cnes.regards.modules.acquisition.plugins.Microscope;

/**
 * Microscope directory product name reader plugin.<br/>
 * This plugin retrieves product name from metadata "_metadata.xml" file name, it is the start of the filename until
 * "_Vx_metadata.xml" (same as product directory without version part)
 * @author Olivier Rousselot
 */
@Plugin(id = "ProductFromDirectoryPlugin", version = "1.0.0-SNAPSHOT",
        description = "Retrieve product name from product files directory name", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class ProductFromDirectoryPlugin implements IProductPlugin {

    @Override
    public String getProductName(Path metadataFilePath) {
        String metadataFilename = metadataFilePath.getFileName().toString();
        String productWithVersion = metadataFilename.substring(0, metadataFilename.indexOf(Microscope.METADATA_SUFFIX));
        return productWithVersion.replaceFirst("(.*)_V\\d+", "$1");
    }
}
