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

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * 
 * @author Christophe Mertz
 * 
 */
@Embeddable
public class FileAcquisitionInformations extends FileProcessInformations {

    /**
     * Repertoire d'acquisition du fichier
     */
    @Column(name = "acquisition_directory")
    private String acquisitionDirectory = null;

    /**
     * Nom du repertoire de travail contenant le fichier associe<br>
     * Ce repertoire de travail est initialise lors de la detection d'un nouveau fichier.
     */
    @Column(name = "working_directory")
    private String workingDirectory = null;

    /**
     * Default constructor
     * 
     */
    public FileAcquisitionInformations() {
        super();
    }

    public String getAcquisitionDirectory() {
        return acquisitionDirectory;
    }

    public void setAcquisitionDirectory(String acquisitionDirectory) {
        this.acquisitionDirectory = acquisitionDirectory;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

}
