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

/**
 *
 * {@link ChainGeneration} builder
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
     * Create a {@link FileAcquisitionInformations}
     * @param directory the directory to scan
     * @return
     */
    public static FileAcquisitionInformationsBuilder build(String directory) {
        final FileAcquisitionInformations fai = new FileAcquisitionInformations();
        fai.setAcquisitionDirectory(directory);
        return new FileAcquisitionInformationsBuilder(fai);
    }

    public FileAcquisitionInformations get() {
        return fileAcqInfos;
    }

    public FileAcquisitionInformationsBuilder withWorkingDirectory(String directory) {
        fileAcqInfos.setWorkingDirectory(directory);
        return this;
    }

}
