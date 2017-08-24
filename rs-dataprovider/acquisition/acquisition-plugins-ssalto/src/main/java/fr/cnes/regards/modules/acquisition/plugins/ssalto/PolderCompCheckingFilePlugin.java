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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import java.io.File;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.ReadFileException;

/**
 * Plugin POLDER pour les produits composes de 2 fichiers
 * 
 * @author Christophe Mertz
 *
 */
public class PolderCompCheckingFilePlugin implements ICheckFilePlugin {

    protected static final int PRODUCT_NAME_MAX_SIZE = 15;

    // Attributes
    private String productName_;

    private int productVersion_;

    private int fileVersion_;

    private String logFilePath_;

    private String nodeIdentifier_;

    @Override
    public boolean runPlugin(File pFileToCheck, String pDataSetId) throws ModuleException {
        boolean result = false;

        // Check file exists
        if (pFileToCheck.exists() && pFileToCheck.canRead()) {

            // Delete extension if any
            String name = pFileToCheck.getName();
            nodeIdentifier_ = name;
            // pFiletoCheck
            if (name.length() > PRODUCT_NAME_MAX_SIZE) {
                productName_ = name.substring(0, PRODUCT_NAME_MAX_SIZE);
            }
            else {
                throw new ModuleException("Invalid POLDER file " + name);
            }

            productVersion_ = 1;
            fileVersion_ = 1;
            logFilePath_ = null;

            result = true;
        }
        else {
            throw new ReadFileException(pFileToCheck.getAbsolutePath());
        }

        return result;
    }

    @Override
    public String getLogFile() {
        return logFilePath_;
    }

    @Override
    public String getProductName() {
        return productName_;
    }

    @Override
    public String getNodeIdentifier() {
        return nodeIdentifier_;
    }

    @Override
    public int getProductVersion() {
        return productVersion_;
    }

    @Override
    public int getFileVersion() {
        return fileVersion_;
    }

}
