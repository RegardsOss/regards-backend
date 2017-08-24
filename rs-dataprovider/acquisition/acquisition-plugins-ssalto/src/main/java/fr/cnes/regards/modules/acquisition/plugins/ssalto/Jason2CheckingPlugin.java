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
 * plugin de verification des fichiers pour les fournitures de jason2 qui ne contiennent qu'un type de fichier. le nom
 * du produit renvoye est le meme que le nom du fichier on ne supprime pas les extensions.
 * 
 * @author Christophe Mertz
 *
 */
public class Jason2CheckingPlugin implements ICheckFilePlugin {

    private static final int PRODUCT_NAME_MAX_SIZE = 128;

    // Attributes
    private String productName_;

    private int productVersion_;

    private int fileVersion_;

    private String logFilePath_;

    /**
     * @DM SIPNG-DM-0060-CN : creation
     * @since 1.3
     */
    private String nodeIdentifier_;

    /**
     * Constructeur de la classe
     * 
     * @since 1.2
     * 
     */
    public Jason2CheckingPlugin() {
        super();
    }

    /**
     * 
     * Methode surchargee
     * 
     * @see ssalto.domain.plugins.decl.ICheckFilePlugin#getFileVersion()
     * @since 1.2
     */
    public int getFileVersion() {
        return fileVersion_;
    }

    /**
     * 
     * Methode surchargee
     * 
     * @see ssalto.domain.plugins.decl.ICheckFilePlugin#getLogFile()
     * @since 1.2
     */
    public String getLogFile() {
        return logFilePath_;
    }

    /**
     * 
     * Methode surchargee
     * 
     * @see ssalto.domain.plugins.decl.ICheckFilePlugin#getProductName()
     * @since 1.2
     */
    public String getProductName() {
        return productName_;
    }

    /**
     * 
     * Methode surchargee
     * 
     * @see ssalto.domain.plugins.decl.ICheckFilePlugin#getProductVersion()
     * @since 1.2
     */
    public int getProductVersion() {
        return productVersion_;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier_;
    }

    /**
     * 
     * Methode surchargee
     * 
     * @throws SsaltoDomainException
     * 
     * @see ssalto.domain.plugins.decl.ICheckFilePlugin#runPlugin(File, String)
     * @since 1.2
     * @DM SIPNG-DM-0060-CN : modification signature
     */
    public boolean runPlugin(File pFiletoCheck, String pDataSetId) throws ModuleException {

        boolean result = false;

        // Check file exists
        if (pFiletoCheck.exists() && pFiletoCheck.canRead()) {

            // Delete extension if any
            String name = pFiletoCheck.getName();
            // pFiletoCheck
            if (name.length() > PRODUCT_NAME_MAX_SIZE) {
                productName_ = name.substring(0, PRODUCT_NAME_MAX_SIZE);
            }
            else {
                productName_ = name;
            }
            nodeIdentifier_ = productName_;
            productVersion_ = 1;
            fileVersion_ = 1;
            logFilePath_ = null; // TODO
            result = true;
        }
        else {
            throw new ReadFileException(pFiletoCheck.getAbsolutePath());
        }

        return result;
    }
}
