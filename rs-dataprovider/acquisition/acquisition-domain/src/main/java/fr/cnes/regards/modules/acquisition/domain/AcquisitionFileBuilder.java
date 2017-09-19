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

import java.time.OffsetDateTime;

import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;

/**
 *
 * {@link AcquisitionFile} builder
 *
 * @author Christophe Mertz
 *
 */
public final class AcquisitionFileBuilder {

    /**
     * Current {@link AcquisitionFile}
     */
    private final AcquisitionFile acqFile;

    private AcquisitionFileBuilder(AcquisitionFile file) {
        this.acqFile = file;
    }

    /**
     * Create a {@link AcquisitionFile}
     * @param name the file name
     * @return
     */
    public static AcquisitionFileBuilder build(String name) {
        final AcquisitionFile af = new AcquisitionFile();
        af.setFileName(name);
        return new AcquisitionFileBuilder(af);
    }

    public AcquisitionFile get() {
        return acqFile;
    }

    public AcquisitionFileBuilder withSize(Long size) {
        acqFile.setSize(size);
        return this;
    }

    public AcquisitionFileBuilder withStatus(String status) {
        acqFile.setStatus(AcquisitionFileStatus.valueOf(status));
        return this;
    }

    public AcquisitionFileBuilder withMetaFile(MetaFile metaFile) {
        acqFile.setMetaFile(metaFile);
        return this;
    }

    public AcquisitionFileBuilder withFileAcquisitionInformations(FileAcquisitionInformations fileAcqInformations) {
        acqFile.setAcquisitionInformations(fileAcqInformations);
        return this;
    }

    public AcquisitionFileBuilder withErrorType(String error) {
        acqFile.setError((ErrorType.valueOf(error)));
        return this;
    }

    public AcquisitionFileBuilder withActivatioDaten(OffsetDateTime date) {
        acqFile.setAcqDate(date);
        return this;
    }

    public AcquisitionFileBuilder withChecksum(String checksum, String algo) {
        acqFile.setChecksum(checksum);
        acqFile.setAlgorithm(algo);
        return this;
    }

}
