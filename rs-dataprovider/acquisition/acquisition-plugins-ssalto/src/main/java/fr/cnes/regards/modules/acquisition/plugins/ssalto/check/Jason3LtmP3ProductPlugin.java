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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.check;

import java.nio.file.Path;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.plugins.IProductPlugin;

/**
 * Jason3LtmP3 product plugin
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 *
 */
@Plugin(description = "Check's plugin for Jason3 LTM products", id = "Jason3LtmP3ProductPlugin", version = "1.0.0",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class Jason3LtmP3ProductPlugin implements IProductPlugin {

    @Override
    public String getProductName(Path filePath) throws ModuleException {
        String productName = filePath.getFileName().toString();
        if (productName.length() > 5) {
            String partOne = productName.substring(0, 4);
            String partTwo = productName.substring(5, productName.length());
            productName = partOne + partTwo;
        } else {
            throw new ModuleException("Cannot compute product name for JSON3_LTM file : " + productName);
        }
        return productName;
    }
}
