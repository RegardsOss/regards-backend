/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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


package fr.cnes.regards.modules.acquisition.domain.chain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.converters.PathAttributeConverter;
import fr.cnes.regards.framework.module.manager.ConfigIgnore;

import javax.persistence.*;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Information about scanned directories, linked to {@link AcquisitionFileInfo}
 *
 * @author Iliana Ghazali
 */

@Entity
@Table(name = "t_scan_dir_info")
public class ScanDirectoryInfo {

    @ConfigIgnore
    @Id
    @SequenceGenerator(name = "ScanDirInfoSequence", initialValue = 1, sequenceName = "seq_scan_dir_info")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ScanDirInfoSequence")
    private Long id;

    /**
     * Repertory to scan
     */
    @Column(name = "scan_directory", nullable = false)
    @Convert(converter = PathAttributeConverter.class)
    private Path scannedDirectory;

    /**
     * Last modification date of a scanned files
     */
    @Column(name = "last_modification_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastModificationDate;

    public ScanDirectoryInfo() {
    }

    public ScanDirectoryInfo(Path scannedDirectory, OffsetDateTime lastModificationDate) {
        this.scannedDirectory = scannedDirectory;
        this.lastModificationDate = lastModificationDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OffsetDateTime getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(OffsetDateTime lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    public Path getScannedDirectory() {
        return scannedDirectory;
    }

    public void setScannedDirectory(Path scannedDirectory) {
        this.scannedDirectory = scannedDirectory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScanDirectoryInfo that = (ScanDirectoryInfo) o;
        return Objects.equals(scannedDirectory, that.scannedDirectory) && Objects.equals(lastModificationDate,
                                                                                         that.lastModificationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scannedDirectory, lastModificationDate);
    }
}
