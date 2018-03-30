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
package fr.cnes.regards.modules.acquisition.service.plugins.cdpp;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.plugins.IProductPlugin;

/**
 * Compute DA_TC_ARCAD3_ISO_DENSITE product names
 *
 * @author Marc Sordi
 *
 */
@Plugin(id = "Arcad3IsoprobeDensiteProductPlugin", version = "1.0.0-SNAPSHOT",
        description = "Compute the product name from data and browse filenames", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class Arcad3IsoprobeDensiteProductPlugin implements IProductPlugin {

    private final String BASE_PRODUCT_NAME = "ISO_DENS_";

    private final String BROWSE_PATTERN = "iso_nete_([0-9]{8}_[0-9]{4})[BC].png";

    private final Pattern browsePattern = Pattern.compile(BROWSE_PATTERN);

    @Override
    public String getProductName(Path filePath) throws ModuleException {
        String productName = filePath.getFileName().toString();

        // Retrieve product name
        Matcher m = browsePattern.matcher(productName);
        if (m.matches()) {
            productName = BASE_PRODUCT_NAME + m.group(1);
        }
        return productName;
    }

}
