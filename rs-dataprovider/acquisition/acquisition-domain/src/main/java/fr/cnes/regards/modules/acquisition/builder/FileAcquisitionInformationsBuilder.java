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
package fr.cnes.regards.modules.acquisition.builder;

import org.omg.CORBA.Current;

import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain2;
import fr.cnes.regards.modules.acquisition.domain.FileAcquisitionInformations;

/**
 *
 * {@link AcquisitionProcessingChain2} builder
 *
 * @author Christophe Mertz
 *
 */
public final class FileAcquisitionInformationsBuilder {

    /**
     * Current {@link FileAcquisitionInformations}
     */
    private final FileAcquisitionInformations fileAcqInfos;

    private FileAcquisitionInformationsBuilder(FileAcquisitionInformations acqInfos) {
        this.fileAcqInfos = acqInfos;
    }

    /**
     * Create a {@link FileAcquisitionInformationsBuilder}
     * @param directory the directory to scan {@link String} value
     * @return the current {@link FileAcquisitionInformationsBuilder}
     */
    public static FileAcquisitionInformationsBuilder build(String directory) {
        final FileAcquisitionInformations fai = new FileAcquisitionInformations();
        fai.setAcquisitionDirectory(directory);
        return new FileAcquisitionInformationsBuilder(fai);
    }

    /**
     * Get the current {@link FileAcquisitionInformations}
     * @return the current {@link FileAcquisitionInformations}
     */
    public FileAcquisitionInformations get() {
        return fileAcqInfos;
    }

    /**
     * Set the working directory property to the {@link Current} {@link FileAcquisitionInformations}
     * @param directory the directory {@link String} value
     * @return the current {@link FileAcquisitionInformationsBuilder}
     */
    public FileAcquisitionInformationsBuilder withWorkingDirectory(String directory) {
        fileAcqInfos.setWorkingDirectory(directory);
        return this;
    }

}
