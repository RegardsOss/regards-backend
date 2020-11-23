/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Information about scanned directories, linked to {@link AcquisitionFileInfo}
 * @author Iliana Ghazali
 */

@Entity
@Table(name="t_scan_dir_info")
public class ScanDirectoriesInfo {

    @Id
    @SequenceGenerator(name= "ScanDirInfoSequence", initialValue = 1, sequenceName = "seq_scan_dir_info")
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
    private OffsetDateTime lastDatePerDir;

    public ScanDirectoriesInfo() {
    }

    public ScanDirectoriesInfo(Path scannedDirectory, OffsetDateTime lastDatePerDir) {
        this.scannedDirectory = scannedDirectory;
        this.lastDatePerDir = lastDatePerDir;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OffsetDateTime getLastDatePerDir() {
        return lastDatePerDir;
    }

    public void setLastDatePerDir(OffsetDateTime lastDatePerDir) {
        this.lastDatePerDir = lastDatePerDir;
    }

    public Path getScannedDirectory() {
        return scannedDirectory;
    }

    public void setScannedDirectory(Path scannedDirectory) {
        this.scannedDirectory = scannedDirectory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScanDirectoriesInfo that = (ScanDirectoriesInfo) o;
        return Objects.equals(scannedDirectory, that.scannedDirectory) && Objects
                .equals(lastDatePerDir, that.lastDatePerDir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scannedDirectory, lastDatePerDir);
    }
}
