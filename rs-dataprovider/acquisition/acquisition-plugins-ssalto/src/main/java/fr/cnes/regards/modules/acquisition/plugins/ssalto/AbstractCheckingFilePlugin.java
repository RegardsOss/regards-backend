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
import java.util.ArrayList;
import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.ReadFileException;

/**
 * Plugin de verification pour de fichiers.<br>
 * L'identifiant du produit retourne correspond a l'identifiant du fichier moins l'extention.

 * @author Christophe Mertz
 *
 */

public abstract class AbstractCheckingFilePlugin implements ICheckFilePlugin {

    /** Attribut de la classe */
    private String productName;

    private int productVersion;

    private int fileVersion;

    private String logFilePath;

    private String nodeIdentifier;

    /** Liste des extensions a supprimer renseignee dans le constructeur de la classe */
    protected final List<String> extensionList = new ArrayList<String>();

    public AbstractCheckingFilePlugin() {
        super();
        initExtensionList();
    }

    protected abstract void initExtensionList();

    /**
     * Cette methode supprime du nom du fichier l'extension _HDR ou _BIN si elle est presente dans le fichier. Sinon
     * renvoie le nom du fichier. Methode surchargee
     * 
     * @see ssalto.domain.plugins.decl.ICheckFilePlugin#runPlugin(java.io.File)
     * @since 1.2
     * @DM SIPNG-DM-0060-CN : modification signature
     */
    @Override
    public boolean runPlugin(File pFileToCheck, String pDataSetId) throws ModuleException {
        boolean result = false;

        // Check file exists
        if (pFileToCheck.exists() && pFileToCheck.canRead()) {

            // Delete extension if any
            String name = pFileToCheck.getName();
            nodeIdentifier = name;
            int indexExtension;
            for (String extension : extensionList) {
                indexExtension = name.indexOf(extension);
                if (indexExtension != -1) {
                    // Compute product name
                    name = name.substring(0, indexExtension);
                    // Quit the iteration
                    break;
                }
            }
            productName = name;
            productVersion = 1;
            fileVersion = 1;
            logFilePath = null;
            result = true;
        } else {
            throw new ReadFileException(pFileToCheck.getAbsolutePath());
        }
        return result;
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
}
