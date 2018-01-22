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

import java.io.File;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.exception.ReadFileException;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;

/**
 * Check's plugin for Jason3 LTM products.<br>
 * The {@link Product} name is the the file name less the extension file. 
 *
 * @author Christophe Mertz
 *
 */
@Plugin(description = "Check's plugin for Jason3 LTM products", id = "Jason3LtmP3CheckingFilePlugin", version = "1.0.0",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class Jason3LtmP3CheckingFilePlugin implements ICheckFilePlugin {

    /**
     * The product name
     */
    private String productName;

    @Override
    public boolean runPlugin(File fileToCheck, String datasetId) throws ModuleException {
        boolean result = false;

        // Check file exists
        if (fileToCheck.exists() && fileToCheck.canRead()) {

            // Delete extension if any
            String name = fileToCheck.getName();

            if (name.length() > 5) {
                String partOne = name.substring(0, 4);
                String partTwo = name.substring(5, name.length());
                productName = partOne + partTwo;
            } else {
                throw new ModuleException("Invalid JSON3_LTM file " + name);
            }

            result = true;
        } else {
            throw new ReadFileException(fileToCheck.getAbsolutePath());
        }

        return result;
    }

    @Override
    public String getProductName() {
        return productName;
    }
}
