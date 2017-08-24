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


public class CheckInPlugin implements ICheckFilePlugin {

    protected static final int PRODUCT_NAME_MAX_SIZE = 128;

    // Attributes
    protected String productName_;

    protected int productVersion_;

    protected int fileVersion_;

    protected String logFilePath_;

    protected String nodeIdentifier_;

    /**
     * Constructeur de la classe
     * 
     * @since 1.2
     */
    public CheckInPlugin() {
        super();
    }

    /**
     * Methode surchargee
     * 
     * @see ssalto.domain.plugins.decl.ICheckFilePlugin#getFileVersion()
     * @since 1.2
     */
    @Override
    public int getFileVersion() {
        return fileVersion_;
    }

    /**
     * Methode surchargee
     * 
     * @see ssalto.domain.plugins.decl.ICheckFilePlugin#getLogFile()
     * @since 1.2
     */
    @Override
    public String getLogFile() {
        return logFilePath_;
    }

    /**
     * Methode surchargee
     * 
     * @see ssalto.domain.plugins.decl.ICheckFilePlugin#getProductName()
     * @since 1.2
     */
    @Override
    public String getProductName() {
        return productName_;
    }

    @Override
    public String getNodeIdentifier() {
        return nodeIdentifier_;
    }

    /**
     * Methode surchargee
     * 
     * @see ssalto.domain.plugins.decl.ICheckFilePlugin#getProductVersion()
     * @since 1.2
     */
    @Override
    public int getProductVersion() {
        return productVersion_;
    }

    /**
     * Methode surchargee
     * 
     * @throws SsaltoDomainException
     * @see ssalto.domain.plugins.decl.ICheckFilePlugin#runPlugin(File, String)
     * @since 1.2
     */
    @Override
    public boolean runPlugin(File pFiletoCheck, String pDataSetId) throws ModuleException {

        boolean result = false;

        // Check file exists
        if (pFiletoCheck.exists() && pFiletoCheck.canRead()) {
            nodeIdentifier_ = pFiletoCheck.getName();
            // Delete extension if any
            String name = pFiletoCheck.getName();
            int indexExtension = name.lastIndexOf('.');
            if (indexExtension > 0) {
                name = name.substring(0, indexExtension);
            }

            // pFiletoCheck
            if (name.length() > PRODUCT_NAME_MAX_SIZE) {
                productName_ = name.substring(0, PRODUCT_NAME_MAX_SIZE);
            }
            else {
                productName_ = name;
            }
            productVersion_ = 1;
            fileVersion_ = 1;
            logFilePath_ = null; // TODO
            result = true;
        }
        else {
            throw new ModuleException("Can't read file " + pFiletoCheck.getAbsolutePath());
        }

        return result;
    }
}
