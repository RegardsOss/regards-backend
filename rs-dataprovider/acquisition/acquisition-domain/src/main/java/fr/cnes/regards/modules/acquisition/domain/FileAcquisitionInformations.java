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
package fr.cnes.regards.modules.acquisition.domain;

import fr.cnes.regards.modules.acquisition.domain.metadata.SupplyDirectory;

/**
 * 
 * @author Christophe Mertz
 * 
 */

public class FileAcquisitionInformations extends FileProcessInformations {

    /**
     * Repertoire d'acquisition du fichier
     * 
     * @since 1.0
     */
    private String acquisitionDirectory_ = null;

    /**
     * Supply directory utilise pour acquerir le fichier. Cet attribut n'est utilise que pour l'acquisition de fichiers
     * afin de mettre a jour le supply dir utilise pour l'acquisition du fichier. Afin de recuperer le repertoire
     * d'acquisition du fichier, utiliser l'attribut acquisitionDirectory_.
     */
    private SupplyDirectory supplyDirectory_ = null;

    /**
     * Nom du repertoire de travail contenant le fichier associe<br>
     * Ce repertoire de travail est initialise lors de la detection d'un nouveau fichier.
     * 
     * @since 1.3
     */
    private String workingDirectory_ = null;

    /**
     * informations sur le processus qui a acquis le fichier
     */
//    private AcquisitionProcessInformations processInformations_ = null;

    /**
     * constructeur par defaut
     * 
     * @since 1.0
     * 
     */
    public FileAcquisitionInformations() {
        super();
    }

    // GETTERS AND SETTERS

    public String getWorkingDirectory() {
        return workingDirectory_;
    }

    /**
     * Initialise le repertoire de travail du fichier<br>
     * 
     * @return Repertoire de travail contenant le fichier
     * @since 1.3
     */
    public void setWorkingDirectory(String pWorkingDir) {
        workingDirectory_ = pWorkingDir;
    }

    public String getAcquisitionDirectory() {
        return acquisitionDirectory_;
    }

    //    public AcquisitionProcessInformations getProcessInformations() {
    //        return processInformations_;
    //    }

    public void setAcquisitionDirectory(String pAcquisitiondirectory) {
        acquisitionDirectory_ = pAcquisitiondirectory;
    }

//    public void setProcessInformations(AcquisitionProcessInformations pProcessInformations) {
//        processInformations_ = pProcessInformations;
//    }

    /**
     * Cet attribut n'est utilise que pour l'acquisition de fichiers afin de mettre a jour le supply dir utilise pour
     * l'acquisition du fichier. Afin de recuperer le repertoire d'acquisition du fichier, utiliser l'attribut
     * acquisitionDirectory_.
     * 
     * @return
     */
    public SupplyDirectory getSupplyDirectory() {
        return supplyDirectory_;
    }

    public void setSupplyDirectory(SupplyDirectory supplyDirectory) {
        supplyDirectory_ = supplyDirectory;
    }
}
