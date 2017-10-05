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

package fr.cnes.regards.modules.acquisition.plugins;

import java.io.File;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;

/**
 * 
 *
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@Plugin(id = "BasicCheckFilePlugin", version = "1.0.0-SNAPSHOT", description = "BasicCheckFilePlugin",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class BasicCheckFilePlugin implements ICheckFilePlugin {

    protected String productName;

    protected String nodeIdentifier;

    @Override
    public boolean runPlugin(File fileToCheck, String dataSetId) throws ModuleException {
        productName = fileToCheck.getName();
        nodeIdentifier = dataSetId + " - " + productName;
        boolean result = false;

        // Check file exists
        if (fileToCheck.exists() && fileToCheck.canRead()) {
            nodeIdentifier = fileToCheck.getName();
            String name = fileToCheck.getName();
            int indexExtension = name.lastIndexOf('.');
            if (indexExtension > 0) {
                name = name.substring(0, indexExtension);
            }
            productName = name;
            result = true;
        } else {
            throw new ModuleException("Can't read file " + fileToCheck.getAbsolutePath());
        }

        return result;
    }

    @Override
    public int getFileVersion() {
        return 1;
    }

    @Override
    public String getLogFile() {
        return null;
    }

    @Override
    public String getProductName() {
        return productName;
    }

    @Override
    public int getProductVersion() {
        return 0;
    }

    @Override
    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

}
