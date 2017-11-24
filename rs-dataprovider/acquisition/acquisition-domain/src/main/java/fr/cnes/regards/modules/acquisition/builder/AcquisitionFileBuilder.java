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

import java.time.OffsetDateTime;

import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.ErrorType;
import fr.cnes.regards.modules.acquisition.domain.FileAcquisitionInformations;
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
     * Create a {@link AcquisitionFileBuilder}
     * @param name the file name {@link String} value
     * @return the current {@link AcquisitionFileBuilder}
     */
    public static AcquisitionFileBuilder build(String name) {
        final AcquisitionFile af = new AcquisitionFile();
        af.setFileName(name);
        return new AcquisitionFileBuilder(af);
    }

    /**
     * Get the current {@link AcquisitionFile}
     * @return the current {@link AcquisitionFile}
     */
    public AcquisitionFile get() {
        return acqFile;
    }

    /**
     * Set the size property to the current {@link AcquisitionFile}
     * @param size the size value
     * @return the current {@link AcquisitionFileBuilder}
     */
    public AcquisitionFileBuilder withSize(Long size) {
        acqFile.setSize(size);
        return this;
    }

    /**
     * Set the {@link AcquisitionFileStatus} property to the current {@link AcquisitionFile}
     * @param status the {@link AcquisitionFileStatus} {@link String} value
     * @return the current {@link AcquisitionFileBuilder}
     */
    public AcquisitionFileBuilder withStatus(String status) {
        acqFile.setStatus(AcquisitionFileStatus.valueOf(status));
        return this;
    }

    /**
     * Set the {@link MetaFile} property to the current {@link AcquisitionFile}
     * @param metaFile the {@link MetaFile}
     * @return the current {@link AcquisitionFileBuilder}
     */
    public AcquisitionFileBuilder withMetaFile(MetaFile metaFile) {
        acqFile.setMetaFile(metaFile);
        return this;
    }

    /**
     * Set the {@link FileAcquisitionInformations} property to the current {@link AcquisitionFile}
     * @param fileAcqInformations the {@link FileAcquisitionInformations}
     * @return the current {@link AcquisitionFileBuilder}
     */
    public AcquisitionFileBuilder withFileAcquisitionInformations(FileAcquisitionInformations fileAcqInformations) {
        acqFile.setAcquisitionInformations(fileAcqInformations);
        return this;
    }

    /**
     * Set the {@link ErrorType} property to the current {@link AcquisitionFile}
     * @param error the {@link ErrorType} {@link String} value
     * @return the current {@link AcquisitionFileBuilder}
     */
    public AcquisitionFileBuilder withErrorType(String error) {
        acqFile.setError((ErrorType.valueOf(error)));
        return this;
    }

    /**
     * Set the acquisition date property to the current {@link AcquisitionFile}
     * @param date the date value
     * @return the current {@link AcquisitionFileBuilder}
     */
    public AcquisitionFileBuilder withActivationDate(OffsetDateTime date) {
        acqFile.setAcqDate(date);
        return this;
    }

    /**
     * Set the checksum and checksumAlgorithm properties to the current {@link AcquisitionFile}
     * @param checksum the checksum value
     * @param algo the checksum algorithm used to calculated the checksum
     * @return the current {@link AcquisitionFileBuilder}
     */
    public AcquisitionFileBuilder withChecksum(String checksum, String algo) {
        acqFile.setChecksum(checksum);
        acqFile.setChecksumAlgorithm(algo);
        return this;
    }

}
