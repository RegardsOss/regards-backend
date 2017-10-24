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

import java.io.File;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.service.exception.ReadFileException;

@Plugin(description = "CheckInPlugin", id = "CheckInPlugin", version = "1.0.0", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class CheckInPlugin implements ICheckFilePlugin {

    protected static final int PRODUCT_NAME_MAX_SIZE = 128;

    protected String productName;

    protected int productVersion;

    protected int fileVersion;

    protected String logFilePath;

    protected String nodeIdentifier;

    public CheckInPlugin() {
        super();
    }

    @Override
    public int getFileVersion() {
        return fileVersion;
    }

    @Override
    public String getLogFile() {
        return logFilePath;
    }

    @Override
    public String getProductName() {
        return productName;
    }

    @Override
    public int getProductVersion() {
        return productVersion;
    }

    @Override
    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    @Override
    public boolean runPlugin(File filetoCheck, String dataSetId) throws ModuleException {

        boolean result = false;

        // Check file exists
        if (filetoCheck.exists() && filetoCheck.canRead()) {
            String name = filetoCheck.getName();

            // Delete extension if any
            int indexExtension = name.lastIndexOf('.');
            if (indexExtension > 0) {
                name = name.substring(0, indexExtension);
            }

            if (name.length() > PRODUCT_NAME_MAX_SIZE) {
                productName = name.substring(0, PRODUCT_NAME_MAX_SIZE);
            } else {
                productName = name;
            }
            nodeIdentifier = filetoCheck.getName();
            productVersion = 1;
            fileVersion = 1;
            logFilePath = null;
            result = true;
        } else {
            throw new ReadFileException(filetoCheck.getAbsolutePath());
        }

        return result;
    }
}
