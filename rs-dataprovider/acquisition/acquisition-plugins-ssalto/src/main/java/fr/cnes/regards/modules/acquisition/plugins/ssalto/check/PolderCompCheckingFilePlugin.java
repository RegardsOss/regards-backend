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
import fr.cnes.regards.modules.acquisition.exception.ReadFileException;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;

/**
 * Plugin POLDER pour les produits composes de 2 fichiers
 * 
 * @author Christophe Mertz
 *
 */
@Plugin(description = "PolderCompCheckingFilePlugin", id = "PolderCompCheckingFilePlugin", version = "1.0.0",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class PolderCompCheckingFilePlugin implements ICheckFilePlugin {

    protected static final int PRODUCT_NAME_MAX_SIZE = 15;

    private String productName;

    @Override
    public boolean runPlugin(String chainLabel, File fileToCheck, String datasetId) throws ModuleException {
        boolean result = false;

        // Check file exists
        if (fileToCheck.exists() && fileToCheck.canRead()) {

            // Delete extension if any
            String name = fileToCheck.getName();

            if (name.length() > PRODUCT_NAME_MAX_SIZE) {
                productName = name.substring(0, PRODUCT_NAME_MAX_SIZE);
            } else {
                throw new ModuleException("Invalid POLDER file " + name);
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
