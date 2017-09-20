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
 * plugin de verification des fichiers generic. Verifi uniquement la taille du nom du fichier
 * 
 * @author Christophe Mertz
 *
 */
@Plugin(description = "GenericCheckingPlugin", id = "GenericCheckingPlugin", version = "1.0.0",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class GenericCheckingPlugin implements ICheckFilePlugin {

    private static final int PRODUCT_NAME_MAX_SIZE = 128;

    private String productName;

    private int productVersion;

    private int fileVersion_;

    private String logFilePath;

    private String nodeIdentifier;

    public GenericCheckingPlugin() {
        super();
    }

    @Override
    public int getFileVersion() {
        return fileVersion_;
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

            // Delete extension if any
            String name = filetoCheck.getName();
            // pFiletoCheck
            if (name.length() > PRODUCT_NAME_MAX_SIZE) {
                productName = name.substring(0, PRODUCT_NAME_MAX_SIZE);
            } else {
                productName = name;
            }
            nodeIdentifier = productName;
            productVersion = 1;
            fileVersion_ = 1;
            logFilePath = null; // TODO
            result = true;
        } else {
            throw new ReadFileException(filetoCheck.getAbsolutePath());
        }

        return result;
    }
}
