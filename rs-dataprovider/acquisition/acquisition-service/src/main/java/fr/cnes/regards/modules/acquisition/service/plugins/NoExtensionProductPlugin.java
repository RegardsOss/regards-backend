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
package fr.cnes.regards.modules.acquisition.service.plugins;

import java.nio.file.Path;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.plugins.IProductPlugin;

/**
 * Compute the product name removing extension from filename
 *
 * @author Marc Sordi
 *
 */
@Plugin(id = "NoExtensionProductPlugin", version = "1.0.0-SNAPSHOT",
        description = "Compute the product name removing extension from filename", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class NoExtensionProductPlugin implements IProductPlugin {

    @Override
    public String getProductName(Path filePath) {
        String productName = filePath.getFileName().toString();
        int indexExtension = productName.lastIndexOf('.');
        if (indexExtension > 0) {
            productName = productName.substring(0, indexExtension);
        }
        return productName;
    }

}
